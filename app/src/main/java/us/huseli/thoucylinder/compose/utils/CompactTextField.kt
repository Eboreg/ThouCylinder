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
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import us.huseli.thoucylinder.compose.FistopyTheme

@Composable
fun CompactTextField(
    value: () -> String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    continuousUpdate: Boolean = false,
    color: Color = MaterialTheme.colorScheme.onSurface,
    textStyle: TextStyle = FistopyTheme.typography.labelLarge.copy(color = color),
    placeholderText: String? = null,
    showClearIcon: Boolean = true,
    height: Dp = 40.dp,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    focusRequester: FocusRequester = remember { FocusRequester() },
    trailingIcon: @Composable (String) -> Unit = {},
    onValueChange: (String) -> Unit = {},
    onImeAction: (String) -> Unit = {},
    onFocusChange: (FocusState) -> Unit = {},
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Go),
) {
    var isFocused by remember { mutableStateOf(false) }
    var mutableValue by remember(value) { mutableStateOf(value()) }

    BasicTextField(
        value = mutableValue,
        interactionSource = interactionSource,
        onValueChange = {
            mutableValue = it
            onValueChange(it)
            if (continuousUpdate) onImeAction(it)
        },
        modifier = modifier
            .height(height)
            .padding(0.dp)
            .onFocusChanged {
                isFocused = it.isFocused
                onFocusChange(it)
            }
            .focusRequester(focusRequester),
        singleLine = true,
        enabled = enabled,
        textStyle = textStyle,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
        keyboardOptions = keyboardOptions,
        keyboardActions = KeyboardActions(
            onSearch = { onImeAction(mutableValue) },
            onSend = { onImeAction(mutableValue) },
            onDone = { onImeAction(mutableValue) },
            onGo = { onImeAction(mutableValue) },
        ),
        decorationBox = { innerTextField ->
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    Box(modifier = Modifier.weight(1f).align(Alignment.CenterVertically)) {
                        innerTextField()
                        if (!isFocused && mutableValue.isEmpty() && placeholderText != null) {
                            Text(placeholderText, style = textStyle, color = color.copy(alpha = 0.5f))
                        }
                    }
                    if (mutableValue.isNotEmpty() && showClearIcon) {
                        Icon(
                            imageVector = Icons.Sharp.Clear,
                            contentDescription = null,
                            modifier = Modifier.size(30.dp).padding(start = 5.dp).clickable {
                                mutableValue = ""
                                onValueChange(mutableValue)
                            },
                            tint = MaterialTheme.colorScheme.outline,
                        )
                    }
                    trailingIcon(mutableValue)
                }
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    color = if (isFocused) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outlineVariant,
                )
            }
        },
    )
}

@Composable
fun CompactSearchTextField(
    value: () -> String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    continuousSearch: Boolean = false,
    color: Color = MaterialTheme.colorScheme.onSurface,
    textStyle: TextStyle = FistopyTheme.typography.labelLarge.copy(color = color),
    placeholderText: String? = null,
    onFocusChanged: (FocusState) -> Unit = {},
    onChange: (String) -> Unit = {},
    onSearch: (String) -> Unit = {},
) {
    CompactTextField(
        value = value,
        continuousUpdate = continuousSearch,
        color = color,
        enabled = enabled,
        textStyle = textStyle,
        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
        placeholderText = placeholderText,
        onFocusChange = onFocusChanged,
        onValueChange = onChange,
        onImeAction = onSearch,
        modifier = modifier,
        trailingIcon = {
            Icon(
                imageVector = Icons.Sharp.Search,
                contentDescription = null,
                modifier = Modifier.size(30.dp).padding(start = 5.dp).clickable { onSearch(it) },
                tint = MaterialTheme.colorScheme.outline,
            )
        },
    )
}
