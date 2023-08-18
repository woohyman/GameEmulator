package nostalgia.appgg;

import java.util.HashSet;
import java.util.Set;

import nostalgia.framework.emulator.Emulator;
import nostalgia.framework.base.EmulatorActivity;
import nostalgia.framework.ui.gamegallery.GalleryActivity;

public class GGGalleryActivity extends GalleryActivity {

    @Override
    public Emulator getEmulatorInstance() {
        return GGEmulator.getInstance();
    }

    @Override
    public Class<? extends EmulatorActivity> getEmulatorActivityClass() {
        return GGEmulatorActivity.class;
    }

    @Override
    protected Set<String> getRomExtensions() {
        HashSet<String> set = new HashSet<>();
        set.add("gg");
        return set;
    }
}
