package com.example.csvtango.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.csvtango.data.Topic
import com.example.csvtango.data.formatDate
import com.example.csvtango.ui.theme.Accent
import com.example.csvtango.ui.theme.AccentSoft
import com.example.csvtango.ui.theme.FieldBg
import com.example.csvtango.ui.theme.Gold
import com.example.csvtango.ui.theme.GoldBg
import com.example.csvtango.ui.theme.GoldLine
import com.example.csvtango.ui.theme.GoldSoft
import com.example.csvtango.ui.theme.Ink
import com.example.csvtango.ui.theme.Line
import com.example.csvtango.ui.theme.Muted
import com.example.csvtango.ui.theme.Paper
import com.example.csvtango.ui.theme.Sheet

/** 암기: 주제 목록. 주제를 누르면 카드 학습으로 들어간다. */
@Composable
fun MemorizeScreen(
    topics: List<Topic>,
    bookmarkCount: Int,
    onBack: () -> Unit,
    onOpenTopic: (Topic) -> Unit,
    onOpenBookmarks: () -> Unit,
    onClearBookmarks: () -> Unit,
    editing: Boolean,
    onToggleEdit: () -> Unit,
    onMove: (Topic, Int) -> Unit,
    onRename: (Topic, String) -> Unit,
    onDelete: (Topic) -> Unit,
) {
    var pendingDelete by remember { mutableStateOf<Topic?>(null) }
    var pendingRename by remember { mutableStateOf<Topic?>(null) }
    var confirmClearBookmarks by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(Paper)) {
        TopBar {
            GhostButton("‹ 홈", onBack)
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("암기", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                Text("単語カード", fontSize = 12.sp, color = Muted)
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        if (editing) "편집 중" else "주제",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (editing) Accent else Muted,
                    )
                    if (topics.isNotEmpty()) {
                        GhostButton(if (editing) "완료" else "편집", onToggleEdit)
                    }
                }
            }

            if (topics.isEmpty()) {
                item { EmptyHome() }
            } else {
                if (!editing) {
                    item {
                        BookmarkRow(
                            count = bookmarkCount,
                            onClick = onOpenBookmarks,
                            onClearAll = { confirmClearBookmarks = true },
                        )
                    }
                }
                itemsIndexed(topics, key = { _, topic -> topic.id }) { index, topic ->
                    TopicItem(
                        topic = topic,
                        editing = editing,
                        canMoveUp = index > 0,
                        canMoveDown = index < topics.lastIndex,
                        onClick = { onOpenTopic(topic) },
                        onMove = { delta -> onMove(topic, delta) },
                        onRename = { pendingRename = topic },
                        onDelete = { pendingDelete = topic },
                        modifier = Modifier.animateItem(),
                    )
                }
            }

            item { Box(Modifier.windowInsetsPadding(WindowInsets.navigationBars)) }
        }
    }

    if (confirmClearBookmarks) {
        AlertDialog(
            onDismissRequest = { confirmClearBookmarks = false },
            title = { Text("북마크 전체 삭제") },
            text = { Text("북마크한 단어 ${bookmarkCount}개를 모두 해제할까요? 단어장은 그대로 남습니다.") },
            confirmButton = {
                TextButton(onClick = { onClearBookmarks(); confirmClearBookmarks = false }) {
                    Text("전체 삭제", color = Accent)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClearBookmarks = false }) { Text("취소", color = Muted) }
            },
            containerColor = Sheet,
        )
    }

    pendingRename?.let { topic ->
        RenameDialog(
            topic = topic,
            onDismiss = { pendingRename = null },
            onConfirm = { title -> onRename(topic, title); pendingRename = null },
        )
    }

    pendingDelete?.let { topic ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("주제 삭제") },
            text = { Text("\"${topic.title}\" 주제를 삭제할까요?") },
            confirmButton = {
                TextButton(onClick = { onDelete(topic); pendingDelete = null }) {
                    Text("삭제", color = Accent)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("취소", color = Muted) }
            },
            containerColor = Sheet,
        )
    }
}

@Composable
private fun TopicItem(
    topic: Topic,
    editing: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onClick: () -> Unit,
    onMove: (Int) -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        enabled = !editing,   // 편집 중에는 눌러도 학습으로 넘어가지 않는다
        shape = RoundedCornerShape(16.dp),
        color = Sheet,
        border = BorderStroke(1.dp, if (editing) Accent.copy(alpha = .35f) else Line),
        modifier = modifier,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 18.dp, end = 10.dp, top = 16.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (editing) 4.dp else 14.dp),
        ) {
            if (editing) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconAction("▲", "위로", enabled = canMoveUp) { onMove(-1) }
                    IconAction("▼", "아래로", enabled = canMoveDown) { onMove(1) }
                }
            } else {
                Badge(text = topic.title.trim().take(1).uppercase().ifEmpty { "語" })
            }

            Column(Modifier.weight(1f)) {
                Text(
                    topic.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "단어 ${topic.cards.size}개 · ${formatDate(topic.createdAt)}",
                    fontSize = 12.5.sp,
                    color = Muted,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }

            if (editing) {
                IconAction("✎", "제목 변경", onClick = onRename)
                IconAction("✕", "삭제", onClick = onDelete)
            }
        }
    }
}

@Composable
private fun RenameDialog(topic: Topic, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var title by remember(topic.id) { mutableStateOf(topic.title) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(topic.id) { focusRequester.requestFocus() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("제목 변경") },
        text = {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (title.isNotBlank()) onConfirm(title) }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Sheet,
                    unfocusedContainerColor = FieldBg,
                    focusedBorderColor = Accent,
                    unfocusedBorderColor = Line,
                    cursorColor = Accent,
                    focusedTextColor = Ink,
                    unfocusedTextColor = Ink,
                ),
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(title) }, enabled = title.isNotBlank()) {
                Text("저장", color = if (title.isNotBlank()) Accent else Muted)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소", color = Muted) }
        },
        containerColor = Sheet,
    )
}

@Composable
private fun IconAction(
    glyph: String,
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .clip(CircleShape)
            .clickable(enabled = enabled, onClickLabel = label, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(glyph, fontSize = 16.sp, color = if (enabled) Muted else Line)
    }
}

/** 목록 맨 위에 놓이는 북마크 모아보기 항목 */
@Composable
private fun BookmarkRow(count: Int, onClick: () -> Unit, onClearAll: () -> Unit) {
    val enabled = count > 0

    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        color = GoldBg,
        border = BorderStroke(1.dp, GoldLine),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .alpha(if (enabled) 1f else 0.6f)
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Badge(text = "★", background = GoldSoft, tint = Gold)

            Column(Modifier.weight(1f)) {
                Text("북마크", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                Text(
                    if (enabled) "단어 ${count}개" else "북마크한 단어가 없습니다",
                    fontSize = 12.5.sp,
                    color = Muted,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }

            if (enabled) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClickLabel = "북마크 전체 삭제", onClick = onClearAll)
                        .border(1.dp, GoldLine, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                ) {
                    Text("전체 삭제", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = Muted)
                }
            }
        }
    }
}

@Composable
private fun EmptyHome() {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 56.dp, horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("🗂️", fontSize = 40.sp)
        Text(
            "아직 등록된 주제가 없습니다.",
            fontSize = 15.sp,
            color = Ink,
            modifier = Modifier.padding(top = 12.dp),
        )
        Text(
            "홈 화면의 [+ CSV 추가]로 첫 단어장을 만들어 보세요.",
            fontSize = 14.sp,
            color = Muted,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
