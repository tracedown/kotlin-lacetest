rootProject.name = "lacelang-kt-lacetest"

// Composite build: resolve sibling repos (local dev) or subdirectories (CI checkout)
fun findSibling(vararg candidates: String): File? =
    candidates.map { file(it) }.firstOrNull { it.isDirectory }

findSibling("../lacelang-kt-validator", "lacelang-kt-validator")?.let { includeBuild(it) }
findSibling("../lacelang-kt-executor", "lacelang-kt-executor")?.let { includeBuild(it) }
