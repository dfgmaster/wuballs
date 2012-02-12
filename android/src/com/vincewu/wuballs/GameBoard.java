package com.vincewu.wuballs;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

class GameBoard {

	/* TODO: make these game options configurable */	
	public final int size = 9; // the x in an x*x board
	public final int numGamePieces = 7; // number of game piece types, not including joker piece 
	public final boolean playJoker = true; // whether to play joker piece
	
	/* Represents game state */
	Wuball[][] board = new Wuball[9][9]; // internal representation of the game board
    int numFreeSlots; //internal accounting tool 


    /* A stream of lottery balls */
    private LinkedList<Wuball> peekList = new LinkedList<Wuball>(); // holding area for pre-computed Wuballs
    private Random randomGenerator = new Random(123); // TODO: remove fixed seed
    
    // Singleton pattern: only 1 game at a time
    private final static GameBoard instance = new GameBoard();
    public static GameBoard getInstance() { return instance; }    
    private GameBoard() {
    	this.numFreeSlots = size*size;
    }
    

	
    /** 
     * return Wuball located at i, j 
     */
    public Wuball getWuball(BoardCell cell) {
    	return board[cell.i][cell.j];
    }    
    
    
    /** 
     *  Retrieves the next x Wuballs 
     */
    public List<Wuball> get(int num) {
    	List<Wuball> result = this.peek(num);
    	for (int y=0; y<num; y++) peekList.remove(y);
    	return result;
    }
    

    /** 
     * Computes the next x Wuballs (and keeps them around) 
     */
    public List<Wuball> peek(int num) {
        List<Wuball> result = new LinkedList<Wuball>();
        if (num > 0) {
        	for (int y=0; y<num; y++) {
                if (peekList.size() >= y+1) {
                    result.add(peekList.get(y));
                }
                else {
                	Wuball aBall = new Wuball();
                	aBall.setType(randomGenerator.nextInt(numGamePieces));
                	result.add(aBall);
                	peekList.add(aBall);
                }
        	}
        }
        return result;
    }    
    
    /**
     * 
     * @param start
     * @param end
     * @return
     */
    public boolean moveBall(GameBoard.BoardCell start, GameBoard.BoardCell end) throws IllegalMoveException {
    	// check if this is a valid move
	    short[][] distanceMap = findRoute(start, end);
	    if (distanceMap[end.i][end.j] > 0) {  // java initializes array to 0
            // a valid move! move the game piece
  		    board[end.i][end.j] = board[start.i][start.j];
  		    board[start.i][start.j] = null;
  		    return true;
	    }
    	return false;    	
    }
    
    /** 
     * @return [][] a computed distance map with respect to <start> (0). 
     * The computed distance at <end> is > 0 if a route exists, and represents 
     * the shortest number of hops.
     *
     * start - BoardCell containing game piece that user wants to move 
     * end - BoardCell representing where user wants to move the game piece
     */
    private short[][] findRoute(BoardCell start, BoardCell end) throws IllegalMoveException {
        if (start == null || end == null) throw new IllegalMoveException("No move commands");
        if (!start.withinBounds() || !end.withinBounds()) {
            throw new IllegalMoveException("Move commands are out of bounds: " + start.toString() + " " + end.toString());
        }   
        if (board[start.i][start.j] == null) {
            throw new IllegalMoveException("No ball at start coordinates: " + start.toString());
        }
        if (board[end.i][end.j] != null) {
            throw new IllegalMoveException("A ball exists at end coordinates: " + end.toString());  
        }
         
        // set up recursion
        short[][] distanceMap = new short[size][size]; // initialized to 0
        distanceMap[start.i][start.j] = 1;
        findRouteRecurse(start, end, distanceMap, new LinkedList<BoardCell>()); 
        return distanceMap;
    }

    /** Private method for recursively computing a distance map */
    private void findRouteRecurse(BoardCell next, BoardCell end, short[][] distanceMap, LinkedList<BoardCell> todoList) {
        if (next.equals(end)) return;

        // try each direction one by one: top, right, bottom, left 
        findRouteRecurseSub(new BoardCell(next.i-1, next.j), distanceMap[next.i][next.j], distanceMap, todoList);
        findRouteRecurseSub(new BoardCell(next.i, next.j+1), distanceMap[next.i][next.j], distanceMap, todoList);
        findRouteRecurseSub(new BoardCell(next.i+1, next.j), distanceMap[next.i][next.j], distanceMap, todoList);
        findRouteRecurseSub(new BoardCell(next.i, next.j-1), distanceMap[next.i][next.j], distanceMap, todoList);

        if (todoList.size() > 0) {
            findRouteRecurse(todoList.pollFirst(), end, distanceMap, todoList);
        }
    }
    
    /** Private method for recursively computing a distance map */
    private void findRouteRecurseSub(BoardCell cell, short lastDistance, short[][] distanceMap, LinkedList<BoardCell> todoList) {
        // skip if we've moved off the grid
        if (cell.withinBounds()) {
        	// skip if there's a game piece already
            if (board[cell.i][cell.j] == null) {
                // skip if we've been to this cell already (or else we'd go in a loop)
                if (distanceMap[cell.i][cell.j] == 0) {	 // java initializes arrays to 0
                    todoList.push(cell);
                    distanceMap[cell.i][cell.j] = ++lastDistance;
                }
            }      
        }
    }
    
    
    
    
    /** @return [][] a list of all connect-5 opportunities. Each connect-5 opportunity is 
     * in turn a list (of BoardCells). The length of each connect-5 is actually >=4, as it 
     * doesn't include the point of reference.
     *
     * cell - BoardCell the point of reference for connect 5 calculations
     */
    public List<List<BoardCell>> connectFive(BoardCell cell) {
    	LinkedList<LinkedList<BoardCell>> results = new LinkedList<LinkedList<BoardCell>>();
    	
        Wuball ball = board[cell.i][cell.j];
        int i,j; // iterators

    	// check vertical
    	LinkedList<BoardCell> vList = new LinkedList<BoardCell>();
    	for (i=cell.i+1; i<size; i++) {
    	    if (ball.equals(board[i][cell.j])) vList.push(new BoardCell(i,cell.j));
    	    else break;
    	}
    	for (i=cell.i-1; i>=0; i--) {
    	    if (ball.equals(board[i][cell.j])) vList.push(new BoardCell(i,cell.j));
    	    else break;
    	}
    	if (vList.size() >= 4) results.push(vList);

    	// check horizontal
    	LinkedList<BoardCell> hList = new LinkedList<BoardCell>();
    	for (j=cell.j+1; j<size; j++) {
    	    if (ball.equals(board[cell.i][j])) hList.push(new BoardCell(cell.i,j));
    	    else break;
    	}
    	for (j=cell.j-1; j>=0; j--) {
    	    if (ball.equals(board[cell.i][j])) hList.push(new BoardCell(cell.i,j));
    	    else break;
    	}
    	if (hList.size() >= 4) results.push(hList);

    	// check diagonal negative slope
    	LinkedList<BoardCell> dnList = new LinkedList<BoardCell>();
    	for (i=cell.i+1,j=cell.j+1; i<size && j<size; i++,j++) {
    	    if (ball.equals(board[i][j])) dnList.push(new BoardCell(i,j));
    	    else break;
    	}
    	for (i=cell.i-1,j=cell.j-1; i>=0 && j>=0; i--,j--) {
    	    if (ball.equals(board[i][j])) dnList.push(new BoardCell(i,j));
    	    else break;
    	}
    	if (dnList.size() >= 4) results.push(dnList);
    	    
    	// check diagonal positive slope
    	LinkedList<BoardCell> dpList = new LinkedList<BoardCell>();
    	for (i=cell.i+1,j=cell.j-1; i<size && j>=0; i++,j--) {
    	    if (ball.equals(board[i][j])) dpList.push(new BoardCell(i,j));
    	    else break;
    	}
    	for (i=cell.i-1,j=cell.j+1; i>=0 && j<size; i--,j++) {
    	    if (ball.equals(board[i][j])) dpList.push(new BoardCell(i,j));
    	    else break;
    	}
    	if (dpList.size() >= 4) results.push(dpList);

    	
	    if (results.size() > 0) {         
		    // remove the game pieces, and skip placing new ones
		    board[cell.i][cell.j] = null;
		    numFreeSlots++;
		    
	        for (int x = 0; x < results.size(); x++) {
	            LinkedList<BoardCell> list = results.get(x);
	            for (int y = 0; y < list.size(); y++) {
		            board[list.get(y).i][list.get(y).j] = null;
		            numFreeSlots++;
	            }
	        }
	    }
	    
	    return (List)results;
    }
    
	/** 
	 * place new Wuball on the game board and return its location
	 */
	public GameBoard.BoardCell placeBall() {
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
	          result = new BoardCell(i,j);
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

		private BoardCell() {}; // don't let anyone create an empty BoardCell
		public BoardCell(int i, int j) {
			this.i = i;
			this.j = j;
		}
		
		public boolean equals(Object o) {
			if (this == o) return true;
			if((o == null) || (o.getClass() != this.getClass())) return false; 
			BoardCell cell = (BoardCell)o;  
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
			return "{" + this.i + "," + this.j  +  "}";
			
		}
		
		/** check whether cell is within Bounds */
	    public boolean withinBounds() {
	        return (this.i>=0 && this.j>=0 && this.i<size && this.j<size);
		}
	}
	
}
