package us.huseli.thoucylinder.compose

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import us.huseli.thoucylinder.R

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SearchForm(
    modifier: Modifier = Modifier,
    isSearching: Boolean,
    initialQuery: String,
    onSearch: (String) -> Unit,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    var query by rememberSaveable(initialQuery) { mutableStateOf(initialQuery) }

    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = query,
            enabled = !isSearching,
            onValueChange = { query = it },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text(
                    text = stringResource(R.string.search_youtube),
                    color = MaterialTheme.colorScheme.outline,
                    style = MaterialTheme.typography.labelLarge,
                )
            },
            textStyle = MaterialTheme.typography.labelLarge,
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Search,
            ),
            keyboardActions = KeyboardActions(
                onSearch = {
                    if (!isSearching && query.length >= 3) {
                        onSearch(query)
                        keyboardController?.hide()
                    }
                },
            ),
            trailingIcon = {
                IconButton(
                    onClick = { onSearch(query) },
                    enabled = !isSearching && query.length >= 3,
                    content = {
                        Icon(
                            imageVector = Icons.Sharp.Search,
                            contentDescription = stringResource(R.string.search),
                        )
                    }
                )
            },
        )
    }
}
