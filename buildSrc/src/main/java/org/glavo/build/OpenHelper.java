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
}
