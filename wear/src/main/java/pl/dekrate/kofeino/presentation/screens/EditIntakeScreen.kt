package pl.dekrate.kofeino.presentation.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import pl.dekrate.kofeino.R
import pl.dekrate.kofeino.presentation.viewmodel.CaffeineViewModel
import pl.dekrate.kofeino.common.domain.model.CaffeineIntake

@Composable
fun EditIntakeScreen(
    intakeId: Long,
    onDeleted: () -> Unit,
    onSaved: () -> Unit,
    viewModel: CaffeineViewModel = hiltViewModel()
) {
    // Ładujemy intake bezpośrednio z DB, a nie z listy bieżącego dnia
    var intake by remember { mutableStateOf<CaffeineIntake?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(intakeId) {
        isLoading = true
        intake = viewModel.getIntakeById(intakeId)
        isLoading = false
    }

    if (isLoading) {
        ScreenScaffold {
            CircularProgressIndicator(modifier = Modifier.padding(16.dp))
        }
        return
    }

    if (intake == null) {
        ScreenScaffold {
            Text(
                text = stringResource(R.string.intake_not_found),
                modifier = Modifier.padding(16.dp)
            )
        }
        return
    }

    val currentIntake = intake!!
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()

    // Show Toast on errors
    LaunchedEffect(state.error) {
        state.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

    // Pre-resolve strings for accessibility
    val caffeineDesc = stringResource(R.string.caffeine_amount)
    val saveDesc = stringResource(R.string.save)
    val deleteDesc = stringResource(R.string.delete)
    val cancelDesc = stringResource(R.string.cancel)
    val confirmDesc = stringResource(R.string.confirm)
    val volumeDecDesc = stringResource(R.string.volume_decrease)
    val volumeIncDesc = stringResource(R.string.volume_increase)

    var caffeineMg by remember(currentIntake.id) { mutableIntStateOf(currentIntake.caffeineMg) }
    var volumeMl by remember(currentIntake.id) { mutableIntStateOf(currentIntake.volumeMl) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }

    ScreenScaffold {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Top),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = currentIntake.drinkName,
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = stringResource(R.string.caffeine_label, caffeineMg),
                style = MaterialTheme.typography.bodyMedium
            )

            // Caffeine adjustment coarse ±5
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = { if (caffeineMg >= CaffeineCoarseStepMg) caffeineMg -= CaffeineCoarseStepMg },
                    enabled = caffeineMg >= CaffeineCoarseStepMg,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 4.dp)
                        .semantics { contentDescription = "$caffeineDesc -5" }
                ) {
                    Text(stringResource(R.string.caffeine_adjustment_decrease, CaffeineCoarseStepMg))
                }
                Button(
                    onClick = { caffeineMg += CaffeineCoarseStepMg },
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp)
                        .semantics { contentDescription = "$caffeineDesc +5" }
                ) {
                    Text(stringResource(R.string.caffeine_adjustment_increase, CaffeineCoarseStepMg))
                }
            }

            // Caffeine adjustment fine ±1
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = { if (caffeineMg >= CaffeineFineStepMg) caffeineMg -= CaffeineFineStepMg },
                    enabled = caffeineMg >= CaffeineFineStepMg,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 4.dp)
                        .semantics { contentDescription = "$caffeineDesc -1" }
                ) {
                    Text(stringResource(R.string.caffeine_adjustment_decrease, CaffeineFineStepMg))
                }
                Button(
                    onClick = { caffeineMg += CaffeineFineStepMg },
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp)
                        .semantics { contentDescription = "$caffeineDesc +1" }
                ) {
                    Text(stringResource(R.string.caffeine_adjustment_increase, CaffeineFineStepMg))
                }
            }

            // Volume adjustment buttons
            Text(
                text = "${stringResource(R.string.volume)}: ${volumeMl}ml",
                style = MaterialTheme.typography.bodySmall
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
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

            // Save button — blokada przed double-click, callback po zapisie do DB
            Button(
                onClick = {
                    if (!isSaving) {
                        isSaving = true
                        viewModel.updateIntake(
                            currentIntake.copy(caffeineMg = caffeineMg, volumeMl = volumeMl),
                            onComplete = {
                                Toast.makeText(context, R.string.drink_added, Toast.LENGTH_SHORT).show()
                                onSaved()
                            },
                            onError = { isSaving = false }
                        )
                    }
                },
                enabled = !isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = saveDesc },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.padding(end = 4.dp))
                }
                Text(if (isSaving) stringResource(R.string.saving) else stringResource(R.string.save))
            }

            // Delete button
            if (!showDeleteConfirm) {
                Button(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = deleteDesc },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.delete))
                }
            } else {
                Text(
                    text = stringResource(R.string.delete_intake_confirm),
                    style = MaterialTheme.typography.labelMedium
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = { showDeleteConfirm = false },
                        modifier = Modifier.semantics { contentDescription = cancelDesc }
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                    Button(
                        onClick = {
                            if (!isDeleting) {
                                isDeleting = true
                                viewModel.deleteIntake(
                                    currentIntake,
                                    onComplete = {
                                        Toast.makeText(context, R.string.delete_intake, Toast.LENGTH_SHORT).show()
                                        onDeleted()
                                    },
                                    onError = { isDeleting = false }
                                )
                            }
                        },
                        enabled = !isDeleting,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.semantics { contentDescription = "$confirmDesc $deleteDesc" }
                    ) {
                        if (isDeleting) {
                            CircularProgressIndicator(modifier = Modifier.padding(end = 4.dp))
                        }
                        Text(if (isDeleting) stringResource(R.string.deleting) else stringResource(R.string.confirm))
                    }
                }
            }
        }
    }
}

private const val CaffeineCoarseStepMg = 5
private const val CaffeineFineStepMg = 1
