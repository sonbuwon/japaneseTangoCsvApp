package com.example.csvtango.data

import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction

/** CSV 바이트 → 단어 카드. */
object CsvParser {

    private val ENCODINGS = listOf("UTF-8", "EUC-KR", "Shift_JIS")

    /** UTF-8 BOM */
    private const val BOM = "\uFEFF"

    private val HEADER_WORDS = setOf(
        "일본어", "히라가나", "한국어", "단어",
        "japanese", "kana", "hiragana", "korean", "meaning",
    )

    /** 값이 없음을 뜻하는 자리표시자 */
    private val PLACEHOLDER = Regex("^[-–—ー]$|^없음$")

    /** 바이트 → 카드. 실패 지점을 나누지 않고 한 번에 처리한다. */
    fun read(bytes: ByteArray): List<Card> = toCards(parse(decode(bytes)))

    /** 한국어(EUC-KR)/일본어(Shift_JIS)로 저장된 CSV 도 열 수 있게 인코딩을 추정한다. */
    fun decode(bytes: ByteArray): String {
        for (name in ENCODINGS) {
            val charset = try {
                Charset.forName(name)
            } catch (e: IllegalArgumentException) {
                continue   // 이 기기가 모르는 인코딩
            }
            try {
                return charset.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString()
            } catch (e: CharacterCodingException) {
                /* 다음 인코딩 시도 */
            }
        }
        return String(bytes, Charsets.UTF_8)   // 최후: 깨진 글자 허용
    }

    /** RFC4180 형식의 CSV 를 2차원 배열로 변환한다. 따옴표 안의 쉼표/줄바꿈을 보존한다. */
    fun parse(raw: String): List<List<String>> {
        val text = raw.removePrefix(BOM).replace("\r\n", "\n").replace('\r', '\n')

        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val field = StringBuilder()
        var inQuotes = false
        var i = 0

        while (i < text.length) {
            val ch = text[i]

            if (inQuotes) {
                if (ch == '"') {
                    if (i + 1 < text.length && text[i + 1] == '"') {
                        field.append('"')   // 이스케이프된 따옴표
                        i++
                    } else {
                        inQuotes = false
                    }
                } else {
                    field.append(ch)
                }
                i++
                continue
            }

            when (ch) {
                '"' -> inQuotes = true
                ',' -> { row.add(field.toString()); field.setLength(0) }
                '\n' -> {
                    row.add(field.toString())
                    rows.add(row)
                    row = mutableListOf()
                    field.setLength(0)
                }
                else -> field.append(ch)
            }
            i++
        }

        row.add(field.toString())
        rows.add(row)

        return rows.filter { line -> line.any { it.isNotBlank() } }
    }

    /**
     * CSV 행 → 단어 카드.
     *
     * 6열: 일본어, 히라가나, 한국어, 예문(일본어), 예문(히라가나), 예문(한국어)
     * 3열: 일본어, 히라가나, 한국어  ← 예전 형식도 그대로 읽는다
     *
     * 형식은 파일 전체를 보고 한 번만 판단한다. 그래야 따옴표 없이 저장돼
     * 한국어 뜻이 여러 칸으로 쪼개진 3열 파일(`一,いち,1, 일`)을 예문으로 오인하지 않는다.
     */
    fun toCards(input: List<List<String>>): List<Card> {
        val rows = if (input.isNotEmpty() && looksLikeHeader(input[0])) input.drop(1) else input

        val hasExamples = rows.count { it.size >= 6 } * 2 > rows.size

        return rows
            .map { r ->
                if (hasExamples) {
                    Card(
                        jp = clean(r, 0),
                        kana = clean(r, 1),
                        ko = clean(r, 2),
                        exJp = clean(r, 3),
                        exKana = clean(r, 4),
                        exKo = clean(r, 5),
                    )
                } else {
                    Card(
                        jp = clean(r, 0),
                        kana = clean(r, 1),
                        ko = r.drop(2).map { it.trim() }.filter { it.isNotEmpty() }.joinToString(", "),
                    )
                }
            }
            .filter { it.jp.isNotEmpty() }
    }

    private fun looksLikeHeader(row: List<String>): Boolean =
        row.take(3).any { it.trim().lowercase() in HEADER_WORDS }

    private fun clean(row: List<String>, index: Int): String {
        val s = row.getOrNull(index).orEmpty().trim()
        return if (PLACEHOLDER.matches(s)) "" else s
    }
}
