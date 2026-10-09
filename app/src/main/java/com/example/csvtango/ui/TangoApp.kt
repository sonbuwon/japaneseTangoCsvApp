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
            topicCount = vm.topics.size,
            wordCount = vm.wordCount,
            bookmarkCount = vm.bookmarkCount,
            onMemorize = vm::goMemorize,
            onQuiz = vm::goQuizSetup,
        )

        Screen.MEMORIZE -> MemorizeScreen(
            topics = vm.topics,
            bookmarkCount = vm.bookmarkCount,
            onBack = vm::goHome,
            onAdd = vm::goUpload,
            onOpenTopic = vm::openTopic,
            onOpenBookmarks = vm::openBookmarks,
            onClearBookmarks = vm::clearBookmarks,
            editing = vm.editing,
            onToggleEdit = { vm.editing = !vm.editing },
            onMove = vm::moveTopic,
            onRename = vm::renameTopic,
            onDelete = vm::deleteTopic,
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
                onBack = { speaker.stop(); vm.goMemorize() },
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
            onBack = vm::goHome,
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

        Screen.QUIZ -> vm.quiz?.let { quiz ->
            QuizScreen(
                session = quiz,
                bookmarked = quiz.pending?.let { vm.isBookmarked(it.question.card) } ?: false,
                speaker = speaker,
                onQuit = { speaker.stop(); vm.goQuizRange() },
                onSubmit = vm::submitAnswer,
                onNext = { speaker.stop(); vm.nextQuestion() },
                onToggleBookmark = vm::toggleQuizBookmark,
                onRetryWrong = vm::retryWrong,
                onRestart = vm::restartQuiz,
                onSetup = vm::goQuizSetup,
                onHome = vm::goHome,
            )
        }
    }
}
