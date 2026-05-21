package pl.dekrate.kofeino.tracker.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import pl.dekrate.kofeino.tracker.R
import pl.dekrate.kofeino.tracker.domain.model.DrinkEntity
import pl.dekrate.kofeino.tracker.presentation.viewmodel.DrinkError
import pl.dekrate.kofeino.tracker.presentation.viewmodel.DrinkViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDrinkScreen(
    onNavigateBack: () -> Unit,
    viewModel: DrinkViewModel = hiltViewModel()
) {
    val drinks by viewModel.allDrinks.collectAsState()
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val drinkAddedText = stringResource(R.string.drink_added)

    // Confirmation state
    var selectedDrink by remember { mutableStateOf<DrinkEntity?>(null) }
    var isLogging by remember { mutableStateOf(false) }

    // Resolve error message to localized string in @Composable context
    val errorMessage = state.error?.let { error ->
        when (error) {
            DrinkError.AddIntakeFailed -> stringResource(R.string.error_add_failed)
        }
    }

    // Show snackbar on error
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (selectedDrink != null) stringResource(R.string.adjust_serving)
                        else stringResource(R.string.select_drink)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (selectedDrink != null) {
                            selectedDrink = null
                        } else {
                            onNavigateBack()
                        }
                    }) {
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
        val drink = selectedDrink
        if (drink != null) {
            // Confirmation content
            AddDrinkConfirmationContent(
                drink = drink,
                isLogging = isLogging,
                onLogDrink = { caffeineMg, volumeMl ->
                    isLogging = true
                    val adjustedDrink = drink.copy(caffeineMg = caffeineMg, volumeMl = volumeMl)
                    viewModel.logDrink(
                        drink = adjustedDrink,
                        onComplete = {
                            isLogging = false
                            selectedDrink = null
                            scope.launch {
                                snackbarHostState.showSnackbar(drinkAddedText)
                            }
                        },
                        onError = {
                            isLogging = false
                        }
                    )
                },
                onCancel = { selectedDrink = null },
                modifier = Modifier.padding(innerPadding)
            )
        } else if (drinks.isEmpty()) {
            // Empty state
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.no_drinks_defined),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(
                    horizontal = 16.dp,
                    vertical = 8.dp
                )
            ) {
                items(drinks, key = { it.id }) { drinkItem ->
                    DrinkItem(
                        name = drinkItem.name,
                        caffeineMg = drinkItem.caffeineMg,
                        volumeMl = drinkItem.volumeMl,
                        onClick = { selectedDrink = drinkItem }
                    )
                }
            }
        }
    }
}

/**
 * Full-screen confirmation content shown after tapping a drink in [AddDrinkScreen].
 * Allows adjusting caffeine (+/-5, +/-1) and volume (+/-10) before logging.
 */
@Composable
private fun AddDrinkConfirmationContent(
    drink: DrinkEntity,
    isLogging: Boolean,
    onLogDrink: (caffeineMg: Int, volumeMl: Int) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var caffeineMg by remember { mutableIntStateOf(drink.caffeineMg) }
    var volumeMl by remember { mutableIntStateOf(drink.volumeMl) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Top),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Drink name
        Text(
            text = drink.name,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        // Caffeine section
        Text(
            text = stringResource(R.string.caffeine_label, caffeineMg),
            style = MaterialTheme.typography.titleMedium
        )
        CaffeineStepper(
            caffeineMg = caffeineMg,
            onCaffeineChange = { delta -> caffeineMg += delta }
        )

        // Volume section
        Text(
            text = stringResource(R.string.volume_label, volumeMl),
            style = MaterialTheme.typography.titleMedium
        )
        VolumeStepper(
            volumeMl = volumeMl,
            onVolumeChange = { delta -> volumeMl += delta }
        )

        Spacer(modifier = Modifier.weight(1f))

        // Log drink button
        Button(
            onClick = { onLogDrink(caffeineMg, volumeMl) },
            enabled = !isLogging,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLogging) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(end = 8.dp),
                    strokeWidth = 2.dp
                )
            }
            Text(
                if (isLogging) stringResource(R.string.saving)
                else stringResource(R.string.log_drink)
            )
        }

        // Cancel button
        OutlinedButton(
            onClick = onCancel,
            enabled = !isLogging,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.cancel))
        }
    }
}

private const val CaffeineCoarseStepMg = 5
private const val CaffeineFineStepMg = 1
private const val VolumeStepMl = 10

@Composable
private fun CaffeineStepper(
    caffeineMg: Int,
    onCaffeineChange: (Int) -> Unit,
) {
    Column {
        // Coarse adjustment (±5 mg)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            OutlinedButton(
                onClick = { onCaffeineChange(-CaffeineCoarseStepMg) },
                enabled = caffeineMg >= CaffeineCoarseStepMg
            ) {
                Text(stringResource(R.string.caffeine_adjustment_decrease, CaffeineCoarseStepMg))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Button(
                onClick = { onCaffeineChange(CaffeineCoarseStepMg) }
            ) {
                Text(stringResource(R.string.caffeine_adjustment_increase, CaffeineCoarseStepMg))
            }
        }
        // Fine adjustment (±1 mg)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            OutlinedButton(
                onClick = { onCaffeineChange(-CaffeineFineStepMg) },
                enabled = caffeineMg >= CaffeineFineStepMg
            ) {
                Text(stringResource(R.string.caffeine_adjustment_decrease, CaffeineFineStepMg))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Button(
                onClick = { onCaffeineChange(CaffeineFineStepMg) }
            ) {
                Text(stringResource(R.string.caffeine_adjustment_increase, CaffeineFineStepMg))
            }
        }
    }
}

@Composable
private fun VolumeStepper(
    volumeMl: Int,
    onVolumeChange: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        OutlinedButton(
            onClick = { onVolumeChange(-VolumeStepMl) },
            enabled = volumeMl >= VolumeStepMl
        ) {
            Text("-10")
        }
        Spacer(modifier = Modifier.width(16.dp))
        Button(
            onClick = { onVolumeChange(VolumeStepMl) }
        ) {
            Text("+10")
        }
    }
}

@Composable
private fun DrinkItem(
    name: String,
    caffeineMg: Int,
    volumeMl: Int,
    onClick: () -> Unit
) {
    val caffeineDesc = stringResource(R.string.drink_caffeine_unit, caffeineMg)
    val volumeDesc = stringResource(R.string.drink_volume_unit, volumeMl)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = "$name, $caffeineDesc, $volumeDesc"
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.drink_volume_unit, volumeMl),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = stringResource(R.string.drink_caffeine_unit, caffeineMg),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
