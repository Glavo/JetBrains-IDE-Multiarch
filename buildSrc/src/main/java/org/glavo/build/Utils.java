package org.glavo.build;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;

final class Utils {
    static TarArchiveEntry copyTarEntry(final TarArchiveEntry entry, int newSize) {
        var newEntry = new TarArchiveEntry(entry.getName());
        newEntry.setSize(newSize);
        newEntry.setMode(entry.getMode());
        newEntry.setCreationTime(entry.getCreationTime());
        return newEntry;
    }
}
