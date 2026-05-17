package pl.dekrate.kofeino.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import pl.dekrate.kofeino.R
import pl.dekrate.kofeino.domain.model.OfficialDrink
import pl.dekrate.kofeino.presentation.viewmodel.OfficialDrinkViewModel
import java.util.Locale

@Composable
fun OfficialDrinksScreen(
    onBack: () -> Unit,
    onDrinkSelected: (OfficialDrink) -> Unit = {},
    viewModel: OfficialDrinkViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val listScrollState = rememberTransformingLazyColumnState()
    var searchText by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Pre-resolve strings for accessibility
    val searchHint = stringResource(R.string.official_drinks_search)
    val searchBtn = stringResource(R.string.official_drinks_search_button)
    val clearBtn = stringResource(R.string.official_drinks_clear_search)
    val retryBtn = stringResource(R.string.official_drinks_retry)

    ScreenScaffold(scrollState = listScrollState) { contentPadding ->
        when {
            state.isLoading && state.drinks.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Text(
                            text = stringResource(R.string.official_drinks_loading),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }

            state.error != null && state.drinks.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(R.string.official_drinks_error),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = viewModel::refresh,
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .semantics { contentDescription = retryBtn }
                    ) {
                        Text(stringResource(R.string.official_drinks_retry))
                    }
                }
            }

            else -> {
                TransformingLazyColumn(
                    state = listScrollState,
                    contentPadding = contentPadding,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.Top)
                ) {
                    // Search bar
                    item {
                        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                            // BasicTextField container
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceContainer,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                if (searchText.isEmpty()) {
                                    Text(
                                        text = stringResource(R.string.official_drinks_search),
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                                BasicTextField(
                                    value = searchText,
                                    onValueChange = { newValue ->
                                        searchText = newValue
                                        viewModel.onSearchQueryChanged(newValue)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .semantics {
                                            testTag = "search_field"
                                            contentDescription = searchHint
                                        },
                                    textStyle = TextStyle(
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 14.sp
                                    ),
                                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                    keyboardActions = KeyboardActions(
                                        onSearch = {
                                            keyboardController?.hide()
                                            viewModel.search(searchText)
                                        }
                                    ),
                                    singleLine = true
                                )
                            }
                            // "Szukaj" button
                            if (searchText.isNotBlank()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    Button(
                                        onClick = {
                                            keyboardController?.hide()
                                            viewModel.search(searchText)
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(end = 4.dp)
                                            .semantics { contentDescription = searchBtn }
                                    ) {
                                        Text(stringResource(R.string.official_drinks_search_button))
                                    }
                                    Button(
                                        onClick = {
                                            searchText = ""
                                            viewModel.clearSearch()
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(start = 4.dp)
                                            .semantics { contentDescription = clearBtn }
                                    ) {
                                        Text(stringResource(R.string.official_drinks_clear_search))
                                    }
                                }
                            }
                        }
                    }

                    // Title
                    item {
                        ListHeader {
                            Text(
                                text = if (state.isSearchMode)
                                    stringResource(R.string.official_drinks_search_results)
                                else
                                    stringResource(R.string.official_drinks_title),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }

                    // Loading in background (when refreshing search results)
                    if (state.isLoading && state.drinks.isNotEmpty()) {
                        item {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
                            )
                        }
                    }

                    if (state.drinks.isEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.official_drinks_no_results),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(16.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        items(state.drinks, key = { it.barcode }) { drink ->
                            Button(
                                onClick = { onDrinkSelected(drink) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp)
                                    .semantics {
                                        contentDescription = "${drink.name} ${drink.brand ?: ""} ${"%.0f".format(drink.caffeineMgPer100ml)} mg/100ml"
                                    }
                            ) {
                                Column {
                                    Text(
                                        text = drink.name,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 2
                                    )
                                    val brand = drink.brand
                                    // Locale.US zapewnia "63.0" z kropką niezależnie od języka systemu
                                    val caffeine = "%.0f mg/100ml".format(
                                        Locale.US,
                                        drink.caffeineMgPer100ml
                                    )
                                    val subtitle = buildString {
                                        if (!brand.isNullOrBlank()) append(brand).append(" · ")
                                        append(caffeine)
                                    }
                                    Text(
                                        text = subtitle,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
