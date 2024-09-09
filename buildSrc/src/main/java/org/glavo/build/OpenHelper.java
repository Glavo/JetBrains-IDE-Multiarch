package org.glavo.build;

import java.util.ArrayList;
import java.util.List;

final class OpenHelper {
    private final List<AutoCloseable> allCloseable = new ArrayList<>();

    <T extends AutoCloseable> T register(T closeable) {
        allCloseable.add(closeable);
        return closeable;
    }

    void onException(Throwable exception) {
        for (int i = allCloseable.size() - 1; i >= 0; i--) {
            try {
                allCloseable.get(i).close();
            } catch (Throwable otherException) {
                exception.addSuppressed(otherException);
            }
        }
    }

    void close() throws Exception {
        List<Throwable> exceptions = null;
        for (int i = allCloseable.size() - 1; i >= 0; i--) {
            try {
                allCloseable.get(i).close();
            } catch (Throwable otherException) {
                if (exceptions == null)
                    exceptions = new ArrayList<>();

                exceptions.add(otherException);
            }
        }

        if (exceptions == null) {
            return;
        }

        if (exceptions.size() == 1 && exceptions.getFirst() instanceof Exception exception) {
            throw exception;
        }

        Exception exception = new Exception("Some exceptions were thrown when closing closeables");
        for (Throwable e : exceptions) {
            exception.addSuppressed(e);
        }
        throw exception;
    }
}
