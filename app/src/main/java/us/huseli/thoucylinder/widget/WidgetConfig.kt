package us.huseli.thoucylinder.widget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import us.huseli.thoucylinder.R
import us.huseli.thoucylinder.compose.FistopyTheme
import us.huseli.thoucylinder.compose.utils.SaveButton
import us.huseli.thoucylinder.managers.WidgetManager
import us.huseli.thoucylinder.stringResource

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WidgetConfig(manager: WidgetManager, onSave: (List<WidgetButton>) -> Unit) {
    val enabledButtons by manager.buttons.collectAsState()
    var currentEnabledButtons by remember { mutableStateOf(enabledButtons) }
    val toggleButton: (WidgetButton) -> Unit = { button ->
        if (!currentEnabledButtons.contains(button)) currentEnabledButtons += button
        else if (currentEnabledButtons.size > 1) currentEnabledButtons -= button
    }

    FistopyTheme {
        Scaffold { paddingValues ->
            Column(
                modifier = Modifier.fillMaxWidth().padding(paddingValues).padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(text = stringResource(R.string.widget_configuration), style = MaterialTheme.typography.titleLarge)

                Text(text = stringResource(R.string.buttons), style = MaterialTheme.typography.titleMedium)
                FlowRow(
                    maxItemsInEachRow = 2,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    for (button in WidgetButton.entries) {
                        InputChip(
                            selected = currentEnabledButtons.contains(button),
                            onClick = { toggleButton(button) },
                            label = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 5.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    Icon(painter = painterResource(button.drawable), contentDescription = null)
                                    Text(stringResource(button.title), style = MaterialTheme.typography.bodyLarge)
                                }
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    SaveButton(onClick = { onSave(currentEnabledButtons) })
                }
            }
        }
    }
}
