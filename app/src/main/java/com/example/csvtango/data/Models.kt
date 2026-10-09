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
)

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
