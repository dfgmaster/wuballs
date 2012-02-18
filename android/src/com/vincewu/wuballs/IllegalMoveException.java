package com.vincewu.wuballs;

public class IllegalMoveException extends Exception {
    private static final long serialVersionUID = -3063955985990790916L;

    public IllegalMoveException(String msg) {
    	super(msg);
    }
}
