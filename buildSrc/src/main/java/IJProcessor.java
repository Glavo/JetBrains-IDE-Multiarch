import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.gradle.api.GradleException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class IJProcessor {
    private enum FileProcessor {
        PRODUCT_INFO("product-info.json") {
            @Override
            void process(IJProcessor processor, TarArchiveEntry entry) throws IOException {
                var result = new StringBuilder();
                var holder = new boolean[]{false};

                new String(processor.baseTar.readAllBytes()).lines().forEach(line -> {
                    result.append(line);
                    result.append('\n');

                    if (line.startsWith("  \"productCode\":")) {
                        if (holder[0]) {
                            throw new GradleException("Duplicate product code: " + line);
                        }
                        holder[0] = true;
                        result.append("  \"envVarBaseName\": \"IDEA\",\n");
                    }
                });

                if (!holder[0]) {
                    throw new GradleException("No product code found");
                }

                var bytes = result.toString().getBytes(StandardCharsets.UTF_8);
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
        REMOTE_LAUNCHER("bin/remote-dev-server", "xplat-launcher"),
        FSNOTIFIER("bin/fsnotifier", "fsnotifier"),
        ;

        private final String path;
        private final String replacement;

        FileProcessor(String path) {
            this.path = path;
            this.replacement = null;
        }

        FileProcessor(String path, String replacement) {
            this.path = path;
            this.replacement = replacement;
        }

        void process(IJProcessor processor, TarArchiveEntry entry) throws IOException {
            if (replacement == null) {
                throw new AssertionError("replacement is null");
            }

            ZipEntry replacementEntry = processor.nativesZip.getEntry(replacement);
            if (replacementEntry == null) {
                throw new GradleException("Missing " + replacement);
            }


        }
    }

    private final Platform platform;
    private final TarArchiveInputStream baseTar;
    private final ZipFile nativesZip;
    private final TarArchiveOutputStream outTar;

    private final String path;

    IJProcessor(Platform platform, TarArchiveInputStream baseTar, ZipFile nativesZip, TarArchiveOutputStream outTar, String path) {
        this.platform = platform;
        this.baseTar = baseTar;
        this.nativesZip = nativesZip;
        this.outTar = outTar;
        this.path = path;
    }

    public void process() throws IOException {
        String prefix;
        {
            TarArchiveEntry it = baseTar.getNextEntry();
            if (it == null || !it.isDirectory()) {
                throw new GradleException("Invalid directory entry: ${it.name}");
            }
            prefix = it.getName();
        }

        var jbrPrefix = prefix + "/jbr";

        var set = EnumSet.allOf(FileProcessor.class);
        var processors = new HashMap<String, FileProcessor>();
        boolean processedJbr = false;

        for (FileProcessor processor : FileProcessor.values()) {
            processors.put(prefix + processor.path, processor);
        }

        TarArchiveEntry entry;
        while ((entry = baseTar.getNextEntry()) != null) {
            String path = entry.getName();

            if (path.startsWith(jbrPrefix)) {
                if (path.equals(jbrPrefix)) {
                    processedJbr = true;
                    // TODO
                }
            } else if (processors.get(path) instanceof FileProcessor processor) {
                processor.process(this, entry);
            } else {
                outTar.putArchiveEntry(entry);
                baseTar.transferTo(outTar);
                outTar.closeArchiveEntry();
            }
        }

        if (!set.isEmpty()) {
            throw new GradleException("These files were not found: " + set);
        } else if (!processedJbr) {
            throw new GradleException("No JBR found");
        }
    }
}
