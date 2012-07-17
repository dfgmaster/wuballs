package com.vincewu.wuballs;

import java.util.LinkedList;
import java.util.List;

import android.R.color;
import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorSet;
import android.animation.Keyframe;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.Display;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnticipateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

public class WuballsActivity extends Activity implements OnClickListener {

    /* Game state */
    private GameBoard gameBoard = null; // user's game; only 1 game at a time
    private int score; // user's score
    // tracks which BoardCells have changed and need repainting
    // private LinkedList<GameBoard.BoardCell> dirtyBoardCells = null; 
    private GameBoard.BoardCell selectedBoardCell = null; // tracks user selection.
    private Wuball movedWuball = null;
    private GameBoard.BoardCell prevSelectedBoardCell = null; // tracks user selection.
    private GameBoard.BoardCell movedFromBoardCell = null; // tracks user selection.
    private GameBoard.BoardCell movedToBoardCell = null; // tracks user selection.
    private LinkedList<GameBoard.BoardCell> newlyPlacedBoardCells = null;
    private LinkedList<GameBoard.BoardCell> removedBoardCells = null;
    private enum GameState {
        INIT, OVER, MOVE_AND_CONNECT, MOVE_AND_ADD, CHANGE_SELECTION
    }
    
    
    private MediaPlayer clickSound;
    private MediaPlayer moveSound;
    private MediaPlayer appearSound;
    private MediaPlayer removeSound;

    
    
    

    // Cache Game UI elements for easy access
    TableLayout boardEl = null;
    TextView scoreEl = null;
    ImageView[] nextWuballsEl = null;
    int[] gamePieces = { R.drawable.wucat_black_active_48, R.drawable.wucat_cobalt_active_48, 
    		             R.drawable.wucat_lime_active_48, R.drawable.wucat_orange_active_48, 
    		             R.drawable.wucat_purple_active_48, R.drawable.wucat_rasberry_active_48,
                         R.drawable.wucat_white_active_48 };
    int jokerPiece = R.drawable.wucat_joker_active_48;

    /**
     * handles click on a board cell
     * 
     * @param v this is a FrameLayout
     */
    public void onClick(View v) {        
        try {
            if (v == null) throw new IllegalMoveException(this.getString(R.string.ex_no_element));
            GameBoard.BoardCell cell = (GameBoard.BoardCell) v.getTag();
            Wuball wuball = gameBoard.getWuball(cell);
            GameState whatChanged = play(cell);
            repaintBoard(whatChanged);
            if (whatChanged == GameState.MOVE_AND_ADD) repaintNextWuballs();
            if (whatChanged == GameState.MOVE_AND_CONNECT) repaintScore();
        } 
        catch (IllegalMoveException imex) {
            logError(imex.getMessage());
        } 
        catch (GameOverException goe) {
            try {
                logError(this.getString(R.string.game_over));
                // TODO: show some congratulatory UI, record score, etc
                
                // clear everything
                rebootGameState();
                for (int x = 0; x < gameBoard.size; x++) {
                    for (int y = 0; y < gameBoard.size; y++) {
                        this.removedBoardCells.push(gameBoard.new BoardCell(x, y));
                    }
                }
                repaintBoard(GameState.OVER);
                
                // start a new game
                startGame();
                repaintBoard(GameState.INIT);
                repaintNextWuballs();
                repaintScore();
            } 
            catch (Exception ex) {
                // something is seriously f'ed up if we are here
            }
        }

    }

    /**
     * User clicked somewhere on the board. Figure out what user intended to do. 
     * 
     * @throws IllegalMoveException if the move doesn't work (e.g. there is no path)
     * @throws GameOverException if the game runs out of free board cells
     * 
     * @param cell
     *            BoardCell that user selected
     * 
     * @return what happened: connect-5, placed new balls, changed selection
     */
    private GameState play(GameBoard.BoardCell cell) throws IllegalMoveException, GameOverException {
        Wuball wuball = gameBoard.getWuball(cell);

        if (this.selectedBoardCell == null) {
            // haven't selected a game piece yet
            if (wuball == null) throw new IllegalMoveException(this.getString(R.string.ex_no_ball) + " " + cell.toString());
            this.selectedBoardCell = cell;
            return GameState.CHANGE_SELECTION;
        } 
        else {
            // user has already selected a game piece, and so we expect they'll
            // click an empty cell
            // if they clicked on another game piece, let's assume they want to
            // change their selection
            if (wuball != null) {
                if (this.selectedBoardCell.equals(cell)) {
                    // assume user wants to cancel the selection
                    this.prevSelectedBoardCell = this.selectedBoardCell;
                    this.selectedBoardCell = null;
                } 
                else {
                    // assume user wants to switch selection
                    GameBoard.BoardCell oldSelection = gameBoard.new BoardCell(
                            this.selectedBoardCell.i, this.selectedBoardCell.j);
                    this.prevSelectedBoardCell = oldSelection;
                    this.selectedBoardCell = cell;
                }
                return GameState.CHANGE_SELECTION;
            } 
            else {
                // let's try to make a move!
                return this.move(this.selectedBoardCell, cell);
            }
        }
    }

    /**
     * User wants to move a game piece. Helper method to try to do that.
     * 
     * @throws IllegalMoveException if the move doesn't work (e.g. there is no path)
     * @throws GameOverException if the game runs out of free board cells
     * 
     * @param start
     * @param end
     * 
     * @return what happened after the move: connect-5 or placed new balls on the board
     */
    private GameState move(GameBoard.BoardCell start, GameBoard.BoardCell end) throws IllegalMoveException, GameOverException {
        boolean placedNewBalls = false; // if the move didn't score, placeNewBalls will be true
        List<GameBoard.BoardCell> path = gameBoard.moveBall(start, end);

        if (path != null) {
            this.movedFromBoardCell = start;
            this.movedToBoardCell = end;
            this.movedWuball = gameBoard.getWuball(end);
            this.selectedBoardCell = null;
            
            /*

            GameBoard.BoardCell cell;
            FrameLayout cellEl;
            ImageView pieceHolder;
            Wuball wuball;
            LinkedList<Animator> animations = new LinkedList<Animator>();
            while (path.size() > 0) {
        		cell = path.remove(0);
                cellEl= (FrameLayout) ((TableRow) boardEl.getChildAt(cell.i)).getChildAt(cell.j);
                pieceHolder = (ImageView) cellEl.getChildAt(0);
                
                pieceHolder.setAlpha(0f);
                */
//                PropertyValuesHolder pvhSX = PropertyValuesHolder.ofFloat("scaleX", 1.0f, 1.2f);
  //              PropertyValuesHolder pvhSY = PropertyValuesHolder.ofFloat("scaleY", 1.0f, 0.0f);
//                ObjectAnimator disappearAnim = ObjectAnimator.ofPropertyValuesHolder(pieceHolder, pvhSX, pvhSY);

            /*
                ObjectAnimator disappearAnim = ObjectAnimator.ofFloat(pieceHolder, "alpha", 1f, 0f);
                disappearAnim.setDuration(50);
                animations.add(disappearAnim);
            }
            if (animations.size() > 0) {
                AnimatorSet moveAnim = new AnimatorSet();
                moveAnim.playSequentially(animations);
                moveAnim.start();
            }
            */

            

            // check if we have any 5+ game pieces in a rows
            List<? extends List<GameBoard.BoardCell>> results = gameBoard.connectFive(end);
            if (results != null && results.size() > 0) {
                // remove the game pieces, and skip placing new ones
                this.score += 2;
                for (int x = 0; x < results.size(); x++) {
                    List<GameBoard.BoardCell> list = results.get(x);
                    this.removedBoardCells.addAll(list);
                    this.score += (2 * list.size());
                }
                this.removedBoardCells.add(end);
            } 
            else {
                // put three balls on the board
            	placedNewBalls = true;
                for (int x = 0; x < 3; x++) {
                    GameBoard.BoardCell newPopulatedCell = gameBoard.placeBall();
                    this.newlyPlacedBoardCells.push(newPopulatedCell);

                    /* TODO: do this later, UI needs to show the ball appearing
                    // check if we have any 5+ game pieces in a row
                    results = gameBoard.connectFive(newPopulatedCell);
                    if (results != null && results.size() > 0) {
                        // remove the game pieces
                        this.score += 2;
                        for (int y = 0; y < results.size(); y++) {
                            List<GameBoard.BoardCell> list = results.get(y);
                            this.removedBoardCells.addAll(list);
                            this.score += (2 * list.size());
                        }
                    }
                    */
                }                
            }
        } 
        else {
            throw new IllegalMoveException(this.getString(R.string.ex_bad_move));
        }

        return (placedNewBalls ? GameState.MOVE_AND_ADD : GameState.MOVE_AND_CONNECT);
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // clean and setup data structures
        rebootGameState();
        startGame();
        
        clickSound = MediaPlayer.create(this, R.raw.click);
        moveSound = MediaPlayer.create(this, R.raw.move);
        appearSound = MediaPlayer.create(this, R.raw.appear);
        removeSound = MediaPlayer.create(this, R.raw.pop);

        // repaint everything
        setContentView(R.layout.main);
        initializeGameUI();
        repaintBoard(GameState.INIT);
        repaintNextWuballs();
        repaintScore();
    }
    
    /** Called when the activity is destroyed. */
    @Override
    public void onDestroy() {
        clickSound.release();
        moveSound.release();
        appearSound.release();
        removeSound.release();
        
        clickSound = null;
        moveSound = null;
        appearSound = null;
        removeSound = null;
    }

    /** Write error messages for debugging TODO: this is being overloaded for UI messages */
    public void logError(String msg) {
        Toast.makeText(this, msg + "tester", Toast.LENGTH_SHORT).show();
    }

    /**
     * Repaints to the game board gui
     */
    private void repaintBoard(GameState whatHappened) {
        LinkedList<Animator> animations = new LinkedList<Animator>();
        
        GameBoard.BoardCell cell;
        FrameLayout cellEl;
        ImageView pieceHolder;
        Wuball wuball;
        
        switch (whatHappened) {
            case INIT:
                while (newlyPlacedBoardCells.size() > 0) {
                    cell = newlyPlacedBoardCells.pop();
                    cellEl= (FrameLayout) ((TableRow) boardEl.getChildAt(cell.i)).getChildAt(cell.j);
                    pieceHolder = (ImageView) cellEl.getChildAt(0);
                    wuball = gameBoard.getWuball(cell);
                    
                    if ((cell.i+cell.j) % 2 == 1) cellEl.setBackgroundColor(Color.rgb(232, 237, 255)); 
                    else cellEl.setBackgroundColor(Color.rgb(255, 255, 255)); 

                    if (wuball.isJokerBall()) pieceHolder.setImageResource(jokerPiece);
                    else pieceHolder.setImageResource(gamePieces[wuball.getType()]);
                }    
            	break;

            case OVER:
                while (removedBoardCells.size() > 0) {
                    cell = removedBoardCells.pop();
                    cellEl= (FrameLayout) ((TableRow) boardEl.getChildAt(cell.i)).getChildAt(cell.j);
                    pieceHolder = (ImageView) cellEl.getChildAt(0);
                    wuball = gameBoard.getWuball(cell);
                    
                    if ((cell.i+cell.j) % 2 == 1) cellEl.setBackgroundColor(Color.rgb(232, 237, 255)); 
                    else cellEl.setBackgroundColor(Color.rgb(255, 255, 255)); 
                    pieceHolder.setImageResource(R.drawable.spacer_48);
                }    
            	break;
            	
            case CHANGE_SELECTION:
            	if (prevSelectedBoardCell != null) {
            		cell = prevSelectedBoardCell;
                    cellEl= (FrameLayout) ((TableRow) boardEl.getChildAt(cell.i)).getChildAt(cell.j);
                    pieceHolder = (ImageView) cellEl.getChildAt(0);
                    wuball = gameBoard.getWuball(cell);

                    // user canceling the previously selected piece
                    if ((cell.i+cell.j) % 2 == 1) cellEl.setBackgroundColor(Color.rgb(232, 237, 255)); 
                    else cellEl.setBackgroundColor(Color.rgb(255, 255, 255)); 
                    this.prevSelectedBoardCell = null;
            	}
            	
            	if (selectedBoardCell != null) {
            		cell = selectedBoardCell;
                    cellEl= (FrameLayout) ((TableRow) boardEl.getChildAt(cell.i)).getChildAt(cell.j);
                    pieceHolder = (ImageView) cellEl.getChildAt(0);
                    wuball = gameBoard.getWuball(cell);	
                    
                    // it's the selected piece. no need to change, just highlight it
                    cellEl.setBackgroundColor(Color.rgb(255, 247, 0)); // TODO: remove hardcode
                    
                    float startY = pieceHolder.getY();
                    ObjectAnimator bounceAnim = ObjectAnimator.ofFloat(pieceHolder, "y", startY, startY-3);
                    bounceAnim.setDuration(50);
                    bounceAnim.setInterpolator(new AccelerateInterpolator());
                    ObjectAnimator bounceBackAnim = ObjectAnimator.ofFloat(pieceHolder, "y", startY);
                    bounceBackAnim.setDuration(70);
                    bounceBackAnim.setInterpolator(new DecelerateInterpolator());
                    // Sequence the down/squash&stretch/up animations
                    AnimatorSet bouncer = new AnimatorSet();
                    bouncer.play(bounceBackAnim).after(bounceAnim);             
                    bouncer.start();                    
                    clickSound.start();
            	}
            	break;
            	
            case MOVE_AND_ADD:          
            case MOVE_AND_CONNECT:
            	if (movedFromBoardCell != null) {
            		cell = movedFromBoardCell;
                    cellEl= (FrameLayout) ((TableRow) boardEl.getChildAt(cell.i)).getChildAt(cell.j);
                    pieceHolder = (ImageView) cellEl.getChildAt(0);

                    // user canceling the previously selected piece
                    if ((cell.i+cell.j) % 2 == 1) cellEl.setBackgroundColor(Color.rgb(232, 237, 255)); 
                    else cellEl.setBackgroundColor(Color.rgb(255, 255, 255)); 
                    this.movedFromBoardCell = null;
                    pieceHolder.setImageResource(R.drawable.spacer_48);
            	}
            	
            	// TODO: animate ball to final location

            	if (movedToBoardCell != null) {
                    cell = movedToBoardCell;
                    cellEl= (FrameLayout) ((TableRow) boardEl.getChildAt(cell.i)).getChildAt(cell.j);
                    pieceHolder = (ImageView) cellEl.getChildAt(0);
                    wuball = this.movedWuball;
                
                    if ((cell.i+cell.j) % 2 == 1) cellEl.setBackgroundColor(Color.rgb(232, 237, 255)); 
                    else cellEl.setBackgroundColor(Color.rgb(255, 255, 255)); 

                    this.movedToBoardCell = null;
                    this.movedWuball = null;

                    if (wuball.isJokerBall()) pieceHolder.setImageResource(jokerPiece);
                    else pieceHolder.setImageResource(gamePieces[wuball.getType()]);                    
            	}           	
            	
            	if (newlyPlacedBoardCells.size() > 0) {
	                while (newlyPlacedBoardCells.size() > 0) {
	                    cell = newlyPlacedBoardCells.pop();
	                    cellEl= (FrameLayout) ((TableRow) boardEl.getChildAt(cell.i)).getChildAt(cell.j);
	                    pieceHolder = (ImageView) cellEl.getChildAt(0);
	                    wuball = gameBoard.getWuball(cell);
	
	                    if ((cell.i+cell.j) % 2 == 1) cellEl.setBackgroundColor(Color.rgb(232, 237, 255)); 
	                    else cellEl.setBackgroundColor(Color.rgb(255, 255, 255)); 
	
	                    if (wuball.isJokerBall()) pieceHolder.setImageResource(jokerPiece);
	                    else pieceHolder.setImageResource(gamePieces[wuball.getType()]);

		                PropertyValuesHolder pvhSX = PropertyValuesHolder.ofFloat("scaleX", 0.2f, 1.0f);
		                PropertyValuesHolder pvhSY = PropertyValuesHolder.ofFloat("scaleY", 0.2f, 1.0f);
		                ObjectAnimator appearAnim = ObjectAnimator.ofPropertyValuesHolder(pieceHolder, pvhSX, pvhSY);	                    
	                    appearAnim.setDuration(300);
	                    animations.add(appearAnim);	                    
	                }
	                
	                AnimatorSet animSet = new AnimatorSet();
	                animSet.playTogether(animations);
	                animSet.start();
                    appearSound.start();
            	}
            	
            	if (removedBoardCells.size() > 0) {
	                while (removedBoardCells.size() > 0) {
	                    cell = removedBoardCells.pop();
	                    cellEl= (FrameLayout) ((TableRow) boardEl.getChildAt(cell.i)).getChildAt(cell.j);
	                    pieceHolder = (ImageView) cellEl.getChildAt(0);
	                    wuball = gameBoard.getWuball(cell);
	                    
	                    if ((cell.i+cell.j) % 2 == 1) cellEl.setBackgroundColor(Color.rgb(232, 237, 255)); 
	                    else cellEl.setBackgroundColor(Color.rgb(255, 255, 255)); 
	                    
	                    float startX = pieceHolder.getX();
	                    float startY = pieceHolder.getY();

	                    PropertyValuesHolder pvhX = PropertyValuesHolder.ofKeyframe("x", 
	                            Keyframe.ofFloat(0f, startX+2), 
	                            Keyframe.ofFloat(0.1f, startX-1), 
	                            Keyframe.ofFloat(0.2f, startX-3), 
	                            Keyframe.ofFloat(0.3f, startX), 
	                            Keyframe.ofFloat(0.4f, startX+1), 
	                            Keyframe.ofFloat(0.5f, startX-1), 
	                            Keyframe.ofFloat(0.6f, startX-3), 
	                            Keyframe.ofFloat(0.7f, startX+2), 
	                            Keyframe.ofFloat(0.8f, startX-1), 
	                            Keyframe.ofFloat(0.9f, startX+2), 
	                            Keyframe.ofFloat(1.0f, startX+1));
	                    PropertyValuesHolder pvhY = PropertyValuesHolder.ofKeyframe("y", 
	                            Keyframe.ofFloat(0f, startX+1), 
	                            Keyframe.ofFloat(0.1f, startX-2), 
	                            Keyframe.ofFloat(0.2f, startX), 
	                            Keyframe.ofFloat(0.3f, startX+2), 
	                            Keyframe.ofFloat(0.4f, startX-1), 
	                            Keyframe.ofFloat(0.5f, startX+2), 
	                            Keyframe.ofFloat(0.6f, startX+1), 
	                            Keyframe.ofFloat(0.7f, startX+1), 
	                            Keyframe.ofFloat(0.8f, startX-1), 
	                            Keyframe.ofFloat(0.9f, startX+2), 
	                            Keyframe.ofFloat(1.0f, startX-2));
	                    PropertyValuesHolder pvhR = PropertyValuesHolder.ofKeyframe("rotation", 
	                            Keyframe.ofFloat(0f, 0), 
	                            Keyframe.ofFloat(0.1f, -1), 
	                            Keyframe.ofFloat(0.2f, 1), 
	                            Keyframe.ofFloat(0.3f, 0), 
	                            Keyframe.ofFloat(0.4f, 1), 
	                            Keyframe.ofFloat(0.5f, -1), 
	                            Keyframe.ofFloat(0.6f, 0), 
	                            Keyframe.ofFloat(0.7f, -1), 
	                            Keyframe.ofFloat(0.8f, 1), 
	                            Keyframe.ofFloat(0.9f, 0), 
	                            Keyframe.ofFloat(1.0f, -1));
	                    
	                    ObjectAnimator shakeAnim = ObjectAnimator.ofPropertyValuesHolder(pieceHolder, pvhX, pvhY, pvhR);                        
	                    shakeAnim.setDuration(100);
	                    shakeAnim.setRepeatCount(3);
	                    shakeAnim.setInterpolator(new LinearInterpolator());
	                    
	                    PropertyValuesHolder pvhSX = PropertyValuesHolder.ofFloat("scaleX", 1.0f, 1.2f);
	                    PropertyValuesHolder pvhSY = PropertyValuesHolder.ofFloat("scaleY", 1.0f, 0.0f);
	                    ObjectAnimator disappearAnim = ObjectAnimator.ofPropertyValuesHolder(pieceHolder, pvhSX, pvhSY);
	                    disappearAnim.setInterpolator(new AnticipateInterpolator(2.0f));
	                    disappearAnim.setDuration(400);
	                    
	                    AnimatorSet bouncer = new AnimatorSet();
	                    bouncer.play(shakeAnim).before(disappearAnim);           
	                    
	                    RemoveWuballAnimatorListener removeWuballCallback = new RemoveWuballAnimatorListener(cellEl, startX, startY);
	                    bouncer.addListener(removeWuballCallback);  
	                    animations.add(bouncer);
	                }
	                
	                AnimatorSet animSet = new AnimatorSet();
	                animSet.playTogether(animations);
                    animSet.addListener(new Connect5Listener());  
	                animSet.start();
            	}
                break;
        }  
    }

    /**
     * Repaints to score board GUI
     */
    private void repaintScore() {
        int currentScore = Integer.parseInt(scoreEl.getText().toString());
        if (currentScore != score) {
            scoreEl.setText(Integer.toString(score));
        }
    }

    /**
     * Repaints the "next x balls" gui
     */
    private void repaintNextWuballs() {
        List<Wuball> nextThree = gameBoard.peek(3);
        
        for (int x = 0; x < 3; x++) {
            Wuball next = nextThree.get(x);
            nextWuballsEl[x].setAlpha(0.3f);

            if (next.isJokerBall()) {
                nextWuballsEl[x].setImageResource(jokerPiece);
            } else {
                nextWuballsEl[x].setImageResource(gamePieces[next.getType()]);
            }
            ObjectAnimator anim = ObjectAnimator.ofFloat(nextWuballsEl[x], "alpha", 0.3f, 1f);
            anim.setDuration(500);
            anim.start();
        }
    }

    /**
     * helper method to draw the game gui
     * also initializes all the gui elements that we need access to later
     */
    private void initializeGameUI() {
        boardEl = (TableLayout) findViewById(R.id.board);
        scoreEl = (TextView) findViewById(R.id.score);
        nextWuballsEl = new ImageView[3];
        nextWuballsEl[0] = (ImageView) findViewById(R.id.nextBall1);
        nextWuballsEl[1] = (ImageView) findViewById(R.id.nextBall2);
        nextWuballsEl[2] = (ImageView) findViewById(R.id.nextBall3);

        // Board should take up entire available width,
        // then set height to be the same as width.
        // TODO: fix/support landscape screen orientation?
        Display display = getWindowManager().getDefaultDisplay();
        int width = display.getWidth();
        ViewGroup.LayoutParams params = boardEl.getLayoutParams();
        params.height = width;
        boardEl.setLayoutParams(params);

        /*
         * Render empty game board. Each cell is an ImageView. <TableLayout>
         * <TableRow> <FrameLayout><ImageView>spacer</ImageView></FrameLayout>
         * ... </TableRow> </table>
         */
        for (int i = 0; i < 9; i++) {
            TableRow tr = new TableRow(this);
            for (int j = 0; j < 9; j++) {
                FrameLayout cell = new FrameLayout(this);
                
                // Grid color TODO: remove hardcode
                if ((i+j) % 2 == 1) cell.setBackgroundColor(Color.rgb(232, 237, 255)); 
                else cell.setBackgroundColor(Color.rgb(255, 255, 255)); 

                ImageView pieceHolder = new ImageView(this);
                pieceHolder.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                pieceHolder.setImageResource(R.drawable.spacer_48);

                cell.addView(pieceHolder, new FrameLayout.LayoutParams(
                        LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
                cell.setOnClickListener(this);
                cell.setTag(gameBoard.new BoardCell(i, j));
                tr.addView(cell, new TableRow.LayoutParams(
                        LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT,
                        1.0f));
            }
            boardEl.addView(tr, new TableLayout.LayoutParams(
                    LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT, 1.0f));
        }
    }

    /**
     * Start a new game
     */
    private void rebootGameState() {
        gameBoard = GameBoard.getInstance();
        gameBoard.restart();
        score = 0;
        this.selectedBoardCell = null;
        this.prevSelectedBoardCell = null;
        this.newlyPlacedBoardCells = new LinkedList<GameBoard.BoardCell>();
        this.removedBoardCells = new LinkedList<GameBoard.BoardCell>();
    }

    private void startGame() {
        // start the game by putting x pieces on the board
        for (int x = 0; x < 5; x++) {
            try {
                newlyPlacedBoardCells.add(gameBoard.placeBall());
            } 
            catch (GameOverException goe) {
                // something is seriously f'ed up if we are here
            }
        }    	
    }
    
    
    public class Connect5Listener implements AnimatorListener {

        public Connect5Listener() {
        }
        
        public void onAnimationCancel(Animator arg0) {
            // TODO Auto-generated method stub
        }

        public void onAnimationEnd(Animator arg0) {
        	removeSound.start();
        }

        public void onAnimationRepeat(Animator arg0) {
            // TODO Auto-generated method stub
        }

        public void onAnimationStart(Animator arg0) {
            // TODO Auto-generated method stub
        }
    }
}