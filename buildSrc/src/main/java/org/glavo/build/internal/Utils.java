package org.glavo.build.internal;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;

final class Utils {
    static TarArchiveEntry copyTarEntry(final TarArchiveEntry entry, long newSize) {
        return copyTarEntry(entry, entry.getName(), newSize);
    }

    static TarArchiveEntry copyTarEntry(final TarArchiveEntry entry, String newName, long newSize) {
        var newEntry = new TarArchiveEntry(newName);
        newEntry.setSize(newSize);
        newEntry.setMode(entry.getMode());
        newEntry.setCreationTime(entry.getCreationTime());
        newEntry.setLastAccessTime(entry.getLastAccessTime());
        newEntry.setLastModifiedTime(entry.getLastModifiedTime());
        return newEntry;
    }
}
