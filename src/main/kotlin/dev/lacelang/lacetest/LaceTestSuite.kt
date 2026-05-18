package dev.lacelang.lacetest

import dev.lacelang.executor.loadConfig
import dev.lacelang.executor.runScript
import dev.lacelang.validator.parse
import org.junit.jupiter.api.DynamicTest
import java.io.File
import java.nio.file.Path

/**
 * Discovers .lace scripts in a directory and runs them as JUnit 5 dynamic tests.
 *
 * expect() failures -> test failure
 * check() failures  -> warning (logged, test still passes)
 */
class LaceTestSuite private constructor(
    private val scriptsDir: File,
    private val baseUrl: String?,
    private val vars: Map<String, Any?>,
    private val config: Map<String, Any?>?,
    private val extensions: List<String>,
    private val extensionPaths: List<String>,
    private val prevResults: Map<String, Any?>?,
    private val recursive: Boolean,
    private val filter: ((File) -> Boolean)?,
    private val quiet: Boolean,
) {

    fun dynamicTests(): List<DynamicTest> {
        val scripts = discoverScripts()
        if (scripts.isEmpty()) {
            return listOf(DynamicTest.dynamicTest("No .lace scripts found in ${scriptsDir.path}") {
                throw AssertionError("No .lace scripts found in ${scriptsDir.absolutePath}")
            })
        }
        return scripts.map { file ->
            val displayName = scriptsDir.toPath().relativize(file.toPath()).toString().replace('\\', '/')
            DynamicTest.dynamicTest(displayName) { runLaceScript(file, displayName) }
        }
    }

    private fun discoverScripts(): List<File> {
        if (!scriptsDir.isDirectory) return emptyList()
        val files = if (recursive) {
            scriptsDir.walkTopDown().filter { it.isFile && it.extension == "lace" }.toList()
        } else {
            scriptsDir.listFiles { f -> f.isFile && f.extension == "lace" }?.toList() ?: emptyList()
        }
        val filtered = if (filter != null) files.filter(filter) else files
        return filtered.sortedBy { it.name }
    }

    @Suppress("UNCHECKED_CAST")
    private fun runLaceScript(file: File, displayName: String) {
        val source = file.readText()

        val ast = try {
            parse(source).toMap()
        } catch (e: Exception) {
            throw AssertionError("Parse error in $displayName: ${e.message}", e)
        }

        val mergedVars = buildMap {
            if (baseUrl != null) put("baseUrl", baseUrl)
            putAll(vars)
        }

        val result = try {
            runScript(
                ast = ast as Map<String, Any?>,
                scriptVars = mergedVars,
                prev = prevResults,
                activeExtensions = extensions.ifEmpty { null },
                extensionPaths = extensionPaths.ifEmpty { null },
                config = config,
            )
        } catch (e: Exception) {
            throw AssertionError("Execution error in $displayName: ${e.message}", e)
        }

        val analysis = analyzeProbeResult(result)

        // Log warnings from check() failures
        if (!quiet && analysis.warnings.isNotEmpty()) {
            System.err.println("  WARN [$displayName] ${analysis.warnings.size} check() warning(s):")
            for (w in analysis.warnings) {
                System.err.println("    - $w")
            }
        }

        // Fail on expect() failures or error outcome
        if (analysis.failures.isNotEmpty() || analysis.outcome == "failure" || analysis.outcome == "timeout") {
            val sb = StringBuilder()
            sb.appendLine("Lace script failed: $displayName")
            sb.appendLine("  Outcome: ${analysis.outcome}")
            sb.appendLine("  Elapsed: ${analysis.elapsedMs}ms")

            if (analysis.errors.isNotEmpty()) {
                sb.appendLine("  Errors:")
                for (e in analysis.errors) sb.appendLine("    - $e")
            }

            if (analysis.failures.isNotEmpty()) {
                sb.appendLine("  Failed assertions (${analysis.failures.size}):")
                for (f in analysis.failures) sb.appendLine("    - $f")
            }

            if (analysis.warnings.isNotEmpty()) {
                sb.appendLine("  Warnings (${analysis.warnings.size}):")
                for (w in analysis.warnings) sb.appendLine("    - $w")
            }

            throw AssertionError(sb.toString())
        }
    }

    companion object {
        fun builder() = Builder()
    }

    class Builder {
        private var scriptsDir: File? = null
        private var baseUrl: String? = null
        private var vars = mutableMapOf<String, Any?>()
        private var config: Map<String, Any?>? = null
        private var extensions = mutableListOf<String>()
        private var extensionPaths = mutableListOf<String>()
        private var prevResults: Map<String, Any?>? = null
        private var recursive = true
        private var filter: ((File) -> Boolean)? = null
        private var quiet = false

        fun scriptsDir(path: String) = apply { scriptsDir = File(path) }
        fun scriptsDir(path: Path) = apply { scriptsDir = path.toFile() }
        fun scriptsDir(dir: File) = apply { scriptsDir = dir }

        fun baseUrl(url: String) = apply { baseUrl = url }

        fun vars(vars: Map<String, Any?>) = apply { this.vars.putAll(vars) }
        fun `var`(key: String, value: Any?) = apply { vars[key] = value }

        fun config(config: Map<String, Any?>) = apply { this.config = config }
        fun configFile(path: String) = apply { this.config = loadConfig(explicitPath = path) }

        fun extension(name: String) = apply { extensions.add(name) }
        fun extensions(names: List<String>) = apply { extensions.addAll(names) }
        fun extensionPath(path: String) = apply { extensionPaths.add(path) }

        fun prevResults(prev: Map<String, Any?>) = apply { prevResults = prev }

        fun recursive(recursive: Boolean) = apply { this.recursive = recursive }

        fun filter(predicate: (File) -> Boolean) = apply { filter = predicate }

        fun quiet(quiet: Boolean = true) = apply { this.quiet = quiet }

        fun build(): LaceTestSuite {
            val dir = scriptsDir ?: throw IllegalArgumentException("scriptsDir is required")
            return LaceTestSuite(
                scriptsDir = dir,
                baseUrl = baseUrl,
                vars = vars.toMap(),
                config = config,
                extensions = extensions.toList(),
                extensionPaths = extensionPaths.toList(),
                prevResults = prevResults,
                recursive = recursive,
                filter = filter,
                quiet = quiet,
            )
        }
    }
}
