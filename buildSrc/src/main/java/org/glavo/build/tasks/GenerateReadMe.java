package org.glavo.build.tasks;

import kala.template.TemplateEngine;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import static java.nio.charset.StandardCharsets.UTF_8;

public abstract class GenerateReadMe extends DefaultTask {

    @OutputFile
    public abstract RegularFileProperty getOutputFile();

    @InputFile
    public abstract RegularFileProperty getTemplateFile();

    @InputFile
    public abstract RegularFileProperty getPropertiesFile();

    @TaskAction
    public void run() throws IOException {
        var properties = new Properties();
        try (var reader = new FileReader(getPropertiesFile().getAsFile().get(), UTF_8)) {
            properties.load(reader);
        }

        TemplateEngine.getDefault().process(getTemplateFile().getAsFile().get().toPath(), getOutputFile().getAsFile().get().toPath(), list -> {
            for (String key : list.split(":")) {
                String value = properties.getProperty(key);
                if (value != null) {
                    return value;
                }
            }

            throw new GradleException("Can't find value for " + list);
        });
    }
}
