package org.glavo.build;

import com.sun.jna.Native;
import com.sun.jna.Platform;

import java.util.Locale;

public enum Arch {
    AARCH64,
    RISCV64,
    LOONGARCH64;

    public static Arch current() {
        return switch (Platform.ARCH) {
            case "aarch64" -> AARCH64;
            case "riscv64" -> RISCV64;
            case "loongarch64" -> LOONGARCH64;
            default -> null;
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
        return this.name().toLowerCase(Locale.ROOT);
    }
}
