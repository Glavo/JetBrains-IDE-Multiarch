package org.glavo.build.tasks;

import org.glavo.build.Arch;
import org.glavo.build.util.IOBuffer;
import org.glavo.build.util.Utils;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public abstract class BuildNative extends DefaultTask {

    private static final Logger LOGGER = Logging.getLogger(BuildNative.class);

    @Input
    @Optional
    public abstract Property<Arch> getArch();

    @Input
    @Optional
    public abstract Property<String> getCC();

    @Input
    @Optional
    public abstract Property<String> getCXX();

    @Input
    @Optional
    public abstract Property<String> getCMake();

    @Input
    @Optional
    public abstract Property<String> getGo();

    @Input
    @Optional
    public abstract Property<String> getCargo();

    @InputDirectory
    public abstract RegularFileProperty getNativeProjectsRoot();

    @OutputFile
    public abstract RegularFileProperty getOutputFile();

    @TaskAction
    public void run() throws IOException {
        Utils.ensureLinux();

        Arch targetArch = getArch().getOrElse(Arch.current());
        Arch osArch = Arch.current();

        String cc = getCC().getOrElse("gcc");
        String cxx = getCXX().getOrElse("g++");
        String cmake = getCMake().getOrElse("cmake");
        String go = getGo().getOrElse("go");
        String cargo = getCargo().getOrElse("cargo");

        Path nativeRoot = Utils.getAsPath(getNativeProjectsRoot()).toAbsolutePath();
        Path buildDir = nativeRoot.resolve("build");

        Utils.deleteDirectory(buildDir);
        Files.createDirectories(buildDir);

        var builder = new ActionsBuilder()
                .env("CC", cc)
                .env("CXX", cxx);

        // LinuxGlobalMenu
        Path linuxGlobalMenuDir = nativeRoot.resolve("LinuxGlobalMenu");
        Path linuxGlobalMenuBuildDir = buildDir.resolve("LinuxGlobalMenu");

        Path libdbusmenuDir = nativeRoot.resolve("libdbusmenu");
        Path libdbusmenuGlibDir = libdbusmenuDir.resolve("libdbusmenu-glib");
        builder.exec("bash", "./configure",
                        "--build=" + triple(osArch),
                        "--host=" + triple(targetArch))
                .working(libdbusmenuDir);
        builder.exec("make")
                .working(libdbusmenuGlibDir);
        builder.copy(
                libdbusmenuGlibDir.resolve(".libs/libdbusmenu-glib.a"),
                linuxGlobalMenuDir.resolve("libdbusmenu-glib.a")
        );

        builder.exec(cmake, "-DCMAKE_BUILD_TYPE=Release",
                "-S", linuxGlobalMenuDir,
                "-B", linuxGlobalMenuBuildDir
        );
        builder.exec(cmake, "--build", linuxGlobalMenuBuildDir);
        builder.addResult(linuxGlobalMenuBuildDir.resolve("libdbm.so"));

        // fsNotifier
        Path fsNotifierDir = nativeRoot.resolve("fsNotifier");
        Path fsNotifierTargetFile = buildDir.resolve("fsnotifier");
        builder.exec(cc, "-O2", "-Wall", "-Wextra", "-Wpedantic",
                "-std=c11",
                "-DVERSION=\"f93937d\"",
                fsNotifierDir.resolve("main.c"), fsNotifierDir.resolve("inotify.c"), fsNotifierDir.resolve("util.c"),
                "-o", fsNotifierTargetFile
        );
        builder.addResult(fsNotifierTargetFile);

        // restarter
        Path restarterDir = nativeRoot.resolve("restarter");
        builder.exec("cargo", "build", "--target=" + triple(targetArch), "--release",
                "--manifest-path=" + restarterDir.resolve("Cargo.toml"));
        builder.addResult(restarterDir.resolve("target/release/restarter"));

        // repair-utility
        Path repairUtilityDir = nativeRoot.resolve("repair-utility");
        Path repairUtilityFile = buildDir.resolve("repair");
        builder.exec(go, "build", "-C", repairUtilityDir, "-o", repairUtilityFile)
                .env("GOOS", "linux")
                .env("GOARCH", targetArch.getGoArch());
        builder.addResult(repairUtilityFile);

        // XPlatLauncher
        Path xplatLauncherDir = nativeRoot.resolve("XPlatLauncher");
        builder.exec(cargo, "build", "--release", "--target=" + triple(targetArch), "--release",
                "--manifest-path=" + xplatLauncherDir.resolve("Cargo.toml"));
        builder.addResult(xplatLauncherDir.resolve("target/release/xplat-launcher"));

        // pty4j
        Path pty4jDir = nativeRoot.resolve("pty4j");
        Path pty4jFile = buildDir.resolve("libpty.so");
        builder.exec(cc, "-shared", "-o", pty4jFile, "-fPIC", "-D_REENTRANT", "-D_GNU_SOURCE",
                "-I", pty4jDir,
                pty4jDir.resolve("exec_pty.c"),
                pty4jDir.resolve("openpty.c"),
                pty4jDir.resolve("pfind.c")
        );

        // ---
        Path nativesZipFile = Utils.getAsPath(getOutputFile());
        Files.createDirectories(nativesZipFile.getParent());
        var buffer = new IOBuffer();

        try (var out = new ZipOutputStream(Files.newOutputStream(nativesZipFile, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE))) {
            for (Action action : builder.actions) {
                switch (action) {
                    case Action.Exec exec -> {
                        LOGGER.lifecycle("Exec: " + exec.commands);

                        ProcessBuilder processBuilder = new ProcessBuilder(exec.commands);
                        processBuilder.inheritIO();

                        if (exec.workingDir != null) {
                            processBuilder.directory(exec.workingDir.toFile());
                        }

                        processBuilder.environment().putAll(builder.env);
                        if (exec.env != null) {
                            processBuilder.environment().putAll(exec.env);
                        }

                        Process process = processBuilder.start();
                        try {
                            process.waitFor();
                        } catch (InterruptedException e) {
                            process.destroy();
                            throw new GradleException("Unexpected interrupted exception", e);
                        }
                    }
                    case Action.Copy copy -> {
                        LOGGER.lifecycle("Copy {} to {}", copy.source, copy.target);
                        Files.copy(copy.source, copy.target);
                    }
                    case Action.AddResult addResult -> {
                        LOGGER.lifecycle("Add {} to result", addResult.file);
                        out.putNextEntry(new ZipEntry(addResult.name));
                        try (var input = Files.newInputStream(addResult.file)) {
                            buffer.copy(input, out);
                        }
                    }
                }
            }
        } catch (Throwable e) {
            getOutputFile().get().getAsFile().delete();
            throw e;
        }
    }

    private static String triple(Arch arch) {
        return arch.normalize() + "-unknown-linux-gnu";
    }

    private sealed interface Action {
        final class Exec implements Action {
            final List<String> commands = new ArrayList<>();
            Map<String, String> env;
            Path workingDir;

            Exec working(Path workingDir) {
                this.workingDir = workingDir;
                return this;
            }

            Exec env(String key, String value) {
                if (env == null) {
                    env = new HashMap<>();
                }

                env.put(key, value);
                return this;
            }
        }

        record Copy(Path source, Path target) implements Action {

        }

        record AddResult(String name, Path file) implements Action {
        }
    }

    private static final class ActionsBuilder {
        final List<Action> actions = new ArrayList<>();
        final Map<String, String> env = new HashMap<>();

        ActionsBuilder env(String key, String value) {
            env.put(key, value);
            return this;
        }

        Action.Exec exec(Object... commands) {
            Action.Exec exec = new Action.Exec();
            for (Object command : commands) {
                exec.commands.add(command.toString());
            }
            this.actions.add(exec);
            return exec;
        }

        void copy(Path source, Path target) {
            var copy = new Action.Copy(source, target);
            this.actions.add(copy);
        }

        void addResult(Path file) {
            var addResult = new Action.AddResult(file.getFileName().toString(), file);
            this.actions.add(addResult);
        }
    }
}
