package com.woohyman.gui.ui;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import com.woohyman.gui.base.EmulatorActivity;
import com.woohyman.gui.base.OpenGLTestActivity;
import com.woohyman.gui.ui.gamegallery.GalleryActivity;

import java.util.HashSet;
import java.util.Set;

import com.woohyman.keyboard.emulator.Emulator;
import com.woohyman.keyboard.utils.EmuUtils;
import com.woohyman.keyboard.utils.NLog;
import com.woohyman.keyboard.utils.PreferenceUtil;

public class NesGalleryActivity extends GalleryActivity {

    private static final int REQUEST_CHECK_OPENGL = 200;

    @Override
    public Emulator getEmulatorInstance() {
        return NesEmulator.getInstance();
    }

    @Override
    public Class<? extends EmulatorActivity> getEmulatorActivityClass() {
        return NesEmulatorActivity.class;
    }

    @Override
    protected Set<String> getRomExtensions() {
        HashSet<String> set = new HashSet<>();
        set.add("nes");
        set.add("fds");
        return set;
    }


    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (PreferenceUtil.getFragmentShader(this) == -1
                && EmuUtils.checkGL20Support(this)) {
            Intent intent = new Intent(this, OpenGLTestActivity.class);
            startActivityForResult(intent, REQUEST_CHECK_OPENGL);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CHECK_OPENGL) {
            NLog.e("opengl", "opengl: " + resultCode);
            PreferenceUtil.setFragmentShader(this, resultCode);
        }
    }
}
