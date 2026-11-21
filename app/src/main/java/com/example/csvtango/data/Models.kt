package com.example.csvtango.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 단어 카드 한 장. 예문이 없는(예전 3열) CSV 에서는 ex* 가 비어 있다. */
data class Card(
    val jp: String,
    val kana: String,
    val ko: String,
    val exJp: String = "",
    val exKana: String = "",
    val exKo: String = "",
    val bookmarked: Boolean = false,
) {
    val hasExample: Boolean get() = exKana.isNotEmpty() || exJp.isNotEmpty()
}

/** CSV 한 개에서 만들어진 단어 묶음. */
data class Topic(
    val id: String,
    val title: String,
    val cards: List<Card>,
    val createdAt: Long,
    /** 들어 있는 [Folder] 의 id. null 이면 폴더 밖(암기 화면 맨 위)에 있다. */
    val folderId: String? = null,
    /** 표시 칸에 제목 앞글자 대신 보여 줄 이모티콘 하나. null 이면 제목 앞글자. */
    val emoji: String? = null,
    /** 암기 목록에서 주제 칸의 배경색(ARGB). null 이면 기본 배경. */
    val color: Long? = null,
) {
    /** 표시 칸에 보여 줄 글자. 이모티콘이 있으면 이모티콘, 없으면 제목 앞글자. */
    val badgeText: String
        get() = emoji ?: title.trim().take(1).uppercase().ifEmpty { "語" }
}

/** 주제 배경으로 고를 수 있는 색(ARGB). 어두운 바탕에서 본문 글자가 잘 읽히도록 어둡게 눌렀다. */
val TOPIC_COLORS: List<Long> = listOf(
    0xFF4A2E33, // 빨강
    0xFF4A3A2A, // 주황
    0xFF46422A, // 노랑
    0xFF2A4235, // 초록
    0xFF27403F, // 청록
    0xFF2A3A52, // 파랑
    0xFF3B3150, // 보라
    0xFF4A2F42, // 분홍
)

/**
 * [input] 에 들어 있는 이모티콘 가운데 마지막 하나를 돌려준다. 없으면 null.
 *
 * 피부색·성별·가족(ZWJ)처럼 여러 코드포인트로 이루어진 이모티콘도 하나로 친다.
 * 이모티콘 입력칸에 새로 고른 이모티콘만 남겨, 한 번에 하나만 설정되게 할 때 쓴다.
 */
fun lastEmoji(input: String): String? {
    var last: String? = null
    var i = 0
    while (i < input.length) {
        val start = i
        val base = input.codePointAt(i)
        i += Character.charCount(base)
        var keycap = false

        // 국기: 지역 표시 문자 두 개가 한 쌍
        if (isRegionalIndicator(base) && i < input.length && isRegionalIndicator(input.codePointAt(i))) {
            i += Character.charCount(input.codePointAt(i))
        }
        while (i < input.length) {
            val cp = input.codePointAt(i)
            when {
                cp == ZWJ -> {
                    i += Character.charCount(cp)
                    if (i < input.length) i += Character.charCount(input.codePointAt(i))
                }
                cp == KEYCAP -> { keycap = true; i += Character.charCount(cp) }
                isEmojiExtender(cp) -> i += Character.charCount(cp)
                else -> break
            }
        }
        if (keycap || isEmojiBase(base)) last = input.substring(start, i)
    }
    return last
}

private const val ZWJ = 0x200D
private const val KEYCAP = 0x20E3

private fun isRegionalIndicator(cp: Int) = cp in 0x1F1E6..0x1F1FF

/** 앞 글자에 붙어 한 이모티콘을 이루는 문자: 이체 선택자, 피부색, 태그(지역 깃발). */
private fun isEmojiExtender(cp: Int) =
    cp == 0xFE0F || cp == 0xFE0E || cp in 0x1F3FB..0x1F3FF || cp in 0xE0020..0xE007F

private fun isEmojiBase(cp: Int) =
    cp in 0x1F000..0x1FAFF ||   // 대부분의 그림 이모티콘·국기
    cp in 0x2600..0x27BF ||     // ☀ ☕ ✈ ✨ ❤ 등
    cp in 0x2300..0x23FF ||     // ⌚ ⏰ 등
    cp in 0x2B00..0x2BFF ||     // ⭐ ⬆ 등
    cp in 0x2190..0x21FF ||     // ↔ ↩ 등
    cp == 0x00A9 || cp == 0x00AE || cp == 0x203C || cp == 0x2049 ||
    cp == 0x2122 || cp == 0x2139 || cp == 0x3030 || cp == 0x303D ||
    cp == 0x3297 || cp == 0x3299

/** 북마크를 하나 더 켜서 전체 개수가 [count] 가 됐을 때 알려 줄 내용. 10, 20, 30… 에 닿을 때마다 만들어진다. */
data class BookmarkMilestone(val count: Int)

/** 전체 북마크 개수가 10개 단위(10, 20, 30…)에 막 닿았는지. */
fun isBookmarkMilestone(count: Int): Boolean = count > 0 && count % 10 == 0

/** 암기 화면에서 주제를 묶어 두는 폴더. */
data class Folder(
    val id: String,
    val name: String,
    val createdAt: Long,
)

/**
 * [same] 이 참인 항목끼리만 순서를 매겨 [target] 을 한 칸 위(-1)나 아래(+1)로 옮긴다.
 * 그 사이에 끼어 있는 다른 항목들은 자리를 지킨다. 옮길 수 없으면 그대로 돌려준다.
 */
fun <T> List<T>.moveAmong(target: T, delta: Int, same: (T) -> Boolean): List<T> {
    val slots = indices.filter { same(this[it]) }
    val from = slots.indexOf(indexOf(target))
    val to = from + delta
    if (from < 0 || to !in slots.indices) return this

    return toMutableList().apply {
        this[slots[from]] = this@moveAmong[slots[to]]
        this[slots[to]] = target
    }
}

/**
 * 학습 화면에서 쓰는 카드.
 *
 * 원본 위치(topicId, index)를 붙여 둔다. 그래야 섞기나 북마크 모아보기로
 * 순서가 바뀌어도 북마크를 원래 자리에 되돌려 쓸 수 있다.
 */
data class StudyCard(
    val card: Card,
    val topicId: String,
    val index: Int,
)

/**
 * 학습 중인 카드 묶음과 현재 위치.
 *
 * [original] 은 순서를 섞기 전의 카드 순서. 섞여 있지 않으면 null.
 * 섞여 있는 동안에는 원래 순서 기준인 [암기] 범위를 정할 수 없다.
 */
data class StudySession(
    val title: String,
    val cards: List<StudyCard>,
    val index: Int = 0,
    val original: List<StudyCard>? = null,
) {
    val current: StudyCard get() = cards[index]

    /** 지금 순서가 섞여 있는지. */
    val shuffled: Boolean get() = original != null
}

private val DATE_FORMAT = SimpleDateFormat("yyyy.MM.dd", Locale.US)

fun formatDate(millis: Long): String = DATE_FORMAT.format(Date(millis))
