package com.woohyman.gui.ui;

import com.liulishuo.filedownloader.FileDownloader;
import com.woohyman.gui.BaseApplication;
import com.woohyman.keyboard.base.EmulatorHolder;

public class NesApplication extends BaseApplication {

    @Override
    public void onCreate() {
        super.onCreate();
        EmulatorHolder.setEmulatorClass(NesEmulator.class);
        FileDownloader.setupOnApplicationOnCreate(this);
    }

    @Override
    public boolean hasGameMenu() {
        return true;
    }
}
