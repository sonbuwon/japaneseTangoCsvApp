package com.example.csvtango.ui

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.csvtango.data.Card
import com.example.csvtango.data.CsvParser
import com.example.csvtango.data.Folder
import com.example.csvtango.ui.theme.Accent
import com.example.csvtango.ui.theme.ErrBg
import com.example.csvtango.ui.theme.ErrInk
import com.example.csvtango.ui.theme.FieldBg
import com.example.csvtango.ui.theme.Ink
import com.example.csvtango.ui.theme.Line
import com.example.csvtango.ui.theme.Muted
import com.example.csvtango.ui.theme.OkBg
import com.example.csvtango.ui.theme.OkInk
import com.example.csvtango.ui.theme.Paper
import com.example.csvtango.ui.theme.Sheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** CSV 파일에 붙는 MIME 타입이 제공자마다 달라 넉넉히 받는다. */
private val CSV_MIME_TYPES = arrayOf(
    "text/csv",
    "text/comma-separated-values",
    "text/plain",
    "application/csv",
    "application/vnd.ms-excel",
    "application/octet-stream",
)

private data class Status(val message: String, val ok: Boolean)

@Composable
fun UploadScreen(
    folders: List<Folder>,
    onBack: () -> Unit,
    onSave: (String, List<Card>, String?) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var title by remember { mutableStateOf("") }
    /** 넣을 폴더의 id. null 이면 폴더 밖. */
    var folderId by remember { mutableStateOf<String?>(null) }
    var fileUri by remember { mutableStateOf<Uri?>(null) }
    var fileName by remember { mutableStateOf("") }
    var status by remember { mutableStateOf<Status?>(null) }
    var saved by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        fileUri = uri
        fileName = context.displayName(uri)
        // 파일 이름을 제목 기본값으로 채워 준다.
        if (title.isBlank()) title = fileName.substringBeforeLast('.')
        status = null
    }

    val save: () -> Unit = save@{
        val name = title.trim()
        val uri = fileUri
        if (name.isEmpty() || uri == null || saved) return@save

        scope.launch {
            val result = runCatching { withContext(Dispatchers.IO) { context.readCards(uri) } }
            val cards = result.getOrNull()
            status = when {
                cards == null -> Status(
                    "파일을 읽지 못했습니다: ${result.exceptionOrNull()?.message}",
                    ok = false,
                )

                cards.isEmpty() -> Status(
                    "단어를 찾지 못했습니다. 일본어·히라가나·한국어 3열 CSV인지 확인해 주세요.",
                    ok = false,
                )

                else -> {
                    onSave(name, cards, folderId)
                    saved = true
                    Status("단어 ${cards.size}개를 불러왔습니다.", ok = true)
                }
            }
        }
    }

    LaunchedEffect(saved) {
        if (saved) {
            delay(600)
            onBack()
        }
    }

    Column(Modifier.fillMaxSize().background(Paper)) {
        TopBar {
            GhostButton("‹ 뒤로", onBack)
            Text("CSV 추가", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Ink)
        }

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 24.dp)
                .windowInsetsPadding(WindowInsets.navigationBars),
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Sheet,
                border = BorderStroke(1.dp, Line),
            ) {
                Column(
                    Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text("주제 제목", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Muted)
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            placeholder = { Text("예: JLPT DAY01 숫자", color = Muted) },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Sheet,
                                unfocusedContainerColor = FieldBg,
                                focusedBorderColor = Accent,
                                unfocusedBorderColor = Line,
                                cursorColor = Accent,
                                focusedTextColor = Ink,
                                unfocusedTextColor = Ink,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text("폴더", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Muted)
                        FolderPickerBox(folders, folderId) { folderId = it }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text("CSV 파일", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Muted)
                        FilePickerBox(fileName) { picker.launch(CSV_MIME_TYPES) }
                        Text(
                            "일본어, 히라가나, 한국어 순서의 3열 CSV (예문까지 6열도 지원)",
                            fontSize = 12.sp,
                            color = Muted,
                        )
                    }

                    status?.let { StatusBox(it) }

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                    ) {
                        GhostButton("취소", onBack)
                        PrimaryButton("저장", save)
                    }
                }
            }
        }
    }
}

@Composable
private fun FilePickerBox(fileName: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = FieldBg,
        border = BorderStroke(1.dp, Line),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("📄", fontSize = 16.sp)
            Text(
                text = fileName.ifEmpty { "CSV 파일 선택…" },
                fontSize = 14.sp,
                color = if (fileName.isEmpty()) Muted else Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** 새 주제를 넣을 폴더를 고르는 칸. 누르면 폴더 밖 + 폴더 목록이 펼쳐진다. */
@Composable
private fun FolderPickerBox(folders: List<Folder>, selectedId: String?, onSelect: (String?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val selected = folders.find { it.id == selectedId }

    Box(Modifier.fillMaxWidth()) {
        Surface(
            onClick = { expanded = true },
            shape = RoundedCornerShape(10.dp),
            color = FieldBg,
            border = BorderStroke(1.dp, Line),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(if (selected != null) "📁" else "🗂️", fontSize = 16.sp)
                Text(
                    text = selected?.name ?: "폴더 밖",
                    fontSize = 14.sp,
                    color = Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text("▾", fontSize = 14.sp, color = Muted)
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = Sheet,
        ) {
            DropdownMenuItem(
                text = { Text("🗂️  폴더 밖", color = Ink) },
                onClick = { onSelect(null); expanded = false },
            )
            folders.forEach { folder ->
                DropdownMenuItem(
                    text = { Text("📁  ${folder.name}", color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    onClick = { onSelect(folder.id); expanded = false },
                )
            }
        }
    }
}

@Composable
private fun StatusBox(status: Status) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (status.ok) OkBg else ErrBg)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(status.message, fontSize = 13.5.sp, color = if (status.ok) OkInk else ErrInk)
    }
}

private fun Context.readCards(uri: Uri): List<Card> =
    contentResolver.openInputStream(uri).use { stream ->
        requireNotNull(stream) { "파일을 열 수 없습니다" }
        CsvParser.read(stream.readBytes())
    }

/** 문서 제공자가 알려 주는 파일 이름. 없으면 URI 의 마지막 조각을 쓴다. */
private fun Context.displayName(uri: Uri): String {
    contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst() && !cursor.isNull(0)) return cursor.getString(0)
    }
    return uri.lastPathSegment?.substringAfterLast('/') ?: "선택한 파일"
}
