package us.huseli.thoucylinder.compose.utils

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> AutocompleteTextField(
    initial: T?,
    getSuggestions: suspend (String) -> List<T>,
    itemToString: (T?) -> String,
    onSelect: (T) -> Unit,
    onTextChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    label: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    var currentText by remember(initial) { mutableStateOf(itemToString(initial)) }
    val isFocused by interactionSource.collectIsFocusedAsState()
    var suggestions by remember { mutableStateOf<List<T>>(emptyList()) }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(currentText) {
        suggestions = getSuggestions(currentText)
    }

    ExposedDropdownMenuBox(
        expanded = isFocused,
        onExpandedChange = {},
    ) {
        OutlinedTextField(
            value = currentText,
            label = label,
            singleLine = singleLine,
            interactionSource = interactionSource,
            onValueChange = {
                currentText = it
                onTextChange(it)
            },
            modifier = modifier.menuAnchor(MenuAnchorType.PrimaryEditable),
        )
        ExposedDropdownMenu(
            expanded = isFocused && suggestions.isNotEmpty(),
            onDismissRequest = {},
        ) {
            suggestions.forEachIndexed { index, suggestion ->
                val text = itemToString(suggestion)

                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .clickable {
                            focusManager.clearFocus()
                            currentText = text
                            onSelect(suggestion)
                        },
                    content = {
                        Text(
                            text = text,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                )
                if (index < suggestions.lastIndex) HorizontalDivider()
            }
        }
    }
}


@Composable
fun AutocompleteTextField(
    initial: String?,
    getSuggestions: suspend (String) -> List<String>,
    onSelect: (String) -> Unit,
    onTextChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    label: @Composable () -> Unit,
) {
    AutocompleteTextField(
        initial = initial,
        getSuggestions = getSuggestions,
        itemToString = { it ?: "" },
        onSelect = onSelect,
        onTextChange = onTextChange,
        modifier = modifier,
        singleLine = singleLine,
        label = label,
    )
}
