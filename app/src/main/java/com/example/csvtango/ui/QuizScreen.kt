package com.example.csvtango.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import androidx.compose.ui.graphics.Color
import com.example.csvtango.data.Card
import com.example.csvtango.data.Kana
import com.example.csvtango.data.PromptKind
import com.example.csvtango.data.QuizQuestion
import com.example.csvtango.data.QuizResult
import com.example.csvtango.data.QuizSession
import com.example.csvtango.data.StudyCard
import com.example.csvtango.speech.JapaneseSpeaker
import com.example.csvtango.speech.SpeechTarget
import com.example.csvtango.speech.firstReading
import com.example.csvtango.ui.theme.Accent
import com.example.csvtango.ui.theme.ErrBg
import com.example.csvtango.ui.theme.ErrInk
import com.example.csvtango.ui.theme.FieldBg
import com.example.csvtango.ui.theme.Gold
import com.example.csvtango.ui.theme.GoldSoft
import com.example.csvtango.ui.theme.Ink
import com.example.csvtango.ui.theme.Line
import com.example.csvtango.ui.theme.Muted
import com.example.csvtango.ui.theme.OkBg
import com.example.csvtango.ui.theme.OkInk
import com.example.csvtango.ui.theme.Paper
import com.example.csvtango.ui.theme.Sheet
import com.example.csvtango.ui.theme.StarOff

/**
 * 시험 화면. 문제 풀이 → 채점 결과 → 다음 문제를 반복하고, 끝나면 결과를 보여 준다.
 *
 * @param bookmarked 방금 채점한 카드가 지금 북마크되어 있는지. (정답 뒤 수동 북마크 버튼 상태)
 */
@Composable
fun QuizScreen(
    session: QuizSession,
    bookmarked: Boolean,
    speaker: JapaneseSpeaker,
    onQuit: () -> Unit,
    onSubmit: (String) -> Unit,
    onNext: () -> Unit,
    onToggleBookmark: () -> Unit,
    isBookmarked: (StudyCard) -> Boolean,
    onToggleCardBookmark: (StudyCard) -> Unit,
    onRetryWrong: () -> Unit,
    onRestart: () -> Unit,
    onSetup: () -> Unit,
    onHome: () -> Unit,
) {
    if (session.finished) {
        ResultView(session, isBookmarked, onToggleCardBookmark, onRetryWrong, onRestart, onSetup, onHome)
        return
    }

    var confirmQuit by remember { mutableStateOf(false) }
    val inProgress = session.results.isNotEmpty() || session.pending != null
    val quit: () -> Unit = { if (inProgress) confirmQuit = true else onQuit() }

    BackHandler(onBack = quit)

    Column(Modifier.fillMaxSize().background(Paper)) {
        TopBar {
            BackButton(quit)
            Text(
                session.title,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text("${session.index + 1} / ${session.questions.size}", fontSize = 13.sp, color = Muted)
        }

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 20.dp)
                .windowInsetsPadding(WindowInsets.navigationBars),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ProgressBar(position = session.index, total = session.questions.size)

            val pending = session.pending
            PromptCard(session.current, revealed = pending != null)
            AnswerField(
                index = session.index,
                katakana = session.current.katakana,
                locked = pending != null,
                onSubmit = onSubmit,
            )

            if (pending == null) {
                Text(
                    if (session.current.katakana) {
                        "가타카나 단어입니다. 입력한 가나는 가타카나로 바뀌어 보입니다. 입력한 뒤 확인을 누르세요."
                    } else {
                        "히라가나로 입력한 뒤 확인을 누르세요. 가타카나로 입력해도 같은 답으로 봅니다."
                    },
                    fontSize = 12.5.sp,
                    lineHeight = 19.sp,
                    color = Muted,
                )
            } else {
                val last = session.index == session.questions.lastIndex
                BigButton(if (last) "결과 보기" else "다음 ›", onNext)
                ResultPanel(pending, bookmarked, speaker, onToggleBookmark)
            }
        }
    }

    if (confirmQuit) {
        AlertDialog(
            onDismissRequest = { confirmQuit = false },
            title = { Text("시험을 그만둘까요?") },
            text = { Text("지금까지 푼 ${session.results.size}문제의 결과는 사라집니다. 북마크는 그대로 남습니다.") },
            confirmButton = {
                TextButton(onClick = { confirmQuit = false; onQuit() }) { Text("그만두기", color = Accent) }
            },
            dismissButton = {
                TextButton(onClick = { confirmQuit = false }) { Text("계속 풀기", color = Muted) }
            },
            containerColor = Sheet,
        )
    }
}

@Composable
private fun ProgressBar(position: Int, total: Int) {
    Box(Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(Line)) {
        Box(
            Modifier
                .fillMaxWidth(if (total == 0) 0f else position.toFloat() / total)
                .fillMaxSize()
                .background(Accent),
        )
    }
}

/** 문제 카드. 채점 전에는 문제만, 채점 뒤에는 질문 종류에 따른 안내만 살짝 바꾼다. */
@Composable
private fun PromptCard(question: QuizQuestion, revealed: Boolean) {
    val script = if (question.katakana) "가타카나" else "히라가나"
    val label = when (question.kind) {
        PromptKind.JAPANESE -> "이 단어의 ${script}는?"
        PromptKind.MEANING -> "이 뜻에 맞는 ${script}는?"
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Sheet,
        border = BorderStroke(1.dp, if (revealed) Line else Accent.copy(alpha = .35f)),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Muted)
            Text(
                question.prompt,
                fontSize = promptSize(question),
                lineHeight = promptSize(question) * 1.2f,
                fontWeight = FontWeight.Bold,
                color = Ink,
                textAlign = TextAlign.Center,
            )
            if (question.kind == PromptKind.MEANING) {
                Text("단어가 가나뿐이라 뜻을 보여 줍니다", fontSize = 11.5.sp, color = StarOff)
            }
        }
    }
}

/**
 * 답 입력란. 문제가 바뀌면 비우고 다시 포커스를 준다.
 *
 * 키보드에는 일본어 입력을 쓰도록 힌트를 준다. 안드로이드에는 키보드를 가타카나 모드로 바꾸는
 * 표준 방법이 없으므로, [katakana] 문제에서는 입력한 히라가나를 가타카나로 바꿔 보여 주고
 * 제출할 때도 가타카나로 바꿔 보낸다. (글자 수가 같아 IME 조합 중에도 커서가 어긋나지 않는다)
 */
@Composable
private fun AnswerField(index: Int, katakana: Boolean, locked: Boolean, onSubmit: (String) -> Unit) {
    var input by remember(index) { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val submit = { if (input.isNotBlank()) onSubmit(if (katakana) Kana.toKatakana(input) else input) }

    LaunchedEffect(index) { focusRequester.requestFocus() }

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = input,
            onValueChange = { if (!locked) input = it },
            placeholder = { Text(if (katakana) "カタカナ" else "ひらがな", color = StarOff) },
            singleLine = true,
            readOnly = locked,
            shape = RoundedCornerShape(12.dp),
            textStyle = TextStyle(fontSize = 20.sp, color = Ink),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done, hintLocales = JapaneseLocale),
            visualTransformation = if (katakana) KatakanaTransformation else VisualTransformation.None,
            keyboardActions = KeyboardActions(onDone = { submit() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Sheet,
                unfocusedContainerColor = FieldBg,
                focusedBorderColor = Accent,
                unfocusedBorderColor = Line,
                cursorColor = Accent,
                focusedTextColor = Ink,
                unfocusedTextColor = Ink,
            ),
            modifier = Modifier.weight(1f).focusRequester(focusRequester),
        )

        Surface(
            onClick = submit,
            enabled = !locked && input.isNotBlank(),
            shape = RoundedCornerShape(12.dp),
            color = if (!locked && input.isNotBlank()) Accent else Line,
        ) {
            Text(
                "확인",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (!locked && input.isNotBlank()) Sheet else Muted,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 17.dp),
            )
        }
    }
}

private val JapaneseLocale = LocaleList("ja-JP")

/** 히라가나를 가타카나로 바꿔 보여 준다. 글자 수가 그대로라 오프셋은 1:1이다. */
private object KatakanaTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText =
        TransformedText(
            AnnotatedString(Kana.toKatakana(text.text), text.spanStyles, text.paragraphStyles),
            OffsetMapping.Identity,
        )
}

/** 채점 결과. 정답이면 초록, 오답이면 빨강 바탕에 단어 전체를 보여 준다. */
@Composable
private fun ResultPanel(
    result: QuizResult,
    bookmarked: Boolean,
    speaker: JapaneseSpeaker,
    onToggleBookmark: () -> Unit,
) {
    val context = LocalContext.current
    val card = result.question.card.card
    val ok = result.correct
    val tint = if (ok) OkInk else ErrInk

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (ok) OkBg else ErrBg,
        border = BorderStroke(1.dp, tint.copy(alpha = .25f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (ok) "○ 정답" else "✕ 오답", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = tint)
                if (!ok) {
                    Text("입력: ${result.input.trim()}", fontSize = 13.sp, color = tint.copy(alpha = .8f))
                }
            }

            HorizontalDivider(color = tint.copy(alpha = .15f))

            Text(card.jp, fontSize = 30.sp, lineHeight = 38.sp, fontWeight = FontWeight.Bold, color = Ink)
            if (card.kana.isNotEmpty()) {
                Text(card.kana, fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold, color = Ink)
            }
            if (card.ko.isNotEmpty()) {
                Text(card.ko, fontSize = 15.sp, lineHeight = 22.sp, color = Muted)
            }

            Row(
                Modifier.padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Chip(
                    text = "🔊 듣기",
                    active = speaker.speaking == SpeechTarget.WORD,
                    onClick = {
                        if (!speaker.japaneseAvailable) {
                            Toast.makeText(context, "이 기기에서는 일본어 음성을 사용할 수 없습니다.", Toast.LENGTH_SHORT).show()
                        } else {
                            speaker.speak(firstReading(card.kana).ifEmpty { card.jp }, SpeechTarget.WORD, 0.9f)
                        }
                    },
                )

                if (ok) {
                    // 맞힌 단어는 북마크를 해제한다. 그래도 남겨 두고 싶으면 다시 켤 수 있다.
                    BookmarkChip(on = bookmarked, onClick = onToggleBookmark)
                } else {
                    Text("★ 북마크에 저장했습니다", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Gold)
                }
            }

            if (card.exJp.isNotEmpty() || card.exKana.isNotEmpty() || card.exKo.isNotEmpty()) {
                ExampleSection(card, speaker, tint)
            }
        }
    }
}

/** 예문(일본어·히라가나·한국어 뜻). 처음에는 접혀 있고 눌러서 펴고 접는다. */
@Composable
private fun ExampleSection(card: Card, speaker: JapaneseSpeaker, tint: Color) {
    val context = LocalContext.current
    var expanded by remember(card) { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Surface(onClick = { expanded = !expanded }, color = Color.Transparent, shape = RoundedCornerShape(8.dp)) {
            Text(
                if (expanded) "▾ 예문 접기" else "▸ 예문 보기",
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = tint,
                modifier = Modifier.padding(vertical = 6.dp),
            )
        }

        if (expanded) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Sheet.copy(alpha = .7f),
                border = BorderStroke(1.dp, tint.copy(alpha = .15f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (card.exJp.isNotEmpty()) {
                        Text(card.exJp, fontSize = 16.sp, lineHeight = 25.sp, color = Ink)
                    }
                    if (card.exKana.isNotEmpty()) {
                        Text(card.exKana, fontSize = 14.sp, lineHeight = 22.sp, color = Ink.copy(alpha = .8f))
                    }
                    if (card.exKo.isNotEmpty()) {
                        Text(card.exKo, fontSize = 14.sp, lineHeight = 22.sp, color = Muted)
                    }
                    if (card.exJp.isNotEmpty() || card.exKana.isNotEmpty()) {
                        Box(Modifier.padding(top = 6.dp)) {
                            Chip(
                                text = "🔊 문장 듣기",
                                active = speaker.speaking == SpeechTarget.SENTENCE,
                                onClick = {
                                    if (!speaker.japaneseAvailable) {
                                        Toast.makeText(context, "이 기기에서는 일본어 음성을 사용할 수 없습니다.", Toast.LENGTH_SHORT).show()
                                    } else {
                                        speaker.speak(card.exKana.ifEmpty { card.exJp }, SpeechTarget.SENTENCE, 0.85f)
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BookmarkChip(on: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (on) GoldSoft else FieldBg,
        border = BorderStroke(1.dp, if (on) Gold else Line),
    ) {
        Text(
            if (on) "★ 북마크됨" else "☆ 북마크 추가",
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (on) Gold else Ink,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
        )
    }
}

@Composable
private fun BigButton(text: String, onClick: () -> Unit, primary: Boolean = true) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (primary) Accent else Sheet,
        border = if (primary) null else BorderStroke(1.dp, Line),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (primary) Sheet else Ink,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 15.dp),
        )
    }
}

/** 시험이 끝난 뒤 점수와 틀린 단어를 보여 준다. */
@Composable
private fun ResultView(
    session: QuizSession,
    isBookmarked: (StudyCard) -> Boolean,
    onToggleBookmark: (StudyCard) -> Unit,
    onRetryWrong: () -> Unit,
    onRestart: () -> Unit,
    onSetup: () -> Unit,
    onHome: () -> Unit,
) {
    val total = session.questions.size
    val correct = session.correctCount
    val wrong = session.wrong
    val percent = if (total == 0) 0 else correct * 100 / total

    Column(Modifier.fillMaxSize().background(Paper)) {
        TopBar {
            BackButton(onHome)
            Text("시험 결과", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Ink)
        }

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp)
                .windowInsetsPadding(WindowInsets.navigationBars),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Sheet,
                border = BorderStroke(1.dp, Line),
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(vertical = 30.dp, horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(session.title, fontSize = 13.sp, color = Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        "$correct / $total",
                        fontSize = 48.sp,
                        lineHeight = 56.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (wrong.isEmpty()) OkInk else Accent,
                    )
                    Text("정답률 $percent%", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                    Text(
                        if (wrong.isEmpty()) "모두 맞혔습니다!" else "틀린 ${wrong.size}개는 북마크에 저장했습니다.",
                        fontSize = 13.sp,
                        color = Muted,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }

            if (wrong.isNotEmpty()) {
                BigButton("틀린 단어만 다시 시험 · ${wrong.size}문제", onRetryWrong)
            }
            BigButton("같은 범위로 다시 시험", onRestart, primary = wrong.isEmpty())
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GhostButton("시험 설정", onSetup, Modifier.weight(1f))
                GhostButton("홈으로", onHome, Modifier.weight(1f))
            }

            if (wrong.isNotEmpty()) {
                Text(
                    "틀린 단어",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Muted,
                    modifier = Modifier.padding(top = 14.dp, bottom = 2.dp),
                )
                Surface(shape = RoundedCornerShape(16.dp), color = Sheet, border = BorderStroke(1.dp, Line)) {
                    Column {
                        wrong.forEachIndexed { i, r ->
                            val card = r.question.card
                            WrongRow(r, bookmarked = isBookmarked(card), onToggleBookmark = { onToggleBookmark(card) })
                            if (i < wrong.lastIndex) HorizontalDivider(color = Line.copy(alpha = .6f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WrongRow(result: QuizResult, bookmarked: Boolean, onToggleBookmark: () -> Unit) {
    val card = result.question.card.card
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // 한자 · 히라가나 · 뜻 · 입력을 각각 한 줄씩 보여 준다.
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(card.jp, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = Ink)
            Text(card.kana, fontSize = 17.sp, color = Ink.copy(alpha = .75f))
            Text(card.ko, fontSize = 16.sp, color = Muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text("입력: ${result.input.trim()}", fontSize = 15.sp, color = ErrInk.copy(alpha = .8f))
        }
        // 누를 때마다 북마크를 켜고 끈다.
        Surface(
            onClick = onToggleBookmark,
            shape = CircleShape,
            color = if (bookmarked) GoldSoft else FieldBg,
            border = BorderStroke(1.dp, if (bookmarked) Gold else Line),
        ) {
            Text(
                if (bookmarked) "★" else "☆",
                fontSize = 18.sp,
                color = if (bookmarked) Gold else StarOff,
                modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
            )
        }
    }
}

private fun promptSize(question: QuizQuestion) = when {
    question.kind == PromptKind.MEANING -> if (question.prompt.length <= 10) 28.sp else 22.sp
    question.prompt.length <= 4 -> 52.sp
    question.prompt.length <= 8 -> 38.sp
    else -> 28.sp
}
