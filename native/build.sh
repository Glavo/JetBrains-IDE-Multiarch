#!/usr/bin/env bash

set -e

NATIVE_DIR=$(dirname "$(realpath "$0")")
NATIVE_BUILD_DIR="$NATIVE_DIR/build"
OUTPUT_DIR="$NATIVE_DIR/out"

OS_ARCH=$(uname -m)
TARGET_ARCH=${TARGET_ARCH:-$OS_ARCH}
CC=${CC:-gcc}

rm -rf "$NATIVE_BUILD_DIR"
rm -rf "$OUTPUT_DIR"
mkdir -p "$NATIVE_BUILD_DIR"
mkdir -p "$OUTPUT_DIR"

## fsNotifier
FSNOTIFIER_DIR="$NATIVE_DIR/fsNotifier"
$CC -O2 -Wall -Wextra -Wpedantic -D "VERSION=\"f93937d\"" -std=c11 \
  "$FSNOTIFIER_DIR/main.c" "$FSNOTIFIER_DIR/inotify.c" "$FSNOTIFIER_DIR/util.c" \
  -o "$OUTPUT_DIR/fsnotifier"

## restarter
cargo build --release --manifest-path="$NATIVE_DIR/restarter/Cargo.toml"
cp "$NATIVE_DIR/restarter/target/release/restarter" "$OUTPUT_DIR/restarter"

## repair-utility
# go build -C repair-utility -o "$OUTPUT_DIR/repair"

## XPlatLauncher
cargo build --release --manifest-path="$NATIVE_DIR/XPlatLauncher/Cargo.toml"
cp "$NATIVE_DIR/XPlatLauncher/target/release/xplat-launcher" "$OUTPUT_DIR/xplat-launcher"

## pty4j
PTY4J_DIR="$NATIVE_DIR/pty4j"
$CC -shared -o "$OUTPUT_DIR/libpty.so" -fPIC -D_REENTRANT -D_GNU_SOURCE -I "$PTY4J_DIR" \
  "$PTY4J_DIR/exec_pty.c" \
  "$PTY4J_DIR/openpty.c" \
  "$PTY4J_DIR/pfind.c"

## package
NATIVES_ZIP="natives-linux-$TARGET_ARCH.zip"
rm -f "$NATIVES_ZIP"
zip -j "$NATIVES_ZIP" "$OUTPUT_DIR"/*
echo "All files are packaged into $(realpath "$NATIVES_ZIP")."
