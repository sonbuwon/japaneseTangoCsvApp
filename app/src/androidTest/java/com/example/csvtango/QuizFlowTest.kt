package com.example.csvtango

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.csvtango.data.Card
import com.example.csvtango.data.Topic
import com.example.csvtango.data.TopicStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** 홈 → 시험 설정 → 문제 풀이 → 결과까지 실제 화면에서 돌려 본다. */
@RunWith(AndroidJUnit4::class)
class QuizFlowTest {

    @get:Rule
    val compose = createEmptyComposeRule()

    private val context: Context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val store get() = TopicStore(context)

    @Before
    fun seed() {
        // 한자 단어 하나, 가나뿐인 단어 하나. 화면 순서를 고정하기 위해 시험은 섞지 않는다.
        store.save(
            listOf(
                Topic(
                    id = "t1",
                    title = "테스트",
                    createdAt = 0L,
                    cards = listOf(
                        Card(jp = "学校", kana = "がっこう", ko = "학교"),
                        Card(jp = "コーヒー", kana = "こーひー", ko = "커피"),
                    ),
                ),
            ),
        )
    }

    @Test
    fun quizFlow_correctThenWrong_bookmarksWrongWord() {
        ActivityScenario.launch(MainActivity::class.java).use {
            // 홈 → 시험
            compose.onNodeWithText("시험").performClick()
            compose.onNodeWithText("주제 시험").performClick()
            compose.onNodeWithText("테스트").performClick()      // 처음엔 아무 주제도 골라져 있지 않다
            compose.onNodeWithText("무작위 여부").performClick()   // 섞기 끄기
            compose.onNodeWithText("단어 목록 보기 · 2개").performClick()

            // 단어 목록 → 범위(기본은 전체)
            compose.onNodeWithText("시험 범위  1번 ~ 2번").assertIsDisplayed()
            compose.onNodeWithText("시험 시작 · 2문제").performClick()

            // 1. 한자 단어: 일본어를 보여 주고 히라가나를 받는다. 가타카나로 답해도 정답.
            compose.onNodeWithText("学校").assertIsDisplayed()
            compose.onNodeWithText("이 단어의 히라가나는?").assertIsDisplayed()
            compose.onNodeWithText("ひらがな").performTextInput("ガッコウ")
            compose.onNodeWithText("확인").performClick()
            compose.onNodeWithText("○ 정답").assertIsDisplayed()

            // 맞힌 단어는 손수 북마크한다.
            compose.onNodeWithText("☆ 북마크 추가").performClick()
            compose.onNodeWithText("★ 북마크됨").assertIsDisplayed()
            compose.onNodeWithText("다음 ›").performClick()

            // 2. 가나뿐인 단어: 한국어 뜻을 보여 준다. 틀리면 자동 북마크.
            compose.onNodeWithText("커피").assertIsDisplayed()
            // 가타카나 단어라 가타카나 입력란이 나온다.
            compose.onNodeWithText("이 뜻에 맞는 가타카나는?").assertIsDisplayed()
            compose.onNodeWithText("カタカナ").performTextInput("こひ")
            compose.onNodeWithText("확인").performClick()
            compose.onNodeWithText("✕ 오답").assertIsDisplayed()
            compose.onNodeWithText("★ 북마크에 저장했습니다").assertIsDisplayed()
            compose.onNodeWithText("결과 보기").performClick()

            // 결과
            compose.onNodeWithText("1 / 2").assertIsDisplayed()
            compose.onNodeWithText("정답률 50%").assertIsDisplayed()
            compose.onNodeWithText("틀린 단어만 다시 시험 · 1문제").assertIsDisplayed()
        }

        val cards = store.load().single().cards
        assertTrue("맞힌 뒤 손수 북마크한 단어", cards[0].bookmarked)
        assertTrue("틀린 단어는 자동 북마크", cards[1].bookmarked)
    }

    @Test
    fun quizFlow_correctWithoutManualBookmark_leavesCardUnmarked() {
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.onNodeWithText("시험").performClick()
            compose.onNodeWithText("주제 시험").performClick()
            compose.onNodeWithText("테스트").performClick()      // 처음엔 아무 주제도 골라져 있지 않다
            compose.onNodeWithText("무작위 여부").performClick()
            compose.onNodeWithText("단어 목록 보기 · 2개").performClick()
            compose.onNodeWithText("시험 시작 · 2문제").performClick()

            compose.onNodeWithText("ひらがな").performTextInput("がっこう")
            compose.onNodeWithText("확인").performClick()
            compose.onNodeWithText("○ 정답").assertIsDisplayed()
            compose.onNodeWithText("다음 ›").performClick()

            compose.onNodeWithText("カタカナ").performTextInput("こーひー")
            compose.onNodeWithText("확인").performClick()
            compose.onNodeWithText("○ 정답").assertIsDisplayed()
            compose.onNodeWithText("결과 보기").performClick()

            compose.onNodeWithText("2 / 2").assertIsDisplayed()
            compose.onNodeWithText("모두 맞혔습니다!").assertIsDisplayed()
        }

        val cards = store.load().single().cards
        assertEquals(2, cards.size)
        assertFalse(cards[0].bookmarked)
        assertFalse(cards[1].bookmarked)
    }
}
