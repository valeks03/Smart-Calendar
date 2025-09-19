package com.example.smartcalendar.presentation.calendar

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
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
import kotlin.math.roundToInt

/**
 * Swipe-to-reveal: свайп ВПРАВО открывает кнопку удаления справа,
 * контент НЕ сдвигается. Обратный свайп закрывает.
 */
@Composable
fun RevealSwipeItem(
    modifier: Modifier = Modifier,
    revealWidth: Dp = 104.dp,
    onDelete: () -> Unit,
    content: @Composable () -> Unit
) {
    val revealPx = with(LocalDensity.current) { revealWidth.toPx() }
    var reveal by remember { mutableStateOf(0f) }                 // 0f..revealPx
    val anim by animateFloatAsState(targetValue = reveal, label = "reveal")

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { change, amount ->
                        change.consume()
                        // свайп ВПРАВО → раскрываем; влево → закрываем
                        reveal = (reveal - amount).coerceIn(0f, revealPx)
                    },
                    onDragEnd = {
                        // щёлк к ближайшему состоянию
                        reveal = if (reveal > revealPx * 0.45f) revealPx else 0f
                    },
                    onDragCancel = { reveal = 0f }
                )
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            content()
        }

        Box(
            modifier = Modifier.matchParentSize(),
            contentAlignment = Alignment.CenterEnd
        ) {
            val panelWidth = anim.roundToInt()
            if (panelWidth > 0) {
                Box(
                    modifier = Modifier
                        .width(with(LocalDensity.current) { panelWidth.toDp() })
                        .fillMaxHeight()
                        .background(
                            Color(0xFFFFE5E5),
                            RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Удалить",
                            tint = Color(0xFFB00020) // яркий акцент
                        )
                    }
                }
            }
        }
    }
}