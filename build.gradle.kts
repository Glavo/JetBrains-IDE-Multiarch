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

import de.undercouch.gradle.tasks.download.Download
import org.glavo.build.Arch
import org.glavo.build.Product
import org.glavo.build.tasks.BuildNative
import org.glavo.build.util.Utils
import org.glavo.build.tasks.ExtractIDE
import org.glavo.build.tasks.GenerateReadMe
import org.glavo.build.tasks.TransformIDE

plugins {
    id("java")
    id("de.undercouch.download") version "5.6.0"
}

group = "org.glavo"
version = "0.1.0"

val downloadDir = layout.buildDirectory.dir("download").get()
val configDir = layout.projectDirectory.dir("config")
val templateDir = layout.projectDirectory.dir("template")

val Download.outputFile: File
    get() = outputFiles.first()

fun nativesFile(arch: Arch) = project.file("resources/natives-linux-${arch.normalize()}.zip")

val arches = listOf(Arch.RISCV64, Arch.LOONGARCH64)
// val products = listOf(Product.IDEA_IC, Product.IDEA_IU)

val jdkProperties: Map<String, String> = Utils.loadProperties(configDir.file("jdk.properties"))
val downloadJDKTasks = arches.associateWith { arch ->
    jdkProperties["jdk.linux.${arch.normalize()}.url"]?.let { url ->
        tasks.register<Download>("downloadJDK-${arch.normalize()}") {
            src(url)
            dest(downloadDir.dir("jdk"))
            overwrite(false)
        }
    }
}

val nativesProperties: Map<String, String> = Utils.loadProperties(configDir.file("natives.properties"))

for (arch in Arch.values()) {
    val isCross = arch != Arch.current()
    val archName = arch.normalize()
    fun findArchProperty(name: String): String? = findProperty("$arch.$name")?.toString()

    val nativesFile = nativesFile(arch)

    val buildNatives = tasks.register<BuildNative>("buildNatives-$archName") {
        nativeProjectsRoot.set(project.file("native"))
        outputFile.set(nativesFile)

        targetArch.set(arch)
        zig.set(findArchProperty("zig"))
        cc.set(findArchProperty("cc") ?: (if (isCross) arch.getTriple(null) + "-gcc" else "gcc"))
        cxx.set(findArchProperty("cxx") ?: (if (isCross) arch.getTriple(null) + "-g++" else "g++"))
        make.set(findArchProperty("make") ?: "make")
        cMake.set(findArchProperty("cmake") ?: "cmake")
        go.set(findArchProperty("go") ?: "go")
        cargo.set(findArchProperty("cargo") ?: "cargo")
    }

    val nativesUrl = nativesProperties["natives.linux.$archName.url"]
    if (nativesUrl != null) {
        tasks.register<Download>("downloadNative-$archName") {
            src(nativesUrl)
            dest(nativesFile)
            overwrite(false)
        }
    } else {
        tasks.register("fetchNatives-$archName") {
            if (!nativesFile.exists()) {
                dependsOn(buildNatives)
            }
        }
    }
}

val allProductProperties: Map<String, String> = Utils.loadProperties(configDir.file("product.properties"))

for (product in Product.values()) {
    val productProperties = product.resolveProperties(allProductProperties)

    val productVersion = productProperties["version"]
    val productVersionAdditional = productProperties["version.additional"]
    val productBaseArch = Arch.of(productProperties["baseArch"])
    val targetVersion = "$productVersion+$productVersionAdditional"

    val downloadProductTask = tasks.register<Download>("download${product.productCode}") {
        inputs.properties(productProperties)

        src(product.getDownloadLink(productVersion, productBaseArch))
        dest(downloadDir.dir("ide"))
        overwrite(false)
    }

    tasks.register<ExtractIDE>("extract${product.productCode}") {
        dependsOn(downloadProductTask)

        sourceFile.set(downloadProductTask.get().outputFile)
        targetDir.set(
            downloadProductTask.get().outputFile.parentFile.resolve(
                product.getFileNameBase(productVersion, productBaseArch)
            )
        )
    }

    for (targetArch in arches) {
        tasks.register<TransformIDE>("transform${product.productCode}-$targetArch") {
            dependsOn(downloadProductTask, "fetchNatives-$targetArch")

            inputs.properties(productProperties)

            downloadJDKTasks[targetArch]?.let {
                dependsOn(it)
                jdkArchive.set(it.get().outputFile)
            }

            ideBaseArch.set(productBaseArch)
            ideProduct.set(product)
            ideBaseTar.set(downloadProductTask.get().outputFile)

            ideTargetArch.set(targetArch)
            ideNativesZipFile.set(nativesFile(targetArch))
            targetFile.set(
                layout.buildDirectory.dir("target").get()
                    .file(product.getFileNameBase(targetVersion, targetArch) + ".tar.gz")
            )
        }
    }
}


tasks.register<GenerateReadMe>("generateReadMe") {
    templateFile.set(templateDir.file("README.md.template"))
    propertiesFile.set(configDir.file("README.properties"))
    outputFile.set(project.file("README.md"))
}


// In order for Intellij to download the source code of dependencies
repositories {
    mavenCentral()
}