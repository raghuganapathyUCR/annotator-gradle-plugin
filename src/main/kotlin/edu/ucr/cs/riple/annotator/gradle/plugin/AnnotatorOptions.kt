package edu.ucr.cs.riple.annotator.gradle.plugin

import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.Input
import org.gradle.kotlin.dsl.listProperty
import org.gradle.kotlin.dsl.property

open class AnnotatorOptions internal constructor(
    objectFactory: ObjectFactory,
    annotatorExtension: AnnotatorExtension
) {
    /**
     * The path to scanner.xml config file.
     */
    @get: Input
    val configPath = objectFactory.property<String>().apply {
        set(annotatorExtension.configPath)
    }
}
