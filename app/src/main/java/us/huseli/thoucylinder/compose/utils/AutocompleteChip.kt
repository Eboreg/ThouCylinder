package us.huseli.thoucylinder.compose.utils

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutocompleteChip(
    originalText: String,
    onSave: (String) -> Unit,
    onDelete: () -> Unit,
    getSuggestions: (String) -> List<String>,
    modifier: Modifier = Modifier,
) {
    var savedText by remember(originalText) { mutableStateOf(originalText) }
    var currentText by remember(savedText) { mutableStateOf(savedText) }
    val suggestions by remember(currentText) { mutableStateOf(getSuggestions(currentText)) }
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    var isTextFieldClicked by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isFocused) {
        if (!isFocused) currentText = savedText
    }

    ExposedDropdownMenuBox(
        expanded = isFocused,
        onExpandedChange = {
            isTextFieldClicked = true
        },
    ) {
        InputChip(
            modifier = modifier.menuAnchor(),
            selected = isFocused,
            onClick = { focusRequester.requestFocus() },
            label = {
                BasicTextField(
                    value = currentText,
                    onValueChange = { currentText = it },
                    modifier = Modifier.width(IntrinsicSize.Min).focusRequester(focusRequester),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                    textStyle = MaterialTheme.typography.labelLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                    interactionSource = interactionSource,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(
                        onGo = {
                            focusManager.clearFocus()
                            if (currentText.isNotEmpty()) {
                                savedText = currentText
                                onSave(currentText)
                            }
                        },
                    ),
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
        ExposedDropdownMenu(
            expanded = isFocused && suggestions.isNotEmpty(),
            onDismissRequest = {
                if (!isTextFieldClicked) {
                    focusManager.clearFocus()
                    currentText = savedText
                }
                isTextFieldClicked = false
            },
            modifier = Modifier.padding(0.dp).width(IntrinsicSize.Max),
        ) {
            suggestions.forEachIndexed { index, suggestion ->
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .clickable {
                            focusManager.clearFocus()
                            savedText = suggestion
                            currentText = suggestion
                            onSave(suggestion)
                        },
                ) {
                    Text(
                        text = suggestion,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1,
                    )
                }
                if (index < suggestions.lastIndex) HorizontalDivider()
            }
        }
    }
}
