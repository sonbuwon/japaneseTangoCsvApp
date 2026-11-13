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

    /** `つく(る)` 처럼 생략해도 되는 부분을 감싸는 괄호 (반각·전각) */
    private val PARENS = charArrayOf('(', ')', '（', '）')
    private val PAREN_GROUP = Regex("[(（][^()（）]*[)）]")

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

    /** 히라가나를 가타카나로 바꾼다. 글자 수는 그대로라 입력 표시 변환에도 쓸 수 있다. */
    fun toKatakana(text: String): String = buildString(text.length) {
        for (c in text) append(if (c in 'ぁ'..'ゖ') c + 0x60 else c)
    }

    private fun isHiragana(c: Char): Boolean = c in 'ぁ'..'ゟ'

    private fun isKatakana(c: Char): Boolean = c in '゠'..'ヿ' || c in 'ｦ'..'ﾟ'

    /** 한자·히라가나 없이 가타카나로만 적힌 단어인지. (コーヒー, パン・ケーキ 등) */
    fun isKatakanaWord(text: String): Boolean =
        text.none { isKanji(it) || isHiragana(it) } && text.any(::isKatakana)

    /** 비교용 형태: 앞뒤·중간 공백과 괄호 기호 제거, 가타카나 → 히라가나. */
    fun normalize(text: String): String =
        toHiragana(text).filterNot { it.isWhitespace() || it == '　' || it in PARENS }

    /** `つく(る)` → `つく` 처럼 괄호로 감싼 부분을 통째로 뺀다. */
    fun withoutParenGroups(text: String): String = text.replace(PAREN_GROUP, "")

    /** 읽기 하나를 정답으로 받아 줄 정규화 형태들: 괄호만 뺀 것과 괄호 안까지 뺀 것. */
    fun answerForms(reading: String): Set<String> =
        setOf(normalize(reading), normalize(withoutParenGroups(reading)))

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

    /** 가타카나 단어면 답도 가타카나로 입력받는다. (채점은 히라가나와 똑같이 한다) */
    val katakana: Boolean = Kana.isKatakanaWord(jp)

    /**
     * 정답으로 받아 주는 형태들(정규화된 것). 히라가나 열의 읽기 전부 + 단어 자체가 가나면 그것도.
     * `つく(る)` 처럼 괄호가 있으면 `つくる` 와 `つく` 둘 다 받는다.
     */
    val answers: Set<String> = buildSet {
        Kana.readings(kana).forEach { addAll(Kana.answerForms(it)) }
        if (jp.none(Kana::isKanji) && jp.any(Kana::isKana)) addAll(Kana.answerForms(jp))
        remove("")
    }

    /** 정답을 하나도 모르는 카드는 낼 수 없다. */
    val answerable: Boolean get() = answers.isNotEmpty()

    /** 같은 단어인지 가리는 키. 일본어(공백 무시)와 히라가나(정규화)가 같으면 다른 주제의 카드라도 같은 단어로 본다. */
    val wordKey: Pair<String, String>
        get() = jp.filterNot { it.isWhitespace() || it == '　' } to Kana.normalize(kana)

    fun isCorrect(input: String): Boolean {
        val normalized = Kana.normalize(input)
        return normalized.isNotEmpty() && normalized in answers
    }
}

data class QuizResult(
    val question: QuizQuestion,
    val input: String,
    val correct: Boolean,
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
