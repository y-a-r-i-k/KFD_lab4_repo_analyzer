package analyzer

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream

/** Тесты CLI: аргументы, текст вывода и код завершения. */
class MainTest {

    @TempDir
    lateinit var root: File

    /** Результат одного запуска run(): код, stdout и stderr. */
    private data class RunResult(val code: Int, val out: String, val err: String)

    private fun runCli(vararg args: String): RunResult {
        val out = ByteArrayOutputStream()
        val err = ByteArrayOutputStream()
        val code = run(arrayOf(*args), PrintStream(out, true, "UTF-8"), PrintStream(err, true, "UTF-8"))
        // Нормализуем переводы строк, чтобы тест проходил и на Windows.
        return RunResult(code, out.toString("UTF-8").replace("\r\n", "\n"), err.toString("UTF-8"))
    }

    @Test
    fun printsBothReportsForSample() {
        File(root, "src").mkdirs()
        File(root, "src/A.kt").writeText("package sample\nclass A")
        File(root, "src/B.kt").writeText("package sample\nclass B\n")
        File(root, "README.md").writeText("")
        File(root, "notes.txt").writeText("ignored")

        val expected = """
            By extension:
            kt: 2 files, 4 lines
            md: 1 file, 0 lines

            Longest files:
            src/A.kt (2)
            src/B.kt (2)
            README.md (0)

        """.trimIndent()

        val result = runCli(root.path)
        assertEquals(EXIT_OK, result.code)
        assertEquals(expected, result.out)
    }

    @Test
    fun printsEmptyReportsForEmptyDirectory() {
        val result = runCli(root.path)
        assertEquals(EXIT_OK, result.code)
        assertEquals("By extension:\n\nLongest files:\n", result.out)
    }

    @Test
    fun failsOnMissingPath() {
        val result = runCli(File(root, "missing").path)
        assertNotEquals(0, result.code)
        assertTrue(result.err.isNotBlank())
        assertEquals("", result.out)
    }

    @Test
    fun failsWhenPathIsFile() {
        val file = File(root, "a.kt").apply { writeText("x") }
        val result = runCli(file.path)
        assertNotEquals(0, result.code)
        assertTrue(result.err.isNotBlank())
    }

    @Test
    fun failsWithoutArguments() {
        val result = runCli()
        assertNotEquals(0, result.code)
        assertTrue(result.err.isNotBlank())
    }
}
