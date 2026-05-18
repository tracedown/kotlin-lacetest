package dev.lacelang.lacetest

import dev.lacelang.executor.runScript
import dev.lacelang.validator.parse
import java.io.File
import java.io.PrintStream

/**
 * Runs all .lace scripts in a directory and produces a structured report.
 *
 * Unlike [LaceTestSuite] (which integrates with JUnit DynamicTests),
 * this class produces a standalone report suitable for CI output or
 * programmatic consumption.
 */
class LaceTestReport private constructor(
    private val scriptsDir: File,
    private val baseUrl: String?,
    private val vars: Map<String, Any?>,
    private val config: Map<String, Any?>?,
    private val extensions: List<String>,
    private val extensionPaths: List<String>,
    private val prevResults: Map<String, Any?>?,
    private val recursive: Boolean,
    private val quiet: Boolean,
) {

    data class ScriptResult(
        val name: String,
        val outcome: String,
        val elapsedMs: Int,
        val failures: List<String>,
        val warnings: List<String>,
        val errors: List<String>,
        val raw: Map<String, Any?>,
    ) {
        val passed: Boolean get() = outcome == "success" && failures.isEmpty() && errors.isEmpty()
    }

    data class Report(
        val results: List<ScriptResult>,
        val totalMs: Long,
    ) {
        val total: Int get() = results.size
        val passed: Int get() = results.count { it.passed }
        val failed: Int get() = results.count { !it.passed }
        val warnings: Int get() = results.sumOf { it.warnings.size }
        val allPassed: Boolean get() = failed == 0

        fun print(out: PrintStream = System.out) {
            out.println()
            out.println("=== Lace Test Report ===")
            out.println()

            for (r in results) {
                val icon = if (r.passed) "PASS" else "FAIL"
                out.println("  [$icon] ${r.name} (${r.elapsedMs}ms)")

                for (e in r.errors) out.println("         ERROR: $e")
                for (f in r.failures) out.println("         FAIL:  $f")
                for (w in r.warnings) out.println("         WARN:  $w")
            }

            out.println()
            out.println("--- $total scripts, $passed passed, $failed failed, $warnings warning(s) [${totalMs}ms] ---")
            out.println()
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun run(): Report {
        val scripts = discoverScripts()
        if (scripts.isEmpty()) {
            return Report(emptyList(), 0)
        }

        val startTime = System.currentTimeMillis()
        val results = scripts.map { file ->
            val displayName = scriptsDir.toPath().relativize(file.toPath()).toString().replace('\\', '/')
            runSingle(file, displayName)
        }
        val totalMs = System.currentTimeMillis() - startTime

        val report = Report(results, totalMs)
        if (!quiet) report.print()
        return report
    }

    @Suppress("UNCHECKED_CAST")
    private fun runSingle(file: File, displayName: String): ScriptResult {
        val source = file.readText()

        val ast = try {
            parse(source).toMap()
        } catch (e: Exception) {
            return ScriptResult(displayName, "error", 0, emptyList(), emptyList(), listOf("Parse error: ${e.message}"), emptyMap())
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
            return ScriptResult(displayName, "error", 0, emptyList(), emptyList(), listOf("Execution error: ${e.message}"), emptyMap())
        }

        val analysis = analyzeProbeResult(result)
        return ScriptResult(
            name = displayName,
            outcome = analysis.outcome,
            elapsedMs = analysis.elapsedMs,
            failures = analysis.failures,
            warnings = analysis.warnings,
            errors = analysis.errors,
            raw = result,
        )
    }

    private fun discoverScripts(): List<File> {
        if (!scriptsDir.isDirectory) return emptyList()
        val files = if (recursive) {
            scriptsDir.walkTopDown().filter { it.isFile && it.extension == "lace" }.toList()
        } else {
            scriptsDir.listFiles { f -> f.isFile && f.extension == "lace" }?.toList() ?: emptyList()
        }
        return files.sortedBy { it.name }
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
        private var quiet = false

        fun scriptsDir(path: String) = apply { scriptsDir = File(path) }
        fun scriptsDir(dir: File) = apply { scriptsDir = dir }
        fun baseUrl(url: String) = apply { baseUrl = url }
        fun vars(vars: Map<String, Any?>) = apply { this.vars.putAll(vars) }
        fun `var`(key: String, value: Any?) = apply { vars[key] = value }
        fun config(config: Map<String, Any?>) = apply { this.config = config }
        fun extension(name: String) = apply { extensions.add(name) }
        fun extensions(names: List<String>) = apply { extensions.addAll(names) }
        fun extensionPath(path: String) = apply { extensionPaths.add(path) }
        fun prevResults(prev: Map<String, Any?>) = apply { prevResults = prev }
        fun recursive(recursive: Boolean) = apply { this.recursive = recursive }
        fun quiet(quiet: Boolean = true) = apply { this.quiet = quiet }

        fun build(): LaceTestReport {
            val dir = scriptsDir ?: throw IllegalArgumentException("scriptsDir is required")
            return LaceTestReport(dir, baseUrl, vars.toMap(), config, extensions.toList(), extensionPaths.toList(), prevResults, recursive, quiet)
        }
    }
}
