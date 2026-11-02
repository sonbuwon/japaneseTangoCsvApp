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
)

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

/** 학습 중인 카드 묶음과 현재 위치. */
data class StudySession(
    val title: String,
    val cards: List<StudyCard>,
    val index: Int = 0,
) {
    val current: StudyCard get() = cards[index]
}

private val DATE_FORMAT = SimpleDateFormat("yyyy.MM.dd", Locale.US)

fun formatDate(millis: Long): String = DATE_FORMAT.format(Date(millis))
