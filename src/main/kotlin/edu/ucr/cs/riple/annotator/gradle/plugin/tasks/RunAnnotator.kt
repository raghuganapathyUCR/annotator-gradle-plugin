import edu.ucr.cs.riple.annotator.gradle.plugin.AnnotatorExtension
import edu.ucr.cs.riple.annotator.gradle.plugin.OutDir

import okhttp3.OkHttpClient
import okhttp3.Request
import org.gradle.api.DefaultTask

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



    private val buildFolder: File get() = File(OutDir.path)
    private val annotatorFolder: File get() = File("${OutDir.path}/annotator")
    private val tsvFilePath: String get() = "$projectPath/build/annotator/paths.tsv"
//    private val tsvFilePath: String get() = "${OutDir.path}/annotator/paths.tsv"

    private val tsvFile: File get() = File(tsvFilePath)



    private val jarUrl = "https://repo.maven.apache.org/maven2/edu/ucr/cs/riple/annotator/annotator-core/1.3.8/annotator-core-1.3.8.jar"
    private val jarVersion = "1.3.8"
    private val jarName = "annotator-core-$jarVersion.jar"
    private val annotatorJarPath: String get() = "${OutDir.path}/annotator/$jarName"





    private val initializerClass = "com.uber.nullaway.annotations.Initializer"



    private val projectType : ModuleType get() = detectType()

    // extra options for the annotator build command
    @Input
    val extraOptions = project.objects.listProperty<String>()

    @Input
    lateinit var annotatorExtension: AnnotatorExtension


// read the enableAnnotator flag which is in the annotatorOptions from AnnotatorPlugin.kt



    @TaskAction
    fun runAnnotator() {

            ensureFolderExists(buildFolder)
            ensureFolderExists(annotatorFolder)

            if (!tsvFile.exists()) {
//               make the annotator build folder
                println("TSV file does not exist")
                tsvFile.parentFile.mkdirs()
                tsvFile.createNewFile()
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
            println(folder.absolutePath + " does not exist. Creating...")
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

//    private fun writePathsToTsv() {
//        try {
//            // use temp location in ./build to build the paths
//            tsvFile.bufferedWriter().use { writer ->
//                val nullAwayConfigPath = "${OutDir.path}/annotator/nullaway.xml"
//                val annotatorConfigPath ="${OutDir.path}/annotator/scanner.xml"
//                writer.write("$nullAwayConfigPath\t$annotatorConfigPath")
//                writer.close()
//            }
//        } catch (e: FileSystemException) {
//            println("FS Error: $e")
//        }
//    }

    // Build the command to compile a single submodule
    private fun buildCompileJavaCommand(): String {
        val projectPath = project.path
        val taskName = getJavaCompileTaskName()
        val excludeTask = "test"

        // Constructing the command
        val command = "./gradlew $projectPath:$taskName -x $excludeTask"

        return command
    }
    private fun callJar() {

        // Set the gradlewCommand based on the module type

        val javaCompileCommand = getJavaCompileTaskName()



        val gradlewCommand = when (projectType) {
            ModuleType.SINGLE_MODULE -> "\"cd $projectPath && ./gradlew $javaCompileCommand -x test\""
            ModuleType.SINGLE_SUBMODULE -> "\"cd ${project.rootProject.projectDir.absolutePath} && ${buildCompileJavaCommand()}\""
            ModuleType.MULTI_MODULE_PARENT -> "\"cd $projectPath && ./gradlew $javaCompileCommand -x test\""
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
        else {
            println("\nAnnotator finished successfully.")
        }
        clearOutDir()
    }

    private fun clearOutDir() {
        val outDir = File(OutDir.path+"/annotator/0")

        val annotatorDir = File("$projectPath/build/annotator")

        if (outDir.exists()) {
            println("Deleting outDir: ${outDir.path}")
            outDir.deleteRecursively()
        }
        if (annotatorDir.exists()) {
            println("Deleting annotatorDir: ${annotatorDir.path}")
            annotatorDir.deleteRecursively()
        }
    }

    private fun detectType(): ModuleType {
        return when {
//            single project detected when the project is the root project and has no submodules
            project == project.rootProject && project.childProjects.isEmpty() -> {
                println("Single module detected")
                ModuleType.SINGLE_MODULE
            }
//            single submodule detected when the project is not the root project and has no submodules
            project != project.rootProject && project.childProjects.isEmpty() -> {
                println("Single submodule detected")
                ModuleType.SINGLE_SUBMODULE
            }
//            multi module parent detected when the project is the root project and has submodules
            else -> {
                println("Multi module parent detected")
                ModuleType.MULTI_MODULE_PARENT
            }
        }
    }

//    function that return "compileJava" if it is present in JavaCompile tasks, else (if android project) return "compileDebugJavaWithJavac"
    private fun getJavaCompileTaskName(): String {
        return if (project.tasks.findByName("compileJava") != null) {

            "compileJava"
        } else {
//            add checks here for whether the project is an android project - does compileDebugJavaWithJavac exist?
//            TODO ask Manu about how to handle if both are not there
//            Nima idea: look for tasks without "test" in their name
            "compileDebugJavaWithJavac"
        }
    }

    enum class ModuleType {
        SINGLE_MODULE,
        SINGLE_SUBMODULE,
        MULTI_MODULE_PARENT
    }

}