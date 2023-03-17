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

import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.ErrorProneOptions
import net.ltgt.gradle.errorprone.ErrorPronePlugin
import net.ltgt.gradle.errorprone.errorprone
import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.withType
import org.gradle.process.CommandLineArgumentProvider
import org.gradle.util.GradleVersion

private const val EXTENSION_NAME = "annotator"

class AnnotatorPlugin : Plugin<Project> {

    companion object {
        const val PLUGIN_ID = "edu.ucr.cs.riple.annotator.gradle.plugin"
    }

    @Override
    override fun apply(project: Project) = with(project) {
        if (GradleVersion.current() < GradleVersion.version("5.2.1")) {
            throw UnsupportedOperationException("$PLUGIN_ID requires at least Gradle 5.2.1")
        }

        val extension = extensions.create(EXTENSION_NAME, AnnotatorExtension::class)

        pluginManager.withPlugin(ErrorPronePlugin.PLUGIN_ID) {
            tasks.withType<JavaCompile>().configureEach {
                val annotatorOptions = (options.errorprone as ExtensionAware).extensions.create(
                    EXTENSION_NAME,
                    AnnotatorExtension::class,
                    extension
                )

            }
        }
    }
}

val ErrorProneOptions.annotator
    get() = (this as ExtensionAware).extensions.getByName(EXTENSION_NAME)

fun ErrorProneOptions.annotator(action: Action<in AnnotatorOptions>) =
    (this as ExtensionAware).extensions.configure(EXTENSION_NAME, action)
