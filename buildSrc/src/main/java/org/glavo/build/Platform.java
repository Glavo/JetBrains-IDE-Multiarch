package org.glavo.build;

public enum Platform {
    LINUX_LOONGARCH64("linux", "loongarch64"),
    LINUX_RISCV64("linux", "riscv64");

    private final String os;
    private final String arch;

    Platform(String os, String arch) {
        this.os = os;
        this.arch = arch;
    }

    public String getOS() {
        return os;
    }

    public String getArch() {
        return arch;
    }
}
