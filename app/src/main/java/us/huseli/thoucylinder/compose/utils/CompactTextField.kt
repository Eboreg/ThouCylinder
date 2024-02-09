package us.huseli.thoucylinder.compose.utils

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Clear
import androidx.compose.material.icons.sharp.Search
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

@Composable
inline fun CompactTextField(
    value: TextFieldValue,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    continuousUpdate: Boolean = false,
    color: Color = MaterialTheme.colorScheme.onSurface,
    textStyle: TextStyle = MaterialTheme.typography.labelLarge.copy(color = color),
    placeholderText: String? = null,
    showClearIcon: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    focusRequester: FocusRequester = remember { FocusRequester() },
    crossinline trailingIcon: @Composable (TextFieldValue) -> Unit = {},
    crossinline onChange: (TextFieldValue) -> Unit = {},
    crossinline onImeAction: (TextFieldValue) -> Unit = {},
    crossinline onFocusChanged: (FocusState) -> Unit = {},
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Go),
) {
    var isFocused by remember { mutableStateOf(false) }
    var textFieldValue by remember(value) { mutableStateOf(value) }

    BasicTextField(
        value = textFieldValue,
        interactionSource = interactionSource,
        onValueChange = {
            textFieldValue = it
            onChange(it)
            if (continuousUpdate) onImeAction(it)
        },
        modifier = modifier
            .height(32.dp)
            .padding(0.dp)
            .onFocusChanged {
                isFocused = it.isFocused
                onFocusChanged(it)
            }
            .focusRequester(focusRequester),
        singleLine = true,
        enabled = enabled,
        textStyle = textStyle,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
        keyboardOptions = keyboardOptions,
        keyboardActions = KeyboardActions(
            onSearch = { onImeAction(textFieldValue) },
            onSend = { onImeAction(textFieldValue) },
            onDone = { onImeAction(textFieldValue) },
            onGo = { onImeAction(textFieldValue) },
        ),
        decorationBox = { innerTextField ->
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    Box(modifier = Modifier.weight(1f).align(Alignment.CenterVertically)) {
                        innerTextField()
                        if (!isFocused && textFieldValue.text.isEmpty() && placeholderText != null) {
                            Text(placeholderText, style = textStyle, color = color.copy(alpha = 0.5f))
                        }
                    }
                    if (textFieldValue.text.isNotEmpty() && showClearIcon) {
                        Icon(
                            imageVector = Icons.Sharp.Clear,
                            contentDescription = null,
                            modifier = Modifier.size(30.dp).padding(start = 5.dp).clickable {
                                textFieldValue = TextFieldValue("")
                                onChange(textFieldValue)
                                onImeAction(textFieldValue)
                            },
                            tint = MaterialTheme.colorScheme.outline,
                        )
                    }
                    trailingIcon(textFieldValue)
                }
                Divider(
                    modifier = Modifier.fillMaxWidth(),
                    color = if (isFocused) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outlineVariant,
                )
            }
        },
    )
}

@Composable
inline fun CompactTextField(
    value: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    continuousUpdate: Boolean = false,
    color: Color = MaterialTheme.colorScheme.onSurface,
    textStyle: TextStyle = MaterialTheme.typography.labelLarge.copy(color = color),
    placeholderText: String? = null,
    showClearIcon: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    focusRequester: FocusRequester = remember { FocusRequester() },
    crossinline trailingIcon: @Composable (String) -> Unit = {},
    crossinline onChange: (String) -> Unit = {},
    crossinline onImeAction: (String) -> Unit = {},
    crossinline onFocusChanged: (FocusState) -> Unit = {},
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Go),
) {
    CompactTextField(
        value = TextFieldValue(value),
        modifier = modifier,
        enabled = enabled,
        continuousUpdate = continuousUpdate,
        color = color,
        textStyle = textStyle,
        placeholderText = placeholderText,
        showClearIcon = showClearIcon,
        interactionSource = interactionSource,
        focusRequester = focusRequester,
        trailingIcon = { trailingIcon(it.text) },
        onChange = { onChange(it.text) },
        onImeAction = { onImeAction(it.text) },
        onFocusChanged = onFocusChanged,
        keyboardOptions = keyboardOptions,
    )
}

@Composable
inline fun CompactSearchTextField(
    value: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    continuousSearch: Boolean = false,
    color: Color = MaterialTheme.colorScheme.onSurface,
    textStyle: TextStyle = MaterialTheme.typography.labelLarge.copy(color = color),
    placeholderText: String? = null,
    crossinline onFocusChanged: (FocusState) -> Unit = {},
    crossinline onChange: (String) -> Unit = {},
    crossinline onSearch: (String) -> Unit = {},
) {
    CompactTextField(
        value = value,
        onChange = onChange,
        onImeAction = onSearch,
        continuousUpdate = continuousSearch,
        color = color,
        textStyle = textStyle,
        placeholderText = placeholderText,
        onFocusChanged = onFocusChanged,
        modifier = modifier,
        enabled = enabled,
        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
        trailingIcon = { searchTerm ->
            Icon(
                imageVector = Icons.Sharp.Search,
                contentDescription = null,
                modifier = Modifier.size(30.dp).padding(start = 5.dp).clickable { onSearch(searchTerm) },
                tint = MaterialTheme.colorScheme.outline,
            )
        },
    )
}
