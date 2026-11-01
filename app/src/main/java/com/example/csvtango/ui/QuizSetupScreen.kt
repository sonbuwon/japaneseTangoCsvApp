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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.csvtango.data.Topic
import com.example.csvtango.ui.theme.Accent
import com.example.csvtango.ui.theme.AccentSoft
import com.example.csvtango.ui.theme.Gold
import com.example.csvtango.ui.theme.GoldBg
import com.example.csvtango.ui.theme.GoldLine
import com.example.csvtango.ui.theme.Ink
import com.example.csvtango.ui.theme.Line
import com.example.csvtango.ui.theme.Muted
import com.example.csvtango.ui.theme.Paper
import com.example.csvtango.ui.theme.Sheet
import com.example.csvtango.ui.theme.StarOff

/**
 * 시험 볼 목록(주제 또는 북마크) 하나와 옵션을 고르는 화면.
 *
 * 북마크를 고르면 그 아래에 북마크가 있는 주제들이 펼쳐지고, 여기서는 여러 주제를 함께 고를 수 있다.
 * [selectedBookmarkTopics] 가 비어 있으면 모든 주제의 북마크를 뜻한다.
 */
@Composable
fun QuizSetupScreen(
    topics: List<Topic>,
    bookmarkCount: Int,
    bookmarkedTopics: List<Pair<Topic, Int>>,
    selectedSource: String?,
    selectedBookmarkTopics: Set<String>,
    shuffle: Boolean,
    wordCount: Int,
    onSelect: (String) -> Unit,
    onToggleBookmarkTopic: (String) -> Unit,
    onAllBookmarkTopics: () -> Unit,
    onToggleShuffle: () -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    Column(Modifier.fillMaxSize().background(Paper)) {
        TopBar {
            GhostButton("‹ 시험", onBack)
            Text("주제 시험", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Ink)
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Text(
                    "시험 볼 목록 (하나만 고릅니다)",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Muted,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }

            val bookmarkSelected = selectedSource == BOOKMARK_SOURCE && bookmarkCount > 0
            val allBookmarkTopics = selectedBookmarkTopics.isEmpty()
            val selectedBookmarkCount =
                if (allBookmarkTopics) bookmarkCount
                else bookmarkedTopics.filter { it.first.id in selectedBookmarkTopics }.sumOf { it.second }

            item {
                val enabled = bookmarkCount > 0
                SelectRow(
                    checked = bookmarkSelected,
                    enabled = enabled,
                    onClick = { onSelect(BOOKMARK_SOURCE) },
                    background = GoldBg,
                    border = GoldLine,
                    badge = { Badge("★", GoldBg, Gold) },
                    title = "북마크",
                    subtitle = when {
                        !enabled -> "북마크한 단어가 없습니다"
                        allBookmarkTopics -> "모든 주제 · 단어 ${bookmarkCount}개"
                        else -> "주제 ${selectedBookmarkTopics.size}개 · 단어 ${selectedBookmarkCount}개"
                    },
                )
            }

            if (bookmarkSelected) {
                item {
                    Text(
                        "북마크 주제 (여러 개 고를 수 있습니다)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Muted,
                        modifier = Modifier.padding(start = 24.dp, top = 4.dp),
                    )
                }

                item {
                    Box(Modifier.padding(start = 24.dp)) {
                        SelectRow(
                            checked = allBookmarkTopics,
                            enabled = true,
                            onClick = onAllBookmarkTopics,
                            background = GoldBg,
                            border = GoldLine,
                            badge = { Badge("★", GoldBg, Gold) },
                            title = "모든 주제",
                            subtitle = "단어 ${bookmarkCount}개",
                        )
                    }
                }

                items(bookmarkedTopics, key = { "bookmark:" + it.first.id }) { (topic, count) ->
                    Box(Modifier.padding(start = 24.dp)) {
                        SelectRow(
                            checked = topic.id in selectedBookmarkTopics,
                            enabled = true,
                            onClick = { onToggleBookmarkTopic(topic.id) },
                            background = GoldBg,
                            border = GoldLine,
                            badge = { Badge(topic.title.trim().take(1).uppercase().ifEmpty { "語" }, GoldBg, Gold) },
                            title = topic.title,
                            subtitle = "북마크 ${count}개 / 단어 ${topic.cards.size}개",
                        )
                    }
                }
            }

            items(topics, key = { it.id }) { topic ->
                SelectRow(
                    checked = topic.id == selectedSource,
                    enabled = true,
                    onClick = { onSelect(topic.id) },
                    badge = { Badge(topic.title.trim().take(1).uppercase().ifEmpty { "語" }) },
                    title = topic.title,
                    subtitle = "단어 ${topic.cards.size}개",
                )
            }

            item {
                Text(
                    "옵션",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Muted,
                    modifier = Modifier.padding(top = 18.dp, bottom = 6.dp),
                )
            }

            item {
                Surface(
                    onClick = onToggleShuffle,
                    shape = RoundedCornerShape(16.dp),
                    color = Sheet,
                    border = BorderStroke(1.dp, Line),
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("순서 섞기", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                            Text(
                                "문제를 무작위 순서로 냅니다",
                                fontSize = 12.5.sp,
                                color = Muted,
                                modifier = Modifier.padding(top = 3.dp),
                            )
                        }
                        Switch(
                            checked = shuffle,
                            onCheckedChange = { onToggleShuffle() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Sheet,
                                checkedTrackColor = Accent,
                                uncheckedThumbColor = Sheet,
                                uncheckedTrackColor = StarOff,
                                uncheckedBorderColor = StarOff,
                            ),
                        )
                    }
                }
            }

            item {
                Text(
                    "한자가 있는 단어는 일본어를, 히라가나·가타카나뿐인 단어는 한국어 뜻을 보여 줍니다. " +
                        "틀린 단어는 북마크에 자동으로 저장되고, 맞힌 단어는 북마크에서 해제됩니다.",
                    fontSize = 12.5.sp,
                    lineHeight = 19.sp,
                    color = Muted,
                    modifier = Modifier.padding(top = 12.dp),
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
                StartButton(
                    text = if (wordCount > 0) "단어 목록 보기 · ${wordCount}개" else "시험 볼 목록을 골라 주세요",
                    enabled = wordCount > 0,
                    onClick = onNext,
                )
            }
        }
    }
}

@Composable
internal fun StartButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        color = if (enabled) Accent else Line,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (enabled) Sheet else Muted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 15.dp),
        )
    }
}

@Composable
internal fun SelectRow(
    checked: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    badge: @Composable () -> Unit,
    title: String,
    subtitle: String,
    background: Color = Sheet,
    border: Color = Line,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        color = background,
        border = BorderStroke(1.dp, if (checked) Accent.copy(alpha = .6f) else border),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .alpha(if (enabled) 1f else 0.6f)
                .padding(start = 18.dp, end = 16.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            badge()

            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(subtitle, fontSize = 12.5.sp, color = Muted, modifier = Modifier.padding(top = 3.dp))
            }

            CheckMark(checked)
        }
    }
}

/** 동그란 선택 표시. 고르면 강조색으로 채우고 체크를 그린다. */
@Composable
private fun CheckMark(checked: Boolean) {
    Surface(
        shape = CircleShape,
        color = if (checked) Accent else Sheet,
        border = BorderStroke(1.5.dp, if (checked) Accent else StarOff),
        modifier = Modifier.size(26.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (checked) Text("✓", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Sheet)
        }
    }
}

@Composable
internal fun Badge(text: String, background: Color = AccentSoft, tint: Color = Accent) {
    Box(
        Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(background),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = tint)
    }
}
