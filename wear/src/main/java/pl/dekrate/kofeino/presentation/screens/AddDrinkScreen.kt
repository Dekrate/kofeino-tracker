package pl.dekrate.kofeino.presentation.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items as foundationItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnScope
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.CompactButton
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import pl.dekrate.kofeino.R
import pl.dekrate.kofeino.common.domain.model.CaffeineIntake
import pl.dekrate.kofeino.common.domain.model.DrinkEntity
import pl.dekrate.kofeino.presentation.viewmodel.AddDrinkViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AddDrinkScreen(
    onDrinkAdded: () -> Unit,
    viewModel: AddDrinkViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    var selectedDrink by remember { mutableStateOf<DrinkEntity?>(null) }
    var isLogging by remember { mutableStateOf(false) }
    val searchHint = stringResource(R.string.search_drinks_hint)

    if (selectedDrink != null) {
        val drink = selectedDrink!!
        AddDrinkConfirmationContent(
            drink = drink,
            isLogging = isLogging,
            onLogDrink = { caffeineMg, volumeMl ->
                if (isLogging) return@AddDrinkConfirmationContent
                isLogging = true
                viewModel.addDrink(
                    drink.copy(caffeineMg = caffeineMg, volumeMl = volumeMl),
                    onComplete = {
                        Toast.makeText(context, R.string.drink_added, Toast.LENGTH_SHORT).show()
                        selectedDrink = null
                        isLogging = false
                        onDrinkAdded()
                    },
                    onError = {
                        isLogging = false
                        Toast.makeText(context, R.string.error_add_failed, Toast.LENGTH_SHORT).show()
                    }
                )
            },
            onCancel = {
                selectedDrink = null
                isLogging = false
            }
        )
    } else {
        val scrollState = rememberTransformingLazyColumnState()
        ScreenScaffold(scrollState = scrollState) { contentPadding ->
            TransformingLazyColumn(
                state = scrollState,
                contentPadding = contentPadding,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.Top)
            ) {
                // ── Search bar ──────────────────────────────────────────
                item {
                    SearchBar(
                        query = state.searchQuery,
                        onQueryChange = { viewModel.onSearchQueryChanged(it) },
                        searchHint = searchHint
                    )
                }

                // ── Recent intakes chips (only when not searching) ──────
                if (!state.searchQuery.isNotBlank() && state.recentIntakes.isNotEmpty()) {
                    item {
                        RecentIntakesSection(
                            recentIntakes = state.recentIntakes,
                            onIntakeSelect = { drink -> selectedDrink = drink }
                        )
                    }
                }

                // ── Drink list ──────────────────────────────────────────
                DrinkListSection(
                    drinks = state.drinks,
                    searchQuery = state.searchQuery,
                    onDrinkSelected = { drink -> selectedDrink = drink },
                    haptic = haptic
                )
            }
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    searchHint: String,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        if (query.isEmpty()) {
            Text(
                text = searchHint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = searchHint
                },
            textStyle = TextStyle(
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            singleLine = true
        )
    }
}

@Composable
private fun RecentIntakesSection(
    recentIntakes: List<CaffeineIntake>,
    onIntakeSelect: (DrinkEntity) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    Column {
        ListHeader {
            Text(
                text = stringResource(R.string.recent_intakes),
                style = MaterialTheme.typography.labelSmall
            )
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            foundationItems(recentIntakes, key = { it.id }) { intake ->
                RecentIntakeChip(
                    intake = intake,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val drink = DrinkEntity(
                            id = intake.drinkId ?: 0L,
                            name = intake.drinkName,
                            caffeineMg = intake.caffeineMg,
                            volumeMl = intake.volumeMl
                        )
                        onIntakeSelect(drink)
                    }
                )
            }
        }
    }
}

private fun TransformingLazyColumnScope.DrinkListSection(
    drinks: List<DrinkEntity>,
    searchQuery: String,
    onDrinkSelected: (DrinkEntity) -> Unit,
    haptic: HapticFeedback,
) {
    item {
        ListHeader {
            Text(
                text = stringResource(R.string.select_drink),
                style = MaterialTheme.typography.titleMedium
            )
        }
    }

    if (drinks.isEmpty()) {
        item {
            Text(
                text = if (searchQuery.isNotBlank()) {
                    stringResource(R.string.no_search_results)
                } else {
                    stringResource(R.string.no_drinks_defined)
                },
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                textAlign = TextAlign.Center
            )
        }
    } else {
        items(drinks, key = { it.id }) { drink ->
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onDrinkSelected(drink)
                },
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .semantics {
                        contentDescription = "${drink.name} ${drink.caffeineMg} mg"
                    }
            ) {
                Text(
                    text = "${drink.name}  ${drink.caffeineMg} mg",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * A small chip displaying a recent caffeine intake.
 * Shows drink name and time, tapping it navigates to confirmation.
 */
@Composable
private fun RecentIntakeChip(
    intake: CaffeineIntake,
    onClick: () -> Unit,
) {
    val timeString = remember(intake.timestamp) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(intake.timestamp))
    }
    CompactButton(
        onClick = onClick,
        modifier = Modifier.width(100.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
        )
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = intake.drinkName,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${intake.caffeineMg} mg • $timeString",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}
