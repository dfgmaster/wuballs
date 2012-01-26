// Author: Vince Wu 1/25/2012
//
// LICENSE (BSD):
//
// Copyright 2012 Vince Wu, all rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
// 
//   1. Redistributions of source code must retain the above copyright
//      notice, this list of conditions and the following disclaimer.
//
//   2. Redistributions in binary form must reproduce the above copyright
//      notice, this list of conditions and the following disclaimer in the
//      documentation and/or other materials provided with the distribution.
// 
//   3. Neither the name of this module nor the names of its contributors may
//      be used to endorse or promote products derived from this software
//      without specific prior written permission.
// 
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
// "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
// LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
// A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
// OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
// SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
// LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
// DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
// THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
// OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.



/** Class representing game pieces on the game board. 
 * @required
 *  type (int) type of game piece
 */
var Wuball = Object.create(null);
Wuball.equals = function(b) {
  return (b != null && (this.type == b.type))
}
/** static class variables
 * @internal
 *  _PEEK_LIST (Array) holding area for pre-computed Wuballs
 *  _JOKER_TYPE (int) type representing a joker piece
 */
Wuball._PEEK_LIST = []; 
Wuball._JOKER_TYPE = -1; 
/** @return boolean - whether the game piece is a joker */
Wuball.isJokerBall = function() {
  return (this.type == Wuball._JOKER_TYPE);
}
/** @return Array - static class method that retrieves the next x Wuballs */
Wuball.get = function(x) { // todo: move this elsewhere
    var result = Wuball.peek(x);
    Wuball._PEEK_LIST.splice(0,x);
    return result;
}
/** @return Array - static class method that just computes the next x Wuballs (and keeps them around) */
Wuball.peek = function(x) { // todo: move this elsewhere
  var result = [];
  if (x>0) {
    for (var y=0; y<x; y++) {
      if (Wuball._PEEK_LIST.length >= y+1) {
        result.push(Wuball._PEEK_LIST[y]);
      }
      else {
        var aBall = Object.create(Wuball);
        aBall.type = Math.floor(Math.random() * GameBoard.NUM_GAME_PIECES);
        result.push(aBall);
        Wuball._PEEK_LIST.push(aBall);          
      }   
    }       
  }
  return result;
}

 
/** Object representing the game board. Should be a singleton object. */
var GameBoard = {
 /* @Required
  *  SIZE (int) the x in an x*x board
  *  PLAY_JOKER (bool) whether to play joker piece
  *  NUM_GAME_PIECES (int) number of game piece types, not including joker piece 
  * 
  * @Internal
  * _BOARD ([][]) internal representation of the game board
  * _NUM_FREE_SLOTS (int) internal accounting tool 
  */
}
GameBoard.init = function(props) {
  var p;
  for (p in props) {GameBoard[p] = props[p]};
  if (GameBoard.SIZE == null || this.NUM_GAME_PIECES == null || GameBoard.PLAY_JOKER == null) {
    throw "Required GameBoard attributes are not defined";  
  } 

  GameBoard._BOARD = new Array(GameBoard.SIZE);
  for (var x = 0; x < GameBoard.SIZE; x++) {
        GameBoard._BOARD[x] = new Array(GameBoard.SIZE);
  }
  GameBoard._NUM_FREE_SLOTS = GameBoard.SIZE*GameBoard.SIZE;
}
/** @return String serialized game state for debugging */
GameBoard.toString = function() {
  var result = [];
  for (var i = 0; i < GameBoard.SIZE; i++) {
    var rowBuf = [];
    for (var j = 0; j < GameBoard.SIZE; j++) {
      (GameBoard._BOARD[i][j] == null) ? rowBuf.push(" - ") : rowBuf.push(" " + GameBoard._BOARD[i][j].type + " ");
    }
    result.push(rowBuf.join(""));
    result.push("\n");
  }
  return result.join("");
}
/** @return Wuball located at i, j */
GameBoard.getWuball = function(i, j) {
	return GameBoard._BOARD[i][j];
}
/** @return BoardCell location of new Wuball placed on the game board */
GameBoard.placeBall = function () {
  var aBall = Wuball.get(1)[0];
  var nextSlot = Math.floor(Math.random() * GameBoard._NUM_FREE_SLOTS);
 
  var count = 0;
  for (var i = 0; i < GameBoard.SIZE; i++) {
    for (var j = 0; j < GameBoard.SIZE; j++) {
      if (GameBoard._BOARD[i][j] == null) {
        if (++count>nextSlot) {
          GameBoard._BOARD[i][j] = aBall;
          GameBoard._NUM_FREE_SLOTS--;
          return BoardCell.newInstance(i,j);
        }
      }
    }
  } 
}
/** @return [][] a computed distance map with respect to <start> (0). 
 * The computed distance at <end> is > 0 if a route exists, and represents 
 * the shortest number of hops.
 *
 * start - BoardCell containing game piece that user wants to move 
 * end - BoardCell representing where user wants to move the game piece
 */
GameBoard.findRoute = function(start, end) {
  if (start == null || end == null) throw "No move commands";
  if (!start.withinBounds() || !end.withinBounds()) {
    throw "Move commands are out of bounds: " + start.toString() + " " + end.toString();
  }   
  if (GameBoard._BOARD[start.i][start.j] == null) {
    throw "No ball at start coordinates: " + start.toString();
  }
  if (GameBoard._BOARD[end.i][end.j] != null) {
    throw "A ball exists at end coordinates: " + end.toString();  
  }
     
  // set up recursion
  var distanceMap = new Array(GameBoard.SIZE);
  for (var i = 0; i < GameBoard.SIZE; i++) distanceMap[i] = new Array(GameBoard.SIZE);
  distanceMap[start.i][start.j] = 0;  
  GameBoard._findRouteRecurse(start, end, distanceMap, []);      

  return distanceMap;
}
/** Private method for recursively computing a distance map */
GameBoard._findRouteRecurse = function(next, end, distanceMap, todoList) {
  if (next.equals(end)) return;

  // try each direction one by one: top, right, bottom, left 
  GameBoard._findRouteRecurseSub(next.i-1, next.j, distanceMap[next.i][next.j], distanceMap, todoList);
  GameBoard._findRouteRecurseSub(next.i, next.j+1, distanceMap[next.i][next.j], distanceMap, todoList);
  GameBoard._findRouteRecurseSub(next.i+1, next.j, distanceMap[next.i][next.j], distanceMap, todoList);
  GameBoard._findRouteRecurseSub(next.i, next.j-1, distanceMap[next.i][next.j], distanceMap, todoList);

  if (todoList.length>0) {
    return GameBoard._findRouteRecurse(todoList.shift(), end, distanceMap, todoList);
  }
  else {
    return;
  }
}
/** Private method for recursively computing a distance map */
GameBoard._findRouteRecurseSub = function(i, j, lastDistance, distanceMap, todoList) {
  var cell = BoardCell.newInstance(i,j);
  // skip if we've moved off the grid
  if (cell.withinBounds()) {
    // skip if there's a game piece already
    if (GameBoard._BOARD[cell.i][cell.j] == null) {
      // skip if we've been to this cell already (or else we'd go in a loop)
      if (distanceMap[cell.i][cell.j] == null) {
        todoList.push(cell);
        distanceMap[cell.i][cell.j] = lastDistance + 1;         
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
GameBoard.connectFive = function(cell) {
  var results = [];
  var ball = GameBoard._BOARD[cell.i][cell.j];

  var i, j; // iterators

  // check vertical
  var vList = [];
  for (i=cell.i+1; i<GameBoard.SIZE; i++) {
    if (ball.equals(GameBoard._BOARD[i][cell.j])) vList.push(BoardCell.newInstance(i,cell.j));
    else break;
  }
  for (i=cell.i-1; i>=0; i--) {
    if (ball.equals(GameBoard._BOARD[i][cell.j])) vList.push(BoardCell.newInstance(i,cell.j));
    else break;
  }
  if (vList.length >= 4) results.push(vList);

  // check horizontal
  var hList = [];
  for (j=cell.j+1; j<GameBoard.SIZE; j++) {
    if (ball.equals(GameBoard._BOARD[cell.i][j])) hList.push(BoardCell.newInstance(cell.i,j));
    else break;
  }
  for (j=cell.j-1; j>=0; j--) {
    if (ball.equals(GameBoard._BOARD[cell.i][j])) hList.push(BoardCell.newInstance(cell.i,j));
    else break;
  }
  if (hList.length >= 4) results.push(hList);

  // check diagonal negative slope
  var dnList = [];
  for (i=cell.i+1,j=cell.j+1; i<GameBoard.SIZE && j<GameBoard.SIZE; i++,j++) {
    if (ball.equals(GameBoard._BOARD[i][j])) dnList.push(BoardCell.newInstance(i,j));
    else break;
  }
  for (i=cell.i-1,j=cell.j-1; i>=0 && j>=0; i--,j--) {
    if (ball.equals(GameBoard._BOARD[i][j])) dnList.push(BoardCell.newInstance(i,j));
    else break;
  }
  if (dnList.length >= 4) results.push(dnList);
    
  // check diagonal positive slope
  var dpList = [];
  for (var i=cell.i+1,j=cell.j-1; i<GameBoard.SIZE && j>=0; i++,j--) {
    if (ball.equals(GameBoard._BOARD[i][j])) dpList.push(BoardCell.newInstance(i,j));
    else break;
  }
  for (var i=cell.i-1,j=cell.j+1; i>=0 && j<GameBoard.SIZE; i--,j++) {
    if (ball.equals(GameBoard._BOARD[i][j])) dpList.push(BoardCell.newInstance(i,j));
    else break;
  }
  if (dpList.length >= 4) results.push(dpList);

  return results;
}


/** Class representing a coordinate on the game board. 
 * @required
 *  i (int) row
 *  j (int) col
 */
var BoardCell = Object.create(null);
/** @return BoardCell - convenience method */
BoardCell.newInstance = function(i, j) {
  var newCell = Object.create(BoardCell);
  newCell.i = i;
  newCell.j = j;
  return newCell;
}
BoardCell.equals = function(b) {
  return (b != null && this.i == b.i && this.j == b.j);
}
BoardCell.toString = function() {
  var msgBuf = ["{", this.i, ",", this.j, "}"];
  return msgBuf.join("");
}
/** @return boolean - whether the game piece is a joker */
BoardCell.withinBounds = function() {
  return (this.i>=0 && this.j>=0 && this.i<GameBoard.SIZE && this.j<GameBoard.SIZE);
}



/** Controller object. Should be a singleton object. */
var Game = {
  score: null,

  // keeps track of which BoardCells have changed on the board during each move
  // (so we don't have to repaint the entire board)
  dirtyBoardCells: [],
	
  // keeps track of user selection. A BoardCell. 
  selectedBoardCell: null
};
/** Main entry point to the game. Initializes game state. */
Game.init = function() {
  try {
    // seed random number generator
    if (!Math.seedrandom) {
      throw("A piece of code is missing: seedrandom.js");
      return;
    }
    Math.seedrandom("vboy"); // TODO: remove hardcode

    // Game attributes, not currently configurable in the GUI. TODO: add this feature
    GameBoard.init({
      SIZE : 9,
      NUM_GAME_PIECES : 7,
      PLAY_JOKER : true
    });
    Game.score = 0;

    // start the game by putting x pieces on the board
    for (var x=0; x<10; x++) Game.dirtyBoardCells.push(GameBoard.placeBall());

    UI.init();
  }
  catch (ex) {
	UI.logError(ex);
	return;
  }
}
/** 
 * Handle user clicking a BoardCell. This is essentially the game loop. 
 * cell (BoardCell) that user selected
 */
Game.play = function(cell) {
  var wuball = GameBoard.getWuball(cell.i, cell.j);

  if (Game.selectedBoardCell == null) { 
    // haven't selected a game piece yet
    if (wuball == null) throw "No ball at start coordinates: " + input.toString();
    Game.selectedBoardCell = cell;
    Game.dirtyBoardCells.push(cell);
  }
  else {
    // user has already selected a game piece, and so we expect they'll click an empty cell
    // if they clicked on another game piece, let's assume they want to change their selection
    if (wuball != null) {
      if (Game.selectedBoardCell.equals(cell)) {
        // assume user wants to cancel the selection
        Game.selectedBoardCell = null;
        Game.dirtyBoardCells.push(cell); 
      }
      else {
        // assume user wants to switch selection 
        var tmp = BoardCell.newInstance(Game.selectedBoardCell.i,Game.selectedBoardCell.j);
        Game.selectedBoardCell = cell;
        Game.dirtyBoardCells.push(tmp, Game.selectedBoardCell); 
      }
    }   
    else {
	  // let's try to make a move!
      Game.move(Game.selectedBoardCell, cell);
    }
  }
 
}
/** Play a move
 * start, end - BoardCell
 */
Game.move = function(start, end) {
  try {
    var placeNewBalls = false;
    
    // check if this is a valid move
    var distanceMap = GameBoard.findRoute(start, end);    
    if (distanceMap[end.i][end.j] != null) {        
      // a valid move! move the game piece
      GameBoard._BOARD[end.i][end.j] = GameBoard._BOARD[start.i][start.j];
      GameBoard._BOARD[start.i][start.j] = null;
      Game.dirtyBoardCells.push(start, end);       
      Game.selectedBoardCell = null; 

      // check if we have any 5+ game pieces in a rows
      var results = GameBoard.connectFive(end);               
      if (results.length > 0) {         
        // remove the game pieces, and skip placing new ones
        GameBoard._BOARD[end.i][end.j] = null;
        GameBoard._NUM_FREE_SLOTS++;
        Game.score += 2;
        for (var x=0; x<results.length; x++) {
          var list = results[x];
          for (var y=0; y<list.length; y++) {
            GameBoard._BOARD[list[y].i][list[y].j] = null;
            Game.dirtyBoardCells.push(list[y]); 
            GameBoard._NUM_FREE_SLOTS++;
            Game.score += 2;
          }
        }
      }
      else {
        placeNewBalls = true;    
      }
    }

    // put three balls on the board
    if (placeNewBalls) {
      for (var x=0; x<3; x++) Game.dirtyBoardCells.push(GameBoard.placeBall());
    }
  }
  catch(ex) {
    UI.logError(ex);
    return;
  }

}


/** View object that handles GUI rendering and event handling. Should be a singleton object. */
var UI = {
  _boardEl: null,
  _scoreEl: null,
  _nextWuballsEl: null,
  _score: null,
  _nextWuballs: [],
  _IMAGES: ["images/bsd.png", "images/panda.png", "images/pug.png", "images/twitter.png",
	              "images/owl.png", "images/hedgehog.png", "images/adium.png"],
  _JOKER_IMAGE: "images/cat-h.png",

  debugMode : false,
  _debugSectionEl: null,
  _debugMessageEl: null,
  _debugInputEl: null,
  _debugSubmitEl: null
}
/** Initialize GUI */
UI.init = function() {
  this._boardEl = document.getElementById("board");
  this._scoreEl = document.getElementById("score");
  this._nextWuballsEl = document.getElementById("nextBalls");

  if (this._boardEl == null || this._scoreEl == null) {
	throw("Expected DOM elements don't exist");
  }  

  // render empty game board
  var c = 0; // odd/even cell counter
  for (var i = GameBoard.SIZE - 1; i >= 0; i--) {
    var rowEl = this._boardEl.insertRow();
    for (var j = GameBoard.SIZE - 1; j >= 0; j--) {    
      var cellEl = rowEl.insertCell();
      cellEl.i = i;
      cellEl.j = j;
      cellEl.className = (c++ % 2 == 1) ? "cell-light" : "cell-dark";
      cellEl.style.width = "65px"; // todo: remove hardcoded values
      cellEl.style.height = "65px"; // todo: remove hardcoded values
      cellEl.onclick = UI.handleClick;
    }
  }

  UI.repaintBoard();
  UI.repaintNextWuballs();
  UI.repaintScore();

  this.debugMode = (window.location.href.indexOf("debug=true") != -1);
  if (this.debugMode) {
    this._debugSectionEl = document.getElementById("debugSection");
    this._debugMessageEl = document.getElementById("debugMessage");
    this._debugBoardEl = document.getElementById("debugBoard");
    this._debugInputEl = document.getElementById("debugInput");
    this._debugSubmitEl = document.getElementById("debugSubmit");
	this._debugSectionEl.style.display = "block";
  }
}
/** Write error messages for debugging */
UI.logError = function(msg) {
  if (this.debugMode) this._debugMessageEl.innerText = msg;
}
UI.clearErrorLog = function() {
  this.logError("");
}
/** Update the scoreboard */
UI.repaintScore = function() {
  // only repaint if needed
  if (this._score != Game.score) {
	this._score = Game.score;
    this._scoreEl.innerText = Game.score;
  }
}
/** Update the 'Next 3 game pieces' section */
UI.repaintNextWuballs = function() {
  var nextThree = Wuball.peek(3);  

  // only repaint if needed
  var same = true; 
  for (var x=0; x < 3; x++) {
    if (!nextThree[x].equals(this._nextWuballs[x])) {
	  same = false;
	  break;
    }
  } 

  if (!same) {
	this._nextWuballs = nextThree.slice(0);
    // todo: get rid of hardcoded dimensions
    this._nextWuballsEl.innerHTML = "<img src=\"" + UI._IMAGES[nextThree[0].type] + "\" width=\"65\" height=\"65\">" +
                                    "<img src=\"" + UI._IMAGES[nextThree[1].type] + "\" width=\"65\" height=\"65\">" +
                                    "<img src=\"" + UI._IMAGES[nextThree[2].type] + "\" width=\"65\" height=\"65\">";
  }
}
/** Update the game board */
UI.repaintBoard = function() {
  while (Game.dirtyBoardCells.length > 0) {
    var cell = Game.dirtyBoardCells.pop();
    var cellEl = this._boardEl.rows[cell.i].cells[cell.j];
    var wuball = GameBoard.getWuball(cell.i,cell.j);

    if (wuball == null) {
	  // clear contents and highlight
      cellEl.innerHTML = "";
      cellEl.style.backgroundColor = null;
    }
    else {
      if (cell.equals(Game.selectedBoardCell)) {
        // it's the selected piece. no need to change, just highlight it
        cellEl.style.backgroundColor = "#f9f733";
      }
      else {
	    // repaint the entire cell, not sure what's in it
        cellEl.style.backgroundColor = null;
        cellEl.innerHTML = "<img src=\"" + UI._IMAGES[wuball.type] + "\">";
      }
    }
  }

  if (this.debugMode) this._debugBoardEl.innerText = GameBoard.toString();
}
/** onclick event handler attached to each cell (<td> element) */
UI.handleClick = function() {
  if (this == null) UI.logError("Selected object doesn't exist!"); 
  try {
    Game.play(BoardCell.newInstance(this.i,this.j));
  }
  catch (ex) {
    UI.logError(ex);	
  }
  UI.repaintBoard();
  UI.repaintNextWuballs();
  UI.repaintScore();

}
/** Debugging: support text based move commands */
UI.handleDebugInput = function(e) {
  // look for window.event in case event isn't passed in
  if (typeof e == 'undefined' && window.event) { e = window.event; }
  if (e.keyCode == 13) this._debugSubmitEl.click();
}   
/** Debugging: support text based move commands */
UI.handleDebugMove = function() {
  this.clearErrorLog();

  // format: {i1,j1} {i2,j2}        
  var parseString = this._debugInputEl.value;
  var fcs, fce, scs, sce, fc, sc;
  fcs = parseString.indexOf("{");
  scs = parseString.indexOf("{",fcs+1);
  fce = parseString.indexOf("}");
  sce = parseString.indexOf("}",fce+1);
  fc = parseString.indexOf(",");
  sc = parseString.indexOf(",",fc+1);
        
  var start = BoardCell.newInstance(parseInt(parseString.substr(fcs+1,fc-1-fcs)),
                                    parseInt(parseString.substr(fc+1,fce-1-fc)));
  var end = BoardCell.newInstance(parseInt(parseString.substr(scs+1,sc-1-scs)), 
                                  parseInt(parseString.substr(sc+1,sce-1-sc)));
  Game.move(start, end);        
}