package org.glavo.build.tasks;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.FileUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.zip.GZIPInputStream;

public abstract class ExtractIDE extends DefaultTask {

    @InputFile
    public abstract Property<File> getSourceFile();

    @OutputDirectory
    public abstract Property<File> getTargetDir();

    @TaskAction
    public void run() throws IOException {
        if (System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("windows")) {
            throw new GradleException("This task should not run on Windows");
        }

        var targetDir = getTargetDir().get().toPath();
        FileUtils.deleteDirectory(targetDir.toFile());
        Files.createDirectories(targetDir);

        try (var tar = new TarArchiveInputStream(new GZIPInputStream(Files.newInputStream(getSourceFile().get().toPath())))) {
            TarArchiveEntry entry = tar.getNextEntry();
            if (entry == null || !entry.isDirectory() || entry.getName().chars().filter(ch -> ch == '/').count() != 1) {
                throw new GradleException("Invalid directory entry: ${it.name}");
            }

            String prefix = entry.getName();

            while ((entry = tar.getNextEntry()) != null) {
                if (!entry.getName().startsWith(prefix)) {
                    throw new GradleException("Invalid entry: " + entry.getName());
                }

                if (entry.isLink()) {
                    throw new GradleException("Unable handle link: " + entry.getName());
                }

                String targetName = entry.getName().substring(prefix.length());
                if (targetName.isEmpty()) {
                    continue;
                }

                Path target = targetDir.resolve(targetName);
                if (entry.isDirectory()) {
                    Files.createDirectories(targetDir);
                } else {
                    if (Files.exists(target)) {
                        throw new GradleException("Duplicate entry: " + entry.getName());
                    }

                    Files.createDirectories(target.getParent());
                    if (entry.isSymbolicLink()) {
                        Files.createSymbolicLink(target, Path.of(entry.getLinkName()));
                    } else {
                        try (var output = Files.newOutputStream(target)) {
                            tar.transferTo(output);
                        }
                    }
                }
            }
        }
    }
}
