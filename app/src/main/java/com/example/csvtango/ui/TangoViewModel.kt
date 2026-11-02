package com.example.csvtango.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.example.csvtango.data.BookmarkMilestone
import com.example.csvtango.data.Card
import com.example.csvtango.data.Folder
import com.example.csvtango.data.QuizQuestion
import com.example.csvtango.data.QuizResult
import com.example.csvtango.data.QuizSession
import com.example.csvtango.data.StudyCard
import com.example.csvtango.data.StudySession
import com.example.csvtango.data.Topic
import com.example.csvtango.data.TopicStore
import com.example.csvtango.data.isBookmarkMilestone
import com.example.csvtango.data.moveAmong
import kotlin.random.Random

enum class Screen {
    /** 첫 화면. [암기]/[시험] 선택 */
    HOME,

    /** 암기: 폴더·주제 목록. 폴더를 열면 같은 화면에 그 폴더의 주제를 보여 준다 */
    MEMORIZE,

    /** 암기 북마크: [전체 북마크]와 북마크가 있는 주제별 목록. 하나를 고르면 카드 학습으로 간다 */
    BOOKMARKS,
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

    var folders by mutableStateOf(store.loadFolders())
        private set

    var screen by mutableStateOf(Screen.HOME)
        private set

    /** 암기 화면에서 열어 둔 폴더의 id. null 이면 맨 위(폴더 밖)를 보고 있다. */
    var openFolderId by mutableStateOf<String?>(null)
        private set

    var session by mutableStateOf<StudySession?>(null)
        private set

    /** 카드가 뒤집혀 있는지. */
    var flipped by mutableStateOf(false)
        private set

    /** 암기 목록의 편집 모드(순서 변경·제목 변경·삭제·폴더 이동). */
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
        openFolderId = null
        screen = Screen.HOME
    }

    fun goMemorize() {
        session = null
        screen = Screen.MEMORIZE
    }

    /** 암기 북마크 목록(전체·주제별). */
    fun goBookmarks() {
        session = null
        editing = false
        screen = Screen.BOOKMARKS
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
        // 북마크가 사라진 주제는 북마크 주제 선택에서 뺀다.
        quizBookmarkTopics = quizBookmarkTopics intersect bookmarkedTopics.map { it.first.id }.toSet()
        // 고른 목록이 사라졌거나(주제 삭제·북마크 해제) 아직 없으면 첫 주제를 골라 둔다.
        if (quizWords().isEmpty()) quizSource = topics.firstOrNull()?.id
        screen = Screen.QUIZ_SETUP
    }

    /** 고른 목록의 단어를 보여 주는 범위 지정 화면. 목록이 바뀌었으면 범위를 전체로 되돌린다. */
    fun goQuizRange() {
        val words = quizWords()
        if (words.isEmpty()) return
        quiz = null
        if (quizRangeSource != quizSourceKey || quizRange.last > words.lastIndex) {
            quizRange = words.indices
            quizRangeSource = quizSourceKey
        }
        screen = Screen.QUIZ_RANGE
    }

    /** 뒤로 가기. 화면마다 한 단계 위로 올라간다. */
    fun back() {
        when (screen) {
            Screen.HOME -> Unit
            Screen.MEMORIZE -> if (openFolderId != null) closeFolder() else goHome()
            Screen.BOOKMARKS -> goMemorize()
            Screen.UPLOAD -> goMemorize()
            Screen.STUDY -> if (studyFromBookmarks) goBookmarks() else goMemorize()
            Screen.QUIZ_MENU -> goHome()
            Screen.QUIZ_SETUP -> goQuizMenu()
            Screen.QUIZ_RANGE -> goQuizSetup()
            Screen.COMBINED_SETUP -> goQuizMenu()
            Screen.QUIZ -> leaveQuiz()
        }
    }

    /* ---------------- 주제 ---------------- */

    private fun newId(): String = System.currentTimeMillis().toString() + Random.nextInt(0x100000).toString(16)

    fun addTopic(title: String, cards: List<Card>) {
        val topic = Topic(
            id = newId(),
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

    /** 같은 폴더(또는 폴더 밖)의 주제들 사이에서 한 칸 위(-1)나 아래(+1)로 옮긴다. */
    fun moveTopic(topic: Topic, delta: Int) {
        val current = topics.find { it.id == topic.id } ?: return
        val folder = folderOf(current)
        val next = topics.moveAmong(current, delta) { folderOf(it) == folder }
        if (next !== topics) update(next)
    }

    /** 주제를 다른 폴더로 옮긴다. [folderId] 가 null 이면 폴더 밖으로 꺼낸다. */
    fun moveTopicToFolder(topic: Topic, folderId: String?) {
        if (folderId != null && folders.none { it.id == folderId }) return
        update(topics.map { if (it.id == topic.id) it.copy(folderId = folderId) else it })
    }

    private fun update(next: List<Topic>) {
        topics = next
        store.save(next)
    }

    /* ---------------- 폴더 ---------------- */

    /** 지금 열려 있는 폴더. */
    val openFolder: Folder?
        get() = folders.find { it.id == openFolderId }

    /** 주제가 실제로 들어 있는 폴더의 id. 사라진 폴더를 가리키면 폴더 밖(null)으로 본다. */
    private fun folderOf(topic: Topic): String? =
        topic.folderId?.takeIf { id -> folders.any { it.id == id } }

    /** [folderId] 폴더(null 이면 폴더 밖)에 든 주제들. 목록 순서 그대로. */
    fun topicsIn(folderId: String?): List<Topic> = topics.filter { folderOf(it) == folderId }

    fun enterFolder(folder: Folder) {
        editing = false
        openFolderId = folder.id
    }

    fun closeFolder() {
        editing = false
        openFolderId = null
    }

    fun createFolder(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        updateFolders(folders + Folder(id = newId(), name = trimmed, createdAt = System.currentTimeMillis()))
    }

    fun renameFolder(folder: Folder, name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        updateFolders(folders.map { if (it.id == folder.id) it.copy(name = trimmed) else it })
    }

    /** 폴더를 지운다. 안에 있던 주제는 지우지 않고 폴더 밖으로 꺼낸다. */
    fun deleteFolder(folder: Folder) {
        if (topics.any { it.folderId == folder.id }) {
            update(topics.map { if (it.folderId == folder.id) it.copy(folderId = null) else it })
        }
        updateFolders(folders.filterNot { it.id == folder.id })
        if (openFolderId == folder.id) closeFolder()
    }

    /** 폴더 목록에서 한 칸 위(-1)나 아래(+1)로 옮긴다. */
    fun moveFolder(folder: Folder, delta: Int) {
        val current = folders.find { it.id == folder.id } ?: return
        val next = folders.moveAmong(current, delta) { true }
        if (next !== folders) updateFolders(next)
    }

    private fun updateFolders(next: List<Folder>) {
        folders = next
        store.saveFolders(next)
    }

    /* ---------------- 북마크 ---------------- */

    /** 원본 주제에서 이 카드가 지금 북마크되어 있는지. */
    fun isBookmarked(studyCard: StudyCard): Boolean =
        topics.find { it.id == studyCard.topicId }?.cards?.getOrNull(studyCard.index)?.bookmarked ?: false

    /**
     * 북마크가 늘어 전체 개수가 10개 단위에 닿았을 때 화면 아래에 잠깐 띄울 알림.
     * 화면이 보여 준 뒤 [consumeBookmarkMilestone] 으로 지운다. 같은 개수에 다시 닿아도 새로 알린다.
     */
    var bookmarkMilestone by mutableStateOf<BookmarkMilestone?>(null)
        private set

    /** 알림 하나를 보여 준 뒤 호출. 다음 알림이 같은 개수여도 상태가 바뀌도록 비워 둔다. */
    fun consumeBookmarkMilestone() {
        bookmarkMilestone = null
    }

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

        // 켤 때만 센다. 끄거나 전체 해제할 때는 알리지 않는다.
        if (on && isBookmarkMilestone(bookmarkCount)) bookmarkMilestone = BookmarkMilestone(bookmarkCount)
    }

    /* ---------------- 학습(암기) ---------------- */

    /** 지금 학습이 북마크 목록에서 시작됐는지. 뒤로 가기에서 돌아갈 화면을 정한다. */
    var studyFromBookmarks by mutableStateOf(false)
        private set

    fun openTopic(topic: Topic) {
        start(topic.title, topic.studyCards(), fromBookmarks = false)
    }

    /** 모든 주제에서 북마크한 단어만 모아 하나의 카드 묶음으로 만든다. */
    fun openBookmarks() {
        start(BOOKMARK_TITLE, bookmarkedCards(), fromBookmarks = true)
    }

    /** 한 주제에서 북마크한 단어만 모아 학습한다. */
    fun openTopicBookmarks(topic: Topic) {
        val current = topics.find { it.id == topic.id } ?: return
        start("★ ${current.title}", current.studyCards().filter { it.card.bookmarked }, fromBookmarks = true)
    }

    /** 북마크한 단어가 있는 주제들과 그 개수. 목록 순서 그대로. */
    val bookmarkedTopics: List<Pair<Topic, Int>>
        get() = topics.mapNotNull { topic ->
            val count = topic.cards.count { it.bookmarked }
            if (count > 0) topic to count else null
        }

    /** 북마크한 단어 전부. [topicIds] 를 주면 그 주제들의 북마크만. (빈 집합은 전체) */
    private fun bookmarkedCards(topicIds: Set<String> = emptySet()): List<StudyCard> =
        topics
            .filter { topicIds.isEmpty() || it.id in topicIds }
            .flatMap { topic -> topic.studyCards().filter { it.card.bookmarked } }

    private fun start(title: String, cards: List<StudyCard>, fromBookmarks: Boolean) {
        if (cards.isEmpty()) return
        session = StudySession(title, cards)
        flipped = false
        studyFromBookmarks = fromBookmarks
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

    /** 북마크 시험에 넣을 주제 id 들. 여러 개 고를 수 있고, 비어 있으면 모든 주제의 북마크. */
    var quizBookmarkTopics by mutableStateOf<Set<String>>(emptySet())
        private set

    var quizShuffle by mutableStateOf(true)
        private set

    /** 목록 안에서 출제할 단어의 위치(0부터, 양끝 포함). */
    var quizRange by mutableStateOf(0..0)
        private set

    /** [quizRange] 를 정한 목록. 다른 목록을 고르면 범위를 전체로 되돌린다. */
    private var quizRangeSource: String? = null

    /** 목록 선택을 한 값으로 묶은 것. 북마크 주제 선택이 바뀌어도 다른 목록으로 본다. */
    private val quizSourceKey: String
        get() = quizSource + "|" + quizBookmarkTopics.sorted().joinToString(",")

    /** 고른 목록의 이름. 북마크에서 주제를 골랐으면 그 주제 이름들을 쓴다. */
    val quizSourceTitle: String
        get() = when {
            quizSource != BOOKMARK_SOURCE -> topics.find { it.id == quizSource }?.title.orEmpty()
            quizBookmarkTopics.isEmpty() -> BOOKMARK_TITLE
            else -> "★ " + topics.filter { it.id in quizBookmarkTopics }.joinToString(", ") { it.title }
        }

    /** 고른 목록의 단어 전부(목록 순서 그대로). */
    fun quizWords(): List<StudyCard> = when (val source = quizSource) {
        null -> emptyList()
        BOOKMARK_SOURCE -> bookmarkedCards(quizBookmarkTopics)
        else -> topics.find { it.id == source }?.studyCards().orEmpty()
    }

    /** 지금 범위로 낼 수 있는 문제 수. */
    val quizQuestionCount: Int
        get() = quizQuestions().size

    fun selectQuizSource(source: String) {
        if (source == BOOKMARK_SOURCE && bookmarkCount == 0) return
        quizSource = source
    }

    /** 북마크 시험에 넣을 주제를 켜거나 끈다. 북마크가 없는 주제는 고를 수 없다. */
    fun toggleQuizBookmarkTopic(topicId: String) {
        if (bookmarkedTopics.none { it.first.id == topicId }) return
        quizSource = BOOKMARK_SOURCE
        quizBookmarkTopics =
            if (topicId in quizBookmarkTopics) quizBookmarkTopics - topicId else quizBookmarkTopics + topicId
    }

    /** 북마크 시험을 모든 주제의 북마크로 되돌린다. */
    fun selectAllQuizBookmarkTopics() {
        quizSource = BOOKMARK_SOURCE
        quizBookmarkTopics = emptySet()
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
