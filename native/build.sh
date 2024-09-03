#!/usr/bin/env bash

set -e

NATIVE_DIR=$(dirname "$(realpath "$0")")
NATIVE_BUILD_DIR="$NATIVE_DIR/build"
PROJECT_ROOT=$(dirname "${NATIVE_DIR}")
PROJECT_BUILD_DIR="$PROJECT_ROOT/build"

OS_ARCH=$(uname -m)

mkdir -p "$NATIVE_BUILD_DIR"

## libdbusmenu
LIBDBUSMENU_DIR="$NATIVE_BUILD_DIR/libdbusmenu"
rm -rf "$LIBDBUSMENU_DIR"
git clone https://github.com/JetBrains/libdbusmenu.git "$LIBDBUSMENU_DIR"
cd "$LIBDBUSMENU_DIR"
git checkout 38d7a2ada4b2a08c535491d43a39825868f2b065
./configure --build "$OS_ARCH-unknown-linux-gnu" --target "$OS_ARCH-unknown-linux-gnu"
cd "$LIBDBUSMENU_DIR/libdbusmenu-glib"
make
cd "$NATIVE_DIR"
