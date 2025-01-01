/*
 * Copyright 2025 Glavo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.glavo.build.util;

import java.util.ArrayList;
import java.util.List;

public final class OpenHelper {
    private final List<AutoCloseable> allCloseable = new ArrayList<>();

    public <T extends AutoCloseable> T register(T closeable) {
        allCloseable.add(closeable);
        return closeable;
    }

    public void onException(Throwable exception) {
        for (int i = allCloseable.size() - 1; i >= 0; i--) {
            try {
                allCloseable.get(i).close();
            } catch (Throwable otherException) {
                exception.addSuppressed(otherException);
            }
        }
    }

    public void close() throws Exception {
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
