package com.example.csvtango.ui

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

/**
 * LazyColumn 안에서 항목을 꾹 눌러 끌어 순서를 바꾸는 상태.
 *
 * 끄는 항목은 "누르기 시작한 자리 + 끈 거리" 에 그린다. 목록 순서가 바뀌어 항목이 다른 자리에
 * 배치되어도, 그리는 시점의 배치 위치를 빼서 계산하므로 손가락 아래에서 튀지 않는다.
 * 이웃 항목의 가운데를 넘으면 한 칸 옮기라고 알린다(onMove).
 */
@Stable
class ReorderState(val listState: LazyListState) {
    /** 지금 끌고 있는 항목의 key. 없으면 null. */
    var draggingKey by mutableStateOf<Any?>(null)
        private set

    /** 누르기 시작했을 때 항목의 배치 위치(px). */
    private var startOffset by mutableIntStateOf(0)

    /** 누른 뒤 끈 거리(px). */
    private var dragged by mutableFloatStateOf(0f)

    /** 한 칸 옮긴 뒤 목록이 다시 배치되기 전에는 또 옮기지 않도록, 기다리는 목록 위치(index). */
    private var awaitingIndex: Int? = null

    fun isDragging(key: Any): Boolean = draggingKey == key

    /** 끄는 항목을 지금 배치된 자리에서 얼마나 옮겨 그릴지(px). 그리는 단계에서 읽는다. */
    fun translationOf(key: Any): Float {
        if (draggingKey != key) return 0f
        val current = listState.layoutInfo.visibleItemsInfo.find { it.key == key } ?: return 0f
        return startOffset + dragged - current.offset
    }

    internal fun start(key: Any) {
        val current = listState.layoutInfo.visibleItemsInfo.find { it.key == key } ?: return
        draggingKey = key
        startOffset = current.offset
        dragged = 0f
        awaitingIndex = null
    }

    internal fun stop() {
        draggingKey = null
        dragged = 0f
        awaitingIndex = null
    }

    /**
     * [key] 항목을 [dy] 만큼 끈다. [groupKeys] 는 함께 순서를 바꾸는 항목들의 key(지금 순서).
     * 이웃의 가운데를 넘으면 [onMove] (-1 위, +1 아래) 를 부른다.
     */
    internal fun drag(key: Any, dy: Float, groupKeys: List<Any>, onMove: (Int) -> Unit) {
        if (draggingKey != key) return
        dragged += dy

        val items = listState.layoutInfo.visibleItemsInfo
        val current = items.find { it.key == key } ?: return

        // 직전에 옮긴 결과가 아직 배치되지 않았으면 기다린다.
        awaitingIndex?.let { expected ->
            if (current.index != expected) return
            awaitingIndex = null
        }

        val index = groupKeys.indexOf(key)
        if (index < 0) return
        val top = startOffset + dragged
        val center = top + current.size / 2f

        val neighbor = when {
            center > current.offset + current.size / 2f && index < groupKeys.lastIndex ->
                items.find { it.key == groupKeys[index + 1] }?.takeIf { center > it.offset + it.size / 2f }
            center < current.offset + current.size / 2f && index > 0 ->
                items.find { it.key == groupKeys[index - 1] }?.takeIf { center < it.offset + it.size / 2f }
            else -> null
        } ?: return

        awaitingIndex = neighbor.index
        onMove(if (neighbor.index > current.index) 1 else -1)
    }
}

@Composable
fun rememberReorderState(listState: LazyListState): ReorderState = remember(listState) { ReorderState(listState) }

/**
 * 꾹 누른 뒤 끌어서 순서를 바꾸게 한다. [enabled] 가 false 면 아무것도 하지 않는다.
 * 끄는 동안에는 항목을 띄워(그림자·살짝 확대) 다른 항목 위에 그린다.
 */
fun Modifier.reorderable(
    state: ReorderState,
    key: Any,
    enabled: Boolean,
    groupKeys: List<Any>,
    onMove: (Int) -> Unit,
): Modifier = if (!enabled) this else composed {
    val haptic = LocalHapticFeedback.current
    val currentKeys by rememberUpdatedState(groupKeys)
    val currentOnMove by rememberUpdatedState(onMove)
    val dragging = state.isDragging(key)

    this
        .zIndex(if (dragging) 1f else 0f)
        .graphicsLayer {
            translationY = state.translationOf(key)
            val scale = if (dragging) 1.03f else 1f
            scaleX = scale
            scaleY = scale
            shadowElevation = if (dragging) 12.dp.toPx() else 0f
            shape = RoundedCornerShape(16.dp)
        }
        .pointerInput(key) {
            detectDragGesturesAfterLongPress(
                onDragStart = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    state.start(key)
                },
                onDragEnd = { state.stop() },
                onDragCancel = { state.stop() },
            ) { change, amount ->
                change.consume()
                state.drag(key, amount.y, currentKeys, currentOnMove)
            }
        }
}
