package dev.lacelang.lacetest

data class AnalysisResult(
    val outcome: String,
    val elapsedMs: Int,
    val failures: List<String>,
    val warnings: List<String>,
    val errors: List<String>,
)

@Suppress("UNCHECKED_CAST")
internal fun analyzeProbeResult(result: Map<String, Any?>): AnalysisResult {
    val outcome = result["outcome"] as? String ?: "unknown"
    val elapsedMs = (result["elapsedMs"] as? Number)?.toInt() ?: 0
    val calls = (result["calls"] as? List<Map<String, Any?>>) ?: emptyList()

    val failures = mutableListOf<String>()
    val warnings = mutableListOf<String>()
    val errors = mutableListOf<String>()

    for (call in calls) {
        val index = (call["index"] as? Number)?.toInt() ?: 0
        val error = call["error"] as? String
        if (error != null) errors.add("call[$index]: $error")

        val request = call["request"] as? Map<String, Any?>
        val method = (request?.get("method") as? String)?.uppercase() ?: "?"
        val url = request?.get("url") as? String ?: "?"
        val callLabel = "call[$index] $method $url"

        if (call["outcome"] == "skipped") continue

        val assertions = (call["assertions"] as? List<Map<String, Any?>>) ?: emptyList()
        for (assertion in assertions) {
            if (assertion["outcome"] != "failed") continue

            val assertMethod = assertion["method"] as? String ?: "unknown"
            val scope = assertion["scope"] as? String
            val actual = assertion["actual"]
            val expected = assertion["expected"]
            val op = assertion["op"] as? String

            val detail = buildString {
                append(callLabel)
                append(" .${assertMethod}(")
                if (scope != null) append("$scope: ")
                append("expected=$expected")
                if (op != null) append(", op=$op")
                append(") actual=$actual")
            }

            if (assertMethod == "check") warnings.add(detail) else failures.add(detail)
        }
    }

    return AnalysisResult(outcome, elapsedMs, failures, warnings, errors)
}
