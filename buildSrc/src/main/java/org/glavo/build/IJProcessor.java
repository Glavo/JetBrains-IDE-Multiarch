package org.glavo.build;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.logging.Logger;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.zip.ZipFile;

public final class IJProcessor {

    final Project project;
    final Task task;
    final Arch baseArch;
    final String productCode;
    final TarArchiveInputStream baseTar;
    final Arch arch;
    final ZipFile nativesZip;
    final TarArchiveOutputStream outTar;

    public IJProcessor(Project project, Task task,
                       Arch baseArch, String productCode, TarArchiveInputStream baseTar,
                       Arch arch, ZipFile nativesZip, TarArchiveOutputStream outTar) {
        this.project = project;
        this.task = task;
        this.baseArch = baseArch;
        this.productCode = productCode;
        this.baseTar = baseTar;
        this.arch = arch;
        this.nativesZip = nativesZip;
        this.outTar = outTar;
    }

    Logger getLogger() {
        return project.getLogger();
    }

    public void process() throws Throwable {
        String prefix;
        {
            TarArchiveEntry it = baseTar.getNextEntry();
            if (it == null || !it.isDirectory()) {
                throw new GradleException("Invalid directory entry: ${it.name}");
            }
            prefix = it.getName();
        }

        var jbrPrefix = prefix + "/jbr";

        var set = EnumSet.allOf(IJFileProcessor.class);
        if (!productCode.equals("IU")) {
            set.removeIf(it -> it.iu);
        }
        var processors = new HashMap<String, IJFileProcessor>();
        boolean processedJbr = false;

        for (IJFileProcessor processor : set) {
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

            } else if (processors.get(path) instanceof IJFileProcessor processor) {
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
