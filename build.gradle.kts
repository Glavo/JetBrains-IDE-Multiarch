import de.undercouch.gradle.tasks.download.Download
import org.glavo.build.Arch
import org.glavo.build.Product
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
val baseArch = Arch.AARCH64

val Download.outputFile: File
    get() = outputFiles.first()

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

val defaultProductProperties: Map<String, String> = Utils.loadProperties(configDir.dir("product").file("default.properties"))

for (product in products) {
    val productProperties = defaultProductProperties + Utils.loadProperties(configDir.dir("product").file("$product.properties"))

    val productVersion = productProperties["product.version"]!!
    val productVersionAdditional = productProperties["product.version.additional"]!!

    val downloadProductTask = tasks.create<Download>("download${product.productCode}") {
        inputs.properties(productProperties)

        src(product.getDownloadLink(productVersion, baseArch))
        dest(downloadDir.dir("ide"))
        overwrite(false)
    }

    tasks.create<ExtractIDE>("extract${product.productCode}") {
        dependsOn(downloadProductTask)

        sourceFile.set(downloadProductTask.outputFile)
        targetDir.set(
            downloadProductTask.outputFile.parentFile.resolve(
                product.getFileNameBase(productVersion, baseArch)
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

            ideBaseArch.set(baseArch)
            ideProduct.set(product)
            ideBaseTar.set(downloadProductTask.outputFile)

            ideArch.set(targetArch)
            ideNativesZipFile.set(
                layout.projectDirectory.dir("resources").file("natives-linux-${targetArch.normalize()}.zip").asFile
            )
            targetFile.set(
                layout.buildDirectory.dir("target").get()
                    .file(product.getFileNameBase("$productVersion+$productVersionAdditional", targetArch) + ".tar.gz").asFile
            )
        }
    }
}

tasks.create<GenerateReadMe>("generateReadMe") {
    templateFile.set(templateDir.file("README.md.template"))
    propertiesFile.set(configDir.file("README.properties"))
    outputFile.set(project.file("README.md"))
}
