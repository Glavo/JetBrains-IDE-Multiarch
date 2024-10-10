import de.undercouch.gradle.tasks.download.Download
import org.glavo.build.Arch
import org.glavo.build.Product
import org.glavo.build.tasks.BuildNative
import org.glavo.build.util.Utils
import org.glavo.build.tasks.ExtractIDE
import org.glavo.build.tasks.GenerateReadMe
import org.glavo.build.tasks.TransformIDE

plugins {
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
val products = listOf(Product.IDEA_IC, Product.IDEA_IU)

val jdkProperties: Map<String, String> = Utils.loadProperties(configDir.file("jdk.properties"))
val downloadJDKTasks = arches.associateWith { arch ->
    jdkProperties["jdk.linux.${arch.normalize()}.url"]?.let { url ->
        tasks.create<Download>("downloadJDK-${arch.normalize()}") {
            src(url)
            dest(downloadDir.dir("jdk"))
            overwrite(false)
        }
    }
}

val allProductProperties: Map<String, String> = Utils.loadProperties(configDir.file("product.properties"))

for (product in products) {
    val productProperties = product.resolveProperties(allProductProperties)

    val productVersion = productProperties["version"]
    val productVersionAdditional = productProperties["version.additional"]
    val productBaseArch = Arch.of(productProperties["baseArch"])
    val targetVersion = "$productVersion+$productVersionAdditional"

    val downloadProductTask = tasks.create<Download>("download${product.productCode}") {
        inputs.properties(productProperties)

        src(product.getDownloadLink(productVersion, productBaseArch))
        dest(downloadDir.dir("ide"))
        overwrite(false)
    }

    tasks.create<ExtractIDE>("extract${product.productCode}") {
        dependsOn(downloadProductTask)

        sourceFile.set(downloadProductTask.outputFile)
        targetDir.set(
            downloadProductTask.outputFile.parentFile.resolve(
                product.getFileNameBase(productVersion, productBaseArch)
            )
        )
    }

    for (targetArch in arches) {
        tasks.create<TransformIDE>("transform${product.productCode}-${targetArch.normalize()}") {
            dependsOn(downloadProductTask)

            inputs.properties(productProperties)

            downloadJDKTasks[targetArch]?.let {
                dependsOn(it)
                jdkArchive.set(it.outputFile)
            }

            ideBaseArch.set(productBaseArch)
            ideProduct.set(product)
            ideBaseTar.set(downloadProductTask.outputFile)

            ideTargetArch.set(targetArch)
            ideNativesZipFile.set(nativesFile(targetArch))
            targetFile.set(
                layout.buildDirectory.dir("target").get()
                    .file(product.getFileNameBase(targetVersion, targetArch) + ".tar.gz")
            )
        }
    }
}

for (arch in Arch.values()) {
    val isCross = arch != Arch.current()
    val archName = arch.normalize()
    fun findArchProperty(name: String): String? = findProperty("$arch.$name")?.toString()

    tasks.create<BuildNative>("buildNative-$archName") {
        nativeProjectsRoot.set(project.file("native"))
        outputFile.set(nativesFile(arch))

        targetArch.set(arch)
        zig.set(findArchProperty("zig"))
        cc.set(findArchProperty("cc") ?: (if (isCross) arch.getTriple(null) + "-gcc" else "gcc"))
        cxx.set(findArchProperty("cxx") ?: (if (isCross) arch.getTriple(null) + "-g++" else "g++"))
        make.set(findArchProperty("make") ?: "make")
        cMake.set(findArchProperty("cmake") ?: "cmake")
        go.set(findArchProperty("go") ?: "go")
        cargo.set(findArchProperty("cargo") ?: "cargo")
    }
}

tasks.create<GenerateReadMe>("generateReadMe") {
    templateFile.set(templateDir.file("README.md.template"))
    propertiesFile.set(configDir.file("README.properties"))
    outputFile.set(project.file("README.md"))
}
