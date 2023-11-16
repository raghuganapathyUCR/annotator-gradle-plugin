import edu.ucr.cs.riple.annotator.gradle.plugin.AnnotatorExtension
import edu.ucr.cs.riple.annotator.gradle.plugin.types.ModuleType
import okhttp3.OkHttpClient
import okhttp3.Request
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.listProperty
import java.io.File
import java.nio.file.Files

open class RunAnnotator : DefaultTask() {

    init{
        group = "annotator"
        description = "Run Annotator on the selected packgage (set NullAway:AnnotatedPackages in build.gradle before running)"
    }

    private val projectPath: String get() = project.projectDir.absolutePath

    private val buildFolder: File get() = File("$projectPath/build")
    private val annotatorFolder: File get() = File("$projectPath/build/annotator")
    private val tsvFilePath: String get() = "$projectPath/build/annotator/paths.tsv"

   // detect the type of the project - subproject or not
    private val projectType : ModuleType get() = detectType()
    // create the tsv file
    private val tsvFile: File get() = File(tsvFilePath)



    private val jarUrl = "https://repo.maven.apache.org/maven2/edu/ucr/cs/riple/annotator/annotator-core/1.3.8/annotator-core-1.3.8.jar"
    private val jarVersion = "1.3.8"
    private val jarName = "annotator-core-$jarVersion.jar"
    private val annotatorJarPath: String get() = "$projectPath/build/annotator/$jarName"

//    from nullaway 10.10 com.example.Initializer , com.uber.nullaway.annotations.Initializer
    private val initializerClass = "com.uber.nullaway.annotations.Initializer"
    // extra options for the annotator build command
    @Input
    val extraOptions = project.objects.listProperty<String>()

    @Input
    lateinit var annotatorExtension: AnnotatorExtension


// read the enableAnnotator flag which is in the annotatorOptions from AnnotatorPlugin.kt



    @TaskAction
    fun runAnnotator() {
        if (annotatorExtension.enableAnnotator.get()) {
            println("Annotator is enabled")
            println("Target Project Path: $projectPath")
            ensureFolderExists(buildFolder)
            ensureFolderExists(annotatorFolder)

            println("TSV File Path: $tsvFilePath")

            if (!tsvFile.exists()) {
                writePathsToTsv()
            }

            downloadJarIfNotExist(jarUrl, annotatorJarPath)

            callJar()
        } else {
            println("Annotator is not enabled")
            logger.warn("Set enableAnnotator to true in build.gradle to enable Annotator")
        }

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
            // use temp location in ./build to build the paths
            tsvFile.bufferedWriter().use { writer ->
                val nullAwayConfigPath = "$projectPath/build/annotator/nullaway.xml"
                val annotatorConfigPath = "$projectPath/build/annotator/scanner.xml"
                writer.write("$nullAwayConfigPath\t$annotatorConfigPath")
                writer.close()
            }
        } catch (e: FileSystemException) {
            println("FS Error: $e")
        }
    }
    private fun callJar() {

        // Set the gradlewCommand based on the module type
        val gradlewCommand = when (projectType) {
            ModuleType.SINGLE_MODULE -> "\"cd $projectPath && ./gradlew build -x test\""
            ModuleType.SINGLE_SUBMODULE -> "\"cd ${project.rootProject.projectDir.absolutePath} && ./gradlew build -p ${project.name} -x test\""
            ModuleType.MULTI_MODULE_PARENT -> "\"cd $projectPath && ./gradlew build -x test\""
        }

       // Build the command to run the annotator
        val buildCommand = listOf(
            "java",
            "-jar", annotatorJarPath,
            "-d", annotatorFolder.toString(),
            "-cp", tsvFilePath,
            "-i", initializerClass,
            "--build-command", gradlewCommand,
            "-cn", "NULLAWAY"
        ) + extraOptions.get()


        println("\nExecuting the following command:\n${buildCommand.joinToString(" ")}")

        // annotate the target project
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

    private fun detectType(): ModuleType {
        return when {
//            single project detected when the project is the root project and has no submodules
            project == project.rootProject && project.childProjects.isEmpty() -> {
                ModuleType.SINGLE_MODULE
            }
//            single submodule detected when the project is not the root project and has no submodules
            project != project.rootProject && project.childProjects.isEmpty() -> {
                ModuleType.SINGLE_SUBMODULE
            }
//            multi module parent detected when the project is the root project and has submodules
            else -> {
                ModuleType.MULTI_MODULE_PARENT
            }
        }
    }

}