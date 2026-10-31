package com.example.csvtango

import com.example.csvtango.data.moveAmong
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class MoveAmongTest {

    // 대문자는 폴더 A, 소문자는 폴더 밖이라고 본다.
    private val list = listOf("A1", "b1", "A2", "b2", "A3")
    private fun inA(s: String) = s.startsWith("A")

    @Test
    fun movesDownPastOtherGroup() {
        assertEquals(listOf("A2", "b1", "A1", "b2", "A3"), list.moveAmong("A1", 1, ::inA))
    }

    @Test
    fun movesUpPastOtherGroup() {
        assertEquals(listOf("A1", "b1", "A3", "b2", "A2"), list.moveAmong("A3", -1, ::inA))
    }

    @Test
    fun otherGroupKeepsItsSlots() {
        assertEquals(listOf("A1", "b2", "A2", "b1", "A3"), list.moveAmong("b1", 1) { !inA(it) })
    }

    @Test
    fun edgesAndMissingAreUnchanged() {
        assertSame(list, list.moveAmong("A1", -1, ::inA))
        assertSame(list, list.moveAmong("A3", 1, ::inA))
        assertSame(list, list.moveAmong("zz", 1, ::inA))
    }

    @Test
    fun plainListMove() {
        assertEquals(listOf("b", "a", "c"), listOf("a", "b", "c").moveAmong("a", 1) { true })
    }
}
