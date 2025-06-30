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
import org.glavo.build.tasks.*
import org.glavo.build.util.Utils

plugins {
    id("java")
    id("de.undercouch.download") version "5.6.0"
}

group = "org.glavo"
version = "0.1.0"

val downloadDir = layout.buildDirectory.dir("download").get()
val configDir = layout.projectDirectory.dir("config")
val templateDir = layout.projectDirectory.dir("template")
val targetDir = layout.buildDirectory.dir("target")

val Download.outputFile: File
    get() = outputFiles.first()

fun nativesFile(arch: Arch) = project.file("resources/natives-linux-${arch.normalize()}.zip")

val arches = listOf(Arch.RISCV64, Arch.LOONGARCH64)

val jdkProperties: Map<String, String> = Utils.loadProperties(configDir.file("jdk.properties"))
val downloadJDKTasks = arches.associateWith { arch ->
    val propertyName = "jdk.linux.${arch.normalize()}.url"
    (findProperty(propertyName) ?: jdkProperties[propertyName])?.toString()?.let { url ->
        if (url.isEmpty()) null
        else tasks.register<Download>("downloadJDK-${arch.normalize()}") {
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
        group = "natives"

        nativeProjectsRoot.set(project.file("native"))
        outputFile.set(nativesFile)

        targetArch.set(arch)
        zig.set(findArchProperty("zig"))
        cc.set(findArchProperty("cc") ?: (if (isCross) arch.getTriple(null) + "-gcc" else "gcc"))
        cxx.set(findArchProperty("cxx") ?: (if (isCross) arch.getTriple(null) + "-g++" else "g++"))
        make.set(findArchProperty("make") ?: "make")
        cMake.set(findArchProperty("cmake") ?: "cmake")
        cargo.set(findArchProperty("cargo") ?: "cargo")
    }

    val nativesUrl = nativesProperties["natives.linux.$archName.url"]
    if (nativesUrl != null) {
        tasks.register<Download>("fetchNatives-$archName") {
            group = "natives"

            src(nativesUrl)
            dest(nativesFile)
            overwrite(false)
        }
    } else {
        tasks.register("fetchNatives-$archName") {
            group = "natives"

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
        group = "download"

        inputs.properties(productProperties)

        src(product.getDownloadLink(productVersion, productBaseArch))
        dest(downloadDir.dir("ide"))
        overwrite(false)
    }

    tasks.register<ExtractIDE>("extract${product.productCode}") {
        group = "download"

        dependsOn(downloadProductTask)

        sourceFile.set(downloadProductTask.get().outputFile)
        targetDir.set(
            downloadProductTask.get().outputFile.parentFile.resolve(
                product.getFileNameBase(productVersion, productBaseArch)
            )
        )
    }

    for (targetArch in arches) {
        val transformTask = tasks.register<TransformIDE>("transform${product.productCode}-$targetArch") {
            group = "build"

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
            targetFile.set(targetDir.map { it.file(product.getFileNameBase(targetVersion, targetArch) + ".tar.gz") })
        }

        tasks.register<CreateDeb>("createDeb${product.productCode}-$targetArch") {
            group = "build"

            dependsOn(transformTask)

            version.set(targetVersion)
            debArch.set(targetArch.debArch)
            ideProduct.set(product)
            tarFile.set(transformTask.flatMap { it.targetFile })
            outputFile.set(targetDir.map { it.file(product.getFileNameBase(targetVersion, targetArch) + ".deb") })
        }

        if (targetArch == Arch.LOONGARCH64) {
            // Some distributions use loong64, others use loongarch64
            // We solve this problem by creating two deb packages at the same time
            tasks.register<CreateDeb>("createDeb${product.productCode}-loong64") {
                group = "build"

                dependsOn(transformTask)

                version.set(targetVersion)
                debArch.set("loong64")
                ideProduct.set(product)
                tarFile.set(transformTask.flatMap { it.targetFile })
                outputFile.set(targetDir.map { it.file(product.getFileNameBase(targetVersion, "loong64") + ".deb") })
            }
        }
    }
}

tasks.register<GenerateReadMe>("generateReadMe") {
    group = "documentation"

    templateFile.set(templateDir.file("README.md.template"))
    propertiesFile.set(configDir.file("README.properties"))
    outputFile.set(project.file("README.md"))
}

// In order for Intellij to download the source code of dependencies
repositories {
    mavenCentral()
}