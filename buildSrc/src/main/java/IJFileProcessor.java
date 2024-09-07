import com.google.gson.*;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.gradle.api.GradleException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;

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

            var holder = new boolean[]{false, false};

            new String(processor.baseTar.readAllBytes()).lines().forEach(line -> {
                if (line.equals("  ${VM_OPTIONS} \\")) {
                    if (holder[0]) {
                        throw new GradleException("Duplicate VM options: " + line);
                    }
                    holder[0] = true;
                    result.append("  -Didea.filewatcher.executable.path=${IDE_HOME}/bin/fsnotifier\\");
                }

                result.append(line);
                result.append('\n');
            });

            if (!holder[0]) {
                throw new GradleException("No VM options found");
            }

            var bytes = result.toString().getBytes(StandardCharsets.UTF_8);
            entry.setSize(bytes.length);
            processor.outTar.putArchiveEntry(entry);
            processor.outTar.write(bytes);
            processor.outTar.closeArchiveEntry();
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

    void process(IJProcessor processor, TarArchiveEntry entry) throws IOException {
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
