package analyzer

import java.io.File
import java.io.PrintStream
import kotlin.system.exitProcess

/** Коды завершения программы. */
const val EXIT_OK = 0
const val EXIT_USAGE = 2
const val EXIT_BAD_PATH = 1

/** Точка входа: вся логика в run(), здесь только код завершения процесса. */
fun main(args: Array<String>) {
    exitProcess(run(args, System.out, System.err))
}

/**
 * Разбирает аргументы, запускает анализ и печатает отчёты.
 * Возвращает код завершения, а не вызывает exitProcess — так run() удобно тестировать.
 */
fun run(args: Array<String>, out: PrintStream, err: PrintStream): Int {
    if (args.size != 1) {
        err.println("Usage: repo-analyzer <directory>")
        return EXIT_USAGE
    }

    val root = File(args[0])
    if (!root.isDirectory) {
        err.println("Error: '${args[0]}' does not exist or is not a directory")
        return EXIT_BAD_PATH
    }

    out.print(formatReport(analyze(root)))
    return EXIT_OK
}

/** Превращает результат анализа в текст обоих отчётов. */
fun formatReport(result: AnalysisResult): String = buildString {
    appendLine("By extension:")
    for (stats in result.byExtension) {
        appendLine("${stats.extension}: ${plural(stats.files, "file")}, ${plural(stats.lines, "line")}")
    }
    appendLine()
    appendLine("Longest files:")
    for (file in result.longestFiles) {
        appendLine("${file.path} (${file.lines})")
    }
}

/** 1 file, 2 files, 0 lines, 1 line. */
private fun plural(count: Int, word: String): String =
    if (count == 1) "$count $word" else "$count ${word}s"
