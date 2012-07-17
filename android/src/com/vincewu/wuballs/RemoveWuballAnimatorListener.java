package com.vincewu.wuballs;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.widget.FrameLayout;
import android.widget.ImageView;

public class RemoveWuballAnimatorListener implements AnimatorListener {

    private FrameLayout cellEl;
    private float x;
    private float y;
    public void setCell(FrameLayout cellEl) {
        this.cellEl = cellEl;
    }
    
    public RemoveWuballAnimatorListener(FrameLayout cellEl, float x, float y) {
        setCell(cellEl);
        this.x = x;
        this.y = y;
    }
    
    public void onAnimationCancel(Animator arg0) {
        // TODO Auto-generated method stub

    }

    public void onAnimationEnd(Animator arg0) {
        ImageView pieceHolder = (ImageView) cellEl.getChildAt(0);
        pieceHolder.setX(x);
        pieceHolder.setY(y);
        pieceHolder.setRotation(0f);
        pieceHolder.setScaleX(1.0f);
        pieceHolder.setScaleY(1.0f);
        
        // scored a connect-5
        pieceHolder.setImageResource(R.drawable.spacer_48);
    }

    public void onAnimationRepeat(Animator arg0) {
        // TODO Auto-generated method stub
    }

    public void onAnimationStart(Animator arg0) {
        // TODO Auto-generated method stub

    }

}
