package com.woohyman.gui.base;

import android.view.View;

import com.woohyman.keyboard.base.ViewPort;

interface EmulatorView {
    void onPause();

    void onResume();

    void setQuality(int quality);

    ViewPort getViewPort();

    View asView();
}
