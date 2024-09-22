package org.glavo.build;

import com.sun.jna.Platform;
import kala.value.LazyValue;

import java.util.Locale;

public enum Arch {
    X86_64,
    AARCH64,
    RISCV64,
    LOONGARCH64;

    private final LazyValue<String> normalizedName = LazyValue.of(() -> switch (this) {
        case X86_64 -> "x86-64";
        default -> this.name().toLowerCase(Locale.ROOT);
    });

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
        return normalizedName.get();
    }

    public String getGoArch() {
        return switch (this) {
            case X86_64 -> "amd64";
            case AARCH64 -> "arm64";
            case LOONGARCH64 -> "loong64";
            default -> normalize();
        };
    }
}
