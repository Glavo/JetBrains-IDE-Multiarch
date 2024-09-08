import com.google.gson.*;
import kala.collection.mutable.MutableList;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.gradle.api.GradleException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

enum IJFileProcessor {
    PRODUCT_INFO("product-info.json") {
        private static void processAdditionalJvmArguments(IJProcessor processor, JsonObject obj) {
            JsonElement additionalJvmArgumentsElement = obj.get("additionalJvmArguments");
            if (additionalJvmArgumentsElement == null) {
                return;
            }

            var arg = "-Djna.boot.library.path=$IDE_HOME/lib/jna/" + processor.baseArch.getName();

            JsonArray additionalJvmArguments = additionalJvmArgumentsElement.getAsJsonArray();
            for (int i = 0; i < additionalJvmArguments.size(); i++) {
                JsonElement element = additionalJvmArguments.get(i);
                if (element.getAsString().equals(arg)) {
                    additionalJvmArguments.set(i, new JsonPrimitive("-Djna.boot.library.path=$IDE_HOME/lib/jna/" + processor.arch.getName()));
                    return;
                }
            }
        }

        @Override
        void process(IJProcessor processor, TarArchiveEntry entry) throws IOException {
            var gson = new GsonBuilder()
                    .setPrettyPrinting()
                    .create();

            JsonObject productInfo = gson.fromJson(new String(processor.baseTar.readAllBytes()), JsonObject.class);
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
                    launch.addProperty("arch", processor.arch.getName());
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
            entry.setSize(bytes.length);
            processor.outTar.putArchiveEntry(entry);
            processor.outTar.write(bytes);
            processor.outTar.closeArchiveEntry();
        }
    },
    IDEA_SH("bin/idea.sh") {
        @Override
        void process(IJProcessor processor, TarArchiveEntry entry) throws IOException {
            var result = new StringBuilder();

            boolean foundVMOptions = false;
            for (String line : new String(processor.baseTar.readAllBytes()).lines().toList()) {
                if (line.contains("-Didea.vendor.name=JetBrains") && line.endsWith("\\")) {
                    if (foundVMOptions) {
                        throw new GradleException("Duplicate JVM options");
                    }
                    foundVMOptions = true;

                    var args = MutableList.from(List.of(line.substring(0, line.length() - 1).trim().split(" ")));
                    args.insert(1, "\"-Didea.filewatcher.executable.path=${IDE_HOME}/bin/fsnotifier\"");

                    int idx = args.indexOf("\"-Djna.boot.library.path=$IDE_HOME/lib/jna/" + processor.baseArch.getName() + "\"");
                    if (idx < 0) {
                        throw new GradleException("Missing jna option");
                    }
                    args.set(idx, "\"-Djna.boot.library.path=$IDE_HOME/lib/jna/" + processor.arch.getName() + "\"");
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
            entry.setSize(bytes.length);
            processor.outTar.putArchiveEntry(entry);
            processor.outTar.write(bytes);
            processor.outTar.closeArchiveEntry();
        }
    },
    UTIL_JAR("lib/util.jar") {
        @Override
        void process(IJProcessor processor, TarArchiveEntry entry) throws Throwable {
            if (processor.arch == Arch.LOONGARCH64) {
                var buffer = new ByteArrayOutputStream();
                try (var input = new ZipInputStream(new ByteArrayInputStream(processor.baseTar.readAllBytes()));
                     var output = new ZipOutputStream(buffer)) {

                    boolean foundOSFacadeImpl = false;
                    ZipEntry zipEntry;
                    while ((zipEntry = input.getNextEntry()) != null) {
                        if (zipEntry.getName().equals("com/pty4j/unix/linux/OSFacadeImpl.class")) {
                            if (foundOSFacadeImpl) {
                                throw new GradleException("Duplicate OSFacadeImpl");
                            }
                            foundOSFacadeImpl = true;

                            //noinspection DataFlowIssue
                            Path replacement = Path.of(IJFileProcessor.class.getResource("OSFacadeImpl.class.bin").toURI());
                            FileTime time = Files.getLastModifiedTime(replacement);
                            ZipEntry newEntry = new ZipEntry(zipEntry.getName());
                            newEntry.setCreationTime(time);
                            newEntry.setLastModifiedTime(time);
                            newEntry.setSize(Files.size(replacement));

                            output.putNextEntry(newEntry);
                            Files.copy(replacement, output);
                            output.close();
                        } else {
                            output.putNextEntry(zipEntry);
                            input.transferTo(output);
                            output.closeEntry();
                        }
                    }
                }

                TarArchiveEntry newEntry = new TarArchiveEntry(path);
                newEntry.setSize(buffer.size());
                processor.outTar.putArchiveEntry(newEntry);
                processor.outTar.write(buffer.toByteArray());
                processor.outTar.closeArchiveEntry();
            } else {
                processor.outTar.putArchiveEntry(entry);
                processor.baseTar.transferTo(processor.outTar);
                processor.outTar.closeArchiveEntry();
            }
        }
    },
    LOCAL_LAUNCHER("bin/idea", "xplat-launcher"),
    REMOTE_LAUNCHER("bin/remote-dev-server", "xplat-launcher", true),
    FSNOTIFIER("bin/fsnotifier", "fsnotifier"),
    ;

    final String path;
    final String replacement;
    final boolean iu;

    IJFileProcessor(String path) {
        this.path = path;
        this.replacement = null;
        this.iu = false;
    }

    IJFileProcessor(String path, String replacement) {
        this(path, replacement, false);
    }

    IJFileProcessor(String path, String replacement, boolean iu) {
        this.path = path;
        this.replacement = replacement;
        this.iu = iu;
    }

    void process(IJProcessor processor, TarArchiveEntry entry) throws Throwable {
        if (replacement == null) {
            throw new AssertionError("replacement is null");
        }

        ZipEntry replacementEntry = processor.nativesZip.getEntry(replacement);
        if (replacementEntry == null) {
            throw new GradleException("Missing " + replacement);
        }

        entry.setSize(replacementEntry.getSize());
        entry.setCreationTime(replacementEntry.getCreationTime());
        entry.setLastModifiedTime(replacementEntry.getLastModifiedTime());
        entry.setLastAccessTime(replacementEntry.getLastAccessTime());

        processor.outTar.putArchiveEntry(entry);
        try (var input = processor.nativesZip.getInputStream(replacementEntry)) {
            input.transferTo(processor.outTar);
        }
        processor.outTar.closeArchiveEntry();
    }
}