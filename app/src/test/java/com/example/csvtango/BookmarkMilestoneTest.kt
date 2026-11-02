package com.example.csvtango

import com.example.csvtango.data.isBookmarkMilestone
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BookmarkMilestoneTest {

    @Test
    fun `10의 배수에 닿으면 알린다`() {
        assertTrue(isBookmarkMilestone(10))
        assertTrue(isBookmarkMilestone(20))
        assertTrue(isBookmarkMilestone(70))
        assertTrue(isBookmarkMilestone(100))
    }

    @Test
    fun `0개나 10의 배수가 아니면 알리지 않는다`() {
        assertFalse(isBookmarkMilestone(0))
        assertFalse(isBookmarkMilestone(1))
        assertFalse(isBookmarkMilestone(9))
        assertFalse(isBookmarkMilestone(11))
        assertFalse(isBookmarkMilestone(65))
    }
}
