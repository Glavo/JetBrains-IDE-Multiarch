package org.glavo.build.transformer;

import com.google.gson.*;
import com.sun.jna.Native;
import kala.collection.mutable.MutableList;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.glavo.build.Arch;
import org.glavo.build.Product;
import org.glavo.build.util.IOBuffer;
import org.glavo.build.util.OpenHelper;
import org.glavo.build.tasks.TransformIDE;
import org.glavo.build.util.Utils;
import org.gradle.api.GradleException;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.*;

import static java.util.Objects.requireNonNullElse;

public abstract class IDETransformer implements AutoCloseable {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    public static final Logger LOGGER = Logging.getLogger(IDETransformer.class);

    protected final TransformIDE task;
    protected final Product product;
    protected final Arch baseArch;
    protected final Arch targetArch;

    protected final ZipFile nativesZip;
    protected final TarArchiveInputStream tarInput;
    protected final TarArchiveOutputStream tarOutput;

    protected final IOBuffer buffer = new IOBuffer();

    private final OpenHelper helper = new OpenHelper();

    public IDETransformer(TransformIDE task) throws Throwable {
        this.task = task;

        this.product = task.getIDEProduct().get();
        this.baseArch = task.getIDEBaseArch().get();
        this.targetArch = task.getIDETargetArch().get();

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

    protected final FileTransformer.Replace getNativeReplacement(String path) throws IOException {
        return getNativeReplacement(path, null);
    }

    protected final FileTransformer.Replace getNativeReplacement(String path, @Nullable String targetPath) throws IOException {
        ZipEntry entry = nativesZip.getEntry(path);
        if (entry == null) {
            throw new GradleException("Missing " + path);
        }

        return new FileTransformer.Replace(nativesZip.getInputStream(entry).readAllBytes(), targetPath);
    }

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
                additionalJvmArguments.set(i, new JsonPrimitive("-Djna.boot.library.path=$IDE_HOME/lib/jna/" + targetArch.normalize()));
                additionalJvmArguments.asList().add(i + 1, new JsonPrimitive("-Didea.filewatcher.executable.path=$IDE_HOME/bin/fsnotifier"));
                return;
            }
        }
    }

    @MustBeInvokedByOverriders
    protected Map<String, FileTransformer> getTransformers() throws IOException {
        Map<String, FileTransformer> transformer = new HashMap<>();
        transformer.put("bin/" + product.getLauncherName(), getNativeReplacement("xplat-launcher"));
        if (!product.isOpenSource()) {
            transformer.put("bin/remote-dev-server", getNativeReplacement("xplat-launcher"));
        }
        transformer.put("bin/fsnotifier", getNativeReplacement("fsnotifier"));
        transformer.put("bin/restarter", getNativeReplacement("restarter"));
        // transformer.put("bin/libdbm.so", getNativeReplacement("libdbm.so"));
        transformer.put("bin/libdbm.so", new FileTransformer.FilterOut());
        transformer.put("lib/pty4j/linux/%s/libpty.so".formatted(baseArch.normalize()),
                getNativeReplacement("libpty.so", "lib/pty4j/linux/%s/libpty.so".formatted(targetArch.normalize())));

        String jniDispatchPath = "linux-%s/libjnidispatch.so".formatted(targetArch.normalize());
        try (var stream = Native.class.getResourceAsStream(jniDispatchPath)) {
            if (stream == null) {
                throw new GradleException(jniDispatchPath + " not found");
            }
            transformer.put("lib/jna/%s/libjnidispatch.so".formatted(baseArch.normalize()),
                    new FileTransformer.Replace(stream.readAllBytes(), "lib/jna/%s/libjnidispatch.so".formatted(targetArch.normalize())));
        }

        transformer.put("product-info.json", new FileTransformer.Transform(raw -> {
            JsonObject productInfo = GSON.fromJson(new String(raw), JsonObject.class);
            JsonObject result = new JsonObject();

            productInfo.asMap().forEach((key, value) -> {
                if (key.equals("launch")) {
                    var launchArray = (JsonArray) value;
                    if (launchArray.size() != 1) {
                        throw new GradleException("Expected exactly one launch");
                    }

                    var launch = launchArray.get(0).getAsJsonObject();
                    launch.addProperty("arch", task.getIDETargetArch().get().normalize());
                    processAdditionalJvmArguments(launch);

                    for (JsonElement element : launch.getAsJsonArray("customCommands")) {
                        processAdditionalJvmArguments(element.getAsJsonObject());
                    }

                }
                result.add(key, value);
                if (key.equals("productCode") && productInfo.get("envVarBaseName") == null) {
                    result.addProperty("envVarBaseName", product.getLauncherName().toUpperCase(Locale.ROOT).replace('-', '_'));
                }
            });

            return GSON.toJson(result).getBytes(StandardCharsets.UTF_8);
        }));

        transformer.put("bin/%s.sh".formatted(product.getLauncherName()), new FileTransformer.Transform(raw -> {
            var result = new StringBuilder();

            boolean foundVMOptions = false;
            for (String line : new String(raw).lines().toList()) {
                if (line.contains("-Didea.vendor.name=JetBrains") && line.endsWith("\\")) {
                    if (foundVMOptions) {
                        throw new GradleException("Duplicate JVM options");
                    }
                    foundVMOptions = true;

                    var args = MutableList.from(List.of(line.substring(0, line.length() - 1).trim().split(" ")));
                    args.insert(1, "\"-Didea.filewatcher.executable.path=$IDE_HOME/bin/fsnotifier\"");

                    int idx = args.indexOf("\"-Djna.boot.library.path=$IDE_HOME/lib/jna/" + baseArch.normalize() + "\"");
                    if (idx < 0) {
                        throw new GradleException("Missing jna option");
                    }
                    args.set(idx, "\"-Djna.boot.library.path=$IDE_HOME/lib/jna/" + targetArch.normalize() + "\"");
                    args.joinTo(result, " ", "  ", " \\\n");
                } else {
                    result.append(line);
                    result.append('\n');
                }
            }

            if (!foundVMOptions) {
                throw new GradleException("No VM options found");
            }

            return result.toString().getBytes(StandardCharsets.UTF_8);
        }));

        if (targetArch == Arch.LOONGARCH64) {
            transformer.put("lib/util.jar", new FileTransformer.Transform(raw -> {
                var buffer = new ByteArrayOutputStream();
                try (var input = new ZipInputStream(new ByteArrayInputStream(raw));
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
                            try (var stream = IDETransformer.class.getResourceAsStream("OSFacadeImpl.class.bin")) {
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
                            this.buffer.copy(input, output);
                            output.closeEntry();
                        }
                    }

                    if (!foundOSFacadeImpl) {
                        throw new GradleException("OSFacadeImpl not found");
                    }
                }

                return buffer.toByteArray();
            }));
        }

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
            buffer.copy(jreTar, tarOutput);
            tarOutput.closeArchiveEntry();
        } while ((entry = jreTar.getNextEntry()) != null);
    }

    public void doTransform() throws Throwable {
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

                        var newEntry = Utils.copyTarEntry(entry, requireNonNullElse(replace.targetPath(), entry.getName()), replace.replacement().length);
                        tarOutput.putArchiveEntry(newEntry);
                        tarOutput.write(replace.replacement());
                        tarOutput.closeArchiveEntry();
                    }
                    case FileTransformer.FilterOut ignored -> {
                        LOGGER.lifecycle("TRANSFORM: Filter out {}", entry.getName());
                    }
                    case FileTransformer.Transform transform -> {
                        LOGGER.lifecycle("TRANSFORM: Transform {}", path);
                        byte[] result = transform.action().apply(tarInput.readAllBytes());
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
                buffer.copy(tarInput, tarOutput);
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

    @Override
    public void close() throws Exception {
        helper.close();
    }
}
