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

import org.apache.commons.compress.archivers.ar.ArArchiveEntry;
import org.apache.commons.compress.archivers.ar.ArArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.glavo.build.Product;
import org.glavo.build.util.Utils;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public abstract class CreateDeb extends DefaultTask {
    public static final Logger LOGGER = Logging.getLogger(CreateDeb.class);

    @Input
    public abstract Property<String> getVersion();

    @Input
    public abstract Property<String> getDebArch();

    @Input
    public abstract Property<Product> getIDEProduct();

    @InputFile
    public abstract RegularFileProperty getTarFile();

    @OutputFile
    public abstract RegularFileProperty getOutputFile();

    private String getInstallPath() {
        return "/usr/share/jetbrains-multiarch/" + getIDEProduct().get().getPackageName();
    }

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

    private static void putEntry(TarArchiveOutputStream output, String name, String content) throws IOException {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        TarArchiveEntry entry = new TarArchiveEntry(name);
        entry.setSize(bytes.length);
        output.putArchiveEntry(entry);
        output.write(bytes);
        output.closeArchiveEntry();
    }

    @TaskAction
    public void run() throws IOException {
        Product product = getIDEProduct().get();

        LOGGER.lifecycle("Creating control.tar.gz");

        ByteArrayOutputStream controlData = new ByteArrayOutputStream();
        try (var output = new TarArchiveOutputStream(new GZIPOutputStream(controlData))) {
            putEntry(output, "./control", getControl());
            putEntry(output, "./postinst", getPostinst());
            putEntry(output, "./prerm", getPrerm());
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

                    String targetFileName = ".%s/%s".formatted(getInstallPath(), name.substring(idx + 1));

                    makeDirectories(directories, output, targetFileName.substring(0, targetFileName.lastIndexOf('/') + 1));

                    output.putArchiveEntry(Utils.copyTarEntry(entry, targetFileName, entry.getSize()));
                    input.transferTo(output);
                    output.closeArchiveEntry();
                }

                putEntry(output, "./usr/share/applications/org.glavo.jetbrains." + product.getPackageName() + ".desktop", getDesktopInfo());
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

    private String getControl() {
        Product product = getIDEProduct().get();
        var lines = new ArrayList<>(List.of(
                "Package: " + product.getPackageName(),
                "Version: " + getVersion().get(),
                "Section: devel",
                "Priority: optional",
                "Architecture: " + getDebArch().get(),
                "Installed-Size: 3145728",
                "Maintainer: Glavo <glavo@isrc.iscas.ac.cn>",
                "Description: " + product.getDescription() + ".",
                "Homepage: https://github.com/Glavo/JetBrains-IDE-Multiarch"
        ));

        // Older packages create a script in /usr/bin instead of using update-alternatives
        // We should make sure old packages are removed
        if (product == Product.IDEA_COMMUNITY || product == Product.IDEA) {
            lines.add("Replaces: intellij-idea-ce-multiarch");
            lines.add("Conflicts: intellij-idea-ce-multiarch");
        } else if (product == Product.PYCHARM_COMMUNITY || product == Product.PYCHARM) {
            lines.add("Replaces: pycharm-community-multiarch");
            lines.add("Conflicts: pycharm-community-multiarch");
        }

        return String.join("\n", lines) + "\n";
    }

    private String getPostinst() {
        Product product = getIDEProduct().get();
        return String.join("\n",
                "#!/bin/bash -e",
                "",
                "if [ \"$1\" = configure ]; then",
                "    update-alternatives --install /usr/bin/%1$s %1$s %2$s/bin/%1$s %3$d".formatted(product.getLauncherName(), getInstallPath(), product.getPriority()),
                "fi",
                "");
    }

    private String getPrerm() {
        Product product = getIDEProduct().get();
        return String.join("\n",
                "#!/bin/bash -e",
                "",
                "if [ \"$1\" = \"remove\" ] || [ \"$1\" = \"deconfigure\" ]; then",
                "    update-alternatives --remove %1$s %2$s/bin/%1$s".formatted(product.getLauncherName(), getInstallPath()),
                "fi",
                "");
    }

    private String getDesktopInfo() {
        Product product = getIDEProduct().get();
        return String.join("\n",
                "[Desktop Entry]",
                "Type=Application",
                "Name=" + product.getFullName(),
                "Version=" + getVersion().get(),
                "Terminal=false",
                "Comment=" + product.getDescription(),
                "Exec=%s/bin/%s %%F".formatted(getInstallPath(), product.getLauncherName()),
                "Icon=%s/bin/%s.svg".formatted(getInstallPath(), product.getLauncherName()),
                "Categories=" + product.getDesktopCategories(),
                "Keywords=" + product.getDesktopKeywords(),
                "");
    }
}
