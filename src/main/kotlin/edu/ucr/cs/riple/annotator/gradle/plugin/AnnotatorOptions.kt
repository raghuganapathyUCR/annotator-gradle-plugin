package edu.ucr.cs.riple.annotator.gradle.plugin

import net.ltgt.gradle.errorprone.CheckSeverity
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.kotlin.dsl.listProperty
import org.gradle.kotlin.dsl.property

open class AnnotatorOptions internal constructor(
//    objectFactory: ObjectFactory,
//    annotatorExtension: AnnotatorExtension
) {
    /**
     * The path to scanner.xml config file.
     */
//    @get: Input
//    val enableAnnotator = objectFactory.property<Boolean>().apply {
//        set(annotatorExtension.enableAnnotator)
//    }
//    @get:Input @get:Optional
//    var depth = objectFactory.property<String>()
//    internal fun asArguments(): Iterable<String> = sequenceOf(
//        stringOption("depth", depth)
//    )
//        .filterNotNull()
//        .asIterable()
//    private fun stringOption(name: String, value: Provider<String>): String? =
//        value.orNull?.let { "-$name=$it" }
}
