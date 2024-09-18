package org.glavo.build.util;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.gradle.api.file.RegularFile;

import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

public final class Utils {
    public static Map<String, String> loadProperties(RegularFile file) throws IOException {
        var path = file.getAsFile().toPath();
        if (Files.exists(path)) {
            var properties = new Properties();
            try (var reader = Files.newBufferedReader(path)) {
                properties.load(reader);
            }

            var result = new HashMap<String, String>();
            properties.forEach((key, value) -> result.put(key.toString(), value.toString()));
            return result;
        } else {
            return Collections.emptyMap();
        }
    }

    public static TarArchiveEntry copyTarEntry(final TarArchiveEntry entry, long newSize) {
        return copyTarEntry(entry, entry.getName(), newSize);
    }

    public static TarArchiveEntry copyTarEntry(final TarArchiveEntry entry, String newName, long newSize) {
        var newEntry = new TarArchiveEntry(newName);
        newEntry.setSize(newSize);
        newEntry.setMode(entry.getMode());
        newEntry.setCreationTime(entry.getCreationTime());
        newEntry.setLastAccessTime(entry.getLastAccessTime());
        newEntry.setLastModifiedTime(entry.getLastModifiedTime());
        return newEntry;
    }
}
