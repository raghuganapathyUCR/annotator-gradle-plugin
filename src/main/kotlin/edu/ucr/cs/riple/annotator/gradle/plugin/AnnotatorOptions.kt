package edu.ucr.cs.riple.annotator.gradle.plugin

import org.gradle.api.model.ObjectFactory

open class AnnotatorOptions internal constructor(
    objectFactory: ObjectFactory,
    annotatorExtension: AnnotatorExtension
) {}
