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
package org.glavo.build;

import com.sun.jna.Platform;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public enum Arch {
    X86_64,
    AARCH64,
    RISCV64,
    LOONGARCH64,
    UNKNOWN;

    private final String normalizedName = name().toLowerCase(Locale.ROOT);

    public static Arch current() {
        return switch (Platform.ARCH) {
            case "x86-64" -> X86_64;
            case "aarch64" -> AARCH64;
            case "riscv64" -> RISCV64;
            case "loongarch64" -> LOONGARCH64;
            default -> UNKNOWN;
        };
    }

    public static Arch of(String name) {
        for (Arch arch : Arch.values()) {
            if (arch.normalize().equals(name)) {
                return arch;
            }
        }

        throw new IllegalArgumentException("Invalid architecture: " + name);
    }

    public String normalize() {
        return normalizedName;
    }

    public String getGoArch() {
        return switch (this) {
            case X86_64 -> "amd64";
            case AARCH64 -> "arm64";
            case LOONGARCH64 -> "loong64";
            default -> normalize();
        };
    }

    public String getTriple(@Nullable String vendor) {
        return "%s%s-linux-gnu".formatted(normalize(), vendor == null ? "" : "-" + vendor);
    }

    public String getRustTriple() {
        //noinspection SwitchStatementWithTooFewBranches
        return switch (this) {
            case RISCV64 -> "riscv64gc-unknown-linux-gnu";
            default -> normalize().replace('-', '_') + "-unknown-linux-gnu";
        };
    }

    @Override
    public String toString() {
        return normalize();
    }
}
