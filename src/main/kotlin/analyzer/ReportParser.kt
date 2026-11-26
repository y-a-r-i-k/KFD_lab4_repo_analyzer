package analyzer

fun analyzeReport(report: String): Summary {
    val files = report.lineSequence()
        .filter { it.isNotBlank() }
        .map(::parseLine)
        .toList()
    return summarize(files)
}

internal fun parseLine(line: String): FileStat {
    val parts = line.split('|', limit = 2)
    require(parts.size == 2) { "Expected path|lines" }
    val path = parts[0].trim()
    require(path.isNotEmpty()) { "Path must not be blank" }
    val lines = requireNotNull(parts[1].trim().toIntOrNull()) {
        "Lines must be an integer"
    }
    require(lines >= 0) { "Lines must not be negative" }
    return FileStat(path, lines)
}
