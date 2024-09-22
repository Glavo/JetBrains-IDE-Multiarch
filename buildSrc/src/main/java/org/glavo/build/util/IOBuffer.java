package org.glavo.build.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class IOBuffer {
    private static final int BUFFER_SIZE = 32 * 1024;

    private final byte[] buffer = new byte[BUFFER_SIZE];

    public void copy(InputStream input, OutputStream output) throws IOException {
        int read;
        while ((read = input.read(buffer, 0, BUFFER_SIZE)) >= 0) {
            output.write(buffer, 0, read);
        }
    }
}
