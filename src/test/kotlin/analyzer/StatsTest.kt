package analyzer

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class StatsTest {
    @Test
    fun summarizesEmptyList() {
        assertEquals(Summary(0, 0), summarize(emptyList()))
    }

    @Test
    fun summarizesFiles() {
        val files = listOf(FileStat("a.kt", 12), FileStat("b.kt", 8))
        assertEquals(Summary(2, 20), summarize(files))
    }
}
