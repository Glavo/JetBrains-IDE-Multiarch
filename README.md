# JetBrains IDEs for Linux LoongArch64/RISC-V 64

This project provides JetBrains IDEs distributions for more architectures.

Currently supported platforms:

* Linux RISC-V 64
* Linux LoongArch64 (New World)


> [!NOTE]
> The Linux LoongArch64 platform has two incompatible ABIs ("Old World" and "New World").
> This project only provides support for the "New World".
> 
> See [this document](https://areweloongyet.com/en/docs/old-and-new-worlds/) for more details.

## Download the pre-built IDEs

We provide pre-built distributions for the IDEs that can be redistributed.

|                                 | RISC-V 64                                                           | LoongArch64                                                               |
|---------------------------------|---------------------------------------------------------------------|---------------------------------------------------------------------------|
| IntelliJ IDEA Community Edition | [2024.3.5+0](https://github.com/Glavo/JetBrains-IDE-Multiarch/releases/tag/idea/2024.3.5+0)   | [2024.2.1+0](https://github.com/Glavo/JetBrains-IDE-Multiarch/releases/tag/idea/2024.2.1+0) |
| PyCharm Community               | [2024.3.5+0](https://github.com/Glavo/JetBrains-IDE-Multiarch/releases/tag/pycharm/2024.3.5+0) | /                                                                         |

As for other IDEs, we are not allowed to redistribute them.
Please [build them yourself](#Building).

## Building

The work of this project is to download the official IDE distribution,
patch the IDE with self-built native binaries and generate distributions for the target platform.

The scripts that do this are based on Gradle and require OpenJDK (>= 21) to run.


With OpenJDK installed and the `JAVA_HOME` environment variable set,
run the following command replacing `$PRODUCT_CODE` with the product code of the IDE you want to build
and `$ARCH` with the simple name of the target architecture:

```
./gradlew transform$PRODUCT_CODE-$ARCH
```

`$PRODUCT_CODE` for IDEs:

* IntelliJ IDEA Community Edition: `IC`
* IntelliJ IDEA Ultimate: `IU`
* PyCharm Community: `PC`
* PyCharm Professional: `PY`
* Goland: `GO`

`$ARCH` for architectures:

* RISC-V 64: `riscv64`
* LoongArch64: `loongarch64`

The IDE distribution will be built into `./build/target/`.

### Build native binaries

We have pre-built native binaries for some platforms.
By default, the project downloads these prebuilt binaries and patches the IDE with them.
You can also build them yourself.

Building native binaries requires:

* GCC
* Cargo (>= 1.82.0)

This project supports cross-compiling native binaries.
To cross-compile binaries, you need to have the GCC Cross-Compiler for your target platform installed and Cargo configured for that.

Running `./gradlew buildNatives-$ARCH` builds native binaries for the target platform,
the built native binaries will be packaged into the file `./resources/natives-linux-$ARCH.zip`.
When this file exists, the script will use it first to patch the IDE instead of downloading the prebuilt binary.

### Customizing the bundled Java runtime

JetBrains IDEs require a Java runtime environment to run.

We have pre-built JDKs for some platforms.
By default, this project downloads these pre-built JDKs and bundles them into the IDE distribution.
You can replace the default Java runtime environment via the project properties:

```
./gradlew transform$PRODUCT_CODE-$ARCH -Pjdk.linux.$ARCH.url="..."
```

The URL should point to a JDK distribution in `.tar.gz` or `.zip` format.

If you want to build the JDK yourself,
we recommend using the [JetBrains Runtime](https://github.com/JetBrains/JetBrainsRuntime) for the best experience.

For Linux LoongArch64, since OpenJDK mainline does not provide full support for LoongArch64,
it is recommended to build based on [loongson/jdk21u](https://github.com/loongson/jdk21u).

## To-do list

* Using JBR on Linux LoongArch64.

## Especially thanks

<img alt="PLCT Logo" src="./PLCT.svg" width="200" height="200">

Thanks to [PLCT Lab](https://plctlab.org) for supporting me.
