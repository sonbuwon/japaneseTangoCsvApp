package com.example.csvtango.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.csvtango.data.QuizQuestion
import com.example.csvtango.data.StudyCard
import com.example.csvtango.ui.theme.Accent
import com.example.csvtango.ui.theme.AccentLine
import com.example.csvtango.ui.theme.AccentSoft
import com.example.csvtango.ui.theme.Ink
import com.example.csvtango.ui.theme.Line
import com.example.csvtango.ui.theme.Muted
import com.example.csvtango.ui.theme.Paper
import com.example.csvtango.ui.theme.Sheet
import com.example.csvtango.ui.theme.StarOff
import kotlin.math.abs
import kotlin.math.roundToInt

/** 고른 목록의 단어를 보여 주고, 그중 몇 번부터 몇 번까지 낼지 정하는 화면. */
@Composable
fun QuizRangeScreen(
    title: String,
    words: List<StudyCard>,
    range: IntRange,
    questionCount: Int,
    onRangeChange: (IntRange) -> Unit,
    onBack: () -> Unit,
    onStart: () -> Unit,
) {
    val whole = range.first == 0 && range.last == words.lastIndex

    /** 누른 단어 쪽으로 가까운 끝을 옮긴다. 범위 밖이면 그쪽 끝을, 안이면 더 가까운 끝을. */
    fun pick(i: Int) {
        val moveStart = i < range.first || (i <= range.last && abs(i - range.first) <= abs(range.last - i))
        onRangeChange(if (moveStart) i..range.last else range.first..i)
    }

    Column(Modifier.fillMaxSize().background(Paper)) {
        TopBar {
            GhostButton("‹ 설정", onBack)
            Text(
                title,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }

        Surface(color = Sheet) {
            Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "시험 범위  ${range.first + 1}번 ~ ${range.last + 1}번",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Ink,
                        modifier = Modifier.weight(1f),
                    )
                    if (!whole) GhostButton("전체", onClick = { onRangeChange(words.indices) })
                }
                if (words.size > 1) RangeBar(words.size, range, onRangeChange)
                Text(
                    "막대를 끌거나 단어를 눌러 시작·끝을 정합니다",
                    fontSize = 12.5.sp,
                    color = Muted,
                )
            }
        }
        HorizontalDivider(color = Line)

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            itemsIndexed(words, key = { _, w -> w.topicId to w.index }) { i, word ->
                WordRow(
                    number = i + 1,
                    word = word,
                    inRange = i in range,
                    edge = i == range.first || i == range.last,
                    onClick = { pick(i) },
                )
            }
        }

        HorizontalDivider(color = Line)
        Surface(color = Sheet) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
                    .windowInsetsPadding(WindowInsets.navigationBars),
            ) {
                Surface(
                    onClick = onStart,
                    enabled = questionCount > 0,
                    shape = RoundedCornerShape(12.dp),
                    color = if (questionCount > 0) Accent else Line,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        if (questionCount > 0) "시험 시작 · ${questionCount}문제" else "이 범위에는 낼 문제가 없습니다",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (questionCount > 0) Sheet else Muted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 15.dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RangeBar(size: Int, range: IntRange, onRangeChange: (IntRange) -> Unit) {
    RangeSlider(
        value = range.first.toFloat()..range.last.toFloat(),
        onValueChange = { v -> onRangeChange(v.start.roundToInt()..v.endInclusive.roundToInt()) },
        valueRange = 0f..(size - 1).toFloat(),
        steps = if (size > 2) size - 2 else 0,
        colors = SliderDefaults.colors(
            thumbColor = Accent,
            activeTrackColor = Accent,
            inactiveTrackColor = Line,
            activeTickColor = Accent.copy(alpha = 0f),
            inactiveTickColor = StarOff.copy(alpha = 0f),
        ),
    )
}

@Composable
private fun WordRow(number: Int, word: StudyCard, inRange: Boolean, edge: Boolean, onClick: () -> Unit) {
    val answerable = QuizQuestion(word).answerable
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (inRange) Sheet else Paper,
        border = BorderStroke(if (edge) 1.5.dp else 1.dp, if (edge) Accent else if (inRange) AccentLine else Line),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .alpha(if (inRange) 1f else 0.45f)
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier
                    .width(36.dp)
                    .background(if (inRange) AccentSoft else Line, RoundedCornerShape(8.dp))
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("$number", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (inRange) Accent else Muted)
            }

            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        word.card.jp,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (word.card.kana.isNotBlank() && word.card.kana != word.card.jp) {
                        Text(word.card.kana, fontSize = 13.sp, color = Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Text(
                    if (answerable) word.card.ko else "${word.card.ko} · 히라가나가 없어 출제되지 않습니다",
                    fontSize = 13.sp,
                    color = Ink.copy(alpha = .75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}
