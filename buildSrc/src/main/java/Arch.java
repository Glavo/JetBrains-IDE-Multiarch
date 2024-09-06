import java.util.Locale;

public enum Arch {
    RISCV64,
    LOONGARCH64;

    public String getName() {
        return this.name().toLowerCase(Locale.ROOT);
    }
}
