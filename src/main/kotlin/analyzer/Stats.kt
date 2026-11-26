package analyzer

data class FileStat(val path: String, val lines: Int)

data class Summary(val files: Int, val totalLines: Int)

fun summarize(files: List<FileStat>): Summary =
    Summary(files.size, files.sumOf { it.lines })
