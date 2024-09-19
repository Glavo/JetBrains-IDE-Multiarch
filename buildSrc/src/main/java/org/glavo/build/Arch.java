package org.glavo.build;

import java.util.Locale;

public enum Arch {
    AARCH64,
    RISCV64,
    LOONGARCH64;

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
