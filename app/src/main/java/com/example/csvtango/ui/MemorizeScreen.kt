package com.example.csvtango.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.animation.core.spring
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
import com.example.csvtango.data.Folder
import com.example.csvtango.data.TOPIC_COLORS
import com.example.csvtango.data.Topic
import com.example.csvtango.data.lastEmoji
import com.example.csvtango.data.formatDate
import com.example.csvtango.ui.theme.Accent
import com.example.csvtango.ui.theme.AccentSoft
import com.example.csvtango.ui.theme.ExcelGreen
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

/**
 * 암기: 폴더·주제 목록. 주제를 누르면 카드 학습으로, 폴더를 누르면 그 폴더의 주제 목록으로 들어간다.
 * [openFolder] 가 null 이면 맨 위(북마크·폴더·폴더 밖 주제)를 보여 준다.
 */
@Composable
fun MemorizeScreen(
    folders: List<Folder>,
    openFolder: Folder?,
    topicsIn: (String?) -> List<Topic>,
    hasTopics: Boolean,
    bookmarkCount: Int,
    recentTopic: Topic?,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onOpenTopic: (Topic) -> Unit,
    onOpenFolder: (Folder) -> Unit,
    onOpenBookmarks: () -> Unit,
    onClearBookmarks: () -> Unit,
    editing: Boolean,
    onToggleEdit: () -> Unit,
    onMove: (Topic, Int) -> Unit,
    onEditTopic: (Topic, String, String?, Long?) -> Unit,
    onDelete: (Topic) -> Unit,
    onMoveToFolder: (Topic, String?) -> Unit,
    onCreateFolder: (String) -> Unit,
    onAddCsv: () -> Unit,
    onMoveFolder: (Folder, Int) -> Unit,
    onRenameFolder: (Folder, String) -> Unit,
    onDeleteFolder: (Folder) -> Unit,
) {
    var pendingDelete by remember { mutableStateOf<Topic?>(null) }
    var pendingEdit by remember { mutableStateOf<Topic?>(null) }
    var pendingMove by remember { mutableStateOf<Topic?>(null) }
    var pendingFolderDelete by remember { mutableStateOf<Folder?>(null) }
    var pendingFolderRename by remember { mutableStateOf<Folder?>(null) }
    var creatingFolder by remember { mutableStateOf(false) }
    var confirmClearBookmarks by remember { mutableStateOf(false) }

    val atRoot = openFolder == null
    val topics = topicsIn(openFolder?.id)
    val shownFolders = if (atRoot) folders else emptyList()

    // 편집 중에 폴더·주제를 꾹 눌러 끌면 순서를 바꾼다. 폴더는 폴더끼리, 주제는 주제끼리.
    val listState = rememberLazyListState()
    val reorder = rememberReorderState(listState)
    val folderKeys = shownFolders.map { "folder:" + it.id }
    val topicKeys = topics.map { it.id }

    Column(Modifier.fillMaxSize().background(Paper)) {
        TopBar {
            if (openFolder == null) {
                BackButton(onBack)
                Row(
                    Modifier.weight(1f),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("단어 암기", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                    Text("単語カード", fontSize = 12.sp, color = Muted)
                }
            } else {
                BackButton(onBack)
                Text(
                    "📁 ${openFolder.name}",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                HomeButton(onHome)
            }
        }

        LazyColumn(
            state = listState,
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
                    // 제목은 편집 중일 때만 보인다. 없을 때도 자리를 두어 오른쪽 버튼이 오른쪽에 붙어 있게 한다.
                    if (editing) {
                        Text("편집 중", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Accent)
                    } else {
                        Box(Modifier)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (atRoot && !editing) {
                            GhostButton("+ 📁", { creatingFolder = true })
                            PrimaryButton("+ csv", onAddCsv, color = ExcelGreen)
                        }
                        if (topics.isNotEmpty() || shownFolders.isNotEmpty()) {
                            GhostButton(if (editing) "완료" else "편집", onToggleEdit)
                        }
                    }
                }
            }

            if (atRoot && !hasTopics && folders.isEmpty()) {
                item { EmptyHome() }
            } else {
                if (atRoot && hasTopics && !editing && recentTopic != null) {
                    item(key = "recent") {
                        RecentTopicRow(
                            topic = recentTopic,
                            folderName = folders.find { it.id == recentTopic.folderId }?.name,
                            onClick = { onOpenTopic(recentTopic) },
                        )
                    }
                    item(key = "recent-divider") {
                        HorizontalDivider(color = Line, modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
                if (atRoot && hasTopics && !editing) {
                    item {
                        BookmarkRow(
                            count = bookmarkCount,
                            onClick = onOpenBookmarks,
                            onClearAll = { confirmClearBookmarks = true },
                        )
                    }
                }
                itemsIndexed(shownFolders, key = { _, folder -> "folder:" + folder.id }) { index, folder ->
                    val key = "folder:" + folder.id
                    FolderItem(
                        folder = folder,
                        topics = topicsIn(folder.id),
                        editing = editing,
                        canMoveUp = index > 0,
                        canMoveDown = index < shownFolders.lastIndex,
                        onClick = { onOpenFolder(folder) },
                        onMove = { delta -> onMoveFolder(folder, delta) },
                        onRename = { pendingFolderRename = folder },
                        onDelete = { pendingFolderDelete = folder },
                        modifier = Modifier
                            .animateItem(placementSpec = if (reorder.isDragging(key)) null else spring())
                            .reorderable(reorder, key, enabled = editing, groupKeys = folderKeys) { delta ->
                                onMoveFolder(folder, delta)
                            },
                    )
                }
                itemsIndexed(topics, key = { _, topic -> topic.id }) { index, topic ->
                    TopicItem(
                        topic = topic,
                        editing = editing,
                        canMoveUp = index > 0,
                        canMoveDown = index < topics.lastIndex,
                        canChangeFolder = folders.isNotEmpty(),
                        onClick = { onOpenTopic(topic) },
                        onMove = { delta -> onMove(topic, delta) },
                        onEdit = { pendingEdit = topic },
                        onChangeFolder = { pendingMove = topic },
                        onDelete = { pendingDelete = topic },
                        modifier = Modifier
                            .animateItem(placementSpec = if (reorder.isDragging(topic.id)) null else spring())
                            .reorderable(reorder, topic.id, enabled = editing, groupKeys = topicKeys) { delta ->
                                onMove(topic, delta)
                            },
                    )
                }
                if (!atRoot && topics.isEmpty()) {
                    item { EmptyFolder() }
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

    pendingEdit?.let { topic ->
        TopicEditDialog(
            topic = topic,
            onDismiss = { pendingEdit = null },
            onConfirm = { title, emoji, color -> onEditTopic(topic, title, emoji, color); pendingEdit = null },
        )
    }

    pendingMove?.let { topic ->
        MoveToFolderDialog(
            topic = topic,
            folders = folders,
            onDismiss = { pendingMove = null },
            onSelect = { folderId -> onMoveToFolder(topic, folderId); pendingMove = null },
        )
    }

    if (creatingFolder) {
        NameDialog(
            title = "새 폴더",
            initial = "",
            confirmLabel = "만들기",
            onDismiss = { creatingFolder = false },
            onConfirm = { name -> onCreateFolder(name); creatingFolder = false },
        )
    }

    pendingFolderRename?.let { folder ->
        NameDialog(
            title = "폴더 이름 변경",
            initial = folder.name,
            onDismiss = { pendingFolderRename = null },
            onConfirm = { name -> onRenameFolder(folder, name); pendingFolderRename = null },
        )
    }

    pendingFolderDelete?.let { folder ->
        val count = topicsIn(folder.id).size
        AlertDialog(
            onDismissRequest = { pendingFolderDelete = null },
            title = { Text("폴더 삭제") },
            text = {
                Text(
                    if (count > 0) "\"${folder.name}\" 폴더를 삭제할까요? 안에 있던 주제 ${count}개는 지워지지 않고 폴더 밖으로 옮겨집니다."
                    else "\"${folder.name}\" 폴더를 삭제할까요?",
                )
            },
            confirmButton = {
                TextButton(onClick = { onDeleteFolder(folder); pendingFolderDelete = null }) {
                    Text("삭제", color = Accent)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingFolderDelete = null }) { Text("취소", color = Muted) }
            },
            containerColor = Sheet,
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
private fun FolderItem(
    folder: Folder,
    topics: List<Topic>,
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
        enabled = !editing,
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
                Badge(text = "📁", background = GoldSoft, tint = Gold)
            }

            Column(Modifier.weight(1f)) {
                Text(
                    folder.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "주제 ${topics.size}개 · 단어 ${topics.sumOf { it.cards.size }}개",
                    fontSize = 12.5.sp,
                    color = Muted,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }

            if (editing) {
                IconAction("✎", "폴더 이름 변경", onClick = onRename)
                IconAction("✕", "폴더 삭제", onClick = onDelete)
            } else {
                Text("›", fontSize = 22.sp, color = Muted, modifier = Modifier.padding(end = 8.dp))
            }
        }
    }
}

@Composable
private fun TopicItem(
    topic: Topic,
    editing: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    canChangeFolder: Boolean,
    onClick: () -> Unit,
    onMove: (Int) -> Unit,
    onEdit: () -> Unit,
    onChangeFolder: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        enabled = !editing,   // 편집 중에는 눌러도 학습으로 넘어가지 않는다
        shape = RoundedCornerShape(16.dp),
        color = topic.color?.let { Color(it) } ?: Sheet,
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
                Badge(text = topic.badgeText)
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
                IconAction("✎", "주제 편집", onClick = onEdit)
                if (canChangeFolder) IconAction("📁", "폴더 이동", onClick = onChangeFolder)
                IconAction("✕", "삭제", onClick = onDelete)
            }
        }
    }
}

/** 이름 하나를 입력받는 대화상자. (폴더 만들기·이름 변경) */
@Composable
private fun NameDialog(
    title: String,
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    confirmLabel: String = "저장",
) {
    var name by remember { mutableStateOf(initial) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (name.isNotBlank()) onConfirm(name) }),
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
            TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) {
                Text(confirmLabel, color = if (name.isNotBlank()) Accent else Muted)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소", color = Muted) }
        },
        containerColor = Sheet,
    )
}

/** 주제 편집에서 바로 고를 수 있는 이모티콘. 키보드로 다른 이모티콘을 넣을 수도 있다. */
private val QUICK_EMOJIS = listOf(
    "📚", "✏️", "🗾", "🌸", "🗻", "⛩️",
    "🍣", "🍜", "🚃", "🏠", "💼", "🏥",
    "🛒", "🎵", "⚽", "🌏", "⭐", "❤️",
)

/**
 * 주제 편집: 제목, 표시 칸 이모티콘(하나만), 배경색을 바꾼다.
 * 이모티콘을 비우면 표시 칸은 예전처럼 제목 앞글자를, 배경색을 [기본]으로 두면 기본 배경을 쓴다.
 */
@Composable
private fun TopicEditDialog(
    topic: Topic,
    onDismiss: () -> Unit,
    onConfirm: (title: String, emoji: String?, color: Long?) -> Unit,
) {
    var title by remember { mutableStateOf(topic.title) }
    var emoji by remember { mutableStateOf(topic.emoji) }
    var color by remember { mutableStateOf(topic.color) }
    val focusRequester = remember { FocusRequester() }
    val preview = topic.copy(title = title, emoji = emoji, color = color)
    val canSave = title.isNotBlank()

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = Sheet,
        unfocusedContainerColor = FieldBg,
        focusedBorderColor = Accent,
        unfocusedBorderColor = Line,
        cursorColor = Accent,
        focusedTextColor = Ink,
        unfocusedTextColor = Ink,
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("주제 편집") },
        text = {
            Column(
                Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // 바뀐 모습을 미리 보여 준다.
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = preview.color?.let { Color(it) } ?: Sheet,
                    border = BorderStroke(1.dp, Line),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Badge(text = preview.badgeText)
                        Text(
                            title.ifBlank { "제목" },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (title.isBlank()) Muted else Ink,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                SectionLabel("제목")
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { if (canSave) onConfirm(title, emoji, color) }),
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                )

                SectionLabel("이모티콘")
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // 새 이모티콘을 넣으면 그것 하나만 남는다. 이모티콘이 아닌 글자는 받지 않는다.
                    OutlinedTextField(
                        value = emoji.orEmpty(),
                        onValueChange = { new -> emoji = if (new.isEmpty()) null else lastEmoji(new) ?: emoji },
                        singleLine = true,
                        placeholder = { Text("키보드로 입력", fontSize = 13.sp, color = Muted) },
                        shape = RoundedCornerShape(10.dp),
                        colors = fieldColors,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { emoji = null }, enabled = emoji != null) {
                        Text("앞글자", color = if (emoji != null) Accent else Muted)
                    }
                }
                QUICK_EMOJIS.chunked(6).forEach { row ->
                    PickerRow(row.size, 6) { i ->
                        val e = row[i]
                        val selected = e == emoji
                        Box(
                            Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selected) AccentSoft else FieldBg)
                                .border(1.dp, if (selected) Accent else Line, RoundedCornerShape(10.dp))
                                .clickable(onClickLabel = "이모티콘 $e") { emoji = if (selected) null else e },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(e, fontSize = 18.sp)
                        }
                    }
                }

                SectionLabel("배경색")
                val palette: List<Long?> = listOf(null) + TOPIC_COLORS
                palette.chunked(5).forEach { row ->
                    PickerRow(row.size, 5) { i ->
                        val c = row[i]
                        val selected = c == color
                        Box(
                            Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(c?.let { Color(it) } ?: Sheet)
                                .border(if (selected) 2.dp else 1.dp, if (selected) Accent else Line, CircleShape)
                                .clickable(onClickLabel = if (c == null) "기본 배경" else "배경색") { color = c },
                            contentAlignment = Alignment.Center,
                        ) {
                            when {
                                selected -> Text("✓", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Ink)
                                c == null -> Text("기본", fontSize = 10.sp, color = Muted)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(title, emoji, color) }, enabled = canSave) {
                Text("저장", color = if (canSave) Accent else Muted)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소", color = Muted) }
        },
        containerColor = Sheet,
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        fontSize = 12.5.sp,
        fontWeight = FontWeight.SemiBold,
        color = Muted,
        modifier = Modifier.padding(top = 6.dp),
    )
}

/** 정사각형 칸 [columns] 개를 한 줄에 고르게 놓는다. 칸이 [count] 개보다 모자라면 빈자리로 둔다. */
@Composable
private fun PickerRow(count: Int, columns: Int, cell: @Composable (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(columns) { i ->
            Box(Modifier.weight(1f).aspectRatio(1f)) {
                if (i < count) cell(i)
            }
        }
    }
}

/** 주제를 옮길 폴더를 고른다. 맨 위 항목은 폴더 밖. 지금 들어 있는 곳은 표시만 하고 누를 수 없다. */
@Composable
private fun MoveToFolderDialog(
    topic: Topic,
    folders: List<Folder>,
    onDismiss: () -> Unit,
    onSelect: (String?) -> Unit,
) {
    val current = topic.folderId?.takeIf { id -> folders.any { it.id == id } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("폴더 이동") },
        text = {
            Column(
                Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    "\"${topic.title}\" 주제를 옮길 곳",
                    fontSize = 13.sp,
                    color = Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                FolderOption("🗂️", "폴더 밖", selected = current == null) { onSelect(null) }
                folders.forEach { folder ->
                    FolderOption("📁", folder.name, selected = current == folder.id) { onSelect(folder.id) }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소", color = Muted) }
        },
        containerColor = Sheet,
    )
}

@Composable
private fun FolderOption(glyph: String, label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = !selected,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) AccentSoft else FieldBg,
        border = BorderStroke(1.dp, if (selected) Accent else Line),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(glyph, fontSize = 16.sp)
            Text(
                label,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (selected) Text("현재", fontSize = 12.5.sp, color = Accent)
        }
    }
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

/** 목록 맨 위, 북마크 위에 놓이는 최근 본 주제. 누르면 바로 카드 학습으로 간다. */
@Composable
private fun RecentTopicRow(topic: Topic, folderName: String?, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = AccentSoft,
        border = BorderStroke(1.dp, Accent.copy(alpha = .35f)),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Badge(text = topic.badgeText)

            Column(Modifier.weight(1f)) {
                Text("최근 본 주제", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Accent)
                Text(
                    topic.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
                Text(
                    listOfNotNull(folderName?.let { "📁 $it" }, "단어 ${topic.cards.size}개").joinToString(" · "),
                    fontSize = 12.5.sp,
                    color = Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }

            Text("›", fontSize = 22.sp, color = Muted, modifier = Modifier.padding(end = 8.dp))
        }
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
private fun EmptyFolder() {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 56.dp, horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("📂", fontSize = 40.sp)
        Text(
            "이 폴더는 비어 있습니다.",
            fontSize = 15.sp,
            color = Ink,
            modifier = Modifier.padding(top = 12.dp),
        )
        Text(
            "암기 화면의 [편집]에서 주제의 📁 버튼으로 옮겨 올 수 있습니다.",
            fontSize = 14.sp,
            color = Muted,
            modifier = Modifier.padding(top = 4.dp),
        )
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
            "위의 [+ csv]로 첫 단어장을 만들어 보세요.",
            fontSize = 14.sp,
            color = Muted,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
