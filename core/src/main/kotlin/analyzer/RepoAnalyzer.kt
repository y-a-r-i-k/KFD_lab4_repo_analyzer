package analyzer

import java.io.File
import java.nio.file.Files

/** Расширения файлов, которые мы считаем (без точки). */
val SUPPORTED_EXTENSIONS: Set<String> = setOf("kt", "kts", "md")

/** Каталоги, в которые не заходим вообще. */
val SKIPPED_DIRECTORIES: Set<String> = setOf(".git", ".gradle", "build")

/** Сколько самых длинных файлов показываем во втором отчёте. */
const val TOP_FILES_LIMIT = 3

/** Одна строка первого отчёта: расширение, число файлов и суммарное число строк. */
data class ExtensionStats(val extension: String, val files: Int, val lines: Int)

/** Один найденный файл: путь относительно корня (всегда через '/') и число строк. */
data class FileLines(val path: String, val lines: Int)

/** Результат анализа: оба отчёта уже отсортированы и готовы к выводу. */
data class AnalysisResult(
    val byExtension: List<ExtensionStats>,
    val longestFiles: List<FileLines>,
)

/**
 * Главная функция анализа.
 * Бросает IllegalArgumentException, если [root] не существует или это не каталог.
 */
fun analyze(root: File): AnalysisResult {
    require(root.isDirectory) { "Not a directory or does not exist: ${root.path}" }

    val files = collectFiles(root)
    return AnalysisResult(
        byExtension = statsByExtension(files),
        longestFiles = longestFiles(files),
    )
}

/** Обходит каталог и возвращает все подходящие файлы с числом строк. */
internal fun collectFiles(root: File): List<FileLines> =
    root.walkTopDown()
        // onEnter вызывается ДО входа в каталог: false = пропустить всю папку.
        // Корень пускаем всегда, даже если он сам называется, например, build.
        .onEnter { dir -> dir == root || shouldEnter(dir) }
        .filter { it.isFile }
        .filter { it.extension in SUPPORTED_EXTENSIONS }
        // Ссылки на файлы проверяем перед чтением.
        .filterNot { Files.isSymbolicLink(it.toPath()) }
        .map { file ->
            FileLines(
                path = file.relativeTo(root).invariantSeparatorsPath,
                lines = countLines(file),
            )
        }
        .toList()

/** Заходим в каталог, только если он не из списка пропускаемых и не символическая ссылка. */
private fun shouldEnter(dir: File): Boolean =
    dir.name !in SKIPPED_DIRECTORIES && !Files.isSymbolicLink(dir.toPath())

/**
 * Считает строки файла. useLines читает UTF-8 и сам закрывает файл.
 * Пустой файл -> 0, "abc" без перевода строки -> 1.
 */
internal fun countLines(file: File): Int =
    // Эксперимент со сборкой: цикл заменён на эквивалентную операцию коллекции.
    // Сигнатура и результат прежние, меняется только тело функции.
    file.useLines { lines -> lines.count() }

/** Первый отчёт: группируем по расширению, сортируем расширения по алфавиту. */
internal fun statsByExtension(files: List<FileLines>): List<ExtensionStats> =
    files.groupBy { it.path.substringAfterLast('.') }
        .map { (ext, group) -> ExtensionStats(ext, group.size, group.sumOf { it.lines }) }
        .sortedBy { it.extension }

/** Второй отчёт: больше строк — выше; при равенстве — по пути по алфавиту. */
internal fun longestFiles(files: List<FileLines>, limit: Int = TOP_FILES_LIMIT): List<FileLines> =
    files.sortedWith(compareByDescending<FileLines> { it.lines }.thenBy { it.path })
        .take(limit)
