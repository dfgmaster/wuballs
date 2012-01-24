

/* Class representing balls on the game board. */
var Ball = Object.create(null);
Ball.NUM_BALL_TYPE = 7;
Ball.BALL_TYPES = ['A','B','C','D','E','F','G'];
Ball.MAGIC_BALL = 0;
Ball.equals = function(b) {
	return (b != null && (this.color == b.color))
}

 
// 9x9 board - TODO: objectify
var board;
var boardSize = 9;
var boardFreeSlots = boardSize*boardSize;
function printBoard() {
    var result = [];
    for (var i = 0; i < boardSize; i++) {
        var rowBuf = [];
        for (var j = 0; j < boardSize; j++) {
            (board[i][j] == null) ? rowBuf.push(" - ") : rowBuf.push(" " + Ball.BALL_TYPES[board[i][j].color] + " ");
        }
        result.push(rowBuf.join(""));
        result.push("\n");
    }
    return result.join("");
}
function placeBall() {
    var aBall = Object.create(Ball);
    aBall.color = Math.floor(Math.random() * Ball.NUM_BALL_TYPE);

    var nextSlot = Math.floor(Math.random() * boardFreeSlots);

    var count = 0;
    for (var i = 0; i < boardSize; i++) {
        for (var j = 0; j < boardSize; j++) {
             if (board[i][j] == null) {
                 if (++count>nextSlot) {
                     board[i][j] = aBall;
                     boardFreeSlots--;
                     return;
                 } 
             }
        }
    } 
}

/**
 * Object representing a cell on the game board.
 */
var BoardCell = Object.create(null);
BoardCell.equals = function(b) {
    return (b != null && this.i == b.i && this.j == b.j);
}
BoardCell.toString = function() {
    var msgBuf = ["{", this.i, ",", this.j, "}"];
    return msgBuf.join("");
}
BoardCell.withinBounds = function() {
    return (this.i>=0 && this.j>=0 && this.i<boardSize && this.j<boardSize);
}


function findRoute(startCoords, endCoords) {
	if (startCoords == null || endCoords == null) throw "No move commands";
	if (!startCoords.withinBounds() || !endCoords.withinBounds()) {
		throw "Move commands are out of bounds: " + startCoords.toString() + " " + endCoords.toString();
	}	
    if (board[startCoords.i][startCoords.j] == null) {
        throw "No ball at start coordinates: " + startCoords.toString();
    }
    if (board[endCoords.i][endCoords.j] != null) {
        throw "A ball exists at end coordinates: " + endCoords.toString();	
    }
	
    
    // initialize distance map and recurse
    var distanceMap = new Array(boardSize);
    for (var i = 0; i < boardSize; i++) {
        distanceMap[i] = new Array(boardSize);
    }
	distanceMap[startCoords.i][startCoords.j] = 0;	

/*	
	alert(startCoords.toString() + " " + endCoords.toString());
    var result = [];
    for (var i = 0; i < boardSize; i++) {
        var rowBuf = [];
        for (var j = 0; j < boardSize; j++) {
            (distanceMap[i][j] == null) ? rowBuf.push("-") : rowBuf.push(distanceMap[i][j]);
        }
        result.push(rowBuf.join(""));
        result.push("\n");
    }
    alert(result.join(""));
*/
    findRouteRecurse(startCoords, endCoords, distanceMap, []);		

    return distanceMap;
}
function findRouteRecurse(nextCoords, endCoords, distanceMap, todoList) {
    if (nextCoords.equals(endCoords)) return;

    // top, right, bottom, left 
    findRouteRecurseSub(nextCoords.i - 1,nextCoords.j,distanceMap[nextCoords.i][nextCoords.j], distanceMap, todoList);
    findRouteRecurseSub(nextCoords.i,nextCoords.j + 1,distanceMap[nextCoords.i][nextCoords.j], distanceMap, todoList);
    findRouteRecurseSub(nextCoords.i + 1,nextCoords.j,distanceMap[nextCoords.i][nextCoords.j], distanceMap, todoList);
    findRouteRecurseSub(nextCoords.i,nextCoords.j - 1,distanceMap[nextCoords.i][nextCoords.j], distanceMap, todoList);

    if (todoList.length>0) {
	    return findRouteRecurse(todoList.shift(), endCoords, distanceMap, todoList);
	}
	else {
		return;
    }
}
function findRouteRecurseSub(i, j, lastDistance, distanceMap, todoList) {
    var thisCell = Object.create(BoardCell);
    thisCell.i = i;
    thisCell.j = j;
    if (thisCell.withinBounds()) {
	    // check there isn't a ball already
	    if (board[thisCell.i][thisCell.j] == null) {

	        // make sure we aren't repeating a cell
	        if (distanceMap[thisCell.i][thisCell.j] == null) {
	            todoList.push(thisCell);
			    distanceMap[thisCell.i][thisCell.j] = lastDistance + 1;   		
         	}
	    }
    }	
}


function testtest(xi,xj,yi,yj) {


	var x = Object.create(BoardCell);
	var y = Object.create(BoardCell);

	x.i = 2; 
	x.j = 0;
	y.i = 4; 
	y.j = 3;
	play(x,y);	

	x.i = 8; 
	x.j = 7;
	y.i = 3; 
	y.j = 3;
	play(x,y);	

	x.i = 1; 
	x.j = 3;
	y.i = 1; 
	y.j = 8;
	play(x,y);	

	x.i = 2; 
	x.j = 8;
	y.i = 1; 
	y.j = 3;
	play(x,y);	
}

function markForRemoval(cell) {

    var ball = board[cell.i][cell.j];
    var results = [];

    // check vertical
    var tempList = [];
    for (var i=cell.i+1; i<boardSize; i++) {
		if (board[i][cell.j] != null && board[i][cell.j].equals(ball)) {
		    var x = Object.create(BoardCell);
            x.i = i;
            x.j = cell.j;	
            tempList.push(x);
		}
		else break;
    }
    for (var i=cell.i-1; i>=0; i--) {
		if (board[i][cell.j] != null && board[i][cell.j].equals(ball)) {
		    var x = Object.create(BoardCell);
            x.i = i;
            x.j = cell.j;					
            tempList.push(x);				
		}
		else break;
    }
    if (tempList.length >= 4) results.push(tempList);

    // check horizontal
    var tempList2 = [];
    for (var j=cell.j+1; j<boardSize; j++) {
		if (board[cell.i][j] != null && board[cell.i][j].equals(ball)) {
		    var x = Object.create(BoardCell);
            x.i = cell.i;
            x.j = j;
            tempList2.push(x);
		}
		else break;
    }
    for (var j=cell.j-1; j>=0; j--) {
		if (board[cell.i][j] != null && board[cell.i][j].equals(ball)) {
		    var x = Object.create(BoardCell);
            x.i = cell.i;
            x.j = j;			
            tempList2.push(x);				
		}
		else break;
    }
    if (tempList2.length >= 4) results.push(tempList2);

    // check diagonal positive slope
    var tempList3 = [];
    for (var i=cell.i+1,j=cell.j+1; i<boardSize && j<boardSize; i++,j++) {
		if (board[i][j] != null && board[i][j].equals(ball)) {
		    var x = Object.create(BoardCell);
            x.i = i;
            x.j = j;
            tempList3.push(x);
		}
		else break;
    }
    for (var i=cell.i-1,j=cell.j-1; i>=0 && j>=0; i--,j--) {
		if (board[i][j] != null && board[i][j].equals(ball)) {
		    var x = Object.create(BoardCell);
            x.i = i;
            x.j = j;			
            tempList3.push(x);				
		}
		else break;
    }
    if (tempList3.length >= 4) results.push(tempList3);
	
    // check diagonal positive slope
    var tempList4 = [];
    for (var i=cell.i+1,j=cell.j-1; i<boardSize && j>=0; i++,j--) {
		if (board[i][j] != null && board[i][j].equals(ball)) {
		    var x = Object.create(BoardCell);
            x.i = i;
            x.j = j;
            tempList4.push(x);
		}
		else break;
    }
    for (var i=cell.i-1,j=cell.j+1; i>=0 && j<boardSize; i--,j++) {
		if (board[i][j] != null && board[i][j].equals(ball)) {
		    var x = Object.create(BoardCell);
            x.i = i;
            x.j = j;			
            tempList4.push(x);				
		}
		else break;
    }
    if (tempList4.length >= 4) results.push(tempList4);

    return results;
}



/** 
 *  Entry point to the game
 */
function init() {
    // create board 
    board = new Array(boardSize);
    for (var i = 0; i < boardSize; i++) {
        board[i] = new Array(boardSize);
    }

    // seed random number generator
    if (!Math.seedrandom) {
        alert("A piece of code is missing: seedrandom.js");
        return;
    }
    Math.seedrandom("vboy");

    // put three balls on the board
    for (var x=0; x<20; x++) placeBall();

    UI.updateBoard();
}



/**
 * startCoords, endCoords - BoardCell
 */
function play(startCoords, endCoords) {
    try {
	    var placeNewBalls = true;
	
	    // check if this is a valid move
	    var distanceMap = findRoute(startCoords, endCoords);	
	    if (distanceMap[endCoords.i][endCoords.j] != null) {		

		    // valid, move the ball
            board[endCoords.i][endCoords.j] = board[startCoords.i][startCoords.j];
            board[startCoords.i][startCoords.j] = null;

		    // check if we have any 5+ balls in a rows
            var results = markForRemoval(endCoords);	           
            if (results.length > 0) {
	           
	           // remove the balls, and skip placing new balls
	           placeNewBalls = false;
	           board[endCoords.i][endCoords.j] = null;
               for (var x=0; x<results.length; x++) {
	               var list = results[x];
	               for (var y=0; y<list.length; y++) {
                       board[list[y].i][list[y].j] = null;
                   }
               }
            }
	    }

	    // put three balls on the board
	    if (placeNewBalls) {
	        for (var x=0; x<3; x++) placeBall();
	    }
	
    }
    catch(ex) {
        UI.errorLog(ex);
        return;
    }

    // wait for user to make next move
    UI.updateBoard();
}



/**
 *  View - handles on paint operations
 */
var UI = {
    debugSectionElId : "debugSection",
    debugBoardElId : "debugBoard",  
    debugInputElId : "debugInput",
    debugSubmitElId : "debugSubmit",
    debugMessageElId : "debugMessage",
    debugMode : true    
}
UI.errorLog = function(msg) {
    if (this.debugMode) {
        var debugMessageEl = document.getElementById(UI.debugMessageElId);
        if (debugMessageEl != null) {
            debugMessageEl.innerText = msg;
        }
    }
}
UI.clearErrorLog = function() {
    this.errorLog("");
}
UI.updateBoard = function() {
    if (this.debugMode) {
        var debugBoardEl = document.getElementById(UI.debugBoardElId);
        if (debugBoardEl != null) {
            debugBoardEl.innerText = printBoard();
        }
    }
}
UI.handleDebugInput = function(e) {
      // look for window.event in case event isn't passed in
      if (typeof e == 'undefined' && window.event) { e = window.event; }
      if (e.keyCode == 13) {
          document.getElementById(UI.debugSubmitElId).click();
      }
}	

UI.handleDebugMove = function() {
    var debugInputEl = document.getElementById(UI.debugInputElId);
    if (debugInputEl != null) {
	    this.clearErrorLog();
        
        var parseString = debugInputEl.value;
        var fcs, fce, scs, sce, fc, sc;
        fcs = parseString.indexOf("{");
        scs = parseString.indexOf("{",fcs+1);
        fce = parseString.indexOf("}");
        sce = parseString.indexOf("}",fce+1);
        fc = parseString.indexOf(",");
        sc = parseString.indexOf(",",fc+1);
        
        var startCoords = Object.create(BoardCell);
        startCoords.i = parseInt(parseString.substr(fcs+1,fc-1-fcs));
        startCoords.j = parseInt(parseString.substr(fc+1,fce-1-fc));
        var endCoords = Object.create(BoardCell);
        endCoords.i = parseInt(parseString.substr(scs+1,sc-1-scs));
        endCoords.j = parseInt(parseString.substr(sc+1,sce-1-sc));
        
        play(startCoords, endCoords);       
    } 
}