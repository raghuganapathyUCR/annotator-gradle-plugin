import edu.ucr.cs.riple.annotator.gradle.plugin.OutDir
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.File

//clean out folder inside the annotator folder
open class CleanOut : DefaultTask() {
    init {
        group = "annotator"
        description = "Clean outputs inside the annotator folder. (removes ./build/annotator/0)"
    }
    @TaskAction
    fun cleanOut() {
//        delete the annotator/out folder inside the build folder
        val annotatorFolder = File("${OutDir.path}/annotator/0")
        if (annotatorFolder.exists()) {
            println("Deleting ${annotatorFolder.absolutePath}")
            annotatorFolder.deleteRecursively()
        }
    }
}