package org.glavo.build;

import java.util.Locale;

public enum Arch {
    AARCH64,
    RISCV64,
    LOONGARCH64;

    public String normalize() {
        return this.name().toLowerCase(Locale.ROOT);
    }
}
