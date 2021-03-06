package org.jetbrains.kotlin.annotation

import org.gradle.api.logging.Logging
import org.jetbrains.kotlin.gradle.plugin.kotlinDebug
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import java.io.File

internal class AnnotationFileUpdaterImpl(private val generatedAnnotationFile: File) : AnnotationFileUpdater {
    private val logger = Logging.getLogger(this.javaClass)
    private val lastSuccessfullyUpdatedFile = File.createTempFile("kapt-annotations-copy", "tmp")

    init {
        if (generatedAnnotationFile.exists()) {
            generatedAnnotationFile.copyTo(lastSuccessfullyUpdatedFile, overwrite = true)
        }
        else {
            lastSuccessfullyUpdatedFile.writeText("")
        }
    }

    override fun updateAnnotations(outdatedClasses: Iterable<JvmClassName>) {
        val outdatedClassesFqNames = outdatedClasses.mapTo(java.util.HashSet<String>()) { it.fqNameForClassNameWithoutDollars.asString() }

        val annotationsProvider = MutableKotlinAnnotationProvider().apply {
            addAnnotationsFrom(lastSuccessfullyUpdatedFile)
            removeClasses(outdatedClassesFqNames)
            logger.kotlinDebug { "Removed annotation entries for fq-names [${outdatedClassesFqNames.joinToString()}]" }

            if (generatedAnnotationFile.exists()) {
                addAnnotationsFrom(generatedAnnotationFile)
                logger.kotlinDebug { "Added annotation entries from $generatedAnnotationFile" }
            }
        }

        generatedAnnotationFile.delete()
        generatedAnnotationFile.bufferedWriter().use { writer ->
            annotationsProvider.writeAnnotations(CompactAnnotationWriter(writer))
            logger.kotlinDebug { "Written updated annotations to $generatedAnnotationFile" }
        }
        generatedAnnotationFile.copyTo(lastSuccessfullyUpdatedFile, overwrite = true)
    }

    override fun revert() {
        lastSuccessfullyUpdatedFile.copyTo(generatedAnnotationFile, overwrite = true)
    }
}