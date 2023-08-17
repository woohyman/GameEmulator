package nostalgia.framework.ui.gamegallery;

import java.io.File;
import java.util.ArrayList;

import nostalgia.framework.data.database.GameDescription;
import nostalgia.framework.annotations.Column;
import nostalgia.framework.annotations.ObjectFromOtherTable;
import nostalgia.framework.annotations.Table;

@Table
public class ZipRomFile {

    @Column(isPrimaryKey = true)
    public long _id;

    @Column
    public String hash;

    @Column
    public String path;

    @ObjectFromOtherTable(columnName = "zipfile_id")
    public ArrayList<GameDescription> games = new ArrayList<>();

    public ZipRomFile() {
    }

    public static String computeZipHash(File zipFile) {
        return zipFile.getAbsolutePath().concat("-" + zipFile.length()).hashCode() + "";
    }
}
