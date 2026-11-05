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
    fun `괄호로 감싼 부분은 넣어도 빼도 정답이다`() {
        val q = question("作る", "つく(る)", "만들다")

        assertTrue(q.isCorrect("つくる"))
        assertTrue(q.isCorrect("つく"))
        assertTrue(q.isCorrect("つく(る)"))
        assertTrue(q.isCorrect("つく（る）"))
        assertFalse(q.isCorrect("つ"))
        assertFalse(q.isCorrect("る"))
    }

    @Test
    fun `전각 괄호와 여러 읽기에도 괄호 생략이 적용된다`() {
        val q = question("お茶", "（お）ちゃ / おちゃ", "차")

        assertTrue(q.isCorrect("おちゃ"))
        assertTrue(q.isCorrect("ちゃ"))
        assertFalse(q.isCorrect("お"))
    }

    @Test
    fun `가나 단어 자체에 괄호가 있어도 생략한 답을 받는다`() {
        val q = question("（お）かね", "", "돈")

        assertEquals(PromptKind.MEANING, q.kind)
        assertTrue(q.isCorrect("おかね"))
        assertTrue(q.isCorrect("かね"))
    }

    @Test
    fun `정규화는 공백을 지우고 가타카나를 히라가나로 바꾼다`() {
        assertEquals("がっこう", Kana.normalize("ガッ コウ"))
        assertEquals("ぱん", Kana.toHiragana("パン"))
        assertTrue(Kana.isKanji('々'))
        assertFalse(Kana.isKanji('あ'))
        assertTrue(Kana.isKana('ー'))
    }

    @Test
    fun `가타카나 단어만 가타카나 입력으로 묻는다`() {
        assertTrue(question("コーヒー", "こーひー", "커피").katakana)
        assertTrue(question("パン・ケーキ", "ぱんけーき", "팬케이크").katakana)
        assertFalse(question("ありがとう", "ありがとう", "고맙다").katakana)
        assertFalse(question("学校", "がっこう", "학교").katakana)
        assertFalse(question("ビール瓶", "びーるびん", "맥주병").katakana)
        assertFalse(question("コピーする", "こぴーする", "복사하다").katakana)
    }

    @Test
    fun `히라가나를 가타카나로 바꾸고 다른 글자는 그대로 둔다`() {
        assertEquals("コーヒー", Kana.toKatakana("こーひー"))
        assertEquals("ヴァイオリン", Kana.toKatakana("ゔぁいおりん"))
        assertEquals("ガッコウ 学校", Kana.toKatakana("がっこう 学校"))
        assertEquals("こーひー".length, Kana.toKatakana("こーひー").length)
    }

    @Test
    fun `가타카나로 바꿔 제출한 답도 정답이다`() {
        val q = question("コーヒー", "こーひー", "커피")

        assertTrue(q.isCorrect(Kana.toKatakana("こーひー")))
    }
}
