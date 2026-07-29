package com.example.puntodeventa.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.puntodeventa.data.model.MenuItem
import androidx.compose.material3.MaterialTheme

/**
 * Modal dialog for creating or editing a menu item.
 *
 * @param editingItem  Non-null when opened via the pencil edit icon (edit mode).
 *                     Null when opened via the "+" Add Card (create mode).
 * @param onSave       Called with (emoji, name) when the user confirms.
 * @param onDelete     Called when the user taps ELIMINAR. Only provided in edit mode;
 *                     null in create mode — the delete button is entirely absent when null.
 * @param onDismiss    Called when the user discards changes or taps outside.
 */
@Composable
fun AddMenuDialog(
    editingItem: MenuItem?,
    onSave: (emoji: String, name: String) -> Unit,
    onDelete: (() -> Unit)?,        // null = create mode → button hidden
    onDismiss: () -> Unit,
) {
    // Pre-populate fields when editing
    var selectedEmoji by rememberSaveable { mutableStateOf(editingItem?.emoji ?: "") }
    var menuName      by rememberSaveable { mutableStateOf(editingItem?.name ?: "") }
    var searchQuery   by rememberSaveable { mutableStateOf("") }
    var showError     by remember { mutableStateOf(false) }

    val filteredEmojis = remember(searchQuery) {
        if (searchQuery.isBlank()) defaultEmojiList
        else defaultEmojiList.filter { entry ->
            entry.tags.any { tag -> tag.contains(searchQuery.trim(), ignoreCase = true) }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape          = RoundedCornerShape(16.dp),
            color          = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                // ── Title ────────────────────────────────────────────────────
                Text(
                    text       = "AGREGAR TU MENU",
                    fontSize   = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.onSurface,
                    textAlign  = TextAlign.Center,
                    modifier   = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(16.dp))

                // ── Emoji picker section ─────────────────────────────────────
                Text(
                    text      = "SELECCIONA TU FOTO DEL MENU",
                    fontSize  = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color     = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier  = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(192.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                        .padding(4.dp)
                ) {
                    LazyVerticalGrid(
                        columns               = GridCells.Fixed(6),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement   = Arrangement.spacedBy(4.dp),
                    ) {
                        items(filteredEmojis) { entry ->
                            val isSelected = selectedEmoji == entry.emoji
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(
                                        border = BorderStroke(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .background(MaterialTheme.colorScheme.surface)
                                    .clickable { selectedEmoji = entry.emoji },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = entry.emoji, fontSize = 24.sp)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // ── Search bar ───────────────────────────────────────────────
                OutlinedTextField(
                    value         = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder   = {
                        Text(text = "BUSCAR EMOJI", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    },
                    leadingIcon   = {
                        Icon(
                            imageVector        = Icons.Default.Search,
                            contentDescription = "Buscar",
                            tint               = MaterialTheme.colorScheme.primary
                        )
                    },
                    singleLine      = true,
                    colors          = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedTextColor     = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor   = MaterialTheme.colorScheme.onSurface,
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    modifier        = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(16.dp))

                // ── Name input section ───────────────────────────────────────
                Text(
                    text       = "ESCRIBE EL NOMBRE DE TU MENU",
                    fontSize   = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(6.dp))

                OutlinedTextField(
                    value         = menuName,
                    onValueChange = {
                        menuName  = it
                        showError = false
                    },
                    placeholder = {
                        Text(text = "Tu Nombre Aqui", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    singleLine  = true,
                    isError     = showError,
                    colors      = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedTextColor     = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor   = MaterialTheme.colorScheme.onSurface,
                        errorBorderColor     = MaterialTheme.colorScheme.error,
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (showError) {
                    Text(
                        text     = if (selectedEmoji.isBlank())
                            "Selecciona un emoji primero."
                        else
                            "El nombre no puede estar vacío.",
                        color    = MaterialTheme.colorScheme.error,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(Modifier.height(20.dp))

                // ── Delete button — only visible in edit mode ────────────────
                // AC-06.1 / AC-06.2: rendered only when onDelete is non-null (edit mode)
                if (onDelete != null) {
                    Button(
                        onClick  = onDelete,
                        colors   = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor   = MaterialTheme.colorScheme.onError
                        ),
                        shape    = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector        = Icons.Default.Delete,
                            contentDescription = "Eliminar menú",
                            modifier           = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text       = "ELIMINAR",
                            fontSize   = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }

                // ── GUARDAR / DESCARTAR buttons — always visible ─────────────
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Confirm
                    Button(
                        onClick = {
                            if (selectedEmoji.isBlank() || menuName.isBlank()) {
                                showError = true
                            } else {
                                onSave(selectedEmoji, menuName.trim())
                            }
                        },
                        colors   = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor   = MaterialTheme.colorScheme.onSecondary
                        ),
                        shape    = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text       = "✓  GUARDAR CAMBIOS",
                            fontSize   = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Cancel
                    Button(
                        onClick  = onDismiss,
                        colors   = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor   = MaterialTheme.colorScheme.onError
                        ),
                        shape    = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text       = "DESCARTAR CAMBIOS",
                            fontSize   = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
