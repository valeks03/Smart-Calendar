package com.example.smartcalendar.presentation.calendar

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun RevealSwipeItem(
    modifier: Modifier = Modifier,
    revealWidth: Dp = 104.dp,               // ширина области с кнопкой
    onDelete: () -> Unit,
    content: @Composable RowScope.() -> Unit
) {
    val revealPx = with(LocalDensity.current) { revealWidth.toPx() }
    var dragX by remember { mutableStateOf(0f) }
    val animatedX by animateFloatAsState(targetValue = dragX, label = "swipeX")

    Box(
        modifier = modifier.clip(RoundedCornerShape(12.dp))
    ) {
        // Кнопка удаления справа
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            val buttonOffset = ((revealPx - animatedX).coerceIn(0f, revealPx)).roundToInt()
            ElevatedButton(
                onClick = { onDelete() },
                colors = ButtonDefaults.elevatedButtonColors(
                    containerColor = Color(0xFFFFE5E5),
                    contentColor = Color(0xFFB00020)
                ),
                modifier = Modifier
                    .offset { IntOffset(x = buttonOffset, y = 0) }
                    .width(revealWidth - 8.dp)
            ) {
                Icon(Icons.Default.Delete, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Удалить")
            }
        }

        // Контент карточки
        Row(
            modifier = Modifier
                .offset { IntOffset(animatedX.roundToInt(), 0) }
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDrag = { change, dragAmount ->
                            change.consume()
                            dragX = (dragX + dragAmount.x).coerceIn(0f, revealPx) // берём только X
                        },
                        onDragEnd = { dragX = if (dragX > revealPx * 0.5f) revealPx else 0f },
                        onDragCancel = { dragX = 0f }
                    )
                }
                .padding(12.dp),
            content = content
        )
    }
}
