package nostalgia.framework.ui;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.blankj.utilcode.constant.PermissionConstants;
import com.blankj.utilcode.util.PermissionUtils;

import java.util.Timer;
import java.util.TimerTask;

import nostalgia.framework.R;

/**
 * Created by huzongyao on 2018/6/4.
 */

public class SplashActivity extends Activity {

    private static final int GET_ALL_FILE_ACCESS = 673;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                Intent intent = new Intent();
                intent.setAction(getString(R.string.action_gallery_page));
                startActivity(intent);
                finish();
            } else {
                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivityForResult(intent, GET_ALL_FILE_ACCESS);
            }
        }else{
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    startWithPermission();
                }
            }, 800L);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == GET_ALL_FILE_ACCESS && resultCode == RESULT_OK) {
            Intent intent = new Intent();
            intent.setAction(getString(R.string.action_gallery_page));
            startActivity(intent);
            finish();
        }
    }

    private void startWithPermission() {
        PermissionUtils.permission(PermissionConstants.STORAGE)
                .callback(new PermissionUtils.SimpleCallback() {
                    @Override
                    public void onGranted() {
                        Intent intent = new Intent();
                        intent.setAction(getString(R.string.action_gallery_page));
                        startActivity(intent);
                        finish();
                    }

                    @Override
                    public void onDenied() {
                        finish();
                    }
                }).request();
    }
}