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
package org.glavo.build.transformer;

import kala.function.CheckedFunction;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public sealed interface FileTransformer {

    default boolean optional() {
        return false;
    }

    record Replace(byte[] replacement, @Nullable String targetPath) implements FileTransformer {
    }

    record FilterOut(boolean optional) implements FileTransformer {
        public FilterOut() {
            this(false);
        }
    }

    record Transform(CheckedFunction<byte[], byte[], IOException> action) implements FileTransformer {
    }
}
