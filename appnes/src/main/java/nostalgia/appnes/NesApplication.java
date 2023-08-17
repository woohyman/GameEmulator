package nostalgia.appnes;

import com.arialyy.aria.core.Aria;
import com.liulishuo.filedownloader.FileDownloader;

import nostalgia.framework.BaseApplication;
import nostalgia.framework.base.EmulatorHolder;

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
