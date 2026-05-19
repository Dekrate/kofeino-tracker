package pl.dekrate.kofeino.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import pl.dekrate.kofeino.R

/**
 * Formularz dodawania/edycji napoju – wyświetlany jako overlay.
 * Używa przycisków +/- do zmiany wartości (przyjazne dla Wear OS).
 */
@Composable
fun AddEditDrinkForm(
    initialName: String,
    initialCaffeineMg: Int,
    initialVolumeMl: Int,
    isEditing: Boolean,
    onDismiss: () -> Unit,
    onSave: (name: String, caffeineMg: Int, volumeMl: Int) -> Unit,
    onDelete: (() -> Unit)?
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    var caffeineMg by remember(initialCaffeineMg) { mutableIntStateOf(initialCaffeineMg) }
    var volumeMl by remember(initialVolumeMl) { mutableIntStateOf(initialVolumeMl) }

    val isNameValid = name.isNotBlank()
    val isCaffeineValid = caffeineMg >= 0
    val isVolumeValid = volumeMl >= 0
    val isFormValid = isNameValid && isCaffeineValid && isVolumeValid
    val caffeineAmountDesc = stringResource(R.string.caffeine_amount)
    val volumeDecDesc = stringResource(R.string.volume_decrease)
    val volumeIncDesc = stringResource(R.string.volume_increase)

    Column(
        modifier = Modifier.fillMaxWidth().padding(8.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Title
        Text(
            text = if (isEditing) stringResource(R.string.edit_drink) else stringResource(R.string.add_new_drink),
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Drink name
        Text(
            text = stringResource(R.string.drink_name),
            style = MaterialTheme.typography.labelMedium
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = MaterialTheme.shapes.small
                )
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            if (name.isEmpty()) {
                Text(
                    text = stringResource(R.string.drink_name_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            BasicTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None
                )
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Caffeine mg
        Text(
            text = "${stringResource(R.string.caffeine_amount)}: $caffeineMg",
            style = MaterialTheme.typography.labelMedium
        )
        // Coarse adjustment (±5)
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { if (caffeineMg >= 5) caffeineMg -= 5 },
                enabled = caffeineMg >= 5,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 4.dp)
                    .semantics { contentDescription = "$caffeineAmountDesc -5" }
            ) {
                Text(stringResource(R.string.caffeine_adjustment_decrease, 5))
            }
            Button(
                onClick = { caffeineMg += 5 },
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp)
                    .semantics { contentDescription = "$caffeineAmountDesc +5" }
            ) {
                Text(stringResource(R.string.caffeine_adjustment_increase, 5))
            }
        }
        // Fine adjustment (±1)
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { if (caffeineMg >= 1) caffeineMg -= 1 },
                enabled = caffeineMg >= 1,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 4.dp)
                    .semantics { contentDescription = "$caffeineAmountDesc -1" }
            ) {
                Text(stringResource(R.string.caffeine_adjustment_decrease_fine))
            }
            Button(
                onClick = { caffeineMg += 1 },
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp)
                    .semantics { contentDescription = "$caffeineAmountDesc +1" }
            ) {
                Text(stringResource(R.string.caffeine_adjustment_increase_fine))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Volume ml
        Text(
            text = "${stringResource(R.string.volume)}: $volumeMl ml",
            style = MaterialTheme.typography.labelMedium
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { if (volumeMl >= 10) volumeMl -= 10 },
                enabled = volumeMl >= 10,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 4.dp)
                    .semantics { contentDescription = volumeDecDesc }
            ) {
                Text("-10")
            }
            Button(
                onClick = { volumeMl += 10 },
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp)
                    .semantics { contentDescription = volumeIncDesc }
            ) {
                Text("+10")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Validation feedback
        if (!isNameValid) {
            Text(
                text = stringResource(R.string.error_name_required),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error
            )
        }
        if (!isCaffeineValid) {
            Text(
                text = stringResource(R.string.error_caffeine_invalid),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        // Save
        Button(
            onClick = { onSave(name.trim(), caffeineMg, volumeMl) },
            modifier = Modifier.fillMaxWidth(),
            enabled = isFormValid,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(stringResource(R.string.save))
        }

        // Delete (only for non-default drinks in edit mode)
        if (onDelete != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = onDelete,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(stringResource(R.string.delete))
            }
        }

        // Cancel
        Spacer(modifier = Modifier.height(4.dp))
        Button(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.cancel))
        }
    }
}
