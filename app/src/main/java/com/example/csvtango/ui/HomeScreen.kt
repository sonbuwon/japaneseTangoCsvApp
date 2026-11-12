package com.example.csvtango.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.csvtango.ui.theme.Accent
import com.example.csvtango.ui.theme.AccentSoft
import com.example.csvtango.ui.theme.Gold
import com.example.csvtango.ui.theme.GoldSoft
import com.example.csvtango.ui.theme.Ink
import com.example.csvtango.ui.theme.Line
import com.example.csvtango.ui.theme.Muted
import com.example.csvtango.ui.theme.OkBg
import com.example.csvtango.ui.theme.OkInk
import com.example.csvtango.ui.theme.Paper
import com.example.csvtango.ui.theme.Sheet

/** 앱 첫 화면. [암기]와 [시험] 중 하나를 고른다. */
@Composable
fun HomeScreen(
    wordCount: Int,
    onMemorize: () -> Unit,
    onQuiz: () -> Unit,
) {
    val hasWords = wordCount > 0

    Column(Modifier.fillMaxSize().background(Paper)) {
        TopBar {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("単語カード", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Ink)
                Text("일본어 단어 카드", fontSize = 12.sp, color = Muted)
            }
        }

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp)
                .windowInsetsPadding(WindowInsets.navigationBars),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "무엇을 할까요?",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Muted,
                modifier = Modifier.padding(bottom = 6.dp),
            )

            ModeCard(
                glyph = "📚",
                title = "암기",
                detail = if (hasWords) null else "암기 화면의 [+ csv]로 단어장을 만듭니다",
                onClick = onMemorize,
            )

            ModeCard(
                glyph = "✏️",
                title = "시험",
                detail = if (hasWords) null else "먼저 암기 화면의 [+ csv]로 단어장을 추가해 주세요",
                enabled = hasWords,
                onClick = onQuiz,
                badgeBackground = GoldSoft,
                badgeTint = Gold,
            )
        }
    }
}

/** 시험 종류 선택. [주제 시험]·[종합 시험] 순으로 보여 준다. */
@Composable
fun QuizMenuScreen(
    topicCount: Int,
    onTopicQuiz: () -> Unit,
    onCombinedQuiz: () -> Unit,
    onBack: () -> Unit,
) {
    Column(Modifier.fillMaxSize().background(Paper)) {
        TopBar {
            BackButton(onBack)
            Text("시험", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Ink)
        }

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp)
                .windowInsetsPadding(WindowInsets.navigationBars),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ModeCard(
                glyph = "✏️",
                title = "주제 시험",
                detail = "주제 ${topicCount}개",
                onClick = onTopicQuiz,
                badgeBackground = GoldSoft,
                badgeTint = Gold,
            )

            ModeCard(
                glyph = "🎯",
                title = "종합 시험",
                detail = "여러 목록에서 무작위로 출제",
                onClick = onCombinedQuiz,
                badgeBackground = OkBg,
                badgeTint = OkInk,
            )
        }
    }
}

@Composable
private fun ModeCard(
    glyph: String,
    title: String,
    detail: String?,
    onClick: () -> Unit,
    enabled: Boolean = true,
    badgeBackground: Color = AccentSoft,
    badgeTint: Color = Accent,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(18.dp),
        color = Sheet,
        border = BorderStroke(1.dp, Line),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier
                .alpha(if (enabled) 1f else 0.55f)
                .padding(horizontal = 20.dp, vertical = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                Modifier.size(54.dp).clip(RoundedCornerShape(14.dp)).background(badgeBackground),
                contentAlignment = Alignment.Center,
            ) {
                Text(glyph, fontSize = 26.sp, color = badgeTint)
            }

            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Ink)
                if (detail != null) Text(detail, fontSize = 12.5.sp, color = Muted)
            }

            Text("›", fontSize = 26.sp, color = Muted)
        }
    }
}
