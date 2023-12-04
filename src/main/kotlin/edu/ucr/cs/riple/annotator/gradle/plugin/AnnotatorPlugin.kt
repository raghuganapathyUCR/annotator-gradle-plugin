/*
 * MIT License
 *
 * Copyright (c) 2023 Nima Karimipour
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package edu.ucr.cs.riple.annotator.gradle.plugin

import CleanAnnotator
import CleanOut
import RunAnnotator
import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.ErrorProneOptions
import net.ltgt.gradle.errorprone.ErrorPronePlugin
import net.ltgt.gradle.errorprone.errorprone

import org.gradle.api.Action


import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware


import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.create

import org.gradle.kotlin.dsl.withType
import org.gradle.util.GradleVersion



//Name of the extension
private const val EXTENSION_NAME = "annotator"

//Version of the annotator-scanner library to add to the target project
private const val ANNOTATOR_SCANNER_VERSION = "edu.ucr.cs.riple.annotator:annotator-scanner:1.3.8"

private const val NULLAWAY_ANNOTATIONS_VERSION = "com.uber.nullaway:nullaway-annotations:0.10.14"

/**
 * The Annotator plugin.
 *
 * This plugin adds the annotator-scanner library to the target project and configures the ErrorProne plugin to use
 * the AnnotatorScanner check.
 */

class AnnotatorPlugin : Plugin<Project> {

    companion object {
        const val PLUGIN_ID = "edu.ucr.cs.riple.annotator.plugin"
    }

    @Override
    override fun apply(project: Project): Unit = with(project) {
        logger.debug("ADDING ANNOTATOR TO TARGET")
        if (GradleVersion.current() < GradleVersion.version("5.2.1")) {
            throw UnsupportedOperationException("$PLUGIN_ID requires at least Gradle 5.2.1")
        }

        val extension = extensions.create(EXTENSION_NAME, AnnotatorExtension::class)

//        val tempDirPath = OutDir.path

        // Add the annotator-scanner library to the target project

        this.dependencies.add("annotationProcessor", ANNOTATOR_SCANNER_VERSION)
        this.dependencies.add("compileOnly", NULLAWAY_ANNOTATIONS_VERSION)



//        print path to paths.tsv

        // Configure the ErrorProne plugin to use the AnnotatorScanner check
        pluginManager.withPlugin(ErrorPronePlugin.PLUGIN_ID) {
            tasks.withType<JavaCompile>().configureEach {

                if (!name.toLowerCase().contains("test")) {
                    options.errorprone {

                        check("AnnotatorScanner", CheckSeverity.ERROR)


                        option("NullAway:SerializeFixMetadata", "true")
                        //need to make this more dynamic, extend the options object to include the path to the scanner.xml file by default
//                        option("NullAway:FixSerializationConfigPath", "${OutDir.path}/annotator/nullaway.xml")
//                        option("AnnotatorScanner:ConfigPath", "${OutDir.path}/annotator/scanner.xml")

                        option("NullAway:FixSerializationConfigPath", project.projectDir.absolutePath + "/build/annotator/nullaway.xml")
                        option("AnnotatorScanner:ConfigPath", project.projectDir.absolutePath + "/build/annotator/scanner.xml")
                    }
                }

            }
        }
        tasks.register("runAnnotator", RunAnnotator::class.java) {
            this.annotatorExtension = extension
        }
        tasks.register("cleanAnnotator", CleanAnnotator::class.java)
//        TODO think about wheter we need this task, usefull for debugging
        tasks.register("cleanAnnotatorOuts", CleanOut::class.java)

    }
}



val ErrorProneOptions.annotator
    get() = (this as ExtensionAware).extensions.getByName(EXTENSION_NAME)

fun ErrorProneOptions.annotator(action: Action<in AnnotatorOptions>) =
        (this as ExtensionAware).extensions.configure(EXTENSION_NAME, action)
