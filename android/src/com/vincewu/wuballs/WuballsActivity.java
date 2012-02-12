package com.vincewu.wuballs;

import java.util.LinkedList;
import java.util.List;

import android.R.color;
import android.R.integer;
import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Display;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;


public class WuballsActivity extends Activity implements OnClickListener {
	
	// Game state
    private GameBoard gameBoard = null; // user's game; only 1 game at a time
    private int score;	// user's score 
    private LinkedList<GameBoard.BoardCell> dirtyBoardCells = null; // tracks which BoardCells have changed
    private GameBoard.BoardCell selectedBoardCell = null; // tracks user selection. 

    // Game UI
    TableLayout boardEl = null;
    TextView scoreEl = null;
    int[] gamePieces = {R.drawable.adium, R.drawable.bsd, R.drawable.hedgehog, R.drawable.owl,
    		            R.drawable.panda, R.drawable.pug, R.drawable.twitter};
    

    /** 
     * handles click on a board cell
     * @param v FrameLayout
     */
    public void onClick(View v) {
        try {
        	if (v == null) throw new IllegalMoveException("Selected object doesn't exist!"); 
        	GameBoard.BoardCell cell = (GameBoard.BoardCell)v.getTag();
            play(cell);
        }
        catch (IllegalMoveException imex) {
        	logError("Illegal Move! " + imex.getMessage());
        }
        catch (Exception ex) {
        	logError(ex.getMessage());
        }
        repaintBoard();
        repaintScore();
    }
    
    /**
     * Handle user clicking a BoardCell. This is essentially the game loop. 
     * @param cell BoardCell that user selected
     */
    private void play(GameBoard.BoardCell cell) throws IllegalMoveException {
    	Wuball wuball = gameBoard.getWuball(cell);
    	
    	if (this.selectedBoardCell == null) {
    	    // haven't selected a game piece yet
    	    if (wuball == null) throw new IllegalMoveException("No ball at start coordinates: " + cell.toString());
    	    this.selectedBoardCell = cell;
    	    this.dirtyBoardCells.push(cell);
    	}
    	else {
    	    // user has already selected a game piece, and so we expect they'll click an empty cell
    	    // if they clicked on another game piece, let's assume they want to change their selection
    	    if (wuball != null) {
        	    if (this.selectedBoardCell.equals(cell)) {
        	    	// assume user wants to cancel the selection
        	        this.selectedBoardCell = null;
        	        this.dirtyBoardCells.push(cell); 
        	    }
        	    else {
        	        // assume user wants to switch selection 
        	        GameBoard.BoardCell oldSelection = gameBoard.new BoardCell(this.selectedBoardCell.i, this.selectedBoardCell.j);
        	        this.selectedBoardCell = cell;
        	        this.dirtyBoardCells.push(oldSelection);
        	        this.dirtyBoardCells.push(selectedBoardCell);         	    	
        	    }
    	    }    	    
    	    else {
    		  // let's try to make a move!
    	      this.move(this.selectedBoardCell, cell);
    	    }
    	}
    }
    
    /**
     * Play a move
     * @param start 
     * @param end
     */
    private void move(GameBoard.BoardCell start, GameBoard.BoardCell end) throws IllegalMoveException {
        boolean placeNewBalls = false;

        boolean validMove = gameBoard.moveBall(start, end);
		if (validMove) {
		    this.dirtyBoardCells.push(start);
  		    this.dirtyBoardCells.push(end);
  		    this.selectedBoardCell = null; 
		    	
	        // check if we have any 5+ game pieces in a rows
	        List<List<GameBoard.BoardCell>> results = gameBoard.connectFive(end);
	        if (results != null && results.size() > 0) {         
	        // remove the game pieces, and skip placing new ones
	            this.score += 2;
		        for (int x = 0; x < results.size(); x++) {
		        	List<GameBoard.BoardCell> list = results.get(x);
		        	this.dirtyBoardCells.addAll(list);
		        	this.score += (2*list.size());
		        }
	        }
  		    else {
  		        placeNewBalls = true;    
  		    }
		}
		else {
			throw new IllegalMoveException("You can't move there!");
		}
		    
		// put three balls on the board
	    if (placeNewBalls) {
	        for (int x = 0; x < 3; x++) this.dirtyBoardCells.push(gameBoard.placeBall());
	    }
    }
    
    
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        initializeGameState();
        setContentView(R.layout.main);
        initializeGameUI();
        repaintBoard();
        repaintScore();
    }  
    
    
    
    /** Write error messages for debugging */
    public void logError (String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    /**
     * Refresh game board section of UI
     */
    private void repaintBoard() {
    	while (dirtyBoardCells.size() > 0) {
    		GameBoard.BoardCell cell = dirtyBoardCells.pop();
    		FrameLayout cellEl =  (FrameLayout)((TableRow)boardEl.getChildAt(cell.i)).getChildAt(cell.j);
    		ImageView pieceHolder = (ImageView)cellEl.getChildAt(0);
    		Wuball wuball = gameBoard.getWuball(cell);
    		
		    if (wuball == null) {
	    	    pieceHolder.setImageResource(R.drawable.spacer);
	    	    pieceHolder.setBackgroundColor(color.transparent);
		    }
		    else {
		        if (cell.equals(selectedBoardCell)) {
		            // it's the selected piece. no need to change, just highlight it
		    	    pieceHolder.setBackgroundColor(Color.rgb(255, 247, 0)); // TODO: remove hardcode
		        }
		        else {
			        // repaint the entire cell, not sure what's in it
		    	    pieceHolder.setBackgroundColor(color.transparent);
		    	    pieceHolder.setImageResource(gamePieces[wuball.getType()]);
		        }
		    }
    	}		
    }
    
    private void repaintScore() {
        int currentScore = Integer.parseInt(scoreEl.getText().toString());
        if (currentScore != score) {
        	scoreEl.setText(Integer.toString(score));
        }
    }
    
    
    private void initializeGameUI() {    	
        boardEl = (TableLayout) findViewById(R.id.board);
        scoreEl = (TextView) findViewById(R.id.score);
        
        // Board should take up entire available width, 
        // then set height to be the same as width.
        // TODO: fix/support landscape screen orientation?
        Display display = getWindowManager().getDefaultDisplay(); 
        int width = display.getWidth();
        ViewGroup.LayoutParams params = boardEl.getLayoutParams();
        params.height = width;  
        boardEl.setLayoutParams(params);                       
        
        /* Render empty game board. Each cell is an ImageView.
         * <TableLayout>
         *   <TableRow>
         *     <FrameLayout><ImageView>spacer</ImageView></FrameLayout>
         *     ...
         *   </TableRow>
         * </table>
         */
        int c = 0;  // odd/even cell counter
        for (int i = 0; i < 9; i++) {
            TableRow tr = new TableRow(this);
            for (int j = 0; j < 9; j++) {
                FrameLayout cell = new FrameLayout(this);
	            if (c++ % 2 == 1) {
	            	cell.setBackgroundColor(Color.rgb(232, 237, 255)); // TODO: remove hardcode
	            }
	            else {
	            	cell.setBackgroundColor(Color.rgb(255, 255, 255));  // TODO: remove hardcode
	            }
	       	    ImageView pieceHolder = new ImageView(this);
	         	pieceHolder.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
	    	    pieceHolder.setImageResource(R.drawable.spacer);

	    	    cell.addView(pieceHolder, new FrameLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));	            
	    	    cell.setOnClickListener(this);
	    	    cell.setTag(gameBoard.new BoardCell(i, j));
	            tr.addView(cell, new TableRow.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT, 1.0f));
            }
            boardEl.addView(tr, new TableLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT, 1.0f));
        }
    }


    
    private void initializeGameState() {
    	gameBoard = GameBoard.getInstance();
    	score = 0;
        dirtyBoardCells = new LinkedList<GameBoard.BoardCell>();

        // start the game by putting x pieces on the board
        for (int x = 0; x < 10; x++) {
        	dirtyBoardCells.add(gameBoard.placeBall());
        }
    }
}