import edu.ucr.cs.riple.annotator.gradle.plugin.OutDir
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.File

//clean annotator folder task - delete all annotator files in "build"
open class CleanAnnotator : DefaultTask() {
    init{
        group = "annotator"
        description = "Clean annotator folder. (removes ./build/annotator)"
    }
    @TaskAction
    fun cleanAnnotator() {
//        delete the annotator folder inside the build folder
        val annotatorFolder = File("${OutDir.path}/annotator")
        if (annotatorFolder.exists()) {
            println("Deleting ${annotatorFolder.absolutePath}")
            annotatorFolder.deleteRecursively()
        }
    }
}
