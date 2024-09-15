package org.glavo.build.internal;

import com.google.gson.*;
import com.sun.jna.Native;
import kala.collection.mutable.MutableList;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.glavo.build.Arch;
import org.gradle.api.GradleException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static org.glavo.build.internal.IJProcessor.LOGGER;

enum IJFileProcessor {
    PRODUCT_INFO("product-info.json") {
        private static void processAdditionalJvmArguments(IJProcessor processor, JsonObject obj) {
            JsonElement additionalJvmArgumentsElement = obj.get("additionalJvmArguments");
            if (additionalJvmArgumentsElement == null) {
                return;
            }

            var arg = "-Djna.boot.library.path=$IDE_HOME/lib/jna/" + processor.baseArch.normalize();

            JsonArray additionalJvmArguments = additionalJvmArgumentsElement.getAsJsonArray();
            for (int i = 0; i < additionalJvmArguments.size(); i++) {
                JsonElement element = additionalJvmArguments.get(i);
                if (element.getAsString().equals(arg)) {
                    additionalJvmArguments.set(i, new JsonPrimitive("-Djna.boot.library.path=$IDE_HOME/lib/jna/" + processor.arch.normalize()));
                    additionalJvmArguments.asList().add(i + 1, new JsonPrimitive("-Didea.filewatcher.executable.path=$IDE_HOME/bin/fsnotifier"));
                    return;
                }
            }
        }

        @Override
        void process(IJProcessor processor, TarArchiveEntry entry, String ijDirPrefix) throws IOException {
            var gson = new GsonBuilder()
                    .setPrettyPrinting()
                    .disableHtmlEscaping()
                    .create();

            JsonObject productInfo = gson.fromJson(new String(processor.tarInput.readAllBytes()), JsonObject.class);
            JsonObject result = new JsonObject();

            productInfo.asMap().forEach((key, value) -> {
                if (key.equals("productCode")) {
                    result.add(key, value);
                    result.addProperty("envVarBaseName", "IDEA");
                } else if (key.equals("launch")) {
                    var launchArray = (JsonArray) value;
                    if (launchArray.size() != 1) {
                        throw new GradleException("Expected exactly one launch");
                    }

                    var launch = launchArray.get(0).getAsJsonObject();
                    launch.addProperty("arch", processor.arch.normalize());
                    processAdditionalJvmArguments(processor, launch);

                    for (JsonElement element : launch.getAsJsonArray("customCommands")) {
                        processAdditionalJvmArguments(processor, element.getAsJsonObject());
                    }

                    result.add(key, value);
                } else {
                    result.add(key, value);
                }
            });

            var bytes = gson.toJson(result).getBytes(StandardCharsets.UTF_8);
            var newEntry = Utils.copyTarEntry(entry, bytes.length);
            processor.tarOutput.putArchiveEntry(newEntry);
            processor.tarOutput.write(bytes);
            processor.tarOutput.closeArchiveEntry();
        }
    },
    IDEA_SH("bin/idea.sh") {
        @Override
        void process(IJProcessor processor, TarArchiveEntry entry, String ijDirPrefix) throws IOException {
            var result = new StringBuilder();

            boolean foundVMOptions = false;
            for (String line : new String(processor.tarInput.readAllBytes()).lines().toList()) {
                if (line.contains("-Didea.vendor.name=JetBrains") && line.endsWith("\\")) {
                    if (foundVMOptions) {
                        throw new GradleException("Duplicate JVM options");
                    }
                    foundVMOptions = true;

                    var args = MutableList.from(List.of(line.substring(0, line.length() - 1).trim().split(" ")));
                    args.insert(1, "\"-Didea.filewatcher.executable.path=$IDE_HOME/bin/fsnotifier\"");

                    int idx = args.indexOf("\"-Djna.boot.library.path=$IDE_HOME/lib/jna/" + processor.baseArch.normalize() + "\"");
                    if (idx < 0) {
                        throw new GradleException("Missing jna option");
                    }
                    args.set(idx, "\"-Djna.boot.library.path=$IDE_HOME/lib/jna/" + processor.arch.normalize() + "\"");
                    args.joinTo(result, " ", "  ", " \\\n");
                } else {
                    result.append(line);
                    result.append('\n');
                }
            }

            if (!foundVMOptions) {
                throw new GradleException("No VM options found");
            }

            var bytes = result.toString().getBytes(StandardCharsets.UTF_8);
            var newEntry = Utils.copyTarEntry(entry, bytes.length);
            processor.tarOutput.putArchiveEntry(newEntry);
            processor.tarOutput.write(bytes);
            processor.tarOutput.closeArchiveEntry();
        }
    },
    UTIL_JAR("lib/util.jar") {
        @Override
        void process(IJProcessor processor, TarArchiveEntry entry, String ijDirPrefix) throws Throwable {
            if (processor.arch == Arch.LOONGARCH64) {
                var buffer = new ByteArrayOutputStream();
                try (var input = new ZipInputStream(new ByteArrayInputStream(processor.tarInput.readAllBytes()));
                     var output = new ZipOutputStream(buffer)) {

                    boolean foundOSFacadeImpl = false;
                    ZipEntry zipEntry;
                    while ((zipEntry = input.getNextEntry()) != null) {
                        if (zipEntry.getName().equals("com/pty4j/unix/linux/OSFacadeImpl.class")) {
                            if (foundOSFacadeImpl) {
                                throw new GradleException("Duplicate OSFacadeImpl");
                            }
                            foundOSFacadeImpl = true;

                            byte[] bytes;
                            try (var stream = IJFileProcessor.class.getResourceAsStream("OSFacadeImpl.class.bin")) {
                                //noinspection DataFlowIssue
                                bytes = stream.readAllBytes();
                            }

                            ZipEntry newEntry = new ZipEntry(zipEntry.getName());
                            newEntry.setSize(bytes.length);

                            output.putNextEntry(newEntry);
                            output.write(bytes);
                            output.closeEntry();
                        } else {
                            output.putNextEntry(zipEntry);
                            input.transferTo(output);
                            output.closeEntry();
                        }
                    }

                    if (!foundOSFacadeImpl) {
                        throw new GradleException("OSFacadeImpl not found");
                    }
                }

                var newEntry = Utils.copyTarEntry(entry, buffer.size());
                processor.tarOutput.putArchiveEntry(newEntry);
                processor.tarOutput.write(buffer.toByteArray());
                processor.tarOutput.closeArchiveEntry();
            } else {
                processor.tarOutput.putArchiveEntry(entry);
                processor.tarInput.transferTo(processor.tarOutput);
                processor.tarOutput.closeArchiveEntry();
            }
        }
    },
    LOCAL_LAUNCHER("bin/idea", "xplat-launcher"),
    REMOTE_LAUNCHER("bin/remote-dev-server", "xplat-launcher") {
        @Override
        boolean isSupported(IJProcessor processor) {
            return processor.productCode.equals("IU");
        }
    },
    FSNOTIFIER("bin/fsnotifier", "fsnotifier"),
    RESTARTER("bin/restarter", "restarter"),
    LIBJNIDISPATCH("lib/jna/$ARCH/libjnidispatch.so", "libjnidispatch.so") {
        private static String getPath(Arch arch, String ijDirPrefix) {
            return ijDirPrefix + "lib/jna/" + arch.normalize() + "/libjnidispatch.so";
        }

        @Override
        String getPath(IJProcessor processor, String ijDirPrefix) {
            return getPath(processor.baseArch, ijDirPrefix);
        }

        @Override
        void process(IJProcessor processor, TarArchiveEntry entry, String ijDirPrefix) throws Throwable {
            LOGGER.lifecycle("Replace libjnidispatch.so ({} -> {})", getPath(processor.baseArch, ijDirPrefix), getPath(processor.arch, ijDirPrefix));

            String jniDispatchPath = "linux-%s/libjnidispatch.so".formatted(processor.arch.normalize());
            byte[] bytes;
            try (var stream = Native.class.getResourceAsStream(jniDispatchPath)) {
                if (stream == null) {
                    throw new GradleException(jniDispatchPath + " not found");
                }

                bytes = stream.readAllBytes();
            }

            var newEntry = Utils.copyTarEntry(entry, getPath(processor.arch, ijDirPrefix), bytes.length);
            processor.tarOutput.putArchiveEntry(newEntry);
            processor.tarOutput.write(bytes);
            processor.tarOutput.closeArchiveEntry();
        }
    },
    LIBPTY("lib/pty4j/linux/$ARCH/libpty.so", "libpty.so") {
        private static String getPath(Arch arch, String ijDirPrefix) {
            return ijDirPrefix + "lib/pty4j/linux/" + arch.normalize() + "/libpty.so";
        }

        @Override
        String getPath(IJProcessor processor, String ijDirPrefix) {
            return getPath(processor.baseArch, ijDirPrefix);
        }

        @Override
        void process(IJProcessor processor, TarArchiveEntry entry, String ijDirPrefix) throws Throwable {
            LOGGER.lifecycle("Replace libpty.so ({} -> {})", getPath(processor.baseArch, ijDirPrefix), getPath(processor.arch, ijDirPrefix));

            //noinspection DataFlowIssue
            ZipEntry replacementEntry = processor.nativesZip.getEntry(replacement);
            if (replacementEntry == null) {
                throw new GradleException("Missing " + replacement);
            }

            var newEntry = Utils.copyTarEntry(entry, getPath(processor.arch, ijDirPrefix), replacementEntry.getSize());
            processor.tarOutput.putArchiveEntry(newEntry);
            try (var input = processor.nativesZip.getInputStream(replacementEntry)) {
                input.transferTo(processor.tarOutput);
            }
            processor.tarOutput.closeArchiveEntry();
        }
    };

    final String path;
    final String replacement;

    IJFileProcessor(String path) {
        this.path = path;
        this.replacement = null;
    }

    IJFileProcessor(String path, String replacement) {
        this.path = path;
        this.replacement = replacement;
    }

    String getPath(IJProcessor processor, String ijDirPrefix) {
        return ijDirPrefix + path;
    }

    boolean isSupported(IJProcessor processor) {
        return true;
    }

    void process(IJProcessor processor, TarArchiveEntry entry, String ijDirPrefix) throws Throwable {
        if (replacement == null) {
            throw new AssertionError("replacement is null");
        }

        LOGGER.info("Replace {} with {}/{}", entry.getName(), processor.nativesZipName, replacement);

        ZipEntry replacementEntry = processor.nativesZip.getEntry(replacement);
        if (replacementEntry == null) {
            throw new GradleException("Missing " + replacement);
        }

        var newEntry = Utils.copyTarEntry(entry, replacementEntry.getSize());
        processor.tarOutput.putArchiveEntry(newEntry);
        try (var input = processor.nativesZip.getInputStream(replacementEntry)) {
            input.transferTo(processor.tarOutput);
        }
        processor.tarOutput.closeArchiveEntry();
    }
}
