// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

use {
    anyhow::{bail, Context, Result},
    std::env,
    std::path::{Path, PathBuf},
    winresource::WindowsResource,
};

macro_rules! cargo {
    ($($arg:tt)*) => {
        println!("cargo:{}", format_args!($($arg)*));
    };
}

fn main() {
    cargo!("rerun-if-changed=build.rs");

    if env::var("CARGO_CFG_TARGET_OS").unwrap() == "windows" {
        embed_metadata().expect("Failed to embed metadata");
    }
}

fn embed_metadata() -> Result<()> {
    assert_eq!(env::var("CARGO_CFG_TARGET_OS")?, "windows");

    let cargo_root_env_var = env::var("CARGO_MANIFEST_DIR")?;
    let cargo_root = PathBuf::from(cargo_root_env_var);

    let manifest_relative_path = "resources/windows/WinLauncher.manifest";
    assert_exists_and_file(&cargo_root.join(manifest_relative_path))?;
    cargo!("rerun-if-changed={manifest_relative_path}");

    let icon_relative_path = "resources/windows/WinLauncher.ico";
    assert_exists_and_file(&cargo_root.join(icon_relative_path))?;

    let mut res = WindowsResource::new();
    res.set_manifest_file(manifest_relative_path);
    res.set_icon_with_id(icon_relative_path, "2000");  // see `resources/windows/resource.h`
    res.compile().context("Failed to embed resources")
}

fn assert_exists_and_file(path: &Path) -> Result<()> {
    if !path.exists() {
        bail!("File '{path:?}' does not exist")
    }
    if !path.is_file() {
        bail!("'{path:?}' is not a file")
    }

    Ok(())
}
