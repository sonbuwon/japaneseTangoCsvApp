package com.example.csvtango

import com.example.csvtango.data.Topic
import com.example.csvtango.data.lastEmoji
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TopicAppearanceTest {

    private fun topic(title: String, emoji: String? = null) =
        Topic(id = "t", title = title, cards = emptyList(), createdAt = 0L, emoji = emoji)

    @Test
    fun badgeShowsFirstLetterByDefault() {
        assertEquals("A", topic(" apple").badgeText)
        assertEquals("語", topic("  ").badgeText)
    }

    @Test
    fun badgeShowsEmojiWhenSet() {
        assertEquals("🍣", topic("apple", emoji = "🍣").badgeText)
    }

    @Test
    fun lastEmojiKeepsOnlyTheNewestOne() {
        assertEquals("😎", lastEmoji("😀😎"))
        assertEquals("🍣", lastEmoji("🍣"))
    }

    @Test
    fun lastEmojiIgnoresPlainText() {
        assertNull(lastEmoji("abc"))
        assertNull(lastEmoji("日本語"))
        assertNull(lastEmoji(""))
        assertEquals("🍜", lastEmoji("🍜a"))
    }

    @Test
    fun lastEmojiTreatsMultiCodePointEmojiAsOne() {
        val family = "👨‍👩‍👧"
        assertEquals(family, lastEmoji("😀$family"))
        assertEquals("👍🏽", lastEmoji("👍🏽"))
        assertEquals("🇯🇵", lastEmoji("🇰🇷🇯🇵"))
        assertEquals("❤️", lastEmoji("❤️"))
        assertEquals("1️⃣", lastEmoji("x1️⃣"))
        assertEquals("⛩️", lastEmoji("⛩️"))
    }
}
