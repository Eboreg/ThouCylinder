package us.huseli.thoucylinder.compose.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import us.huseli.thoucylinder.compose.utils.CancelButton
import us.huseli.thoucylinder.compose.utils.SaveButton

@Composable
fun SingleStringSettingDialog(
    currentValue: String?,
    placeholderText: String = "",
    title: @Composable () -> Unit = {},
    subtitle: @Composable () -> Unit = {},
    onCancelClick: () -> Unit,
    onDismissRequest: () -> Unit = onCancelClick,
    onSave: (String) -> Unit,
) {
    var value by rememberSaveable(currentValue) { mutableStateOf(currentValue ?: "") }

    AlertDialog(
        shape = MaterialTheme.shapes.small,
        onDismissRequest = onDismissRequest,
        dismissButton = { CancelButton(onClick = onCancelClick) },
        confirmButton = { SaveButton(onClick = { onSave(value) }) },
        title = title,
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                subtitle()
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    placeholder = { Text(text = placeholderText) },
                    singleLine = true,
                )
            }
        },
    )
}
