package org.glavo.build.transformer;

import com.google.gson.*;
import kala.function.CheckedFunction;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.glavo.build.processor.IDEProcessor;
import org.glavo.build.util.OpenHelper;
import org.glavo.build.tasks.TransformIDE;
import org.glavo.build.util.Utils;
import org.gradle.api.GradleException;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public abstract class IDETransformer {
    public static final Logger LOGGER = Logging.getLogger(IDEProcessor.class);

    protected final TransformIDE task;
    protected final ZipFile nativesZip;
    protected final TarArchiveInputStream tarInput;
    protected final TarArchiveOutputStream tarOutput;

    private static final int BUFFER_SIZE = 32 * 1024;
    private final byte[] buffer = new byte[BUFFER_SIZE];

    private final OpenHelper helper = new OpenHelper();

    public IDETransformer(TransformIDE task) throws Throwable {
        this.task = task;

        try {
            this.nativesZip = helper.register(new ZipFile(task.getIDENativesZipFile().get().getAsFile()));
            this.tarInput = helper.register(new TarArchiveInputStream(
                    helper.register(new GZIPInputStream(
                            helper.register(Files.newInputStream(task.getIDEBaseTar().get().getAsFile().toPath()))))));
            this.tarOutput = helper.register(new TarArchiveOutputStream(
                    helper.register(new GZIPOutputStream(
                            helper.register(Files.newOutputStream(task.getTargetFile().get().getAsFile().toPath(),
                                    StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING))))));
            tarOutput.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
        } catch (Throwable e) {
            helper.onException(e);
            throw e;
        }
    }

    protected final void copy(InputStream input, OutputStream output) throws IOException {
        int read;
        while ((read = input.read(buffer, 0, BUFFER_SIZE)) >= 0) {
            output.write(buffer, 0, read);
        }
    }

    @MustBeInvokedByOverriders
    protected Map<String, FileTransformer> getTransformers() {
        Map<String, FileTransformer> transformer = new HashMap<>();
        transformer.put("product-info.json", new FileTransformer.Transform(new CheckedFunction<TarArchiveEntry, byte[], IOException>() {
            private void processAdditionalJvmArguments(JsonObject obj) {
                JsonElement additionalJvmArgumentsElement = obj.get("additionalJvmArguments");
                if (additionalJvmArgumentsElement == null) {
                    return;
                }

                var arg = "-Djna.boot.library.path=$IDE_HOME/lib/jna/" + task.getIDEBaseArch().get().normalize();

                JsonArray additionalJvmArguments = additionalJvmArgumentsElement.getAsJsonArray();
                for (int i = 0; i < additionalJvmArguments.size(); i++) {
                    JsonElement element = additionalJvmArguments.get(i);
                    if (element.getAsString().equals(arg)) {
                        additionalJvmArguments.set(i, new JsonPrimitive("-Djna.boot.library.path=$IDE_HOME/lib/jna/" + task.getIDEArch().get().normalize()));
                        additionalJvmArguments.asList().add(i + 1, new JsonPrimitive("-Didea.filewatcher.executable.path=$IDE_HOME/bin/fsnotifier"));
                        return;
                    }
                }
            }

            @Override
            public byte[] applyChecked(TarArchiveEntry entry) throws IOException {
                var gson = new GsonBuilder()
                        .setPrettyPrinting()
                        .disableHtmlEscaping()
                        .create();

                JsonObject productInfo = gson.fromJson(new String(tarInput.readAllBytes()), JsonObject.class);
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
                        launch.addProperty("arch", task.getIDEArch().get().normalize());
                        processAdditionalJvmArguments(launch);

                        for (JsonElement element : launch.getAsJsonArray("customCommands")) {
                            processAdditionalJvmArguments(element.getAsJsonObject());
                        }

                        result.add(key, value);
                    } else {
                        result.add(key, value);
                    }
                });

                return gson.toJson(result).getBytes(StandardCharsets.UTF_8);
            }

        }));

        return transformer;
    }

    private void copyJRE(String jbrPrefix, TarArchiveInputStream jreTar) throws IOException {
        TarArchiveEntry entry = jreTar.getNextEntry();
        if (entry == null) {
            throw new GradleException(task.getJDKArchive().get().getAsFile() + " is empty");
        }

        int idx = entry.getName().indexOf('/');
        if (idx < 0) {
            throw new GradleException("Invalid first entry: " + entry.getName());
        }

        String prefix = entry.getName().substring(0, idx + 1);

        do {
            if (!entry.getName().startsWith(prefix)) {
                throw new GradleException("Invalid directory entry: " + entry.getName());
            }
            String newName = jbrPrefix + entry.getName().substring(prefix.length());

            LOGGER.info("Copying {}/{} to {}", task.getJDKArchive().get().getAsFile().getName(), entry.getName(), newName);
            entry.setName(newName);
            tarOutput.putArchiveEntry(entry);
            copy(jreTar, tarOutput);
            tarOutput.closeArchiveEntry();
        } while ((entry = jreTar.getNextEntry()) != null);
    }

    public void transform() throws Throwable {
        String prefix;
        {
            TarArchiveEntry it = tarInput.getNextEntry();
            if (it == null || !it.isDirectory()) {
                throw new GradleException("Invalid directory entry: ${it.name}");
            }
            prefix = it.getName();
        }

        LOGGER.lifecycle("Processing {}", prefix);

        var jbrPrefix = prefix + "jbr/";

        Map<String, FileTransformer> transformers = new HashMap<>();
        getTransformers().forEach((path, transformer) -> {
            transformers.put(prefix + path, transformer);
        });

        boolean processedJbr = false;

        TarArchiveEntry entry;
        while ((entry = tarInput.getNextEntry()) != null) {
            String path = entry.getName();

            if (path.startsWith(jbrPrefix)) {
                if (path.equals(jbrPrefix)) {
                    processedJbr = true;
                    if (task.getJDKArchive().get() == null) {
                        LOGGER.warn("No JRE provided");
                    } else {
                        LOGGER.lifecycle("Copying JRE from {}", task.getJDKArchive().get());
                        try (var jreTar = new TarArchiveInputStream(new GZIPInputStream(Files.newInputStream(task.getJDKArchive().get().getAsFile().toPath())))) {
                            copyJRE(jbrPrefix, jreTar);
                        }
                    }
                } else {
                    LOGGER.info("Skip JBR entry: {}", path);
                }
            } else if (transformers.remove(path) instanceof FileTransformer transformer) {
                switch (transformer) {
                    case FileTransformer.Replace replace -> {
                        LOGGER.lifecycle("TRANSFORM: Replace {} with {}/{}", entry.getName(), task.getIDENativesZipFile().get().getAsFile().getName(), replace.replacement());

                        ZipEntry replacementEntry = nativesZip.getEntry(replace.replacement());
                        if (replacementEntry == null) {
                            throw new GradleException("Missing " + replace.replacement());
                        }

                        var newEntry = Utils.copyTarEntry(entry, replacementEntry.getSize());
                        tarOutput.putArchiveEntry(newEntry);
                        try (var input = nativesZip.getInputStream(replacementEntry)) {
                            copy(input, tarOutput);
                        }
                        tarOutput.closeArchiveEntry();
                    }
                    case FileTransformer.FilterOut __ -> {
                        LOGGER.lifecycle("TRANSFORM: Filter out {}", entry.getName());
                    }
                    case FileTransformer.Transform transform -> {
                        LOGGER.lifecycle("TRANSFORM: Transform {}", path);
                        byte[] result = transform.action().apply(entry);
                        tarOutput.putArchiveEntry(Utils.copyTarEntry(entry, result.length));
                        tarOutput.write(result);
                        tarOutput.closeArchiveEntry();
                    }
                }
            } else if (entry.isSymbolicLink()) {
                LOGGER.info("Copying symbolic link {} -> {}", path, entry.getLinkName());
                tarOutput.putArchiveEntry(entry);
                tarOutput.closeArchiveEntry();
            } else {
                LOGGER.info("Copying {}", path);
                tarOutput.putArchiveEntry(entry);
                copy(tarInput, tarOutput);
                tarOutput.closeArchiveEntry();
            }
        }

        if (!transformers.isEmpty()) {
            throw new GradleException("These files were not found: " + transformers.entrySet());
        }

        if (!processedJbr) {
            throw new GradleException("No JBR found");
        }
    }
}
