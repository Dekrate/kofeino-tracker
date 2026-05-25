package pl.dekrate.kofeino.tracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import pl.dekrate.kofeino.tracker.R
import pl.dekrate.kofeino.tracker.data.sync.SyncStatusTracker
import pl.dekrate.kofeino.common.domain.model.OfficialDrink
import pl.dekrate.kofeino.tracker.presentation.viewmodel.OfficialDrinksError
import pl.dekrate.kofeino.tracker.presentation.viewmodel.OfficialDrinksViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfficialDrinksScreen(
    syncStatusTracker: SyncStatusTracker,
    onNavigateBack: () -> Unit,
    viewModel: OfficialDrinksViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Resolve error message
    val errorMessage = state.error?.let { error ->
        when (error) {
            OfficialDrinksError.SearchFailed -> stringResource(R.string.error_search_failed)
            OfficialDrinksError.ImportFailed -> stringResource(R.string.import_error)
        }
    }

    // Show error in snackbar
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    // Pre-resolve strings (must be @Composable context)
    val importedText = stringResource(R.string.imported_as_drink)

    // Show import success in snackbar
    LaunchedEffect(state.successMessage) {
        state.successMessage?.let {
            snackbarHostState.showSnackbar(message = importedText)
            viewModel.clearSuccessMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.official_drinks_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    SyncStatusChip(syncStatusTracker = syncStatusTracker)
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search bar + search button
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = { viewModel.onQueryChanged(it) },
                    placeholder = { Text(stringResource(R.string.search_hint)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (state.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.clearSearch() }) {
                                Icon(
                                    imageVector = Icons.Filled.Clear,
                                    contentDescription = stringResource(R.string.search_clear)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        imeAction = ImeAction.Search
                    ),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                        onSearch = {
                            keyboardController?.hide()
                            viewModel.searchImmediate(state.searchQuery)
                        }
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Search button below the field (like watch: trigger even without IME)
                if (state.searchQuery.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = {
                                keyboardController?.hide()
                                viewModel.searchImmediate(state.searchQuery)
                            },
                            modifier = Modifier.weight(1f).padding(end = 4.dp)
                        ) {
                            Text(stringResource(R.string.search_hint).removeSuffix("…"))
                        }
                        OutlinedButton(
                            onClick = {
                                keyboardController?.hide()
                                viewModel.clearSearch()
                            },
                            modifier = Modifier.weight(1f).padding(start = 4.dp)
                        ) {
                            Text(stringResource(R.string.search_clear))
                        }
                    }
                }
            }

            HorizontalDivider()

            when {
                // Initial loading (first load, no results yet)
                state.isLoading && state.drinks.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.search_loading),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Background loading (refreshing search results)
                state.isLoading && state.drinks.isNotEmpty() -> {
                    OfficialDrinkList(
                        drinks = state.drinks,
                        importedDrinkNames = state.importedDrinkNames,
                        importingBarcode = state.importingBarcode,
                        onImport = { viewModel.importAsDrink(it) },
                        searchQuery = state.searchQuery,
                        isLoading = true
                    )
                }

                // Empty initial state (no search, no results)
                !state.isSearchMode && state.drinks.isEmpty() && !state.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.type_to_search),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(32.dp)
                        )
                    }
                }

                // Search mode with no results
                state.isSearchMode && state.drinks.isEmpty() && !state.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = stringResource(R.string.no_search_results),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.no_search_results_hint),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Results
                else -> {
                    OfficialDrinkList(
                        drinks = state.drinks,
                        importedDrinkNames = state.importedDrinkNames,
                        importingBarcode = state.importingBarcode,
                        onImport = { viewModel.importAsDrink(it) },
                        searchQuery = state.searchQuery,
                        isLoading = false
                    )
                }
            }
        }
    }
}

@Composable
private fun OfficialDrinkList(
    drinks: List<OfficialDrink>,
    importedDrinkNames: Set<String>,
    importingBarcode: String?,
    onImport: (OfficialDrink) -> Unit,
    searchQuery: String,
    isLoading: Boolean
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Loading indicator for background refresh
        if (isLoading) {
            item(key = "loading_bar") {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            }
        }

        items(drinks, key = { it.barcode }) { drink ->
            val isAlreadyImported = drink.name in importedDrinkNames
            OfficialDrinkItem(
                officialDrink = drink,
                isAlreadyImported = isAlreadyImported,
                isImporting = importingBarcode == drink.barcode,
                onImport = { onImport(drink) }
            )
        }
    }
}

@Composable
private fun OfficialDrinkItem(
    officialDrink: OfficialDrink,
    isAlreadyImported: Boolean,
    isImporting: Boolean,
    onImport: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = officialDrink.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Brand + caffeine per 100ml
                val subtitle = buildString {
                    if (!officialDrink.brand.isNullOrBlank()) {
                        append(officialDrink.brand)
                        append(" · ")
                    }
                    append(
                        stringResource(
                            R.string.caffeine_per_100ml,
                            officialDrink.caffeineMgPer100ml
                        )
                    )
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            Spacer(Modifier.width(12.dp))

            if (isAlreadyImported) {
                OutlinedButton(
                    onClick = {},
                    enabled = false,
                    contentPadding = ButtonDefaults.TextButtonContentPadding,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = stringResource(R.string.imported_as_drink),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            } else {
                Button(
                    onClick = onImport,
                    enabled = !isImporting,
                    contentPadding = ButtonDefaults.TextButtonContentPadding,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    if (isImporting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.import_drink),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}
