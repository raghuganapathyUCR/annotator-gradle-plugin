import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.File

//clean annotator folder task - delete all annotator files in "build"
open class CleanAnnotator : DefaultTask() {

    @TaskAction
    fun cleanAnnotator() {
        val projectPath = project.projectDir.absolutePath
        println("Target Project Path: $projectPath")
//        delete the annotator folder inside the build folder
        val annotatorFolder = File("$projectPath/build/annotator")
        if (annotatorFolder.exists()) {
            annotatorFolder.deleteRecursively()
        }
    }
}
