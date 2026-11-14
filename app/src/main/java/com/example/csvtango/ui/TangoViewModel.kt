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

    /** 주제 시험: 목록(주제·북마크) 하나 이상과 옵션 선택. 폴더는 접고 펼 수 있다 */
    QUIZ_SETUP,

    /** 시험: 고른 목록을 하나씩 차례로 보여 주며 목록마다 출제 범위를 정한다 */
    QUIZ_RANGE,

    /** 종합 시험: 목록 여러 개와 문제 수를 고른다. 순서는 항상 무작위 */
    COMBINED_SETUP,
    QUIZ,
}

const val BOOKMARK_TITLE = "★ 북마크"

/** 시험 목록으로 북마크를 골랐을 때의 id. 주제 id 와 겹치지 않는다. */
const val BOOKMARK_SOURCE = "__bookmarks__"

/** 주제 시험 화면에서 폴더 밖 주제 묶음을 접고 펼 때 쓰는 키. 폴더 id 와 겹치지 않는다. */
const val NO_FOLDER = "__no_folder__"

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
        quizFromStudy = false
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

    /** 시험 메뉴에서 [주제 시험]으로 들어온다. 고른 목록 없이, 폴더는 모두 접힌 채로 시작한다. */
    fun startQuizSetup() {
        quizSources = emptySet()
        quizBookmarkTopics = emptySet()
        quizExpandedFolders = emptySet()
        goQuizSetup()
    }

    fun goQuizSetup() {
        quiz = null
        // 북마크가 사라진 주제는 북마크 주제 선택에서 뺀다.
        quizBookmarkTopics = quizBookmarkTopics intersect bookmarkedTopics.map { it.first.id }.toSet()
        // 지워진 주제, 비어 버린 북마크는 선택에서 뺀다.
        val ids = topics.map { it.id }.toSet() + (if (bookmarkCount > 0) setOf(BOOKMARK_SOURCE) else emptySet())
        quizSources = quizSources intersect ids
        screen = Screen.QUIZ_SETUP
    }

    /** 고른 목록 중 [step] 번째의 단어를 보여 주는 범위 지정 화면. 범위가 없거나 맞지 않으면 전체로 둔다. */
    fun goQuizRange(step: Int = 0) {
        val sources = orderedQuizSources
        // 고른 목록이 모두 사라졌으면(주제 삭제·북마크가 모두 해제됨) 목록 선택으로 돌아간다.
        if (sources.isEmpty()) return goQuizSetup()
        val index = step.coerceIn(0, sources.lastIndex)
        val words = quizWords(sources[index])
        quiz = null
        quizRangeStep = index
        val key = quizSourceKey(sources[index])
        val range = quizRanges[key]
        if (words.isNotEmpty() && (range == null || range.last > words.lastIndex)) {
            quizRanges = quizRanges + (key to words.indices)
        }
        screen = Screen.QUIZ_RANGE
    }

    /** 범위 화면의 [다음]. 마지막 목록이면 아무것도 하지 않는다. */
    fun nextQuizRange() {
        if (quizRangeStep < orderedQuizSources.lastIndex) goQuizRange(quizRangeStep + 1)
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
            Screen.QUIZ_RANGE -> if (quizRangeStep > 0) goQuizRange(quizRangeStep - 1) else goQuizSetup()
            Screen.COMBINED_SETUP -> goQuizMenu()
            Screen.QUIZ -> leaveQuiz()
        }
    }

    /* ---------------- 주제 ---------------- */

    private fun newId(): String = System.currentTimeMillis().toString() + Random.nextInt(0x100000).toString(16)

    fun addTopic(title: String, cards: List<Card>, folderId: String? = null) {
        val topic = Topic(
            id = newId(),
            title = title,
            cards = cards,
            createdAt = System.currentTimeMillis(),
            folderId = folderId?.takeIf { id -> folders.any { it.id == id } },
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

    /** 암기 화면에 보이는 순서: 폴더 순서대로 그 안의 주제, 그다음 폴더 밖 주제. */
    val orderedTopics: List<Topic>
        get() = folders.flatMap { topicsIn(it.id) } + topicsIn(null)

    /** [orderedTopics] 를 폴더별로 묶은 것. 폴더 밖 주제는 null 폴더로, 빈 폴더는 뺀다. */
    val topicGroups: List<Pair<Folder?, List<Topic>>>
        get() = (folders.map<Folder, Pair<Folder?, List<Topic>>> { it to topicsIn(it.id) } + (null to topicsIn(null)))
            .filter { it.second.isNotEmpty() }

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

    /** 북마크한 단어가 있는 주제들과 그 개수. 암기 화면 순서대로. */
    val bookmarkedTopics: List<Pair<Topic, Int>>
        get() = orderedTopics.mapNotNull { topic ->
            val count = topic.cards.count { it.bookmarked }
            if (count > 0) topic to count else null
        }

    /** 북마크한 단어 전부. [topicIds] 를 주면 그 주제들의 북마크만. (빈 집합은 전체) */
    private fun bookmarkedCards(topicIds: Set<String> = emptySet()): List<StudyCard> =
        orderedTopics
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

    /**
     * [순서 섞기]를 켜고 끈다.
     * 켤 때는 매번 새로 무작위로 섞고 첫 카드부터 본다.
     * 끌 때는 원래 순서로 되돌리고, 보고 있던 카드의 원래 자리로 간다.
     */
    fun shuffle() {
        val current = session ?: return
        val original = current.original
        session = if (original == null) {
            current.copy(cards = current.cards.shuffled(), index = 0, original = current.cards)
        } else {
            // 섞여 있는 동안 바뀐 북마크를 원래 순서의 카드에도 반영한다.
            val restored = original.map { sc ->
                val on = isBookmarked(sc)
                if (sc.card.bookmarked == on) sc else sc.copy(card = sc.card.copy(bookmarked = on))
            }
            val viewing = current.current
            val index = restored.indexOfFirst { it.topicId == viewing.topicId && it.index == viewing.index }
            current.copy(cards = restored, index = index.coerceAtLeast(0), original = null)
        }
        flipped = false
    }

    /* ---------------- 암기(학습 화면에서 범위를 정해 보는 입력 시험) ---------------- */

    /** 지금 시험이 학습 화면의 [암기]에서 시작됐는지. 끝내면 학습 화면으로 돌아간다. */
    var quizFromStudy by mutableStateOf(false)
        private set

    /** 결과 화면의 [홈으로]·뒤로 가기. [암기]에서 시작했으면 홈 대신 그 플래시 카드로 돌아간다. */
    fun finishQuiz() {
        if (quizFromStudy) returnToStudy() else goHome()
    }

    /** 학습 카드 중 [range](0부터, 양끝 포함) 안의 문제. 정답을 아는 카드만, 겹치는 단어는 하나만. */
    private fun studyQuizQuestions(range: IntRange): List<QuizQuestion> {
        val current = session ?: return emptyList()
        if (current.shuffled) return emptyList()
        return current.cards
            .filterIndexed { i, _ -> i in range }
            .map(::QuizQuestion)
            .filter { it.answerable }
            .distinctBy { it.wordKey }
    }

    /** [암기] 범위 안에서 낼 수 있는 문제 수. */
    fun studyQuizCount(range: IntRange): Int = studyQuizQuestions(range).size

    /** [암기] 범위로 입력 시험을 시작한다. 주제 시험과 같은 화면·채점 규칙(틀리면 북마크)을 쓴다. */
    fun startStudyQuiz(range: IntRange) {
        val current = session ?: return
        val questions = studyQuizQuestions(range)
        if (questions.isEmpty()) return
        val whole = range.first == 0 && range.last == current.cards.lastIndex
        val title = if (whole) current.title else "${current.title} (${range.first + 1}~${range.last + 1})"
        quizCombined = false
        quizFromStudy = true
        launchQuiz(title, questions)
    }

    /** 시험을 마치고 학습 화면으로 돌아온다. 시험 중 바뀐 북마크를 학습 카드에도 반영한다. */
    private fun returnToStudy() {
        val current = session ?: return goMemorize()
        quiz = null
        session = current.copy(
            cards = current.cards.map { sc ->
                val on = isBookmarked(sc)
                if (sc.card.bookmarked == on) sc else sc.copy(card = sc.card.copy(bookmarked = on))
            },
        )
        flipped = false
        screen = Screen.STUDY
    }

    fun toggleBookmark() {
        val current = session ?: return
        toggleBookmarkAt(current.index)
    }

    /** 학습 카드 [index] 번째(지금 순서 기준)의 북마크를 켜거나 끈다. 단어 목록의 별에서 쓴다. */
    fun toggleBookmarkAt(index: Int) {
        val current = session ?: return
        val studyCard = current.cards.getOrNull(index) ?: return
        val on = !studyCard.card.bookmarked

        setBookmark(studyCard, on)

        // 화면에 보이는 사본에도 반영한다. (카드를 다시 그리지 않으므로 뒤집힌 상태가 유지된다)
        session = current.copy(
            cards = current.cards.mapIndexed { i, sc ->
                if (i == index) sc.copy(card = sc.card.copy(bookmarked = on)) else sc
            },
        )
    }

    /* ---------------- 시험 ---------------- */

    var quiz by mutableStateOf<QuizSession?>(null)
        private set

    /** 시험 볼 목록들. 주제 id 또는 [BOOKMARK_SOURCE]. 여러 개 고를 수 있고, 처음엔 아무것도 고르지 않는다. */
    var quizSources by mutableStateOf<Set<String>>(emptySet())
        private set

    /** 북마크 시험에 넣을 주제 id 들. 여러 개 고를 수 있고, 비어 있으면 모든 주제의 북마크. */
    var quizBookmarkTopics by mutableStateOf<Set<String>>(emptySet())
        private set

    /** 주제 시험 화면에서 펼쳐 둔 폴더들. 폴더 밖 주제는 [NO_FOLDER]. 처음엔 모두 접혀 있다. */
    var quizExpandedFolders by mutableStateOf<Set<String>>(emptySet())
        private set

    var quizShuffle by mutableStateOf(true)
        private set

    /** 목록별 출제 범위(0부터, 양끝 포함). 키는 [quizSourceKey]. */
    private var quizRanges by mutableStateOf<Map<String, IntRange>>(emptyMap())

    /** 범위 지정 화면에서 지금 보고 있는 목록의 순서(0부터). */
    var quizRangeStep by mutableStateOf(0)
        private set

    /** 고른 목록들을 화면 순서(북마크 먼저, 그다음 폴더·주제 순)로. */
    val orderedQuizSources: List<String>
        get() = buildList {
            if (BOOKMARK_SOURCE in quizSources && bookmarkCount > 0) add(BOOKMARK_SOURCE)
            orderedTopics.forEach { if (it.id in quizSources) add(it.id) }
        }

    /** 범위를 기억하는 키. 북마크는 고른 북마크 주제가 바뀌면 다른 목록으로 본다. */
    private fun quizSourceKey(source: String): String =
        if (source == BOOKMARK_SOURCE) source + "|" + quizBookmarkTopics.sorted().joinToString(",") else source

    /** 목록 하나의 이름. 북마크에서 주제를 골랐으면 그 주제 이름들을 쓴다. */
    fun quizSourceTitle(source: String): String = when {
        source != BOOKMARK_SOURCE -> topics.find { it.id == source }?.title.orEmpty()
        quizBookmarkTopics.isEmpty() -> BOOKMARK_TITLE
        else -> "★ " + orderedTopics.filter { it.id in quizBookmarkTopics }.joinToString(", ") { it.title }
    }

    /** 목록 하나의 단어 전부(목록 순서 그대로). */
    fun quizWords(source: String): List<StudyCard> = when (source) {
        BOOKMARK_SOURCE -> if (BOOKMARK_SOURCE in quizSources) bookmarkedCards(quizBookmarkTopics) else emptyList()
        else -> topics.find { it.id == source }?.studyCards().orEmpty()
    }

    /** 고른 목록들의 단어 수 합. (같은 카드가 겹쳐도 목록마다 센다) */
    val quizSelectedWordCount: Int
        get() = orderedQuizSources.sumOf { quizWords(it).size }

    /** 목록 하나의 지금 범위. 정한 적 없으면 전체. */
    fun quizRange(source: String): IntRange =
        quizRanges[quizSourceKey(source)] ?: quizWords(source).indices

    /** 범위 화면에서 보고 있는 목록. */
    val currentQuizSource: String?
        get() = orderedQuizSources.getOrNull(quizRangeStep)

    /** 목록 하나의 범위 안에서 낼 수 있는 문제 수. */
    fun quizQuestionCount(source: String): Int = quizQuestions(source).size

    /** 고른 목록 모두에서 낼 수 있는 문제 수. */
    val quizQuestionCount: Int
        get() = quizQuestions().size

    /** 목록을 켜거나 끈다. 북마크가 없으면 북마크는 고를 수 없다. */
    fun toggleQuizSource(source: String) {
        if (source == BOOKMARK_SOURCE && bookmarkCount == 0) return
        quizSources = if (source in quizSources) quizSources - source else quizSources + source
    }

    /** 북마크 시험에 넣을 주제를 켜거나 끈다. 북마크가 없는 주제는 고를 수 없다. */
    fun toggleQuizBookmarkTopic(topicId: String) {
        if (bookmarkedTopics.none { it.first.id == topicId }) return
        quizSources = quizSources + BOOKMARK_SOURCE
        quizBookmarkTopics =
            if (topicId in quizBookmarkTopics) quizBookmarkTopics - topicId else quizBookmarkTopics + topicId
    }

    /** 북마크 시험을 모든 주제의 북마크로 되돌린다. */
    fun selectAllQuizBookmarkTopics() {
        quizSources = quizSources + BOOKMARK_SOURCE
        quizBookmarkTopics = emptySet()
    }

    /** 주제 시험 화면에서 폴더를 접거나 편다. [folderId] 가 null 이면 폴더 밖 주제. */
    fun toggleQuizFolder(folderId: String?) {
        val key = folderId ?: NO_FOLDER
        quizExpandedFolders = if (key in quizExpandedFolders) quizExpandedFolders - key else quizExpandedFolders + key
    }

    fun toggleQuizShuffle() {
        quizShuffle = !quizShuffle
    }

    /** 범위 화면에서 보고 있는 목록의 범위를 바꾼다. */
    fun changeQuizRange(range: IntRange) {
        val source = currentQuizSource ?: return
        val last = quizWords(source).lastIndex
        if (last < 0) return
        val first = range.first.coerceIn(0, last)
        quizRanges = quizRanges + (quizSourceKey(source) to first..range.last.coerceIn(first, last))
    }

    /** 목록 하나의 범위 안 카드 중 정답을 아는 카드만. */
    private fun quizQuestions(source: String): List<QuizQuestion> {
        val range = quizRange(source)
        return quizWords(source)
            .filterIndexed { i, _ -> i in range }
            .map(::QuizQuestion)
            .filter { it.answerable }
    }

    /** 고른 목록 모두의 문제. 목록 순서대로 잇고, 여러 주제에 겹치는 단어는 처음 나온 것 하나만. */
    private fun quizQuestions(): List<QuizQuestion> =
        orderedQuizSources
            .flatMap(::quizQuestions)
            .distinctBy { it.wordKey }

    fun startQuiz() {
        val questions = quizQuestions()
        if (questions.isEmpty()) return

        val title = orderedQuizSources.joinToString(" · ") { source ->
            val range = quizRange(source)
            val whole = range.first == 0 && range.last == quizWords(source).lastIndex
            if (whole) quizSourceTitle(source)
            else "${quizSourceTitle(source)} (${range.first + 1}~${range.last + 1})"
        }
        quizCombined = false
        quizFromStudy = false
        launchQuiz(title, questions)
    }

    /** 종합 시험은 순서를 무조건 섞는다. 다시 시험·틀린 단어 다시 시험도 마찬가지. */
    private fun launchQuiz(title: String, questions: List<QuizQuestion>) {
        quiz = QuizSession(title, if (quizCombined || quizShuffle) questions.shuffled() else questions)
        screen = Screen.QUIZ
    }

    /** 시험을 그만두면 시험을 고른 화면으로 돌아간다. (일반: 범위 지정, 종합: 종합 시험 설정) */
    fun leaveQuiz() {
        when {
            quizFromStudy -> returnToStudy()
            quizCombined -> goCombinedSetup()
            else -> goQuizRange(quizRangeStep)
        }
    }

    /** 결과 화면의 [시험 설정]. [암기]에서 시작했으면 학습 화면으로 돌아가 범위를 다시 정한다. */
    fun goQuizOriginSetup() {
        when {
            quizFromStudy -> returnToStudy()
            quizCombined -> goCombinedSetup()
            else -> goQuizSetup()
        }
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

    /** 고른 목록들의 단어를 모은 것. 여러 주제에 겹치는 단어는 하나만, 정답을 아는 카드만. */
    private fun combinedPool(): List<QuizQuestion> {
        val fromTopics = orderedTopics.filter { it.id in combinedSources }.flatMap { it.studyCards() }
        val fromBookmarks = if (BOOKMARK_SOURCE in combinedSources) bookmarkedCards() else emptyList()
        return (fromTopics + fromBookmarks)
            .map(::QuizQuestion)
            .filter { it.answerable }
            .distinctBy { it.wordKey }
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
        quizFromStudy = false
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
