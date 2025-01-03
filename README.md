# JetBrains IDEs for Linux LoongArch64/RISC-V 64

This project provides JetBrains IDEs distributions for more architectures.

Currently supported platforms:

* Linux RISC-V 64
* Linux LoongArch64 (New World)

## Download the pre-built IDEs

We provide pre-built distributions for the IDEs that can be redistributed.

|                                 | RISC-V 64                                                               | LoongArch64                                                               |
|---------------------------------|-------------------------------------------------------------------------|---------------------------------------------------------------------------|
| IntelliJ IDEA Community Edition | [2024.3.1.1+0](https://github.com/Glavo/JetBrains-IDE-Multiarch/releases/download/idea%2F2024.3.1.1%2B0/ideaIC-2024.3.1.1+0-riscv64.tar.gz)       | [2024.2.1+0](https://github.com/Glavo/JetBrains-IDE-Multiarch/releases/download/idea%2F2024.2.1%2B0/ideaIC-2024.2.1+0-loongarch64.tar.gz) |
| PyCharm Community               | [2024.3.1.1+0](https://github.com/Glavo/JetBrains-IDE-Multiarch/releases/download/pycharm%2F2024.3.1.1%2B0/pycharm-community-2024.3.1.1+0-riscv64.tar.gz) | /                                                                         |

As for other IDEs, we are not allowed to redistribute them.
Please [build them yourself](#Building).

## Building

Environmental requirements:

* OpenJDK (>= 21)

We have pre-built native libraries for the Linux RISC-V 64 platform, which are automatically downloaded when building.
If you want to build for other platforms, you will also need the following dependencies:

* GCC
* cargo (>= 1.82.0) 
* Go (>= 1.20)

If you want to cross-compile this project for another platform, 
make sure you have the cross compiler for the target platform installed and have configured cargo correctly.

Then run the following command replacing `$PRODUCT_CODE` with the product code of the IDE you want to build 
and `$ARCH` with the simple name of the target architecture:

```shell
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

## To-do list

* Using JBR on Linux LoongArch64.

## Especially thanks

<img alt="PLCT Logo" src="./PLCT.svg" width="200" height="200">

Thanks to [PLCT Lab](https://plctlab.org) for supporting me.
