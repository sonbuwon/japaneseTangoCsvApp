package com.example.csvtango.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.example.csvtango.data.Card
import com.example.csvtango.data.QuizQuestion
import com.example.csvtango.data.QuizResult
import com.example.csvtango.data.QuizSession
import com.example.csvtango.data.StudyCard
import com.example.csvtango.data.StudySession
import com.example.csvtango.data.Topic
import com.example.csvtango.data.TopicStore
import kotlin.random.Random

enum class Screen {
    /** 첫 화면. [암기]/[시험] 선택 */
    HOME,

    /** 암기: 주제 목록 */
    MEMORIZE,
    UPLOAD,
    STUDY,

    /** 시험: [주제 시험]·[종합 시험] 선택 */
    QUIZ_MENU,

    /** 주제 시험: 목록(주제·북마크) 하나와 옵션 선택 */
    QUIZ_SETUP,

    /** 시험: 고른 목록의 단어를 보여 주고 출제 범위를 정한다 */
    QUIZ_RANGE,

    /** 종합 시험: 목록 여러 개와 문제 수를 고른다. 순서는 항상 무작위 */
    COMBINED_SETUP,
    QUIZ,
}

const val BOOKMARK_TITLE = "★ 북마크"

/** 시험 목록으로 북마크를 골랐을 때의 id. 주제 id 와 겹치지 않는다. */
const val BOOKMARK_SOURCE = "__bookmarks__"

class TangoViewModel(app: Application) : AndroidViewModel(app) {

    private val store = TopicStore(app)

    var topics by mutableStateOf(store.load())
        private set

    var screen by mutableStateOf(Screen.HOME)
        private set

    var session by mutableStateOf<StudySession?>(null)
        private set

    /** 카드가 뒤집혀 있는지. */
    var flipped by mutableStateOf(false)
        private set

    /** 암기 주제 목록의 편집 모드(순서 변경·제목 변경·삭제). */
    var editing by mutableStateOf(false)

    val bookmarkCount: Int
        get() = topics.sumOf { topic -> topic.cards.count { it.bookmarked } }

    val wordCount: Int
        get() = topics.sumOf { it.cards.size }

    /* ---------------- 화면 전환 ---------------- */

    fun goHome() {
        session = null
        quiz = null
        editing = false
        screen = Screen.HOME
    }

    fun goMemorize() {
        session = null
        screen = Screen.MEMORIZE
    }

    fun goUpload() {
        editing = false
        screen = Screen.UPLOAD
    }

    fun goQuizMenu() {
        quiz = null
        screen = Screen.QUIZ_MENU
    }

    fun goQuizSetup() {
        quiz = null
        // 고른 목록이 사라졌거나(주제 삭제·북마크 해제) 아직 없으면 첫 주제를 골라 둔다.
        if (quizWords().isEmpty()) quizSource = topics.firstOrNull()?.id
        screen = Screen.QUIZ_SETUP
    }

    /** 고른 목록의 단어를 보여 주는 범위 지정 화면. 목록이 바뀌었으면 범위를 전체로 되돌린다. */
    fun goQuizRange() {
        val words = quizWords()
        if (words.isEmpty()) return
        quiz = null
        if (quizRangeSource != quizSource || quizRange.last > words.lastIndex) {
            quizRange = words.indices
            quizRangeSource = quizSource
        }
        screen = Screen.QUIZ_RANGE
    }

    /** 뒤로 가기. 화면마다 한 단계 위로 올라간다. */
    fun back() {
        when (screen) {
            Screen.HOME -> Unit
            Screen.MEMORIZE -> goHome()
            Screen.UPLOAD -> goHome()
            Screen.STUDY -> goMemorize()
            Screen.QUIZ_MENU -> goHome()
            Screen.QUIZ_SETUP -> goQuizMenu()
            Screen.QUIZ_RANGE -> goQuizSetup()
            Screen.COMBINED_SETUP -> goQuizMenu()
            Screen.QUIZ -> leaveQuiz()
        }
    }

    /* ---------------- 주제 ---------------- */

    fun addTopic(title: String, cards: List<Card>) {
        val topic = Topic(
            id = System.currentTimeMillis().toString() + Random.nextInt(0x100000).toString(16),
            title = title,
            cards = cards,
            createdAt = System.currentTimeMillis(),
        )
        update(listOf(topic) + topics)
    }

    fun renameTopic(topic: Topic, title: String) {
        val name = title.trim()
        if (name.isEmpty()) return
        update(topics.map { if (it.id == topic.id) it.copy(title = name) else it })
    }

    fun deleteTopic(topic: Topic) {
        update(topics.filterNot { it.id == topic.id })
    }

    /** 모든 주제의 북마크를 해제한다. */
    fun clearBookmarks() {
        update(
            topics.map { topic ->
                topic.copy(cards = topic.cards.map { if (it.bookmarked) it.copy(bookmarked = false) else it })
            },
        )
    }

    /** 목록에서 한 칸 위(-1)나 아래(+1)로 옮긴다. */
    fun moveTopic(topic: Topic, delta: Int) {
        val from = topics.indexOfFirst { it.id == topic.id }
        val to = from + delta
        if (from < 0 || to !in topics.indices) return

        update(topics.toMutableList().apply { add(to, removeAt(from)) })
    }

    private fun update(next: List<Topic>) {
        topics = next
        store.save(next)
    }

    /* ---------------- 북마크 ---------------- */

    /** 원본 주제에서 이 카드가 지금 북마크되어 있는지. */
    fun isBookmarked(studyCard: StudyCard): Boolean =
        topics.find { it.id == studyCard.topicId }?.cards?.getOrNull(studyCard.index)?.bookmarked ?: false

    /** 원본 주제의 카드에 북마크를 켜거나 끈다. 주제가 사라졌으면 아무것도 하지 않는다. */
    private fun setBookmark(studyCard: StudyCard, on: Boolean) {
        val topic = topics.find { it.id == studyCard.topicId } ?: return
        if (studyCard.index !in topic.cards.indices) return
        if (topic.cards[studyCard.index].bookmarked == on) return

        update(
            topics.map { t ->
                if (t.id != topic.id) t
                else t.copy(
                    cards = t.cards.mapIndexed { i, c ->
                        if (i == studyCard.index) c.copy(bookmarked = on) else c
                    },
                )
            },
        )
    }

    /* ---------------- 학습(암기) ---------------- */

    fun openTopic(topic: Topic) {
        start(topic.title, topic.studyCards())
    }

    /** 모든 주제에서 북마크한 단어만 모아 하나의 카드 묶음으로 만든다. */
    fun openBookmarks() {
        val cards = bookmarkedCards()
        if (cards.isEmpty()) return
        start(BOOKMARK_TITLE, cards)
    }

    private fun bookmarkedCards(): List<StudyCard> =
        topics.flatMap { topic -> topic.studyCards().filter { it.card.bookmarked } }

    private fun start(title: String, cards: List<StudyCard>) {
        if (cards.isEmpty()) return
        session = StudySession(title, cards)
        flipped = false
        screen = Screen.STUDY
    }

    private fun Topic.studyCards(): List<StudyCard> =
        cards.mapIndexed { i, card -> StudyCard(card, id, i) }

    fun flip() {
        flipped = !flipped
    }

    /** 처음/끝에서 반대쪽 카드로 순환한다. */
    fun move(step: Int) {
        val current = session ?: return
        val total = current.cards.size
        session = current.copy(index = ((current.index + step) % total + total) % total)
        flipped = false
    }

    /** 진행 막대를 끌어 특정 카드로 바로 이동한다. */
    fun jumpTo(index: Int) {
        val current = session ?: return
        val next = index.coerceIn(0, current.cards.lastIndex)
        if (next == current.index) return

        session = current.copy(index = next)
        flipped = false
    }

    fun shuffle() {
        val current = session ?: return
        session = current.copy(cards = current.cards.shuffled(), index = 0)
        flipped = false
    }

    fun toggleBookmark() {
        val current = session ?: return
        val studyCard = current.current
        val on = !studyCard.card.bookmarked

        setBookmark(studyCard, on)

        // 화면에 보이는 사본에도 반영한다. (카드를 다시 그리지 않으므로 뒤집힌 상태가 유지된다)
        session = current.copy(
            cards = current.cards.mapIndexed { i, sc ->
                if (i == current.index) sc.copy(card = sc.card.copy(bookmarked = on)) else sc
            },
        )
    }

    /* ---------------- 시험 ---------------- */

    var quiz by mutableStateOf<QuizSession?>(null)
        private set

    /** 시험 볼 목록. 주제 id 또는 [BOOKMARK_SOURCE]. 한 번에 하나만 고른다. */
    var quizSource by mutableStateOf<String?>(null)
        private set

    var quizShuffle by mutableStateOf(true)
        private set

    /** 목록 안에서 출제할 단어의 위치(0부터, 양끝 포함). */
    var quizRange by mutableStateOf(0..0)
        private set

    /** [quizRange] 를 정한 목록. 다른 목록을 고르면 범위를 전체로 되돌린다. */
    private var quizRangeSource: String? = null

    /** 고른 목록의 이름. */
    val quizSourceTitle: String
        get() = if (quizSource == BOOKMARK_SOURCE) BOOKMARK_TITLE
        else topics.find { it.id == quizSource }?.title.orEmpty()

    /** 고른 목록의 단어 전부(목록 순서 그대로). */
    fun quizWords(): List<StudyCard> = when (val source = quizSource) {
        null -> emptyList()
        BOOKMARK_SOURCE -> bookmarkedCards()
        else -> topics.find { it.id == source }?.studyCards().orEmpty()
    }

    /** 지금 범위로 낼 수 있는 문제 수. */
    val quizQuestionCount: Int
        get() = quizQuestions().size

    fun selectQuizSource(source: String) {
        if (source == BOOKMARK_SOURCE && bookmarkCount == 0) return
        quizSource = source
    }

    fun toggleQuizShuffle() {
        quizShuffle = !quizShuffle
    }

    fun changeQuizRange(range: IntRange) {
        val last = quizWords().lastIndex
        if (last < 0) return
        val first = range.first.coerceIn(0, last)
        quizRange = first..range.last.coerceIn(first, last)
    }

    /** 범위 안의 카드 중 정답을 아는 카드만. */
    private fun quizQuestions(): List<QuizQuestion> =
        quizWords()
            .filterIndexed { i, _ -> i in quizRange }
            .map(::QuizQuestion)
            .filter { it.answerable }

    fun startQuiz() {
        val questions = quizQuestions()
        if (questions.isEmpty()) return

        val whole = quizRange.first == 0 && quizRange.last == quizWords().lastIndex
        val title = if (whole) quizSourceTitle
        else "$quizSourceTitle (${quizRange.first + 1}~${quizRange.last + 1})"
        quizCombined = false
        launchQuiz(title, questions)
    }

    /** 종합 시험은 순서를 무조건 섞는다. 다시 시험·틀린 단어 다시 시험도 마찬가지. */
    private fun launchQuiz(title: String, questions: List<QuizQuestion>) {
        quiz = QuizSession(title, if (quizCombined || quizShuffle) questions.shuffled() else questions)
        screen = Screen.QUIZ
    }

    /** 시험을 그만두면 시험을 고른 화면으로 돌아간다. (일반: 범위 지정, 종합: 종합 시험 설정) */
    fun leaveQuiz() {
        if (quizCombined) goCombinedSetup() else goQuizRange()
    }

    /** 결과 화면의 [시험 설정]. */
    fun goQuizOriginSetup() {
        if (quizCombined) goCombinedSetup() else goQuizSetup()
    }

    /* ---------------- 종합 시험 ---------------- */

    /** 지금 시험이 종합 시험인지. */
    private var quizCombined = false

    /** 종합 시험에 넣을 목록들. 주제 id 또는 [BOOKMARK_SOURCE]. 여러 개 고른다. */
    var combinedSources by mutableStateOf<Set<String>>(emptySet())
        private set

    /** 입력란에 적힌 문제 수(숫자만). */
    var combinedCountText by mutableStateOf("20")
        private set

    fun goCombinedSetup() {
        quiz = null
        // 지워진 주제, 비어 버린 북마크는 선택에서 뺀다.
        val ids = topics.map { it.id }.toSet() + (if (bookmarkCount > 0) setOf(BOOKMARK_SOURCE) else emptySet())
        combinedSources = combinedSources intersect ids
        screen = Screen.COMBINED_SETUP
    }

    fun toggleCombinedSource(source: String) {
        if (source == BOOKMARK_SOURCE && bookmarkCount == 0) return
        combinedSources = if (source in combinedSources) combinedSources - source else combinedSources + source
    }

    fun changeCombinedCount(text: String) {
        combinedCountText = text.filter(Char::isDigit).take(4)
    }

    /** 고른 목록들의 단어를 모은 것. 같은 카드는 한 번만, 정답을 아는 카드만. */
    private fun combinedPool(): List<QuizQuestion> {
        val fromTopics = topics.filter { it.id in combinedSources }.flatMap { it.studyCards() }
        val fromBookmarks = if (BOOKMARK_SOURCE in combinedSources) bookmarkedCards() else emptyList()
        return (fromTopics + fromBookmarks)
            .distinctBy { it.topicId to it.index }
            .map(::QuizQuestion)
            .filter { it.answerable }
    }

    /** 고른 목록들에서 낼 수 있는 최대 문제 수. */
    val combinedAvailable: Int
        get() = combinedPool().size

    /** 실제로 낼 문제 수. 입력한 수가 낼 수 있는 수보다 많으면 낼 수 있는 만큼만. */
    val combinedQuestionCount: Int
        get() = minOf(combinedCountText.toIntOrNull() ?: 0, combinedAvailable)

    fun startCombinedQuiz() {
        val count = combinedQuestionCount
        if (count <= 0) return
        quizCombined = true
        launchQuiz("종합 시험", combinedPool().shuffled().take(count))
    }

    /** 답을 채점한다. 틀리면 그 단어를 북마크하고, 맞히면 북마크를 해제한다. */
    fun submitAnswer(input: String) {
        val current = quiz ?: return
        if (current.finished || current.pending != null || input.isBlank()) return

        val question = current.current
        val correct = question.isCorrect(input)
        setBookmark(question.card, !correct)

        quiz = current.copy(pending = QuizResult(question, input, correct))
    }

    /** 채점 결과를 확정하고 다음 문제로 넘어간다. */
    fun nextQuestion() {
        val current = quiz ?: return
        val pending = current.pending ?: return
        quiz = current.copy(results = current.results + pending, pending = null)
    }

    /** 방금 채점한 카드의 북마크를 켜거나 끈다. (맞힌 단어를 손수 저장할 때) */
    fun toggleQuizBookmark() {
        val card = quiz?.pending?.question?.card ?: return
        setBookmark(card, !isBookmarked(card))
    }

    /** 결과 화면의 틀린 단어 목록에서 북마크를 켜거나 끈다. */
    fun toggleCardBookmark(card: StudyCard) {
        setBookmark(card, !isBookmarked(card))
    }

    /** 틀린 문제만 모아 다시 본다. */
    fun retryWrong() {
        val current = quiz ?: return
        val wrong = current.wrong.map { it.question }
        if (wrong.isEmpty()) return
        launchQuiz(current.title, wrong)
    }

    /** 같은 문제를 처음부터 다시 본다. */
    fun restartQuiz() {
        val current = quiz ?: return
        launchQuiz(current.title, current.questions)
    }
}
