package pl.dekrate.kofeino.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.OutlinedButton
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import pl.dekrate.kofeino.R
import pl.dekrate.kofeino.domain.model.DrinkEntity

/**
 * Full-screen confirmation dialog shown after tapping a drink in [AddDrinkScreen].
 * Allows adjusting caffeine (stepper +/-5) and volume (stepper +/-10)
 * before logging the intake.
 *
 * @param drink The selected drink with default values.
 * @param isLogging True while the log operation is in progress (prevents double-click).
 * @param onLogDrink Called with adjusted caffeineMg and volumeMl when user confirms.
 * @param onCancel Called when user cancels.
 */
@Composable
fun AddDrinkConfirmationContent(
    drink: DrinkEntity,
    isLogging: Boolean,
    onLogDrink: (caffeineMg: Int, volumeMl: Int) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var caffeineMg by remember(drink) { mutableIntStateOf(drink.caffeineMg) }
    var volumeMl by remember(drink) { mutableIntStateOf(drink.volumeMl) }

    // Pre-resolve strings for accessibility (semantics blocks are not @Composable)
    val caffeineDesc = stringResource(R.string.caffeine_amount)
    val volumeDecDesc = stringResource(R.string.volume_decrease)
    val volumeIncDesc = stringResource(R.string.volume_increase)
    val logDrinkDesc = stringResource(R.string.log_drink)
    val cancelDesc = stringResource(R.string.cancel)

    ScreenScaffold(modifier = modifier) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Drink name — prominent header
            item {
                Text(
                    text = drink.name,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            // Subtitle
            item {
                Text(
                    text = stringResource(R.string.adjust_serving),
                    style = MaterialTheme.typography.labelMedium
                )
            }

            // Caffeine display
            item {
                Text(
                    text = stringResource(R.string.caffeine_label, caffeineMg),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Caffeine stepper coarse +/- 5
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = { if (caffeineMg >= 5) caffeineMg -= 5 },
                        enabled = !isLogging && caffeineMg >= 5,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 4.dp)
                            .semantics { contentDescription = "$caffeineDesc -5" }
                    ) {
                        Text(stringResource(R.string.caffeine_adjustment_decrease, 5))
                    }
                    Button(
                        onClick = { caffeineMg += 5 },
                        enabled = !isLogging,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 4.dp)
                            .semantics { contentDescription = "$caffeineDesc +5" }
                    ) {
                        Text(stringResource(R.string.caffeine_adjustment_increase, 5))
                    }
                }
            }

            // Caffeine stepper fine +/- 1
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = { if (caffeineMg >= 1) caffeineMg -= 1 },
                        enabled = !isLogging && caffeineMg >= 1,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 4.dp)
                            .semantics { contentDescription = "$caffeineDesc -1" }
                    ) {
                        Text(stringResource(R.string.caffeine_adjustment_decrease_fine))
                    }
                    Button(
                        onClick = { caffeineMg += 1 },
                        enabled = !isLogging,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 4.dp)
                            .semantics { contentDescription = "$caffeineDesc +1" }
                    ) {
                        Text(stringResource(R.string.caffeine_adjustment_increase_fine))
                    }
                }
            }

            // Volume display
            item {
                Text(
                    text = "${stringResource(R.string.volume)}: ${volumeMl}ml",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Volume stepper +/- 10
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = { if (volumeMl >= 10) volumeMl -= 10 },
                        enabled = !isLogging && volumeMl >= 10,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 4.dp)
                            .semantics { contentDescription = volumeDecDesc }
                    ) {
                        Text("-10")
                    }
                    Button(
                        onClick = { volumeMl += 10 },
                        enabled = !isLogging,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 4.dp)
                            .semantics { contentDescription = volumeIncDesc }
                    ) {
                        Text("+10")
                    }
                }
            }

            // Log drink button — primary action, disables during save
            item {
                Button(
                    onClick = { onLogDrink(caffeineMg, volumeMl) },
                    enabled = !isLogging,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = logDrinkDesc },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    if (isLogging) {
                        CircularProgressIndicator(modifier = Modifier.padding(end = 4.dp))
                    }
                    Text(
                        if (isLogging) stringResource(R.string.saving)
                        else stringResource(R.string.log_drink)
                    )
                }
            }

            // Cancel button — dismisses the dialog
            item {
                OutlinedButton(
                    onClick = onCancel,
                    enabled = !isLogging,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = cancelDesc }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    }
}
