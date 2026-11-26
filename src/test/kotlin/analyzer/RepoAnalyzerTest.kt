package analyzer

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Files

/** Тесты расчётов: обход, подсчёт и сортировка. CLI здесь не запускается. */
class RepoAnalyzerTest {

    @TempDir
    lateinit var root: File

    /** Пример из задания: четыре файла. */
    private fun createSample() {
        File(root, "src").mkdirs()
        File(root, "src/A.kt").writeText("package sample\nclass A")
        File(root, "src/B.kt").writeText("package sample\nclass B\n")
        File(root, "README.md").writeText("")
        File(root, "notes.txt").writeText("ignored")
    }

    @Test
    fun reportsByExtensionForSample() {
        createSample()
        val expected = listOf(
            ExtensionStats("kt", files = 2, lines = 4),
            ExtensionStats("md", files = 1, lines = 0),
        )
        assertEquals(expected, analyze(root).byExtension)
    }

    @Test
    fun reportsLongestFilesForSample() {
        createSample()
        val expected = listOf(
            FileLines("src/A.kt", 2),
            FileLines("src/B.kt", 2),
            FileLines("README.md", 0),
        )
        assertEquals(expected, analyze(root).longestFiles)
    }

    @Test
    fun emptyDirectoryGivesEmptyReports() {
        assertEquals(AnalysisResult(emptyList(), emptyList()), analyze(root))
    }

    @Test
    fun missingPathIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            analyze(File(root, "does-not-exist"))
        }
    }

    @Test
    fun filePathIsRejected() {
        val file = File(root, "file.kt").apply { writeText("x") }
        assertThrows(IllegalArgumentException::class.java) { analyze(file) }
    }

    @Test
    fun countsLinesWithAndWithoutTrailingNewline() {
        assertEquals(0, countLines(File(root, "empty.md").apply { writeText("") }))
        assertEquals(1, countLines(File(root, "one.md").apply { writeText("abc") }))
        assertEquals(1, countLines(File(root, "one-nl.md").apply { writeText("abc\n") }))
        assertEquals(3, countLines(File(root, "three.md").apply { writeText("a\n\nc") }))
    }

    @Test
    fun skipsGitGradleAndBuildDirectories() {
        for (dir in listOf(".git", ".gradle", "build", "sub/build")) {
            File(root, dir).mkdirs()
            File(root, "$dir/Hidden.kt").writeText("x\ny\n")
        }
        File(root, "Visible.kts").writeText("x\n")

        val result = analyze(root)
        assertEquals(listOf(ExtensionStats("kts", 1, 1)), result.byExtension)
        assertEquals(listOf(FileLines("Visible.kts", 1)), result.longestFiles)
    }

    @Test
    fun keepsOnlyTopThreeSortedByLinesThenPath() {
        File(root, "b.kt").writeText("1\n2\n")
        File(root, "a.kt").writeText("1\n2\n")
        File(root, "c.md").writeText("1\n2\n3\n")
        File(root, "d.kts").writeText("1\n")

        val expected = listOf(FileLines("c.md", 3), FileLines("a.kt", 2), FileLines("b.kt", 2))
        assertEquals(expected, analyze(root).longestFiles)
    }

    @Test
    fun skipsSymbolicLinks() {
        val outside = Files.createTempDirectory("outside").toFile()
        File(outside, "Linked.kt").writeText("1\n2\n3\n")
        File(root, "Real.kt").writeText("1\n")

        // На Windows без прав разработчика ссылки создать нельзя — тогда тест пропускается.
        val created = runCatching {
            Files.createSymbolicLink(File(root, "linkDir").toPath(), outside.toPath())
            Files.createSymbolicLink(File(root, "Link.kt").toPath(), File(outside, "Linked.kt").toPath())
        }.isSuccess
        try {
            assumeTrue(created, "Символические ссылки недоступны в этой системе")
            assertEquals(listOf(FileLines("Real.kt", 1)), analyze(root).longestFiles)
        } finally {
            outside.deleteRecursively()
        }
    }
}
