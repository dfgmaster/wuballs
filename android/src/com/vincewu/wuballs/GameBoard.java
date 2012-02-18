package com.vincewu.wuballs;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

class GameBoard {

    /* TODO: make these game options configurable and private */
    public final int size = 9; // the x in an x*x board
    public final int numGamePieces = 7; // number of game piece types, not including joker piece
    public final boolean playJoker = true; // whether to play joker piece
    public final float jokerFrequency = 0.2f; // how often the joker ball plays

    /* Internal game state */
    private Wuball[][] board; // internal representation of the game board
    private int numFreeSlots; // internal accounting tool
    private LinkedList<Wuball> peekList; // holding area for pre-computed Wuballs
    private Random randomGenerator; 

    // Singleton pattern: only 1 game at a time
    private final static GameBoard instance = new GameBoard();
    public static GameBoard getInstance() {return instance;}
    private GameBoard() {restart();}

    /**
     * Clear all state 
     */
    public void restart() {
        board = new Wuball[9][9];
        this.numFreeSlots = size * size;
        peekList = new LinkedList<Wuball>();
        randomGenerator = new Random(123); // TODO: remove fixed seed
    }

    /**
     * return Wuball located at i, j
     */
    public Wuball getWuball(BoardCell cell) {
        return board[cell.i][cell.j];
    }

    /**
     * Retrieves the next x Wuballs
     */
    public List<Wuball> get(int num) {
        List<Wuball> result = this.peek(num);
        for (int y = 0; y < num; y++)
            peekList.remove(y);
        return result;
    }

    /**
     * Computes the next x Wuballs (and caches them for later)
     */
    public List<Wuball> peek(int num) {
        List<Wuball> result = new LinkedList<Wuball>();
        if (num > 0) {
            for (int y = 0; y < num; y++) {
                if (peekList.size() >= y + 1) {
                    result.add(peekList.get(y));
                } else {
                    Wuball aBall = new Wuball();
                    if (playJoker
                            && randomGenerator.nextFloat() < jokerFrequency) {
                        aBall.setJokerType();
                    } else {
                        aBall.setType(randomGenerator.nextInt(numGamePieces));
                    }
                    result.add(aBall);
                    peekList.add(aBall);
                }
            }
        }
        return result;
    }

    /**
     * Move a piece from start to end, if valid.
     * Will update game board. 
     * @param start
     * @param end
     * @return true if this was a valid move
     */
    public boolean moveBall(GameBoard.BoardCell start, GameBoard.BoardCell end)
            throws IllegalMoveException {
        // check if this is a valid move
        short[][] distanceMap = findRoute(start, end);
        if (distanceMap[end.i][end.j] > 0) { // java initializes array to 0
            // a valid move! move the game piece
            board[end.i][end.j] = board[start.i][start.j];
            board[start.i][start.j] = null;
            return true;
        }
        return false;
    }

    /**
     * @return [][] a computed distance map with respect to <start> (0). The
     *         computed distance at <end> is > 0 if a route exists, and
     *         represents the shortest number of hops.
     * 
     *         start - BoardCell containing game piece that user wants to move
     *         end - BoardCell representing where user wants to move the game
     *         piece
     */
    private short[][] findRoute(BoardCell start, BoardCell end)
            throws IllegalMoveException {
        if (start == null || end == null)
            throw new IllegalMoveException("Internal: no move commands");
        if (!start.withinBounds() || !end.withinBounds()) {
            throw new IllegalMoveException("Internal: move commands are out of bounds: "
                    + start.toString() + " " + end.toString());
        }
        if (board[start.i][start.j] == null) {
            throw new IllegalMoveException("Internal: no ball at start coordinates: "
                    + start.toString());
        }
        if (board[end.i][end.j] != null) {
            throw new IllegalMoveException("Internal: a ball exists at end coordinates: "
                    + end.toString());
        }

        // set up recursion
        short[][] distanceMap = new short[size][size]; // initialized to 0
        distanceMap[start.i][start.j] = 1;
        findRouteRecurse(start, end, distanceMap, new LinkedList<BoardCell>());
        return distanceMap;
    }

    /** Private method for recursively computing a distance map */
    private void findRouteRecurse(BoardCell next, BoardCell end,
            short[][] distanceMap, LinkedList<BoardCell> todoList) {
        if (next.equals(end))
            return;

        // try each direction one by one: top, right, bottom, left
        findRouteRecurseSub(new BoardCell(next.i - 1, next.j),
                distanceMap[next.i][next.j], distanceMap, todoList);
        findRouteRecurseSub(new BoardCell(next.i, next.j + 1),
                distanceMap[next.i][next.j], distanceMap, todoList);
        findRouteRecurseSub(new BoardCell(next.i + 1, next.j),
                distanceMap[next.i][next.j], distanceMap, todoList);
        findRouteRecurseSub(new BoardCell(next.i, next.j - 1),
                distanceMap[next.i][next.j], distanceMap, todoList);

        if (todoList.size() > 0) {
            findRouteRecurse(todoList.pollFirst(), end, distanceMap, todoList);
        }
    }

    /** Private method for recursively computing a distance map */
    private void findRouteRecurseSub(BoardCell cell, short lastDistance,
            short[][] distanceMap, LinkedList<BoardCell> todoList) {
        // skip if we've moved off the grid
        if (cell.withinBounds()) {
            // skip if there's a game piece already
            if (board[cell.i][cell.j] == null) {
                // skip if we've been to this cell already (or else we'd go in a
                // loop)
                if (distanceMap[cell.i][cell.j] == 0) { // java initializes
                                                        // arrays to 0
                    todoList.push(cell);
                    distanceMap[cell.i][cell.j] = ++lastDistance;
                }
            }
        }
    }

    /**
     * Compute all connect-5 opportunities using "cell" as point of reference
     * @param cell the point of reference for connect 5 calculations
     * @return [][] a list of all connect-5 opportunities. Each connect-5
     *         opportunity is in turn a list (of BoardCells). The length of each
     *         connect-5 is actually >=4, as it doesn't include the point of
     *         reference.
     */
    public List<? extends List<BoardCell>> connectFive(BoardCell cell) {
        LinkedList<LinkedList<BoardCell>> results = new LinkedList<LinkedList<BoardCell>>();
        Wuball ball = board[cell.i][cell.j];

        /*
         * These are considered valid connect-5 configurations: - 5 or more in a
         * row - 5 or more in a column - 5 or more diagonally (x2 directions) It
         * is possible to have >1 connect-5 with one move
         */

        int i, j; // cell coordinates
        int x, y; // for loop indexes
        int[] fi, fj, si, sj; // store cell coordinates for subroutine

        // check vertical
        fi = new int[size - (cell.i + 1)];
        fj = new int[fi.length];
        for (x = 0, i = cell.i + 1; i < size; x++, i++) {
            fi[x] = i;
            fj[x] = cell.j;
        }
        si = new int[cell.i];
        sj = new int[si.length];
        for (x = 0, i = cell.i - 1; i >= 0; x++, i--) {
            si[x] = i;
            sj[x] = cell.j;
        }
        connectFiveHelper(ball, fi, fj, si, sj, results);

        // check horizontal
        fj = new int[size - (cell.j + 1)];
        fi = new int[fj.length];
        for (x = 0, j = cell.j + 1; j < size; x++, j++) {
            fi[x] = cell.i;
            fj[x] = j;
        }
        sj = new int[cell.j];
        si = new int[sj.length];
        for (x = 0, j = cell.j - 1; j >= 0; x++, j--) {
            si[x] = cell.i;
            sj[x] = j;
        }
        connectFiveHelper(ball, fi, fj, si, sj, results);

        // check diagonal negative slope
        fi = new int[Math.min(size - (cell.i + 1), size - (cell.j + 1))];
        fj = new int[fi.length];
        for (x = 0, i = cell.i + 1, j = cell.j + 1; i < size && j < size; x++, i++, j++) {
            fi[x] = i;
            fj[x] = j;
        }
        si = new int[Math.min(cell.i, cell.j)];
        sj = new int[si.length];
        for (x = 0, i = cell.i - 1, j = cell.j - 1; i >= 0 && j >= 0; x++, i--, j--) {
            si[x] = i;
            sj[x] = j;
        }
        connectFiveHelper(ball, fi, fj, si, sj, results);

        // check diagonal positive slope
        fi = new int[Math.min(size - (cell.i + 1), cell.j)];
        fj = new int[fi.length];
        for (x = 0, i = cell.i + 1, j = cell.j - 1; i < size && j >= 0; x++, i++, j--) {
            fi[x] = i;
            fj[x] = j;
        }
        si = new int[Math.min(cell.i, size - (cell.j + 1))];
        sj = new int[si.length];
        for (x = 0, i = cell.i - 1, j = cell.j + 1; i >= 0 && j < size; x++, i--, j++) {
            si[x] = i;
            sj[x] = j;
        }
        connectFiveHelper(ball, fi, fj, si, sj, results);

        // if there are any connect-5 results,
        // remove the game pieces from the board
        if (results.size() > 0) {
            board[cell.i][cell.j] = null;
            numFreeSlots++;

            for (x = 0; x < results.size(); x++) {
                LinkedList<BoardCell> list = results.get(x);
                for (y = 0; y < list.size(); y++) {
                    board[list.get(y).i][list.get(y).j] = null;
                    numFreeSlots++;
                }
            }
        }

        return results;
    }

    /**
     * Helper method that calculates the connect-5 opportunities. It sweeps on
     * both sides according to the {i, j} series provided in the method params
     * 
     * @param ball
     *            that the user moved
     * @param fi
     *            first series, row i
     * @param fj
     *            first series, col j
     * @param si
     *            second series, row i
     * @param sj
     *            second series, row j
     * @param results
     *            [][] a list of all connect-5 opportunities. Each connect-5
     *            opportunity is in turn a list (of BoardCells). The length of
     *            each connect-5 is actually >=4, as it doesn't include the
     *            point of reference.
     */
    private void connectFiveHelper(Wuball ball, int[] fi, int[] fj, int[] si,
            int[] sj, LinkedList<LinkedList<BoardCell>> results) {

        LinkedList<BoardCell> vList = new LinkedList<BoardCell>();
        Wuball firstNonJokerBall = null;

        // Note: Joker ball matches any ball, but all the other non-Joker balls
        // must match
        // one another to be a valid connect-5.
        //
        // For each direction (vertical, horizontal, diagonal x2) we try to
        // consume balls
        // on both sides.
        //
        // If we moved a non-Joker ball, the logic is simple: consume balls
        // until the next
        // ball doesn't match.
        //
        // If we moved a Joker ball, we don't know until later what the actual
        // matching piece is.
        // We start on one side, and the first non-Joker piece we see is what
        // all others have to match.
        // -- It's possible to have no specific matching piece (i.e. 5 or more
        // jokers in a row)
        // -- It's possible to have 2 matches (e.g. XXJJJYY, joker pieces
        // flanked by different
        // consecutive pieces on either side, each utilizing the jokers to
        // connect-5)

        int x;
        for (x = 0; x < fi.length; x++) {
            if (ball.equals(board[fi[x]][fj[x]])) {
                // If user moved a regular ball, add it, we're done.
                // Otherwise, the joker case is easy: joker matches joker.
                if (!ball.isJokerBall() || board[fi[x]][fj[x]].isJokerBall())
                    vList.add(new BoardCell(fi[x], fj[x]));
                else {
                    // User moved a joker ball, and this is a normal ball
                    // Non-Joker balls must match, so keep track of the first
                    // one we see
                    if (firstNonJokerBall == null
                            || firstNonJokerBall.equals(board[fi[x]][fj[x]])) {
                        if (firstNonJokerBall == null)
                            firstNonJokerBall = board[fi[x]][fj[x]];
                        vList.add(new BoardCell(fi[x], fj[x]));
                    } else
                        break;
                }
            } else
                break;
        }
        boolean possibleSecondMatch = true; // Joker case: if user moves a
                                            // Joker, possible to have two
                                            // connect-5s
        for (x = 0; x < si.length; x++) {
            if (ball.equals(board[si[x]][sj[x]])) {
                // If user moved a regular ball, add it, we're done.
                // Otherwise, the joker case is easy: joker matches joker.
                if (!ball.isJokerBall() || board[si[x]][sj[x]].isJokerBall())
                    vList.add(0, new BoardCell(si[x], sj[x]));
                else {
                    // User moved a joker ball, and this is a normal ball
                    // Non-Joker balls must match, so keep track of the first
                    // one we see
                    if (firstNonJokerBall == null
                            || firstNonJokerBall.equals(board[si[x]][sj[x]])) {
                        if (firstNonJokerBall == null)
                            firstNonJokerBall = board[si[x]][sj[x]];
                        vList.add(0, new BoardCell(si[x], sj[x]));
                        possibleSecondMatch = false; // same ball, so no longer
                                                     // possible to have another
                                                     // connect-5
                    } else if (possibleSecondMatch) {
                        // Situation here is that we have jokers in the middle
                        // flanked by different
                        // pieces on either side. Possibly have more than 1
                        // connect-5 (e.g. XXJJJYY)

                        // Store the first connect-5, if it exists
                        LinkedList<BoardCell> tmpList = new LinkedList<BoardCell>(vList);
                        if (tmpList.size() >= 4) results.push(tmpList);

                        // Try for a second connect-5.
                        // Keep the consecutive Jokers we've seen already.
                        vList.clear();
                        firstNonJokerBall = board[si[x]][sj[x]];
                        vList.add(0, new BoardCell(si[x], sj[x]));
                        possibleSecondMatch = false;
                        for (int y = 0; y < tmpList.size(); y++) {
                            BoardCell nextCell = tmpList.get(y);
                            if (board[nextCell.i][nextCell.j].isJokerBall())
                                vList.add(nextCell);
                            else
                                break;
                        }
                    } else
                        break;
                }
            } else
                break; // this ball doesn't match the ball the user moved
        }
        if (vList.size() >= 4)
            results.push(vList);
    }

    /**
     * place new Wuball on the game board and return its location
     */
    public GameBoard.BoardCell placeBall() throws GameOverException {
        if (this.numFreeSlots <= 0)
            throw new GameOverException();

        Wuball aBall = get(1).get(0);
        int nextSlot = randomGenerator.nextInt(this.numFreeSlots);
        BoardCell result = null;

        int count = 0;
        OUTER: for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (board[i][j] == null) {
                    if (++count > nextSlot) {
                        board[i][j] = aBall;
                        this.numFreeSlots--;
                        result = new BoardCell(i, j);
                        break OUTER;
                    }
                }
            }
        }

        return result;
    }

    class BoardCell {
        int i = -1; // row
        int j = -1; // column

        private BoardCell() {
        }; // don't let anyone create an empty BoardCell

        public BoardCell(int i, int j) {
            this();
            this.i = i;
            this.j = j;
            // TODO: check bounds? if (!withinBounds()) throw new NotWithinBoundsException();
        }

        public boolean equals(Object o) {
            if (this == o)
                return true;
            if ((o == null) || (o.getClass() != this.getClass()))
                return false;
            BoardCell cell = (BoardCell) o;
            return (cell.i == this.i && cell.j == this.j);
        }

        public int hashCode() {
            int hash = 7;
            hash = 31 * hash + this.i;
            hash = 31 * hash + this.j;
            return hash;
        }

        /** for debugging */
        public String toString() {
            return "{" + this.i + "," + this.j + "}";

        }

        /** check whether cell is within Bounds */
        public boolean withinBounds() {
            return (this.i >= 0 && this.j >= 0 && this.i < size && this.j < size);
        }
    }

}
