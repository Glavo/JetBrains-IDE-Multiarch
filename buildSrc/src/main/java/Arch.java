import java.util.Locale;

public enum Arch {
    AARCH64,
    RISCV64,
    LOONGARCH64;

    public String getName() {
        return this.name().toLowerCase(Locale.ROOT);
    }
}
