#!/usr/bin/env bash

set -e

NATIVE_DIR=$(dirname "$(realpath "$0")")
NATIVE_BUILD_DIR="$NATIVE_DIR/build"
PROJECT_ROOT=$(dirname "${NATIVE_DIR}")
PROJECT_BUILD_DIR="$PROJECT_ROOT/build"
OUTPUT_DIR="$NATIVE_BUILD_DIR/libs"

OS_ARCH=$(uname -m)

CC=${CC:-gcc}

rm -rf "$NATIVE_BUILD_DIR"
mkdir -p "$NATIVE_BUILD_DIR"
mkdir -p "$OUTPUT_DIR"

## libdbusmenu
LIBDBUSMENU_DIR="$NATIVE_BUILD_DIR/libdbusmenu"
git clone https://github.com/JetBrains/libdbusmenu.git "$LIBDBUSMENU_DIR"
cd "$LIBDBUSMENU_DIR"
git checkout 38d7a2ada4b2a08c535491d43a39825868f2b065
./configure --build "$OS_ARCH-unknown-linux-gnu" --target "$OS_ARCH-unknown-linux-gnu"
cd "$LIBDBUSMENU_DIR/libdbusmenu-glib"
make
cp "$LIBDBUSMENU_DIR/libdbusmenu-glib/.libs/libdbusmenu-glib.a" "$NATIVE_DIR/LinuxGlobalMenu/"
cd "$NATIVE_DIR"

## LinuxGlobalMenu
cmake -DCMAKE_BUILD_TYPE=Release -S "$NATIVE_DIR/LinuxGlobalMenu/" -B "$NATIVE_BUILD_DIR/LinuxGlobalMenu/"
cmake --build
cp "$NATIVE_BUILD_DIR/LinuxGlobalMenu/libdbm.so" "$OUTPUT_DIR/libdbm.so"

## fsNotifier
$CC -O2 -Wall -Wextra -Wpedantic -D "VERSION=\"f93937d\"" -std=c11 \
  "$NATIVE_DIR/main.c" "$NATIVE_DIR/inotify.c" "$NATIVE_DIR/util.c" \
  -o "$OUTPUT_DIR/fsnotifier"
