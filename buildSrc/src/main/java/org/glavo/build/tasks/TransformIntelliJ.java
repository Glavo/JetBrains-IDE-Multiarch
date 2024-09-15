package org.glavo.build.tasks;

import org.glavo.build.Arch;
import org.glavo.build.internal.IJProcessor;
import org.gradle.api.DefaultTask;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;

import java.io.File;

public abstract class TransformIntelliJ extends DefaultTask {

    @Input
    public abstract Property<Arch> getBaseArch();

    @Input
    public abstract Property<String> getProductCode();

    @InputFile
    public abstract Property<File> getBaseTar();

    @Input
    public abstract Property<Arch> getArch();

    @InputFile
    public abstract Property<File> getNativesZipFile();

    @Optional
    @InputFile
    public abstract Property<File> getJreFile();

    @OutputFile
    public abstract Property<File> getOutTar();

    @TaskAction
    public void run() throws Throwable {
        try (var processor = new IJProcessor(this)) {
            processor.process();
        }
    }
}
