package pl.dekrate.kofeino.presentation.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import pl.dekrate.kofeino.R
import pl.dekrate.kofeino.presentation.viewmodel.CaffeineViewModel

@Composable
fun AddDrinkScreen(
    onDrinkAdded: () -> Unit,
    viewModel: CaffeineViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val drinks = state.drinks
    val scrollState = rememberTransformingLazyColumnState()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    ScreenScaffold(scrollState = scrollState) { contentPadding ->
        TransformingLazyColumn(
            state = scrollState,
            contentPadding = contentPadding,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.Top)
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
                        text = stringResource(R.string.no_drinks_defined),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            } else {
                items(drinks, key = { it.id }) { drink ->
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.addDrink(drink)
                            Toast.makeText(context, R.string.drink_added, Toast.LENGTH_SHORT).show()
                            onDrinkAdded()
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
    }
}
