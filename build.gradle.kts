import de.undercouch.gradle.tasks.download.Download
import org.glavo.build.Arch
import org.glavo.build.tasks.ExtractIntelliJ
import org.glavo.build.tasks.GenerateReadMe
import org.glavo.build.tasks.TransformIntelliJ

plugins {
    id("de.undercouch.download") version "5.6.0"
}

group = "org.glavo"
version = property("idea.version") as String

val downloadDir = layout.buildDirectory.dir("download").get()
val ijBaseArch = Arch.AARCH64
val ijProductCode = property("idea.product_code") as String
val ijVersionAdditional = project.property("idea.version.additional") as String
val ijFileNameBase = "idea$ijProductCode-$version-${ijBaseArch.normalize()}"

var downloadIJ = tasks.create<Download>("downloadIJ") {
    src("https://download.jetbrains.com/idea/$ijFileNameBase.tar.gz")
    dest(downloadDir)
    overwrite(false)
}

val ijTar: File
    get() = downloadIJ.outputFiles.first()

tasks.create<ExtractIntelliJ>("extractIntelliJ") {
    dependsOn(downloadIJ)

    sourceFile.set(ijTar)
    targetDir.set(downloadDir.dir(ijFileNameBase).asFile)
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
        baseTar.set(ijTar)

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
