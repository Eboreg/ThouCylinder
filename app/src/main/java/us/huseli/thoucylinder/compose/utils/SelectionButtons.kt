package us.huseli.thoucylinder.compose.utils

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.MoreVert
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import us.huseli.thoucylinder.stringResource

@Immutable
data class SelectionAction(
    val icon: ImageVector,
    val description: Int,
    val onClick: () -> Unit,
    val alwaysShowButton: Boolean = false,
    val showDescriptionOnButton: Boolean = false,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionButtons(
    actions: ImmutableList<SelectionAction>,
    itemCount: () -> Int,
    tonalElevation: Dp = 2.dp,
    maxButtonCount: Int = 3,
) {
    var isBottomSheetVisible by rememberSaveable { mutableStateOf(false) }
    val buttonActions = remember(actions, maxButtonCount) {
        val mutableButtonActions = actions.subList(0, maxButtonCount).toMutableList()

        actions.minus(mutableButtonActions.toSet()).filter { it.alwaysShowButton }.forEach { action ->
            if (mutableButtonActions.size >= maxButtonCount) {
                val removeIdx = mutableButtonActions.indexOfLast { !it.alwaysShowButton }
                if (removeIdx > -1) mutableButtonActions.removeAt(removeIdx)
            }
            mutableButtonActions.add(action)
        }
        mutableButtonActions.toList()
    }
    val menuActions = remember(buttonActions) { actions.minus(buttonActions.toSet()) }

    AnimatedSection(visible = itemCount() > 0, tonalElevation = tonalElevation) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Badge(
                    modifier = Modifier.height(32.dp),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    content = { Text(itemCount().toString(), fontWeight = FontWeight.Bold) },
                )

                for (action in buttonActions) {
                    SmallOutlinedButton(
                        onClick = action.onClick,
                        content = {
                            Icon(action.icon, stringResource(action.description))
                            if (action.showDescriptionOnButton) {
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(stringResource(action.description))
                            }
                        },
                    )
                }
            }

            if (menuActions.size < actions.size) {
                IconButton(
                    onClick = { isBottomSheetVisible = !isBottomSheetVisible },
                    content = { Icon(Icons.Sharp.MoreVert, null) },
                )
                if (isBottomSheetVisible) ModalBottomSheet(onDismissRequest = { isBottomSheetVisible = false }) {
                    for (action in actions) {
                        BottomSheetItem(
                            icon = action.icon,
                            text = stringResource(action.description),
                            onClick = {
                                action.onClick()
                                isBottomSheetVisible = false
                            }
                        )
                    }
                }
            }
        }
    }
}
