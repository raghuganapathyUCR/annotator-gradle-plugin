import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.File

//clean out folder inside the annotator folder
open class CleanOut : DefaultTask() {

    @TaskAction
    fun cleanOut() {
        val projectPath = project.projectDir.absolutePath
        println("Target Project Path: $projectPath")
//        delete the annotator/out folder inside the build folder
        val annotatorFolder = File("$projectPath/build/annotator/0")
        if (annotatorFolder.exists()) {
            annotatorFolder.deleteRecursively()
        }
    }
}