import de.undercouch.gradle.tasks.download.Download
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.glavo.build.Arch
import org.glavo.build.IJProcessor
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.GZIPInputStream
import kotlin.io.path.exists
import kotlin.io.path.outputStream

plugins {
    id("de.undercouch.download") version "5.6.0"
}

group = "org.glavo"
version = property("idea.version") as String

val downloadDir = layout.buildDirectory.dir("download").get()
val baseArch = Arch.AARCH64
val baseArchName = baseArch.getName()
val ijProductCode = property("idea.product_code") as String
val ijDir = downloadDir.dir("idea$ijProductCode-$version-$baseArchName")

var downloadIJ = tasks.create<Download>("downloadIJ") {
    src("https://download.jetbrains.com/idea/idea$ijProductCode-$version-$baseArchName.tar.gz")
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

tasks.create("extractIJ") {
    dependsOn(downloadIJ)
    outputs.dir(ijDir)

    doLast {
        if (System.getProperty("os.name").lowercase().contains("windows")) {
            throw GradleException("This task should not run on Windows")
        }

        val targetDir = ijDir.asFile.toPath()
        targetDir.toFile().deleteRecursively()
        Files.createDirectories(targetDir)

        openTarInputStream(ijTar) { tar ->
            val prefix = tar.nextEntry.let {
                if (it == null || !it.isDirectory || it.name.count { ch -> ch == '/' } != 1) {
                    throw GradleException("Invalid directory entry: ${it.name}")
                }

                it.name
            }

            do {
                val entry = tar.nextEntry ?: break

                entry.apply {
                    logger.info("Extracting $name (size=$size isDirectory=$isDirectory isSymbolicLink=$isSymbolicLink)")
                }

                if (!entry.name.startsWith(prefix)) {
                    throw GradleException("Invalid entry: ${entry.name}")
                }

                if (entry.isLink) {
                    throw GradleException("Unable handle link: ${entry.name}")
                }

                val targetName = entry.name.substring(prefix.length)
                if (targetName.isEmpty())
                    continue

                val target = targetDir.resolve(targetName)

                if (entry.isDirectory) {
                    Files.createDirectories(target)
                } else {
                    if (target.exists()) {
                        throw GradleException("Duplicate entry ${entry.name}")
                    }
                    if (entry.isSymbolicLink) {
                        Files.createSymbolicLink(target, Path.of(entry.linkName))
                    } else {
                        target.outputStream().use { tar.copyTo(it) }
                    }
                }
            } while (true)
        }
    }
}

val targetDir = layout.buildDirectory.dir("target").get()

val arches = listOf(Arch.RISCV64, Arch.LOONGARCH64)

for (arch in arches) {
    tasks.create("createFor$arch") {
        dependsOn(downloadIJ)

        val nativesZip = layout.projectDirectory.dir("resources").file("natives-linux-${arch.getName()}.zip")
        val output = targetDir.file("idea$ijProductCode-$version-${arch.getName()}.tar.gz")

        inputs.files(ijTar, nativesZip)
        outputs.file(output)

        doLast {
            IJProcessor(
                this,
                baseArch, ijProductCode, ijTar,
                arch, nativesZip.asFile.toPath(), output.asFile.toPath()
            ).apply { process() }
        }
    }
}