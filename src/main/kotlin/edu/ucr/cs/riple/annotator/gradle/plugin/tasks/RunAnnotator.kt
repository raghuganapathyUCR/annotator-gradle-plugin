import edu.ucr.cs.riple.annotator.gradle.plugin.AnnotatorExtension
import edu.ucr.cs.riple.annotator.gradle.plugin.OutDir
import edu.ucr.cs.riple.annotator.gradle.plugin.types.ModuleType

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException

import edu.ucr.cs.riple.core.Annotator
import edu.ucr.cs.riple.core.Config

import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

import org.gradle.kotlin.dsl.listProperty
import java.io.File


open class RunAnnotator : DefaultTask() {

    init {
        group = "annotator"
        description =
            "Run Annotator on the selected packgage (set NullAway:AnnotatedPackages in build.gradle before running)"
    }

    private val projectPath: String get() = project.projectDir.absolutePath


    private val buildFolder: File get() = File(OutDir.path)
    private val annotatorFolder: File get() = File("${OutDir.path}/annotator")
    private val tsvFilePath: String get() = "$projectPath/build/annotator/paths.tsv"
//    private val tsvFilePath: String get() = "${OutDir.path}/annotator/paths.tsv"

    private val tsvFile: File get() = File(tsvFilePath)


    private val jarUrl =
        "https://repo.maven.apache.org/maven2/edu/ucr/cs/riple/annotator/annotator-core/1.3.8/annotator-core-1.3.8.jar"
    private val jarVersion = "1.3.8"
    private val jarName = "annotator-core-$jarVersion.jar"
    private val annotatorJarPath: String get() = "${OutDir.path}/annotator/$jarName"


    private val initializerClass = "com.uber.nullaway.annotations.Initializer"


    private val projectType: ModuleType get() = detectType()

    // extra options for the annotator build command
    @Input
    val extraOptions = project.objects.listProperty<String>()

    @Input
    lateinit var annotatorExtension: AnnotatorExtension



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


        callJar()
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


    // Build the command to compile a single submodule
    private fun buildCompileJavaCommand(): String {
        val projectPath = project.path
        val taskName = getJavaCompileTaskName()
        val excludeTask = "test"

        // Constructing the command

        return "./gradlew $projectPath:$taskName -x $excludeTask"
    }

    private fun callJar() {

        // Set the gradlewCommand based on the module type

        val javaCompileCommand = getJavaCompileTaskName()


        val gradlewCommand = when (projectType) {
            ModuleType.SINGLE_MODULE -> "\"cd $projectPath && ./gradlew $javaCompileCommand -x test\""
            ModuleType.SINGLE_SUBMODULE -> "\"cd ${project.rootProject.projectDir.absolutePath} && ${buildCompileJavaCommand()}\""
            ModuleType.MULTI_MODULE_PARENT -> "\"cd ${project.rootProject.projectDir.absolutePath} && ./gradlew $javaCompileCommand -x test\""
        }

        // Build the command to run the annotator
        val buildCommand = listOf(
            "-d", annotatorFolder.toString(),
            "-cp", tsvFilePath,
            "-i", initializerClass,
            "--build-command", gradlewCommand,
            "-cn", "NULLAWAY"
        ) + extraOptions.get()


//        trying to use the Annotator API to start the annotator
        val config = Config(buildCommand.toTypedArray())
        val annotator = Annotator(config)
        annotator.start()

        clearOutDir()
    }

    private fun clearOutDir() {
        val outDir = File(OutDir.path + "/annotator/0")

//        val annotatorDir = File("$projectPath/build/annotator")

        if (outDir.exists()) {
            println("Deleting outDir: ${outDir.path}")
            outDir.deleteRecursively()
        }
//        if (annotatorDir.exists()) {
//            println("Deleting annotatorDir: ${annotatorDir.path}")
//            annotatorDir.deleteRecursively()
//        }
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
            if (project.tasks.findByName("compileDebugJavaWithJavac") != null) {
                "compileDebugJavaWithJavac"
            } else {
                throw GradleException("Could not find compileJava or compileDebugJavaWithJavac task")
            }
        }
    }
}