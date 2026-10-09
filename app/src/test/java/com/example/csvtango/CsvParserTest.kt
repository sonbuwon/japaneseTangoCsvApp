package com.example.csvtango

import com.example.csvtango.data.CsvParser
import org.junit.Assert.assertEquals
import org.junit.Test

class CsvParserTest {

    @Test
    fun `6열 파일은 예문까지 읽는다`() {
        val cards = CsvParser.read(
            """
            일본어,히라가나,한국어,예문,예문(히라가나),예문(한국어)
            一,いち,1 / 하나,一つください。,ひとつください。,하나 주세요.
            """.trimIndent().toByteArray(),
        )

        assertEquals(1, cards.size)
        assertEquals("一", cards[0].jp)
        assertEquals("いち", cards[0].kana)
        assertEquals("1 / 하나", cards[0].ko)
        assertEquals("ひとつください。", cards[0].exKana)
        assertEquals("하나 주세요.", cards[0].exKo)
    }

    @Test
    fun `따옴표 없이 쪼개진 3열 파일은 뜻을 다시 합친다`() {
        val cards = CsvParser.read("一,いち,1, 하나\n二,に,2, 둘".toByteArray())

        assertEquals(listOf("1, 하나", "2, 둘"), cards.map { it.ko })
        assertEquals("", cards[0].exJp)
    }

    @Test
    fun `따옴표 안의 쉼표와 줄바꿈을 보존한다`() {
        val rows = CsvParser.parse("\"가,나\",\"두\n줄\"")

        assertEquals(listOf(listOf("가,나", "두\n줄")), rows)
    }

    @Test
    fun `자리표시자와 빈 일본어 행은 걸러낸다`() {
        val cards = CsvParser.read("一,いち,-,-,-,-\n,,,,,\n".toByteArray())

        assertEquals(1, cards.size)
        assertEquals("", cards[0].ko)
        assertEquals("", cards[0].exKana)
    }

    @Test
    fun `BOM 이 붙은 UTF-8 파일도 읽는다`() {
        val cards = CsvParser.read("\uFEFF一,いち,하나".toByteArray())

        assertEquals("一", cards[0].jp)
    }
}
