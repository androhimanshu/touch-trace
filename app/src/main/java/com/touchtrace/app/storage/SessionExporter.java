package com.touchtrace.app.storage;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import androidx.core.content.FileProvider;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Builds share intents for single files and zipped bulk exports. */
public final class SessionExporter {

    /** Share a single JSON file (latest gesture). */
    public static Intent shareFileIntent(Context ctx, File file) {
        Uri uri = uriFor(ctx, file);
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("application/json");
        i.putExtra(Intent.EXTRA_STREAM, uri);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        return Intent.createChooser(i, "Export gesture file");
    }

    /**
     * Zips all file*.json files in {@code dir} into {@code touchtrace_export.zip}
     * (placed in the same directory) and returns a share intent.
     * Returns null if there are no gesture files.
     */
    public static Intent shareAllIntent(Context ctx, File dir) throws IOException {
        File[] jsonFiles = dir.listFiles(
                (d, name) -> name.matches("file\\d+\\.json"));
        if (jsonFiles == null || jsonFiles.length == 0) return null;

        // Sort numerically: file1.json, file2.json, …
        Arrays.sort(jsonFiles, Comparator.comparingInt(SessionExporter::indexFromFile));

        File zip = new File(dir, "touchtrace_export.zip");
        try (ZipOutputStream zos = new ZipOutputStream(
                new BufferedOutputStream(new FileOutputStream(zip)))) {
            byte[] buf = new byte[8_192];
            for (File f : jsonFiles) {
                zos.putNextEntry(new ZipEntry(f.getName()));
                try (FileInputStream fis = new FileInputStream(f)) {
                    int len;
                    while ((len = fis.read(buf)) > 0) zos.write(buf, 0, len);
                }
                zos.closeEntry();
            }
        }

        Uri uri = uriFor(ctx, zip);
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("application/zip");
        i.putExtra(Intent.EXTRA_STREAM, uri);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        return Intent.createChooser(i,
                "Export " + jsonFiles.length + " gesture files");
    }

    private static Uri uriFor(Context ctx, File file) {
        return FileProvider.getUriForFile(
                ctx, ctx.getPackageName() + ".fileprovider", file);
    }

    private static int indexFromFile(File f) {
        try {
            return Integer.parseInt(
                    f.getName().replace("file", "").replace(".json", ""));
        } catch (NumberFormatException e) { return 0; }
    }
}
