package org.glavo.build.tasks;

import org.glavo.build.Arch;
import org.glavo.build.Product;
import org.glavo.build.processor.IDEProcessor;
import org.glavo.build.processor.IJFileProcessor;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;

import java.util.Set;

public abstract class TransformIDE extends DefaultTask {

    @Input
    public abstract Property<Arch> getIDEBaseArch();

    @Input
    public abstract Property<Product> getIDEProduct();

    @InputFile
    public abstract RegularFileProperty getIDEBaseTar();

    @Input
    public abstract Property<Arch> getIDEArch();

    @InputFile
    public abstract RegularFileProperty getIDENativesZipFile();

    @Optional
    @InputFile
    public abstract RegularFileProperty getJDKArchive();

    @Input
    public abstract Property<Set<IJFileProcessor>> getFileProcessors();

    @OutputFile
    public abstract RegularFileProperty getTargetFile();

    @TaskAction
    public void run() throws Throwable {
        try (var processor = new IDEProcessor(this)) {
            processor.process();
        }
    }
}
