package us.huseli.thoucylinder.compose.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Search
import androidx.compose.material.icons.sharp.UnfoldLess
import androidx.compose.material.icons.sharp.UnfoldMore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.compose.utils.CompactTextField
import us.huseli.thoucylinder.externalcontent.ExternalListType
import us.huseli.thoucylinder.externalcontent.SearchCapability
import us.huseli.thoucylinder.externalcontent.SearchParams
import us.huseli.thoucylinder.stringResource

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchFieldSection(
    capabilities: List<SearchCapability>,
    currentValues: SearchParams,
    listType: ExternalListType,
    onSearch: (SearchParams) -> Unit,
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    var artist by rememberSaveable(currentValues) { mutableStateOf(currentValues.artist ?: "") }
    var album by rememberSaveable(currentValues) { mutableStateOf(currentValues.album ?: "") }
    var track by rememberSaveable(currentValues) { mutableStateOf(currentValues.track ?: "") }
    var freeText by rememberSaveable(currentValues) { mutableStateOf(currentValues.freeText ?: "") }

    val isUnfoldable =
        remember(capabilities) { capabilities.contains(SearchCapability.FREE_TEXT) && capabilities.size > 1 }
    var isUnfolded by rememberSaveable(capabilities) { mutableStateOf(!capabilities.contains(SearchCapability.FREE_TEXT)) }
    val params = remember(artist, album, track, freeText, isUnfolded) {
        SearchParams(
            artist = artist.takeIf { it.isNotBlank() && isUnfolded },
            album = album.takeIf { it.isNotBlank() && isUnfolded },
            freeText = freeText.takeIf { it.isNotBlank() && !isUnfolded },
            track = track.takeIf { it.isNotBlank() && isUnfolded },
        )
    }
    val canSearch = remember(params) { params.isNotEmpty() }

    val search: () -> Unit = {
        onSearch(params)
        keyboardController?.hide()
    }

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                maxItemsInEachRow = 2,
            ) {
                if (isUnfolded) {
                    when (listType) {
                        ExternalListType.ALBUMS -> {
                            if (capabilities.contains(SearchCapability.ALBUM)) CompactTextField(
                                value = { album },
                                placeholderText = stringResource(R.string.title),
                                onValueChange = { album = it },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
                                onImeAction = { search() },
                            )
                            if (capabilities.contains(SearchCapability.TRACK)) CompactTextField(
                                value = { track },
                                placeholderText = stringResource(R.string.track),
                                onValueChange = { track = it },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
                                onImeAction = { search() },
                            )
                        }
                        ExternalListType.TRACKS -> {
                            if (capabilities.contains(SearchCapability.TRACK)) CompactTextField(
                                value = { track },
                                placeholderText = stringResource(R.string.title),
                                onValueChange = { track = it },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
                                onImeAction = { search() },
                            )
                            if (capabilities.contains(SearchCapability.ALBUM)) CompactTextField(
                                value = { album },
                                placeholderText = stringResource(R.string.album),
                                onValueChange = { album = it },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
                                onImeAction = { search() },
                            )
                        }
                    }
                    if (capabilities.contains(SearchCapability.ARTIST)) CompactTextField(
                        value = { artist },
                        placeholderText = stringResource(R.string.artist),
                        onValueChange = { artist = it },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
                        onImeAction = { search() },
                    )
                } else {
                    CompactTextField(
                        value = { freeText },
                        placeholderText = stringResource(R.string.free_text),
                        onValueChange = { freeText = it },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
                        onImeAction = { search() },
                    )
                }
            }
        }

        if (isUnfoldable) {
            IconButton(
                onClick = { isUnfolded = !isUnfolded },
                content = { Icon(if (isUnfolded) Icons.Sharp.UnfoldLess else Icons.Sharp.UnfoldMore, null) },
            )
        }

        IconButton(
            onClick = search,
            content = { Icon(Icons.Sharp.Search, contentDescription = null) },
            enabled = canSearch,
        )
    }
}
