package com.example.csvtango

import com.example.csvtango.data.Card
import com.example.csvtango.data.Kana
import com.example.csvtango.data.PromptKind
import com.example.csvtango.data.QuizQuestion
import com.example.csvtango.data.StudyCard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuizTest {

    private fun question(jp: String, kana: String, ko: String) =
        QuizQuestion(StudyCard(Card(jp = jp, kana = kana, ko = ko), topicId = "t", index = 0))

    @Test
    fun `한자 단어는 일본어를 보여 주고 히라가나를 정답으로 받는다`() {
        val q = question("学校", "がっこう", "학교")

        assertEquals(PromptKind.JAPANESE, q.kind)
        assertEquals("学校", q.prompt)
        assertTrue(q.isCorrect("がっこう"))
        assertFalse(q.isCorrect("がこう"))
    }

    @Test
    fun `가나뿐인 단어는 한국어 뜻을 보여 준다`() {
        val hiragana = question("ありがとう", "ありがとう", "고맙다")
        val katakana = question("コーヒー", "こーひー", "커피")

        assertEquals(PromptKind.MEANING, hiragana.kind)
        assertEquals("고맙다", hiragana.prompt)
        assertEquals(PromptKind.MEANING, katakana.kind)
        assertEquals("커피", katakana.prompt)
    }

    @Test
    fun `가나 단어인데 뜻이 없으면 단어를 보여 준다`() {
        val q = question("はい", "はい", "")

        assertEquals(PromptKind.JAPANESE, q.kind)
    }

    @Test
    fun `가타카나로 답해도 히라가나와 같게 본다`() {
        val q = question("コーヒー", "こーひー", "커피")

        assertTrue(q.isCorrect("コーヒー"))
        assertTrue(q.isCorrect(" こーひー "))
    }

    @Test
    fun `읽기가 여러 개면 어느 것이든 정답이다`() {
        val q = question("四", "し / よん", "4")

        assertTrue(q.isCorrect("し"))
        assertTrue(q.isCorrect("よん"))
        assertFalse(q.isCorrect("しよん"))
    }

    @Test
    fun `히라가나 열이 비어 있어도 단어가 가나면 단어로 채점한다`() {
        val q = question("すし", "", "초밥")

        assertTrue(q.answerable)
        assertTrue(q.isCorrect("すし"))
    }

    @Test
    fun `한자 단어에 히라가나가 없으면 낼 수 없다`() {
        val q = question("学校", "", "학교")

        assertFalse(q.answerable)
        assertFalse(q.isCorrect(""))
    }

    @Test
    fun `정규화는 공백을 지우고 가타카나를 히라가나로 바꾼다`() {
        assertEquals("がっこう", Kana.normalize("ガッ コウ"))
        assertEquals("ぱん", Kana.toHiragana("パン"))
        assertTrue(Kana.isKanji('々'))
        assertFalse(Kana.isKanji('あ'))
        assertTrue(Kana.isKana('ー'))
    }
}
