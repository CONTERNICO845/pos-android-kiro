package com.example.puntodeventa.ui.newproduct

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.puntodeventa.ui.theme.InputBorder
import com.example.puntodeventa.ui.theme.InputText
import com.example.puntodeventa.ui.theme.ModalBodyText
import com.example.puntodeventa.ui.theme.ModalSurface

// ── Emoji catalogue — at least 50 food & object emojis ───────────────────────
val EMOJI_LIST: List<String> = listOf(
    "🍕", "🍔", "🌮", "🍣", "🍜",
    "🍝", "🍛", "🍲", "🥘", "🥗",
    "🍱", "🍤", "🍗", "🍖", "🥩",
    "🥦", "🥕", "🍞", "🧀", "🥚",
    "🍳", "🧆", "🥐", "🥞", "🧇",
    "🍩", "🍪", "🎂", "🍰", "🧁",
    "🍫", "🍭", "🍬", "🍦", "🍧",
    "🍨", "🍻", "🥤", "☕", "🍵",
    "🧃", "🧋", "🛒", "🍺", "🥂",
    "🍷", "🍾", "🎁", "🎉", "🎊",
    "🎈", "🎀", "🏆", "🥇", "🎯",
    "🎲", "🎮", "🌽", "🥑", "🍎"
)

/**
 * A button that displays the currently selected emoji and a chevron indicating
 * whether the emoji grid is expanded. When [expanded] is true, an [AnimatedVisibility]
 * block shows a [LazyVerticalGrid] of all [EMOJI_LIST] entries.
 *
 * Requirements: 2.1, 2.2, 2.3
 *
 * @param emoji           The currently selected emoji string (never blank).
 * @param expanded        Whether the emoji picker grid is currently visible.
 * @param onToggle        Called when the button is tapped to open/close the grid.
 * @param onEmojiSelected Called with the tapped emoji string when the user picks one.
 * @param modifier        Optional [Modifier] applied to the outermost layout.
 */
@Composable
fun EmojiPickerButton(
    emoji: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    onEmojiSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Outer column so the grid sits directly below the button
    androidx.compose.foundation.layout.Column(modifier = modifier) {

        // ── Toggle button ──────────────────────────────────────────────────
        OutlinedButton(
            onClick = onToggle,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = InputText
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, InputBorder)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Current emoji
                Text(
                    text = emoji,
                    fontSize = 22.sp
                )
                // Chevron — up when expanded, down when collapsed (Req 2.2, 2.3)
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp
                                  else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Cerrar selector de emoji"
                                         else "Abrir selector de emoji",
                    tint = InputBorder
                )
            }
        }

        // ── Animated emoji grid ────────────────────────────────────────────
        AnimatedVisibility(visible = expanded) {
            Surface(
                color = ModalSurface,
                tonalElevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    modifier = Modifier
                        .fillMaxWidth()
                        // Cap height so the grid scrolls rather than expanding infinitely
                        .heightIn(max = 240.dp)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(EMOJI_LIST) { item ->
                        Text(
                            text = item,
                            fontSize = 24.sp,
                            color = ModalBodyText,
                            modifier = Modifier
                                .clickable { onEmojiSelected(item) }
                                .padding(6.dp)
                        )
                    }
                }
            }
        }
    }
}
