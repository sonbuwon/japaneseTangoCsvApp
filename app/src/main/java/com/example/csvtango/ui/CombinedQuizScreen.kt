package com.example.csvtango.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.csvtango.data.Folder
import com.example.csvtango.data.Topic
import com.example.csvtango.ui.theme.Accent
import com.example.csvtango.ui.theme.FieldBg
import com.example.csvtango.ui.theme.Gold
import com.example.csvtango.ui.theme.GoldBg
import com.example.csvtango.ui.theme.GoldLine
import com.example.csvtango.ui.theme.Ink
import com.example.csvtango.ui.theme.Line
import com.example.csvtango.ui.theme.Muted
import com.example.csvtango.ui.theme.Paper
import com.example.csvtango.ui.theme.Sheet
import com.example.csvtango.ui.theme.StarOff

/** 종합 시험 설정. 목록을 여러 개 고르고 문제 수를 적는다. 순서는 항상 무작위. */
@Composable
fun CombinedQuizScreen(
    topicGroups: List<Pair<Folder?, List<Topic>>>,
    bookmarkCount: Int,
    selected: Set<String>,
    countText: String,
    available: Int,
    questionCount: Int,
    onToggle: (String) -> Unit,
    onCountChange: (String) -> Unit,
    onBack: () -> Unit,
    onStart: () -> Unit,
) {
    val requested = countText.toIntOrNull() ?: 0

    Column(Modifier.fillMaxSize().background(Paper).imePadding()) {
        TopBar {
            BackButton(onBack)
            Text("종합 시험", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Ink)
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Text(
                    "문제 수",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Muted,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }

            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = countText,
                        onValueChange = onCountChange,
                        placeholder = { Text("20", color = StarOff) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        textStyle = TextStyle(fontSize = 18.sp, color = Ink, textAlign = TextAlign.Center),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Sheet,
                            unfocusedContainerColor = FieldBg,
                            focusedBorderColor = Accent,
                            unfocusedBorderColor = Line,
                            cursorColor = Accent,
                        ),
                        modifier = Modifier.width(110.dp),
                    )
                    Text("문제", fontSize = 15.sp, color = Ink)
                    Box(Modifier.weight(1f))
                    if (available > 0) GhostButton("최대 ${available}", onClick = { onCountChange(available.toString()) })
                }
            }

            item {
                val note = when {
                    requested <= 0 -> "1 이상의 숫자를 입력해 주세요"
                    selected.isEmpty() -> "아래에서 목록을 하나 이상 골라 주세요"
                    requested > available -> "고른 목록에 낼 수 있는 단어가 ${available}개라 ${available}문제만 냅니다"
                    else -> "고른 목록에서 무작위로 ${requested}문제를 냅니다"
                }
                Text(note, fontSize = 12.5.sp, color = Muted)
            }

            item {
                Text(
                    "시험 범위 (여러 개 선택)",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Muted,
                    modifier = Modifier.padding(top = 18.dp, bottom = 6.dp),
                )
            }

            item {
                val enabled = bookmarkCount > 0
                SelectRow(
                    checked = BOOKMARK_SOURCE in selected && enabled,
                    enabled = enabled,
                    onClick = { onToggle(BOOKMARK_SOURCE) },
                    background = GoldBg,
                    border = GoldLine,
                    badge = { Badge("★", GoldBg, Gold) },
                    title = "북마크",
                    subtitle = if (enabled) "단어 ${bookmarkCount}개" else "북마크한 단어가 없습니다",
                )
            }

            val showFolders = topicGroups.any { it.first != null }
            topicGroups.forEach { (folder, topics) ->
                if (showFolders) {
                    item(key = "folder:" + (folder?.id ?: "none")) { FolderDivider(folder?.name) }
                }
                items(topics, key = { it.id }) { topic ->
                    SelectRow(
                        checked = topic.id in selected,
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
                        questionCount > 0 -> "종합 시험 시작 · ${questionCount}문제"
                        selected.isEmpty() -> "목록을 하나 이상 골라 주세요"
                        available == 0 -> "고른 목록에 낼 수 있는 단어가 없습니다"
                        else -> "문제 수를 입력해 주세요"
                    },
                    enabled = questionCount > 0,
                    onClick = onStart,
                )
            }
        }
    }
}
