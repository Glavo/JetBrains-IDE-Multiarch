package org.glavo.build.util;

import com.sun.jna.Platform;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.gradle.api.GradleException;
import org.gradle.api.file.RegularFile;
import org.gradle.api.file.RegularFileProperty;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

public final class Utils {
    public static void ensureLinux() {
        if (!Platform.isLinux()) {
            throw new GradleException("This task should only be run on Linux");
        }
    }

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

    public static Path getAsPath(RegularFileProperty property) {
        return property.get().getAsFile().toPath();
    }

    public static void deleteDirectory(Path path) throws IOException {
        if (Files.exists(path)) {
            Files.walkFileTree(path, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc)
                        throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
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
