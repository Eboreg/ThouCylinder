package us.huseli.thoucylinder.compose.utils

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup

@Composable
fun AutocompleteChip(
    text: String,
    onSave: (String) -> Unit,
    onDelete: () -> Unit,
    getSuggestions: (String) -> List<String>,
    totalAreaHeight: Dp,
    modifier: Modifier = Modifier,
    rootOffsetY: Dp = 0.dp,
    focus: Boolean = false,
) {
    val density = LocalDensity.current
    val focusRequester = remember { FocusRequester() }
    var editMode by rememberSaveable { mutableStateOf(focus) }
    var positionInRoot by remember { mutableStateOf(DpOffset.Zero) }
    var suggestions by remember { mutableStateOf(getSuggestions(text)) }

    val maxPopupHeight = totalAreaHeight - (rootOffsetY + positionInRoot.y) - 32.dp

    LaunchedEffect(editMode) {
        if (editMode) focusRequester.requestFocus()
    }

    InputChip(
        modifier = modifier,
        selected = editMode,
        onClick = { editMode = true },
        label = {
            CompactTextField(
                enabled = editMode,
                value = TextFieldValue(text, TextRange(text.length)),
                onImeAction = {
                    editMode = false
                    onSave(it.text)
                },
                modifier = Modifier
                    .width(IntrinsicSize.Min)
                    .onGloballyPositioned { coords ->
                        val pos = coords.positionInRoot()
                        positionInRoot = with(density) { DpOffset(pos.x.toDp(), pos.y.toDp()) }
                    },
                showClearIcon = false,
                focusRequester = focusRequester,
                onFocusChange = { if (!it.isFocused) editMode = false },
                onValueChange = { suggestions = getSuggestions(it.text) },
            )
        },
        trailingIcon = {
            Icon(
                imageVector = Icons.Sharp.Close,
                contentDescription = null,
                modifier = Modifier.clickable { onDelete() },
            )
        }
    )

    if (editMode) {
        Popup(onDismissRequest = { editMode = false }) {
            Card(
                colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.padding(top = 32.dp).heightIn(max = maxPopupHeight),
                shape = MaterialTheme.shapes.extraSmall,
            ) {
                Column(modifier = Modifier.padding(vertical = 5.dp)) {
                    suggestions.forEach {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                                .clickable {
                                    editMode = false
                                    onSave(it)
                                },
                        ) {
                            Text(text = it, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
            }
        }
    }
}
