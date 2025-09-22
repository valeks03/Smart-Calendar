package com.example.smartcalendar.presentation.calendar

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt

/**
 * Gmail/Telegram style:
 * - Свайп ВЛЕВО сдвигает контент.
 * - Справа под ним появляется красная панель с кнопкой удаления.
 * - Когда закрыто — задняя панель имеет ширину 0 и не видна.
 */
@Composable
fun RevealSwipeItem(
    modifier: Modifier = Modifier,
    revealWidth: Dp = 88.dp,
    onDelete: () -> Unit,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val revealPx = with(density) { revealWidth.toPx() }

    // Смещение переднего слоя: 0f (закрыто) .. -revealPx (открыто)
    var offsetX by remember { mutableStateOf(0f) }
    val animX by animateFloatAsState(targetValue = offsetX, label = "reveal-offset")

    // Ширина задней панели = величине раскрытия (0..revealWidth)
    val backWidthDp by remember(animX) {
        mutableStateOf(with(density) { (-animX).coerceIn(0f, revealPx).toDp() })
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
    ) {
        // --- Задняя панель (видна только когда backWidthDp > 0) ---
        Row(
            modifier = Modifier
                .matchParentSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.weight(1f))
            // панель «растёт» от 0 до revealWidth
            Box(
                modifier = Modifier
                    .width(backWidthDp)
                    .fillMaxHeight()
                    .background(
                        color = Color(0xFFFFE5E5), // мягкий красный
                        shape = RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (backWidthDp > 0.dp) {
                    IconButton(onClick = {
                        onDelete()
                        offsetX = 0f
                    }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Удалить",
                            tint = Color(0xFFB00020)
                        )
                    }
                }
            }
        }

        // --- Передний слой: сам контент (полностью перекрывает задний) ---
        Box(
            modifier = Modifier
                .zIndex(1f)
                .fillMaxWidth() // важно: перекрываем панель при закрытом состоянии
                .offset { IntOffset(animX.roundToInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            // влево (открыть) — отрицательно, вправо (закрыть) — положительно
                            offsetX = (offsetX + dragAmount).coerceIn(-revealPx, 0f)
                        },
                        onDragEnd = {
                            offsetX = if (offsetX < -revealPx * 0.45f) -revealPx else 0f
                        },
                        onDragCancel = { offsetX = 0f }
                    )
                }
        ) {
            content() // без фонов/паддингов — всё берём из вызывающего места
        }
    }
}
