/*
 * Copyright 2025 Glavo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.glavo.build.tasks;

import kala.template.TemplateEngine;
import org.apache.commons.compress.archivers.ar.ArArchiveEntry;
import org.apache.commons.compress.archivers.ar.ArArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.glavo.build.Arch;
import org.glavo.build.Product;
import org.glavo.build.util.Utils;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public abstract class CreateDeb extends DefaultTask {
    public static final Logger LOGGER = Logging.getLogger(CreateDeb.class);

    @Input
    public abstract Property<String> getVersion();

    @Input
    public abstract Property<Arch> getIDETargetArch();

    @Input
    public abstract Property<Product> getIDEProduct();

    @InputFile
    public abstract RegularFileProperty getTarFile();

    @OutputFile
    public abstract RegularFileProperty getOutputFile();

    @InputDirectory
    public abstract RegularFileProperty getConfigDir();

    private static void makeDirectories(Set<String> directories, TarArchiveOutputStream output, String dirName) throws IOException {
        assert dirName.endsWith("/");

        if (directories.contains(dirName))
            return;

        if (dirName.length() >= 2) {
            int idx = dirName.lastIndexOf('/', dirName.length() - 2);

            if (idx > 0) {
                String parentName = dirName.substring(0, idx + 1);
                makeDirectories(directories, output, parentName);
            }
        }

        directories.add(dirName);
        output.putArchiveEntry(new TarArchiveEntry(dirName));
        output.closeArchiveEntry();
    }

    @TaskAction
    public void run() throws IOException {
        Path configDir = getConfigDir().get().getAsFile().toPath();
        Product product = getIDEProduct().get();

        var properties = Map.of(
                "version", getVersion().get(),
                "arch", getIDETargetArch().get().getDebArch()
        );

        LOGGER.lifecycle("Creating control.tar.gz");

        ByteArrayOutputStream controlData = new ByteArrayOutputStream();
        try (var output = new TarArchiveOutputStream(new GZIPOutputStream(controlData));
             Stream<Path> stream = Files.list(configDir.resolve("control"))) {
            for (Path path : stream.toList()) {
                String fileName = path.getFileName().toString();
                String content = Files.readString(path);

                if (fileName.endsWith(".template")) {
                    fileName = fileName.substring(0, fileName.length() - ".template".length());
                    content = TemplateEngine.getDefault().process(Files.readString(path), properties);
                }

                byte[] contentBytes = content.getBytes();

                TarArchiveEntry entry = new TarArchiveEntry("./" + fileName);
                entry.setSize(contentBytes.length);
                output.putArchiveEntry(entry);
                output.write(contentBytes);
                output.closeArchiveEntry();
            }
        }

        Path outputFile = getOutputFile().get().getAsFile().toPath();
        Files.createDirectories(outputFile.getParent());

        Path tempDataTar = Files.createTempFile("data-", ".tar.gz");

        LOGGER.lifecycle("Creating data.tar.gz");
        try (var output = new TarArchiveOutputStream(new GZIPOutputStream(Files.newOutputStream(tempDataTar,
                StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)))) {
            output.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);

            Set<String> directories = new HashSet<>();

            try (var input = new TarArchiveInputStream(new GZIPInputStream(new FileInputStream(getTarFile().get().getAsFile())))) {
                String prefix = null;

                TarArchiveEntry entry;
                while ((entry = input.getNextEntry()) != null) {
                    String name = entry.getName();
                    if (name.endsWith("/"))
                        continue;

                    int idx = name.indexOf('/');
                    if (idx <= 0) {
                        throw new IOException("Invalid entry: " + name);
                    }

                    if (prefix == null) {
                        prefix = name.substring(0, idx);
                    } else if (!name.regionMatches(0, prefix, 0, idx)) {
                        throw new IOException("Invalid entry: " + name);
                    }

                    String targetFileName = "./usr/share/jetbrains-multiarch/" + product.getFileNamePrefix() + "/"
                                            + name.substring(idx + 1);

                    makeDirectories(directories, output, targetFileName.substring(0, targetFileName.lastIndexOf('/') + 1));

                    output.putArchiveEntry(Utils.copyTarEntry(entry, targetFileName, entry.getSize()));
                    input.transferTo(output);
                    output.closeArchiveEntry();
                }

                {
                    Path launcherFile = configDir.resolve("launcher.sh");
                    entry = new TarArchiveEntry("./usr/bin/" + product.getLauncherName());
                    entry.setSize(Files.size(launcherFile));
                    entry.setMode(0x81ed);

                    output.putArchiveEntry(entry);
                    Files.copy(launcherFile, output);
                    output.closeArchiveEntry();
                }

                {
                    Path desktopTemplate = configDir.resolve("desktop.template");
                    byte[] processed = TemplateEngine.getDefault().process(Files.readString(desktopTemplate), properties).getBytes();

                    entry = new TarArchiveEntry("./usr/share/applications/org.glavo.jetbrains." + product.getProductCode().toLowerCase(Locale.ROOT) + ".desktop");
                    entry.setSize(processed.length);
                    output.putArchiveEntry(entry);
                    output.write(processed);
                    output.closeArchiveEntry();
                }
            }
        } catch (Throwable e) {
            try {
                Files.deleteIfExists(tempDataTar);
            } catch (Throwable e2) {
                e.addSuppressed(e2);
            }
            throw e;
        }


        LOGGER.lifecycle("Creating deb file");
        try (var output = new ArArchiveOutputStream(Files.newOutputStream(outputFile,
                StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE))) {
            {
                byte[] bytes = "2.0\n".getBytes(StandardCharsets.UTF_8);
                ArArchiveEntry entry = new ArArchiveEntry("debian-binary", bytes.length);
                output.putArchiveEntry(entry);
                output.write(bytes);
                output.closeArchiveEntry();
            }

            {
                ArArchiveEntry entry = new ArArchiveEntry("control.tar.gz", controlData.size());
                output.putArchiveEntry(entry);
                controlData.writeTo(output);
                output.closeArchiveEntry();
            }

            {
                ArArchiveEntry entry = new ArArchiveEntry(tempDataTar, "data.tar.gz");
                output.putArchiveEntry(entry);
                Files.copy(tempDataTar, output);
                output.closeArchiveEntry();
            }
        } finally {
            Files.deleteIfExists(tempDataTar);
        }
    }
}
