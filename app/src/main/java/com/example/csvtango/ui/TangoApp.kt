package com.example.csvtango.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.csvtango.speech.rememberJapaneseSpeaker

@Composable
fun TangoApp(vm: TangoViewModel = viewModel()) {
    val speaker = rememberJapaneseSpeaker()

    // 다른 카드/문제로 넘어가면 읽던 소리를 멈춘다. (뒤집기만 할 때는 그대로 둔다)
    LaunchedEffect(vm.screen, vm.session?.title, vm.session?.index, vm.quiz?.index) { speaker.stop() }

    BackHandler(enabled = vm.screen != Screen.HOME) {
        if (vm.screen == Screen.MEMORIZE && vm.editing) {
            vm.editing = false
        } else {
            speaker.stop()
            vm.back()
        }
    }

    when (vm.screen) {
        Screen.HOME -> HomeScreen(
            wordCount = vm.wordCount,
            onMemorize = vm::goMemorize,
            onQuiz = vm::goQuizMenu,
        )

        Screen.QUIZ_MENU -> QuizMenuScreen(
            topicCount = vm.topics.size,
            onTopicQuiz = vm::goQuizSetup,
            onCombinedQuiz = vm::goCombinedSetup,
            onBack = vm::goHome,
        )

        Screen.MEMORIZE -> MemorizeScreen(
            folders = vm.folders,
            openFolder = vm.openFolder,
            topicsIn = vm::topicsIn,
            hasTopics = vm.topics.isNotEmpty(),
            bookmarkCount = vm.bookmarkCount,
            onBack = vm::back,
            onOpenTopic = vm::openTopic,
            onOpenFolder = vm::enterFolder,
            onOpenBookmarks = vm::goBookmarks,
            onClearBookmarks = vm::clearBookmarks,
            editing = vm.editing,
            onToggleEdit = { vm.editing = !vm.editing },
            onMove = vm::moveTopic,
            onRename = vm::renameTopic,
            onDelete = vm::deleteTopic,
            onMoveToFolder = vm::moveTopicToFolder,
            onCreateFolder = vm::createFolder,
            onAddCsv = vm::goUpload,
            onMoveFolder = vm::moveFolder,
            onRenameFolder = vm::renameFolder,
            onDeleteFolder = vm::deleteFolder,
        )

        Screen.BOOKMARKS -> BookmarkListScreen(
            totalCount = vm.bookmarkCount,
            topics = vm.bookmarkedTopics,
            onBack = vm::back,
            onOpenAll = vm::openBookmarks,
            onOpenTopic = vm::openTopicBookmarks,
        )

        Screen.UPLOAD -> UploadScreen(
            onBack = vm::goMemorize,
            onSave = vm::addTopic,
        )

        Screen.STUDY -> vm.session?.let { session ->
            StudyScreen(
                session = session,
                flipped = vm.flipped,
                speaker = speaker,
                backLabel = if (vm.studyFromBookmarks) "‹ 북마크" else "‹ 주제",
                onBack = { speaker.stop(); vm.back() },
                onFlip = vm::flip,
                onMove = vm::move,
                onSeek = vm::jumpTo,
                onShuffle = vm::shuffle,
                onToggleBookmark = vm::toggleBookmark,
            )
        }

        Screen.QUIZ_SETUP -> QuizSetupScreen(
            topics = vm.topics,
            bookmarkCount = vm.bookmarkCount,
            selectedSource = vm.quizSource,
            shuffle = vm.quizShuffle,
            wordCount = vm.quizWords().size,
            onSelect = vm::selectQuizSource,
            onToggleShuffle = vm::toggleQuizShuffle,
            onBack = vm::goQuizMenu,
            onNext = vm::goQuizRange,
        )

        Screen.QUIZ_RANGE -> QuizRangeScreen(
            title = vm.quizSourceTitle,
            words = vm.quizWords(),
            range = vm.quizRange,
            questionCount = vm.quizQuestionCount,
            onRangeChange = vm::changeQuizRange,
            onBack = vm::goQuizSetup,
            onStart = vm::startQuiz,
        )

        Screen.COMBINED_SETUP -> CombinedQuizScreen(
            topics = vm.topics,
            bookmarkCount = vm.bookmarkCount,
            selected = vm.combinedSources,
            countText = vm.combinedCountText,
            available = vm.combinedAvailable,
            questionCount = vm.combinedQuestionCount,
            onToggle = vm::toggleCombinedSource,
            onCountChange = vm::changeCombinedCount,
            onBack = vm::goQuizMenu,
            onStart = vm::startCombinedQuiz,
        )

        Screen.QUIZ -> vm.quiz?.let { quiz ->
            QuizScreen(
                session = quiz,
                bookmarked = quiz.pending?.let { vm.isBookmarked(it.question.card) } ?: false,
                speaker = speaker,
                onQuit = { speaker.stop(); vm.leaveQuiz() },
                onSubmit = vm::submitAnswer,
                onNext = { speaker.stop(); vm.nextQuestion() },
                onToggleBookmark = vm::toggleQuizBookmark,
                isBookmarked = vm::isBookmarked,
                onToggleCardBookmark = vm::toggleCardBookmark,
                onRetryWrong = vm::retryWrong,
                onRestart = vm::restartQuiz,
                onSetup = vm::goQuizOriginSetup,
                onHome = vm::goHome,
            )
        }
    }
}
