package com.example.csvtango.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.csvtango.speech.JapaneseSpeaker
import com.example.csvtango.speech.rememberJapaneseSpeaker
import com.example.csvtango.ui.theme.Gold
import com.example.csvtango.ui.theme.Ink
import com.example.csvtango.ui.theme.Sheet
import kotlinx.coroutines.flow.filterNotNull

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

    // 북마크가 10, 20, 30… 개가 되면 화면 아래에 잠깐 떠 있다가 사라지는 알림을 띄운다.
    // 알림 상태를 LaunchedEffect 의 키로 쓰면 비우는 순간 효과가 취소되어 스낵바가 뜨지 않으므로,
    // 한 번만 시작하는 효과 안에서 snapshotFlow 로 상태를 지켜본다.
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        snapshotFlow { vm.bookmarkMilestone }
            .filterNotNull()
            .collect { milestone ->
                vm.consumeBookmarkMilestone()
                snackbar.showSnackbar(
                    message = "★ 북마크가 ${milestone.count}개가 되었습니다",
                    duration = SnackbarDuration.Short,
                )
            }
    }

    Box(Modifier.fillMaxSize()) {
        Screens(vm = vm, speaker = speaker)

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 16.dp, vertical = 24.dp),
        ) { data ->
            Snackbar(
                containerColor = Ink,
                contentColor = Sheet,
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(
                    text = data.visuals.message,
                    color = Gold,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun Screens(vm: TangoViewModel, speaker: JapaneseSpeaker) {
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
            topicGroups = vm.topicGroups,
            bookmarkCount = vm.bookmarkCount,
            bookmarkedTopics = vm.bookmarkedTopics,
            selectedSource = vm.quizSource,
            selectedBookmarkTopics = vm.quizBookmarkTopics,
            shuffle = vm.quizShuffle,
            wordCount = vm.quizWords().size,
            onSelect = vm::selectQuizSource,
            onToggleBookmarkTopic = vm::toggleQuizBookmarkTopic,
            onAllBookmarkTopics = vm::selectAllQuizBookmarkTopics,
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
            topicGroups = vm.topicGroups,
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
