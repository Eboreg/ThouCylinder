package us.huseli.thoucylinder.compose.utils

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup

@Composable
fun <T> AutocompleteTextField(
    initial: T?,
    getSuggestions: (String) -> Collection<T>,
    itemToString: (T?) -> String,
    onSelect: (T) -> Unit,
    onTextChange: (String) -> Unit,
    totalAreaHeight: Dp,
    modifier: Modifier = Modifier,
    rootOffsetY: Dp = 0.dp,
    singleLine: Boolean = true,
    label: @Composable (() -> Unit)? = null,
) {
    val density = LocalDensity.current
    var textFieldValue by remember { mutableStateOf(TextFieldValue(itemToString(initial))) }
    val focusRequester = remember { FocusRequester() }
    var showPopup by rememberSaveable { mutableStateOf(false) }
    var textFieldPositionY by remember { mutableStateOf(0.dp) }
    var textFieldSize by remember { mutableStateOf(DpSize.Zero) }
    var suggestions by remember(textFieldValue) { mutableStateOf(getSuggestions(textFieldValue.text)) }
    val maxPopupHeight by remember(totalAreaHeight, rootOffsetY, textFieldPositionY, textFieldSize) {
        mutableStateOf(totalAreaHeight - rootOffsetY - textFieldPositionY - textFieldSize.height)
    }
    val popupInnerPadding = PaddingValues(horizontal = 16.dp, vertical = 5.dp)
    var popupOffsetY by remember { mutableIntStateOf(0) }

    OutlinedTextField(
        value = textFieldValue,
        label = label,
        singleLine = singleLine,
        onValueChange = {
            textFieldValue = it
            suggestions = getSuggestions(it.text)
            showPopup = suggestions.isNotEmpty()
            onTextChange(it.text)
        },
        modifier = modifier
            .onGloballyPositioned { coords ->
                with(density) {
                    textFieldSize = coords.boundsInRoot().let { DpSize(it.width.toDp(), it.height.toDp()) }
                    textFieldPositionY = coords.positionInRoot().y.toDp()
                    popupOffsetY = coords.boundsInRoot().height.toInt()
                }
            }
            .focusRequester(focusRequester)
            .onFocusChanged { showPopup = it.isFocused },
    )

    if (showPopup) {
        Popup(offset = IntOffset(0, popupOffsetY)) {
            Card(
                elevation = CardDefaults.outlinedCardElevation(),
                modifier = Modifier
                    .heightIn(max = maxPopupHeight)
                    .width(textFieldSize.width),
                shape = MaterialTheme.shapes.extraSmall,
            ) {
                Column(modifier = Modifier.padding(popupInnerPadding)) {
                    suggestions.forEach {
                        Row(
                            modifier = Modifier
                                .padding(vertical = 5.dp)
                                .clickable {
                                    val text = itemToString(it)
                                    showPopup = false
                                    textFieldValue = TextFieldValue(text = text, selection = TextRange(text.length))
                                    onSelect(it)
                                },
                            content = {
                                Text(text = itemToString(it), color = MaterialTheme.colorScheme.onPrimaryContainer)
                            },
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun AutocompleteTextField(
    initial: String?,
    getSuggestions: (String) -> Collection<String>,
    onSelect: (String) -> Unit,
    onTextChange: (String) -> Unit,
    totalAreaHeight: Dp,
    modifier: Modifier = Modifier,
    rootOffsetY: Dp = 0.dp,
    singleLine: Boolean = true,
    label: @Composable (() -> Unit)? = null,
) {
    AutocompleteTextField(
        initial = initial,
        getSuggestions = getSuggestions,
        itemToString = { it ?: "" },
        onSelect = onSelect,
        onTextChange = onTextChange,
        totalAreaHeight = totalAreaHeight,
        modifier = modifier,
        rootOffsetY = rootOffsetY,
        singleLine = singleLine,
        label = label,
    )
}
