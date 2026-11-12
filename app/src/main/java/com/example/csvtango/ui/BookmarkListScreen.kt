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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.csvtango.data.Topic
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
 * 암기 북마크 목록. 맨 위의 [전체 북마크] 를 누르면 모든 주제의 북마크를,
 * 그 아래 주제를 누르면 그 주제의 북마크만 플래시카드로 본다.
 * [topics] 는 북마크한 단어가 있는 주제와 그 개수.
 */
@Composable
fun BookmarkListScreen(
    totalCount: Int,
    topics: List<Pair<Topic, Int>>,
    onBack: () -> Unit,
    onOpenAll: () -> Unit,
    onOpenTopic: (Topic) -> Unit,
) {
    Column(Modifier.fillMaxSize().background(Paper)) {
        TopBar {
            BackButton(onBack)
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("북마크", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                Text("ブックマーク", fontSize = 12.sp, color = Muted)
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (totalCount == 0) {
                item { EmptyBookmarks() }
            } else {
                item { AllBookmarksRow(count = totalCount, onClick = onOpenAll) }
                items(topics, key = { (topic, _) -> topic.id }) { (topic, count) ->
                    TopicBookmarkItem(
                        topic = topic,
                        count = count,
                        onClick = { onOpenTopic(topic) },
                        modifier = Modifier.animateItem(),
                    )
                }
            }

            item { Box(Modifier.windowInsetsPadding(WindowInsets.navigationBars)) }
        }
    }
}

/** 목록 맨 위: 모든 주제의 북마크를 한 번에 본다. */
@Composable
private fun AllBookmarksRow(count: Int, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = GoldBg,
        border = BorderStroke(1.dp, GoldLine),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Badge(text = "★", background = GoldSoft, tint = Gold)
            Column(Modifier.weight(1f)) {
                Text("전체 북마크", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                Text(
                    "모든 주제 · 단어 ${count}개",
                    fontSize = 12.5.sp,
                    color = Muted,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
        }
    }
}

@Composable
private fun TopicBookmarkItem(
    topic: Topic,
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = Sheet,
        border = BorderStroke(1.dp, Line),
        modifier = modifier,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Badge(text = topic.title.trim().take(1).uppercase().ifEmpty { "語" })
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
                    "북마크 ${count}개 / 단어 ${topic.cards.size}개",
                    fontSize = 12.5.sp,
                    color = Muted,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
            Text("★ $count", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Gold)
        }
    }
}

@Composable
private fun EmptyBookmarks() {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 56.dp, horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("☆", fontSize = 40.sp, color = Muted)
        Text(
            "북마크한 단어가 없습니다.",
            fontSize = 15.sp,
            color = Ink,
            modifier = Modifier.padding(top = 12.dp),
        )
        Text(
            "카드 학습이나 시험에서 ★ 를 눌러 단어를 모아 두세요.",
            fontSize = 14.sp,
            color = Muted,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
