import de.undercouch.gradle.tasks.download.Download
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.glavo.build.Arch
import org.glavo.build.tasks.ExtractIntelliJ
import org.glavo.build.tasks.GenerateReadMe
import org.glavo.build.tasks.TransformIntelliJ
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.GZIPInputStream

plugins {
    id("de.undercouch.download") version "5.6.0"
}

group = "org.glavo"
version = property("idea.version") as String

val downloadDir = layout.buildDirectory.dir("download").get()
val ijBaseArch = Arch.AARCH64
val ijBaseArchName = ijBaseArch.normalize()
val ijProductCode = property("idea.product_code") as String
val ijDir = downloadDir.dir("idea$ijProductCode-$version-$ijBaseArchName")
val ijVersionAdditional = project.property("idea.version.additional") as String

var downloadIJ = tasks.create<Download>("downloadIJ") {
    src("https://download.jetbrains.com/idea/idea$ijProductCode-$version-$ijBaseArchName.tar.gz")
    dest(downloadDir)
    overwrite(false)
}

val ijTar: Path
    get() = downloadIJ.outputFiles.first().toPath()

inline fun openTarInputStream(file: Path, action: (TarArchiveInputStream) -> Unit) {
    Files.newInputStream(file).use { rawInput ->
        GZIPInputStream(rawInput).use { gzipInput ->
            TarArchiveInputStream(gzipInput).use(action)
        }
    }
}

tasks.create<ExtractIntelliJ>("extractIntelliJ") {
    dependsOn(downloadIJ)

    sourceFile.set(ijTar.toFile())
    targetDir.set(ijDir.asFile)
}

val arches = listOf(Arch.RISCV64, Arch.LOONGARCH64)

for (targetArch in arches) {
    val downloadJRE = findProperty("idea.jdk.linux.${targetArch.normalize()}.url")?.let { url ->
        tasks.create<Download>("downloadJREFor${targetArch.normalize()}") {
            src(url)
            dest(downloadDir)
            overwrite(false)
        }
    }

    tasks.create<TransformIntelliJ>("createFor$targetArch") {
        dependsOn(downloadIJ)
        if (downloadJRE != null) {
            dependsOn(downloadJRE)
            jreFile.set(downloadJRE.outputFiles.first())
        }

        baseArch.set(ijBaseArch)
        productCode.set(ijProductCode)
        baseTar.set(ijTar.toFile())

        arch.set(targetArch)
        nativesZipFile.set(layout.projectDirectory.dir("resources").file("natives-linux-${targetArch.normalize()}.zip").asFile)
        outTar.set(layout.buildDirectory.dir("target").get().file("idea$ijProductCode-$version+$ijVersionAdditional-${targetArch.normalize()}.tar.gz").asFile)
    }
}

tasks.create<GenerateReadMe>("generateReadMe") {
    templateFile.set(project.file("template/README.md.template"))
    propertiesFile.set(project.file("template/README.properties"))
    outputFile.set(project.file("README.md"))
}
