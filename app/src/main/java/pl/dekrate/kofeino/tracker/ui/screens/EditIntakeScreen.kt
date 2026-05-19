package pl.dekrate.kofeino.tracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import pl.dekrate.kofeino.tracker.R
import pl.dekrate.kofeino.tracker.presentation.viewmodel.EditIntakeError
import pl.dekrate.kofeino.tracker.presentation.viewmodel.EditIntakeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditIntakeScreen(
    intakeId: Long,
    onNavigateBack: () -> Unit,
    viewModel: EditIntakeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }

    // Load intake on first composition
    LaunchedEffect(intakeId) {
        viewModel.loadIntake(intakeId)
    }

    // Resolve error message to localized string in @Composable context
    val errorMessage = state.error?.let { error ->
        when (error) {
            EditIntakeError.NotFound -> stringResource(R.string.intake_not_found)
            EditIntakeError.LoadFailed -> stringResource(R.string.error_load_failed)
            EditIntakeError.SaveFailed -> stringResource(R.string.error_save_failed)
            EditIntakeError.DeleteFailed -> stringResource(R.string.error_delete_failed)
        }
    }

    // Show snackbar on error
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    // Title text based on current state
    val topBarTitle = when {
        state.isLoading || state.intake == null -> stringResource(R.string.edit_intake_title)
        else -> state.drinkName
    }

    // Single Scaffold covering all states so snackbarHost is always present
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = topBarTitle, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        when {
            state.isLoading -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                }
            }

            state.intake == null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.intake_not_found),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }
            }

            else -> {
                // --- Edit form ---
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // --- Caffeine section ---
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stringResource(R.string.caffeine_label, state.caffeineMg),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Coarse adjustment (±5 mg)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.updateCaffeineMg(-5) },
                                    enabled = state.caffeineMg >= 5,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(stringResource(R.string.caffeine_adjustment_decrease, 5))
                                }
                                Button(
                                    onClick = { viewModel.updateCaffeineMg(5) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(stringResource(R.string.caffeine_adjustment_increase, 5))
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Fine adjustment (±1 mg)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.updateCaffeineMg(-1) },
                                    enabled = state.caffeineMg >= 1,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(stringResource(R.string.caffeine_adjustment_decrease_fine))
                                }
                                Button(
                                    onClick = { viewModel.updateCaffeineMg(1) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(stringResource(R.string.caffeine_adjustment_increase_fine))
                                }
                            }
                        }
                    }

                    // --- Volume section ---
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stringResource(R.string.volume_label, state.volumeMl),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.updateVolumeMl(-10) },
                                    enabled = state.volumeMl >= 10,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(stringResource(R.string.volume_adjustment_decrease, 10))
                                }
                                Button(
                                    onClick = { viewModel.updateVolumeMl(10) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(stringResource(R.string.volume_adjustment_increase, 10))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    HorizontalDivider()

                    // --- Save button ---
                    Button(
                        onClick = {
                            if (!isSaving) {
                                isSaving = true
                                viewModel.save(
                                    onComplete = {
                                        isSaving = false
                                        onNavigateBack()
                                    },
                                    onError = { isSaving = false }
                                )
                            }
                        },
                        enabled = !isSaving,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.padding(end = 8.dp),
                                strokeWidth = 2.dp
                            )
                        }
                        Text(if (isSaving) stringResource(R.string.saving) else stringResource(R.string.save))
                    }

                    // --- Delete button ---
                    if (!showDeleteConfirm) {
                        Button(
                            onClick = { showDeleteConfirm = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text(stringResource(R.string.delete))
                        }
                    }

                    // --- Delete confirmation dialog ---
                    if (showDeleteConfirm) {
                        AlertDialog(
                            onDismissRequest = { showDeleteConfirm = false },
                            title = { Text(stringResource(R.string.delete_intake_confirm)) },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        if (!isDeleting) {
                                            isDeleting = true
                                            viewModel.delete(
                                                onComplete = {
                                                    isDeleting = false
                                                    showDeleteConfirm = false
                                                    onNavigateBack()
                                                },
                                                onError = { isDeleting = false }
                                            )
                                        }
                                    },
                                    enabled = !isDeleting,
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    if (isDeleting) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.padding(end = 4.dp),
                                            strokeWidth = 2.dp
                                        )
                                    }
                                    Text(
                                        text = if (isDeleting) stringResource(R.string.deleting)
                                        else stringResource(R.string.delete),
                                        modifier = Modifier.testTag("confirm_delete")
                                    )
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDeleteConfirm = false }) {
                                    Text(stringResource(R.string.cancel))
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
