package org.glavo.build;

import com.sun.jna.Platform;

import java.util.Locale;

public enum Arch {
    X86_64,
    AARCH64,
    RISCV64,
    LOONGARCH64;

    public static Arch current() {
        return switch (Platform.ARCH) {
            case "x86-64" -> X86_64;
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

    public String getGoArch() {
        return switch (this) {
            case X86_64 -> "amd64";
            case AARCH64 -> "arm64";
            case LOONGARCH64 -> "loong64";
            default -> normalize();
        };
    }

    public String getTriple(String vendor) {
        return "%s%s-linux-gnu".formatted(normalize(), vendor == null ? "" : "-" + vendor);
    }

    public String getRustTriple() {
        return switch (this) {
            case RISCV64 -> "riscv64gc-unknown-linux-gnu";
            default -> normalize().replace('-', '_') + "-unknown-linux-gnu";
        };
    }
}
