package com.vincewu.wuballs;

class Wuball {
    private static int jokerType = -1; // type representing a joker piece  
	private int type;
	private boolean newBall = true;

	public void setJokerType() {
		this.type = jokerType;
	}
	
	public void setType(int type) {
		this.type = type;  // TODO: check bounds?
	}
	
	public int getType() {
		return type;
	}

    /** whether the ball represents a Joker piece */
    public boolean isJokerBall() {
    	return this.type == jokerType;
    }
    
    /**
     * @return whether Wuball has been moved
     */
    public boolean hasMoved() {
        return !newBall;        
    }

    /**
     * @return whether Wuball has moved
     */
    public void setMoved() {
        this.newBall = false;        
    }

	public boolean equals(Object o) {
		if (this == o) return true;
		if((o == null) || (o.getClass() != this.getClass())) return false; 
		Wuball ball = (Wuball)o;  
		// Joker ball matches any ball
	    return (ball.type == this.type || ball.isJokerBall() || this.isJokerBall()); 
	}
	
	public int hashCode() {
		int hash = 7;
		hash = 31 * hash + this.type;
		return hash;
	}
}
