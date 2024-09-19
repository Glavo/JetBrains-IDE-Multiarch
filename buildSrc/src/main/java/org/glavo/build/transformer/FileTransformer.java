package org.glavo.build.transformer;

import kala.function.CheckedBiConsumer;
import kala.function.CheckedFunction;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;

import java.io.IOException;
import java.io.OutputStream;

public sealed interface FileTransformer {

    public record Replace(String replacement) implements FileTransformer {
    }

    public record FilterOut() implements FileTransformer {
    }

    public record Transform(
            CheckedFunction<TarArchiveEntry, byte[], IOException> action) implements FileTransformer {
    }
}
