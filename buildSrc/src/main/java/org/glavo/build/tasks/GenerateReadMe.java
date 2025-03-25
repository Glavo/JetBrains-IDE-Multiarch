/*
 * Copyright 2025 Glavo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
import java.nio.file.Files;
import java.util.Properties;
import java.util.function.Function;

import static java.nio.charset.StandardCharsets.UTF_8;

public abstract class GenerateReadMe extends DefaultTask {

    @OutputFile
    public abstract RegularFileProperty getOutputFile();

    @InputFile
    public abstract RegularFileProperty getTemplateFile();

    @InputFile
    public abstract RegularFileProperty getPropertiesFile();

    private static String process(String input, Properties properties) {
        return TemplateEngine.getDefault().process(input, new Function<>() {
            @Override
            public Object apply(String name) {
                String property = properties.getProperty(name);
                if (property == null) {
                    throw new GradleException("Can't find value for " + name);
                }

                return TemplateEngine.getDefault().process(property, this);
            }
        });
    }

    @TaskAction
    public void run() throws IOException {
        var properties = new Properties();
        try (var reader = new FileReader(getPropertiesFile().getAsFile().get(), UTF_8)) {
            properties.load(reader);
        }

        Files.writeString(getOutputFile().getAsFile().get().toPath(),
                process(Files.readString(getTemplateFile().getAsFile().get().toPath()), properties));
    }
}
