package br.com.mwconsulting.championcompanion.core.privacy

import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Phase003PrivacyGuardTest {
    @Test
    fun `phase three persistence scopes contain no prohibited location or route data`() {
        val root = projectRoot()
        val prohibitedTerms = listOf(
            "latitude", "longitude", "altitude", "rota", "polyline", "segmentos de rota",
            "GPX", "TCX", "FIT", "ExerciseRoute",
        )
        val scopes = listOf(
            "app/src/main/java/br/com/mwconsulting/championcompanion/core/model",
            "app/src/main/java/br/com/mwconsulting/championcompanion/core/database",
            "app/src/main/java/br/com/mwconsulting/championcompanion/data",
            "app/src/main/java/br/com/mwconsulting/championcompanion/core/logging",
            "app/src/test/java/br/com/mwconsulting/championcompanion",
            "app/src/androidTest/java/br/com/mwconsulting/championcompanion",
        )
        val allowedPolicyTests = setOf(
            "app/src/test/java/br/com/mwconsulting/championcompanion/core/logging/DebugStructuredLoggerTest.kt",
            "app/src/test/java/br/com/mwconsulting/championcompanion/core/privacy/Phase003PrivacyGuardTest.kt",
        )
        val violations = scopes.flatMap { scope ->
            kotlinFiles(root.resolve(scope)).filter { file -> file.relativeTo(root).path !in allowedPolicyTests }.flatMap { file ->
                val content = file.readText()
                prohibitedTerms.filter { term -> content.contains(term, ignoreCase = true) }
                    .map { term -> "${file.relativeTo(root)}: $term" }
            }
        }

        assertEquals("Only explicit privacy policy tests may name prohibited terms.", emptyList<String>(), violations)
    }

    @Test
    fun `manifest disables auto backup and references deny all rules`() {
        val root = projectRoot()
        val manifest = root.resolve("app/src/main/AndroidManifest.xml").readText()
        val backupRules = root.resolve("app/src/main/res/xml/backup_rules.xml").readText()
        val extractionRules = root.resolve("app/src/main/res/xml/data_extraction_rules.xml").readText()

        assertTrue(manifest.contains("android:allowBackup=\"false\""))
        assertTrue(manifest.contains("@xml/backup_rules"))
        assertTrue(manifest.contains("@xml/data_extraction_rules"))
        assertTrue(backupRules.contains("<exclude domain=\"database\" path=\".\""))
        assertTrue(extractionRules.contains("<exclude domain=\"database\" path=\".\""))
    }

    private fun projectRoot(): File = generateSequence(File(requireNotNull(System.getProperty("user.dir"))).absoluteFile) { it.parentFile ?: return@generateSequence null }
        .first { it.resolve("app/src/main/AndroidManifest.xml").isFile }

    private fun kotlinFiles(scope: File): List<File> = Files.walk(scope.toPath()).use { paths ->
        paths.filter { it.toString().endsWith(".kt") }.map { it.toFile() }.toList()
    }
}
