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

import org.glavo.build.Arch;
import org.glavo.build.Product;
import org.glavo.build.transformer.IDETransformer;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;


public abstract class TransformIDE extends DefaultTask {

    @Input
    public abstract Property<Arch> getIDEBaseArch();

    @Input
    public abstract Property<Product> getIDEProduct();

    @InputFile
    public abstract RegularFileProperty getIDEBaseTar();

    @Input
    public abstract Property<Arch> getIDETargetArch();

    @InputFile
    public abstract RegularFileProperty getIDENativesZipFile();

    @Optional
    @InputFile
    public abstract RegularFileProperty getJDKArchive();

    @OutputFile
    public abstract RegularFileProperty getTargetFile();

    @TaskAction
    public void run() throws Throwable {
        try (var transformer = new IDETransformer(this) {}) {
            transformer.doTransform();
        }
    }
}
