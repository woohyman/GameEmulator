package nostalgia.framework.ui.gamegallery;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;

import com.blankj.utilcode.util.StringUtils;
import com.blankj.utilcode.util.UriUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import nostalgia.framework.Emulator;
import nostalgia.framework.R;
import nostalgia.framework.base.EmulatorActivity;
import nostalgia.framework.ui.gamegallery.RomsFinder.OnRomsFinderListener;
import nostalgia.framework.utils.DatabaseHelper;
import nostalgia.framework.utils.DialogUtils;
import nostalgia.framework.utils.FileUtils;
import nostalgia.framework.utils.NLog;

abstract public class BaseGameGalleryActivity extends AppCompatActivity
        implements OnRomsFinderListener {

    private static final String TAG = "BaseGameGalleryActivity";

    protected Set<String> exts;
    protected Set<String> inZipExts;
    protected boolean reloadGames = true;
    protected boolean reloading = false;
    private RomsFinder romsFinder = null;
    private DatabaseHelper dbHelper = null;
    private SharedPreferences sharedPreferences;

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        HashSet<String> exts = new HashSet<>(getRomExtensions());
        exts.addAll(getArchiveExtensions());
        dbHelper = new DatabaseHelper(this);
        SharedPreferences pref = getSharedPreferences("android50comp", Context.MODE_PRIVATE);
        String androidVersion = Build.VERSION.RELEASE;

        if (!pref.getString("androidVersion", "").equals(androidVersion)) {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            dbHelper.onUpgrade(db, Integer.MAX_VALUE - 1, Integer.MAX_VALUE);
            db.close();
            Editor editor = pref.edit();
            editor.putString("androidVersion", androidVersion);
            editor.apply();
            NLog.i(TAG, "Reinit DB " + androidVersion);
        }
        reloadGames = true;
        sharedPreferences = getSharedPreferences("user", MODE_PRIVATE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!FileUtils.isSDCardRWMounted()) {
            showSDCardFailed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (romsFinder != null) {
            romsFinder.stopSearch();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        if (resultCode == RESULT_OK && requestCode == 0) {
            if (romsFinder == null) {
                reloadGames = false;
                DocumentFile dfile = DocumentFile.fromTreeUri(this, data.getData());
                File file = UriUtils.uri2File(dfile.getUri());
                editor.putString("path", file.getAbsolutePath());
                editor.commit();
                romsFinder = new RomsFinder(exts, inZipExts, this, this, reloading, file);
                romsFinder.start();
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    protected void reloadGames(boolean searchNew, File selectedFolder) {
        String path = sharedPreferences.getString("path", "");
        if (!StringUtils.isEmpty(path)) {
            if (romsFinder == null) {
                reloadGames = false;
                File file = new File(path);
                romsFinder = new RomsFinder(exts, inZipExts, this, this, reloading, file);
                romsFinder.start();
            }
            return;
        }

        if (selectedFolder == null) {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            intent.addCategory(Intent.CATEGORY_DEFAULT);
            startActivityForResult(intent, 0);
        }

        if (romsFinder == null) {
            reloading = searchNew;
        }
    }

    @Override
    public void onRomsFinderFoundGamesInCache(ArrayList<GameDescription> oldRoms) {
        setLastGames(oldRoms);
    }

    @Override
    public void onRomsFinderNewGames(ArrayList<GameDescription> roms) {
        setNewGames(roms);
    }

    @Override
    public void onRomsFinderEnd(boolean searchNew) {
        romsFinder = null;
        reloading = false;
    }

    @Override
    public void onRomsFinderCancel(boolean searchNew) {
        romsFinder = null;
        reloading = false;
    }

    protected void stopRomsFinding() {
        if (romsFinder != null) {
            romsFinder.stopSearch();
        }
    }

    public void showSDCardFailed() {
        runOnUiThread(() -> {
            AlertDialog dialog = new Builder(BaseGameGalleryActivity.this)
                    .setTitle(R.string.error)
                    .setMessage(R.string.gallery_sd_card_not_mounted)
                    .setOnCancelListener(dialog1 -> finish())
                    .setPositiveButton(R.string.exit, (dialog1, which) -> finish())
                    .create();
            DialogUtils.show(dialog, true);
        });
    }

    public abstract Class<? extends EmulatorActivity> getEmulatorActivityClass();

    abstract public void setLastGames(ArrayList<GameDescription> games);

    abstract public void setNewGames(ArrayList<GameDescription> games);

    abstract protected Set<String> getRomExtensions();

    public abstract Emulator getEmulatorInstance();

    protected Set<String> getArchiveExtensions() {
        HashSet<String> set = new HashSet<>();
        set.add("zip");
        return set;
    }

}
