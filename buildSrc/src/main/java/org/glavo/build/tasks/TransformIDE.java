package org.glavo.build.tasks;

import org.glavo.build.Arch;
import org.glavo.build.Product;
import org.glavo.build.processor.IJProcessor;
import org.gradle.api.DefaultTask;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;

import java.io.File;

public abstract class TransformIDE extends DefaultTask {

    @Input
    public abstract Property<Arch> getIDEBaseArch();

    @Input
    public abstract Property<Product> getIDEProduct();

    @InputFile
    public abstract Property<File> getIDEBaseTar();

    @Input
    public abstract Property<Arch> getIDEArch();

    @InputFile
    public abstract Property<File> getIDENativesZipFile();

    @Optional
    @InputFile
    public abstract Property<File> getJDKArchive();

    @OutputFile
    public abstract Property<File> getTargetFile();

    @TaskAction
    public void run() throws Throwable {
        try (var processor = new IJProcessor(this)) {
            processor.process();
        }
    }
}
