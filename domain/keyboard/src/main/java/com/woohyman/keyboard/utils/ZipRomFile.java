package com.woohyman.keyboard.utils;

import com.woohyman.keyboard.annotations.Column;
import com.woohyman.keyboard.annotations.ObjectFromOtherTable;
import com.woohyman.keyboard.annotations.Table;
import com.woohyman.keyboard.data.database.GameDescription;

import java.io.File;
import java.util.ArrayList;

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
