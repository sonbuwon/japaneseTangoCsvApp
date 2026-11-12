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
import com.example.csvtango.data.Folder
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
 * 시험 볼 목록(주제·북마크)을 하나 이상 고르고 옵션을 정하는 화면. 처음엔 아무것도 고르지 않은 상태.
 *
 * 북마크를 고르면 그 아래에 북마크가 있는 주제들이 펼쳐지고, 여기서는 여러 주제를 함께 고를 수 있다.
 * [selectedBookmarkTopics] 가 비어 있으면 모든 주제의 북마크를 뜻한다.
 * 폴더별 묶음은 머리줄을 눌러 접고 펼 수 있고, [expandedFolders] 에 없는 폴더는 접혀 있다.
 */
@Composable
fun QuizSetupScreen(
    topicGroups: List<Pair<Folder?, List<Topic>>>,
    bookmarkCount: Int,
    bookmarkedTopics: List<Pair<Topic, Int>>,
    selectedSources: Set<String>,
    selectedBookmarkTopics: Set<String>,
    expandedFolders: Set<String>,
    shuffle: Boolean,
    wordCount: Int,
    onToggle: (String) -> Unit,
    onToggleBookmarkTopic: (String) -> Unit,
    onAllBookmarkTopics: () -> Unit,
    onToggleFolder: (String?) -> Unit,
    onToggleShuffle: () -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    Column(Modifier.fillMaxSize().background(Paper)) {
        TopBar {
            BackButton(onBack)
            Text("주제 시험", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Ink)
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
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
                        Text(
                            "무작위 여부",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Ink,
                            modifier = Modifier.weight(1f),
                        )
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

            val bookmarkSelected = BOOKMARK_SOURCE in selectedSources && bookmarkCount > 0
            val allBookmarkTopics = selectedBookmarkTopics.isEmpty()
            val selectedBookmarkCount =
                if (allBookmarkTopics) bookmarkCount
                else bookmarkedTopics.filter { it.first.id in selectedBookmarkTopics }.sumOf { it.second }

            item {
                val enabled = bookmarkCount > 0
                SelectRow(
                    checked = bookmarkSelected,
                    enabled = enabled,
                    onClick = { onToggle(BOOKMARK_SOURCE) },
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

            val showFolders = topicGroups.any { it.first != null }
            topicGroups.forEach { (folder, topics) ->
                val expanded = !showFolders || (folder?.id ?: NO_FOLDER) in expandedFolders
                if (showFolders) {
                    item(key = "folder:" + (folder?.id ?: "none")) {
                        FolderHeader(
                            name = folder?.name,
                            expanded = expanded,
                            topicCount = topics.size,
                            selectedCount = topics.count { it.id in selectedSources },
                            onClick = { onToggleFolder(folder?.id) },
                        )
                    }
                }
                if (expanded) items(topics, key = { it.id }) { topic ->
                    SelectRow(
                        checked = topic.id in selectedSources,
                        enabled = true,
                        onClick = { onToggle(topic.id) },
                        badge = { Badge(topic.title.trim().take(1).uppercase().ifEmpty { "語" }) },
                        title = topic.title,
                        subtitle = "단어 ${topic.cards.size}개",
                    )
                }
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
                    text = when {
                        wordCount <= 0 -> "시험 볼 목록을 골라 주세요"
                        selectedSources.size > 1 -> "범위 정하기 · 목록 ${selectedSources.size}개 · ${wordCount}개"
                        else -> "단어 목록 보기 · ${wordCount}개"
                    },
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

/**
 * 주제 시험 화면에서 폴더를 구분하는 줄. 누르면 그 폴더의 주제를 접거나 편다.
 * 접혀 있을 때도 고른 주제 수를 보여 줘서 접힌 폴더 안의 선택을 알 수 있게 한다.
 */
@Composable
private fun FolderHeader(
    name: String?,
    expanded: Boolean,
    topicCount: Int,
    selectedCount: Int,
    onClick: () -> Unit,
) {
    Surface(onClick = onClick, color = Color.Transparent, modifier = Modifier.padding(top = 8.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                (if (expanded) "▾ " else "▸ ") + (if (name != null) "📁 $name" else "폴더 밖"),
                fontSize = 13.sp,
                color = Muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            Text(
                if (selectedCount > 0) "${selectedCount}/${topicCount} 선택" else "주제 ${topicCount}개",
                fontSize = 12.sp,
                fontWeight = if (selectedCount > 0) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selectedCount > 0) Accent else StarOff,
            )
            HorizontalDivider(color = Line, modifier = Modifier.weight(1f))
        }
    }
}

/** 주제 목록에서 폴더를 구분하는 연한 이름과 줄. [name] 이 null 이면 폴더 밖 주제. */
@Composable
internal fun FolderDivider(name: String?) {
    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            if (name != null) "📁 $name" else "폴더 밖",
            fontSize = 12.5.sp,
            color = StarOff,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        HorizontalDivider(color = Line, modifier = Modifier.weight(1f))
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
