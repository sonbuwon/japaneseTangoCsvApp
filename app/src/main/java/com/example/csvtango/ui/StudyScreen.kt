package com.example.csvtango.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import com.example.csvtango.data.Card
import com.example.csvtango.data.StudySession
import com.example.csvtango.speech.JapaneseSpeaker
import com.example.csvtango.speech.SpeechTarget
import com.example.csvtango.speech.firstReading
import com.example.csvtango.ui.theme.Accent
import com.example.csvtango.ui.theme.AccentSoft
import com.example.csvtango.ui.theme.FieldBg
import com.example.csvtango.ui.theme.Gold
import com.example.csvtango.ui.theme.Ink
import com.example.csvtango.ui.theme.Line
import com.example.csvtango.ui.theme.Muted
import com.example.csvtango.ui.theme.Paper
import com.example.csvtango.ui.theme.Sheet
import com.example.csvtango.ui.theme.StarOff

@Composable
fun StudyScreen(
    session: StudySession,
    flipped: Boolean,
    speaker: JapaneseSpeaker,
    backLabel: String = "‹ 주제",
    onBack: () -> Unit,
    onFlip: () -> Unit,
    onMove: (Int) -> Unit,
    onSeek: (Int) -> Unit,
    onShuffle: () -> Unit,
    onToggleBookmark: () -> Unit,
) {
    val card = session.current.card
    var wordListOpen by remember { mutableStateOf(false) }

    BackHandler(enabled = wordListOpen) { wordListOpen = false }

    Box(Modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize().background(Paper)) {
        TopBar {
            GhostButton(backLabel, onBack)
            Text(
                session.title,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            // 단어 목록 패널 열기
            Box(
                Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClickLabel = "단어 목록") { wordListOpen = true }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text("☰", fontSize = 20.sp, color = Muted)
            }
        }

        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 20.dp)
                .windowInsetsPadding(WindowInsets.navigationBars),
        ) {
            Progress(
                position = session.index + 1,
                total = session.cards.size,
                bookmarks = session.cards.count { it.card.bookmarked },
                onSeek = onSeek,
            )

            Flashcard(
                card = card,
                flipped = flipped,
                speaker = speaker,
                onFlip = onFlip,
                onSwipe = onMove,
                onToggleBookmark = onToggleBookmark,
                modifier = Modifier.weight(1f).padding(top = 6.dp),
            )

            Row(
                Modifier.fillMaxWidth().padding(top = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                NavButton("‹ 이전", Modifier.weight(1f)) { onMove(-1) }
                NavButton("다음 ›", Modifier.weight(1f)) { onMove(1) }
            }

            Surface(
                onClick = onShuffle,
                shape = RoundedCornerShape(10.dp),
                color = Paper,
                border = BorderStroke(1.dp, Line),
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            ) {
                Text(
                    "🔀 순서 섞기",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Muted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 13.dp),
                )
            }
        }
    }

    // 어두운 배경 — 누르면 닫힌다
    AnimatedVisibility(visible = wordListOpen, enter = fadeIn(), exit = fadeOut()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = .35f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { wordListOpen = false },
        )
    }

    // 오른쪽에서 미끄러져 나오는 단어 목록
    AnimatedVisibility(
        visible = wordListOpen,
        enter = slideInHorizontally { it },
        exit = slideOutHorizontally { it },
        modifier = Modifier.align(Alignment.CenterEnd),
    ) {
        WordListPanel(
            session = session,
            onPick = { i ->
                onSeek(i)
                wordListOpen = false
            },
        )
    }
    }
}

/** 주제의 단어를 일본어·히라가나·한국어로 훑어보는 패널. 누르면 그 카드로 이동한다. */
@Composable
private fun WordListPanel(session: StudySession, onPick: (Int) -> Unit) {
    val listState = rememberLazyListState()

    // 열면 지금 보고 있는 카드가 바로 보이게 한다.
    LaunchedEffect(Unit) {
        listState.scrollToItem((session.index - 2).coerceAtLeast(0))
    }

    Surface(
        color = Sheet,
        modifier = Modifier.fillMaxHeight().width(300.dp),
        shadowElevation = 8.dp,
    ) {
        Column(Modifier.windowInsetsPadding(WindowInsets.systemBars)) {
            Row(
                Modifier.fillMaxWidth().padding(start = 18.dp, end = 8.dp, top = 14.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "단어 목록",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Ink,
                    modifier = Modifier.weight(1f),
                )
                Text("${session.cards.size}개", fontSize = 13.sp, color = Muted)
            }
            HorizontalDivider(color = Line)

            LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                itemsIndexed(session.cards) { i, sc ->
                    val current = i == session.index
                    // 일본어 · 히라가나 · 한국어를 한 줄에 늘어놓는다
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(if (current) AccentSoft else Sheet)
                            .clickable { onPick(i) }
                            .padding(horizontal = 18.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            sc.card.jp,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (current) Accent else Ink,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (sc.card.kana.isNotEmpty()) {
                            Text(
                                sc.card.kana,
                                fontSize = 12.5.sp,
                                color = Muted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Text(
                            sc.card.ko,
                            fontSize = 12.5.sp,
                            color = Ink.copy(alpha = .75f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        if (sc.card.bookmarked) {
                            Text("★", fontSize = 13.sp, color = Gold)
                        }
                    }
                    HorizontalDivider(color = Line.copy(alpha = .6f))
                }
            }
        }
    }
}

@Composable
private fun Progress(position: Int, total: Int, bookmarks: Int, onSeek: (Int) -> Unit) {
    var dragging by remember { mutableStateOf(false) }
    var barWidth by remember { mutableIntStateOf(0) }

    val fraction by animateFloatAsState(
        targetValue = position.toFloat() / total,
        // 끄는 동안에는 손가락을 그대로 따라오게 한다.
        animationSpec = if (dragging) snap() else tween(200),
        label = "progress",
    )

    val knobSize = if (dragging) 18.dp else 13.dp
    val knobPx = with(LocalDensity.current) { knobSize.toPx() }

    Column {
        // 카드 높이를 줄이지 않도록 진행 표시와 같은 줄에 둔다.
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("$position / $total", fontSize = 13.sp, color = Muted)
            Text("북마크 ${bookmarks}개", fontSize = 12.5.sp, color = StarOff)
        }

        // 막대 자체는 얇지만, 손가락으로 잡을 수 있게 여백까지 포함해 터치를 받는다.
        Box(
            Modifier
                .fillMaxWidth()
                .height(22.dp)
                .onSizeChanged { barWidth = it.width }
                .pointerInput(total) {
                    detectTapGestures { offset -> onSeek(indexAt(offset.x, size.width, total)) }
                }
                .pointerInput(total) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            dragging = true
                            onSeek(indexAt(offset.x, size.width, total))
                        },
                        onDragEnd = { dragging = false },
                        onDragCancel = { dragging = false },
                    ) { change, _ -> onSeek(indexAt(change.position.x, size.width, total)) }
                },
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(if (dragging) 6.dp else 4.dp)
                    .clip(CircleShape)
                    .background(Line),
            ) {
                Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().background(Accent))
            }

            // 끌 수 있다는 표시. 채워진 끝을 가운데로 잡되, 양 끝에서는 막대에 딱 붙인다.
            Box(
                Modifier
                    .offset {
                        val x = barWidth * fraction - knobPx / 2
                        IntOffset(x.coerceIn(0f, (barWidth - knobPx).coerceAtLeast(0f)).roundToInt(), 0)
                    }
                    .size(knobSize)
                    .clip(CircleShape)
                    .background(Accent),
            )
        }
    }
}

/** 막대 위 x 좌표 → 카드 번호 */
private fun indexAt(x: Float, width: Int, total: Int): Int {
    if (width <= 0) return 0
    return ((x / width) * total).toInt().coerceIn(0, total - 1)
}

@Composable
private fun Flashcard(
    card: Card,
    flipped: Boolean,
    speaker: JapaneseSpeaker,
    onFlip: () -> Unit,
    onSwipe: (Int) -> Unit,
    onToggleBookmark: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rotation by animateFloatAsState(
        targetValue = if (flipped) 180f else 0f,
        animationSpec = tween(500, easing = CubicBezierEasing(.4f, .1f, .2f, 1f)),
        label = "flip",
    )

    // 좌우 스와이프: 카드가 손가락을 따라오다가, 충분히 밀거나 빠르게 튕기면 넘어간다.
    val scope = rememberCoroutineScope()
    val swipeX = remember { Animatable(0f) }
    val currentOnSwipe by rememberUpdatedState(onSwipe)

    Box(
        modifier
            .fillMaxWidth()
            .heightIn(min = 260.dp)
            .pointerInput(Unit) {
                val velocity = VelocityTracker()
                detectHorizontalDragGestures(
                    onDragStart = { velocity.resetTracking() },
                    onDragEnd = {
                        val width = size.width.toFloat()
                        val vx = velocity.calculateVelocity().x
                        val x = swipeX.value
                        // 왼쪽으로 밀면 다음, 오른쪽으로 밀면 이전 카드
                        val step = when {
                            x < -width * .25f || (vx < -1200f && x < 0f) -> 1
                            x > width * .25f || (vx > 1200f && x > 0f) -> -1
                            else -> 0
                        }
                        scope.launch {
                            if (step == 0) {
                                swipeX.animateTo(0f, spring())
                            } else {
                                swipeX.animateTo(-step * width * 1.2f, tween(160))
                                currentOnSwipe(step)
                                // 새 카드는 반대쪽에서 들어온다.
                                swipeX.snapTo(step * width * 1.2f)
                                swipeX.animateTo(0f, tween(220))
                            }
                        }
                    },
                    onDragCancel = { scope.launch { swipeX.animateTo(0f, spring()) } },
                ) { change, amount ->
                    change.consume()
                    velocity.addPosition(change.uptimeMillis, change.position)
                    scope.launch { swipeX.snapTo(swipeX.value + amount) }
                }
            }
            .graphicsLayer {
                translationX = swipeX.value
                rotationZ = if (size.width > 0f) swipeX.value / size.width * 8f else 0f
                alpha = if (size.width > 0f) 1f - (abs(swipeX.value) / size.width * .5f).coerceAtMost(.5f) else 1f
            },
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Sheet,
            border = BorderStroke(1.dp, Line),
            shadowElevation = 2.dp,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationY = rotation
                    cameraDistance = 14f * density
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onFlip,
                ),
        ) {
            // 절반을 넘어가면 뒷면을 보여 준다. (뒷면은 다시 뒤집어 글자가 거울처럼 보이지 않게 한다)
            if (rotation <= 90f) {
                FrontFace(card, speaker, onToggleBookmark)
            } else {
                Box(Modifier.graphicsLayer { rotationY = 180f }) { BackFace(card) }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FrontFace(card: Card, speaker: JapaneseSpeaker, onToggleBookmark: () -> Unit) {
    val context = LocalContext.current

    Face(
        hint = "눌러서 뒤집기 · 좌우로 밀어 넘기기",
        // 앞면에 붙여 카드와 함께 넘어가게 한다.
        overlay = {
            BookmarkButton(
                on = card.bookmarked,
                onClick = onToggleBookmark,
                modifier = Modifier.align(Alignment.TopEnd).padding(10.dp),
            )
        },
    ) {
        Text(
            card.jp,
            fontSize = wordSize(card.jp),
            fontWeight = FontWeight.Bold,
            color = Ink,
            textAlign = TextAlign.Center,
            lineHeight = wordSize(card.jp) * 1.15f,
        )

        if (card.exJp.isNotEmpty()) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(FieldBg)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Text(card.exJp, fontSize = 15.sp, lineHeight = 25.sp, color = Ink, textAlign = TextAlign.Center)
            }
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Chip(
                text = "🔊 단어 듣기",
                active = speaker.speaking == SpeechTarget.WORD,
                onClick = {
                    if (!speaker.japaneseAvailable) context.toastNoVoice()
                    else speaker.speak(firstReading(card.kana).ifEmpty { card.jp }, SpeechTarget.WORD, 0.9f)
                },
            )

            if (card.hasExample) {
                Chip(
                    text = "🔊 예문 듣기",
                    active = speaker.speaking == SpeechTarget.SENTENCE,
                    onClick = {
                        if (!speaker.japaneseAvailable) context.toastNoVoice()
                        else speaker.speak(card.exKana.ifEmpty { card.exJp }, SpeechTarget.SENTENCE, 0.85f)
                    },
                )
            }

            Chip(text = "📖 네이버 사전", active = false, onClick = { context.openNaverDict(card.jp) })
        }
    }
}

@Composable
private fun BackFace(card: Card) {
    Face(hint = "카드를 눌러 되돌리기") {
        Text(
            card.kana.ifEmpty { "—" },
            fontSize = 34.sp,
            lineHeight = 42.sp,
            fontWeight = FontWeight.SemiBold,
            color = Ink,
            textAlign = TextAlign.Center,
        )
        Text(
            card.ko.ifEmpty { "—" },
            fontSize = 20.sp,
            lineHeight = 28.sp,
            color = Muted,
            textAlign = TextAlign.Center,
        )

        if (card.exKana.isNotEmpty() || card.exKo.isNotEmpty()) {
            Box(Modifier.width(44.dp).height(1.dp).background(Line))
        }
        if (card.exKana.isNotEmpty()) {
            Text(card.exKana, fontSize = 15.sp, lineHeight = 25.sp, color = Ink, textAlign = TextAlign.Center)
        }
        if (card.exKo.isNotEmpty()) {
            Text(card.exKo, fontSize = 14.sp, lineHeight = 23.sp, color = Muted, textAlign = TextAlign.Center)
        }
    }
}

/** 카드 한 면의 공통 배치: 가운데 정렬 + 내용이 길면 스크롤, 아래쪽에 안내 문구. */
@Composable
private fun Face(
    hint: String,
    overlay: @Composable (BoxScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, top = 26.dp, bottom = 44.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            content = content,
        )
        Text(
            hint,
            fontSize = 11.5.sp,
            color = Muted,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp),
        )
        overlay?.invoke(this)
    }
}

@Composable
private fun BookmarkButton(on: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(42.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (on) "★" else "☆",
            fontSize = 23.sp,
            color = if (on) Gold else StarOff,
        )
    }
}

@Composable
private fun NavButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = Sheet,
        border = BorderStroke(1.dp, Line),
        modifier = modifier,
    ) {
        Text(
            text,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Ink,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
        )
    }
}

/** 글자 수에 따라 단어 크기를 줄인다. (웹의 clamp() 자리) */
private fun wordSize(word: String) = when {
    word.length <= 4 -> 56.sp
    word.length <= 8 -> 40.sp
    word.length <= 14 -> 30.sp
    else -> 24.sp
}

private fun Context.openNaverDict(word: String) {
    val url = "https://ja.dict.naver.com/#/search?query=" + Uri.encode(word)
    try {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(this, "사전을 열 수 있는 앱이 없습니다.", Toast.LENGTH_SHORT).show()
    }
}

private fun Context.toastNoVoice() {
    Toast.makeText(this, "이 기기에서는 일본어 음성을 사용할 수 없습니다.", Toast.LENGTH_SHORT).show()
}
