import okhttp3.OkHttpClient
import okhttp3.Request
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.listProperty
import java.io.File
import java.nio.file.Files

open class RunAnnotator : DefaultTask() {
    @Input
    val extraOptions = project.objects.listProperty<String>()
    private val projectPath: String get() = project.projectDir.absolutePath

    private val buildFolder: File get() = File("$projectPath/build")
    private val annotatorFolder: File get() = File("$projectPath/build/annotator")
    private val tsvFilePath: String get() = "$projectPath/build/annotator/paths.tsv"
    private val tsvFile: File get() = File(tsvFilePath)
    private val jarUrl = "https://repo.maven.apache.org/maven2/edu/ucr/cs/riple/annotator/annotator-core/1.3.8/annotator-core-1.3.8.jar"
    private val jarVersion = "1.3.8"
    private val jarName = "annotator-core-$jarVersion.jar"
    private val annotatorJarPath: String get() = "$projectPath/build/annotator/$jarName"
    private val initializerClass = "com.example.Initializer"
    @TaskAction
    fun runAnnotator() {
        println("Target Project Path: $projectPath")

        ensureFolderExists(buildFolder)
        ensureFolderExists(annotatorFolder)

        println("TSV File Path: $tsvFilePath")

        if (!tsvFile.exists()) {
            writePathsToTsv()
        }

        downloadJarIfNotExist(jarUrl, annotatorJarPath)

        callJar()
    }
    private fun downloadJarIfNotExist(url: String, destPath: String) {
        val jarFile = File(destPath)
        if (!jarFile.exists()) {
            try {
                println("Downloading jar file from $url to $destPath")

                val client = OkHttpClient()
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()

                if (response.isSuccessful) {
                    response.body?.bytes()?.let { Files.write(jarFile.toPath(), it) }
                } else {
                    println("Failed to download Annotator jar file. HTTP Status Code: ${response.code}")
                }
            } catch (e: Exception) {
                println("Error downloading jar file from $url. Error: $e")
            }
        }else{
            println("Jar file already exists at $destPath")
        }
    }
    private fun ensureFolderExists(folder: File) {
        if (!folder.exists()) {
            folder.mkdir()
        }
    }

    private fun writePathsToTsv() {
        try {
            tsvFile.bufferedWriter().use { writer ->
                val nullAwayConfigPath = "$projectPath/build/annotator/nullaway.xml"
                val annotatorConfigPath = "$projectPath/build/annotator/scanner.xml"
                writer.write("$nullAwayConfigPath\t$annotatorConfigPath")
            }
        } catch (e: FileSystemException) {
            println("FS Error: $e")
        }
    }
    private fun callJar() {
        val gradlewCommand = "\"cd $projectPath && ./gradlew build -x test\""

        val buildCommand = listOf(
            "java",
            "-jar", annotatorJarPath,
            "-d", annotatorFolder.toString(),
            "-cp", tsvFilePath,
            "-i", initializerClass,
            "--build-command", gradlewCommand,
            "-cn", "NULLAWAY",
        )+ extraOptions.get()

        println("\nExecuting the following command:\n${buildCommand.joinToString(" ")}")

        val process = ProcessBuilder(buildCommand)
            .start()

        // Read the output stream
        val reader = process.inputStream.bufferedReader()
        reader.forEachLine { println(it) }

        // Read the error stream
        val errorReader = process.errorStream.bufferedReader()
        errorReader.forEachLine { System.err.println(it) }

        val exitCode = process.waitFor()

        if (exitCode != 0) {
            println("\nError: Annotator returned with exit code $exitCode.")
        }
    }

}