package com.woohyman.gui.ui.multitouchbutton;

import android.view.MotionEvent;

import nostalgia.framework.ui.multitouchbutton.OnMultitouchEventListener;

public interface MultitouchBtnInterface {
    void onTouchEnter(MotionEvent event);

    void onTouchExit(MotionEvent event);

    void setOnMultitouchEventlistener(OnMultitouchEventListener listener);

    int getId();

    boolean isPressed();

    void requestRepaint();

    void removeRequestRepaint();

    boolean isRepaintState();

    int getVisibility();

}
