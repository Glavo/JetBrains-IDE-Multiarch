package org.glavo.build.transformer;

import kala.function.CheckedFunction;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public sealed interface FileTransformer {

    record Replace(byte[] replacement, @Nullable String targetPath) implements FileTransformer {
        public Replace(byte[] replacement) {
            this(replacement, null);
        }
    }

    record FilterOut() implements FileTransformer {
    }

    record Transform(CheckedFunction<byte[], byte[], IOException> action) implements FileTransformer {
    }
}
