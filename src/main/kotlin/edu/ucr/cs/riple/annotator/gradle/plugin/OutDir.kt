package edu.ucr.cs.riple.annotator.gradle.plugin

import java.nio.file.Files

object OutDir {
    val path: String by lazy {
        Files.createTempDirectory("annotator_temp").toFile().absolutePath
    }
}