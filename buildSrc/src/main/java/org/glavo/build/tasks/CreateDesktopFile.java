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

import org.gradle.api.DefaultTask;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.util.Map;

public abstract class CreateDesktopFile extends DefaultTask {

    private static void writeDesktopFile(Writer writer, Map<String, Map<String, String>> info) throws IOException {
        boolean first = true;
        for (var groupEntry : info.entrySet()) {
            if (first) {
                first = false;
            } else {
                writer.write('\n');
            }

            writer.write("[" + groupEntry.getKey() + "]\n");
            for (Map.Entry<String, String> entry : groupEntry.getValue().entrySet()) {
                writer.write(entry.getKey() + "=" + entry.getValue() + "\n");
            }
        }
    }

    @OutputFile
    public abstract Property<File> getDesktopFile();

    @Input
    public abstract Property<Map<String, Map<String, String>>> getDesktopInfo();

    @TaskAction
    public void run() throws IOException {
        File desktopFile = getDesktopFile().get();
        desktopFile.getParentFile().mkdirs();
        try (Writer writer = Files.newBufferedWriter(desktopFile.toPath())) {
            writeDesktopFile(writer, getDesktopInfo().get());
        }
    }
}
