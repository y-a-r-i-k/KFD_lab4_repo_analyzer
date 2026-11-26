package analyzer

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class ReportParserTest {
    @Test
    fun analyzesReportAndSkipsBlankLines() {
        assertEquals(Summary(2, 20), analyzeReport("a.kt|12\n\nb.kt|8\n"))
    }

    @Test
    fun parsesLineAndTrimsPath() {
        assertEquals(FileStat("a.kt", 12), parseLine(" a.kt | 12 "))
    }

    @Test
    fun rejectsMissingSeparator() {
        assertThrows(IllegalArgumentException::class.java) { parseLine("a.kt") }
    }

    @Test
    fun rejectsNegativeLines() {
        assertThrows(IllegalArgumentException::class.java) { parseLine("a.kt|-1") }
    }

    @Test
    fun rejectsNonNumericLines() {
        assertThrows(IllegalArgumentException::class.java) { parseLine("a.kt|many") }
    }
}
