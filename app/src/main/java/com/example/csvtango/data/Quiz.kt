package com.example.csvtango.data

/** 시험 문제에서 무엇을 보여 줄지. */
enum class PromptKind {
    /** 일본어(한자) 단어를 보여 주고 히라가나를 묻는다. */
    JAPANESE,

    /** 단어가 가나뿐이라 한국어 뜻을 보여 주고 히라가나를 묻는다. */
    MEANING,
}

/** 가나·한자 판별과 답안 비교용 정규화. */
object Kana {

    /** `し / よん`, `1, 일` 처럼 여러 읽기를 나누는 구분자 */
    private val SEPARATORS = charArrayOf('/', ',', '、', '･', '・', '|')

    fun isKanji(c: Char): Boolean =
        c in '一'..'鿿' ||      // CJK 통합 한자
            c in '㐀'..'䶿' ||  // 확장 A
            c in '豈'..'﫿' ||  // 호환 한자
            c == '々'               // 々 (반복 기호)

    fun isKana(c: Char): Boolean =
        c in 'ぁ'..'ゟ' ||      // 히라가나
            c in '゠'..'ヿ' ||  // 가타카나 (ー 포함)
            c in 'ｦ'..'ﾟ'     // 반각 가타카나

    /** 가타카나를 히라가나로 바꾼다. 나머지 글자는 그대로 둔다. */
    fun toHiragana(text: String): String = buildString(text.length) {
        for (c in text) append(if (c in 'ァ'..'ヶ') c - 0x60 else c)
    }

    /** 비교용 형태: 앞뒤·중간 공백 제거, 가타카나 → 히라가나. */
    fun normalize(text: String): String =
        toHiragana(text).filterNot { it.isWhitespace() || it == '　' }

    /** 구분자로 나뉜 읽기들을 각각 돌려준다. 빈 것은 뺀다. */
    fun readings(text: String): List<String> =
        text.split(*SEPARATORS).map { it.trim() }.filter { it.isNotEmpty() }
}

/** 시험 문제 하나. 카드에서 보여 줄 것과 정답으로 받아 줄 것을 미리 정해 둔다. */
data class QuizQuestion(val card: StudyCard) {

    private val jp get() = card.card.jp
    private val kana get() = card.card.kana
    private val ko get() = card.card.ko

    /** 한자가 있으면 단어를, 가나뿐이면 뜻을 보여 준다. 뜻이 없으면 어쩔 수 없이 단어를 보여 준다. */
    val kind: PromptKind =
        if (jp.any(Kana::isKanji) || jp.none(Kana::isKana) || ko.isBlank()) PromptKind.JAPANESE
        else PromptKind.MEANING

    val prompt: String get() = if (kind == PromptKind.JAPANESE) jp else ko

    /** 정답으로 받아 주는 형태들(정규화된 것). 히라가나 열의 읽기 전부 + 단어 자체가 가나면 그것도. */
    val answers: Set<String> = buildSet {
        Kana.readings(kana).forEach { add(Kana.normalize(it)) }
        if (jp.none(Kana::isKanji) && jp.any(Kana::isKana)) add(Kana.normalize(jp))
        remove("")
    }

    /** 정답을 하나도 모르는 카드는 낼 수 없다. */
    val answerable: Boolean get() = answers.isNotEmpty()

    fun isCorrect(input: String): Boolean {
        val normalized = Kana.normalize(input)
        return normalized.isNotEmpty() && normalized in answers
    }
}

data class QuizResult(
    val question: QuizQuestion,
    val input: String,
    val correct: Boolean,
    /** 맞혀서 원래 있던 북마크를 해제했는지. */
    val unbookmarked: Boolean = false,
)

/**
 * 진행 중인 시험.
 *
 * [pending] 은 방금 채점했지만 아직 "다음"을 누르지 않은 결과. 이때 화면은 정답을 보여 준다.
 */
data class QuizSession(
    val title: String,
    val questions: List<QuizQuestion>,
    val results: List<QuizResult> = emptyList(),
    val pending: QuizResult? = null,
) {
    val index: Int get() = results.size
    val finished: Boolean get() = results.size >= questions.size
    val current: QuizQuestion get() = questions[index]
    val correctCount: Int get() = results.count { it.correct }
    val wrong: List<QuizResult> get() = results.filterNot { it.correct }
}
