package pl.dekrate.kofeino.presentation.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.EdgeButtonSize
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.OutlinedButton
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import pl.dekrate.kofeino.R
import pl.dekrate.kofeino.presentation.viewmodel.CaffeineViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    onNavigateToAddDrink: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToManageDrinks: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
    onEditIntake: (Long) -> Unit,
    viewModel: CaffeineViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val scrollState = rememberTransformingLazyColumnState()
    val context = LocalContext.current

    // Show Toast on errors
    LaunchedEffect(state.error) {
        state.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

    // Pre-resolve strings for accessibility (semantics blocks are not @Composable)
    val addDrinkDesc = stringResource(R.string.add_drink)
    val todayCaffeineDesc = stringResource(R.string.today_caffeine)
    val historyDesc = stringResource(R.string.history)
    val manageDrinksDesc = stringResource(R.string.manage_drinks)
    val settingsDesc = stringResource(R.string.settings)
    val noDrinksDesc = stringResource(R.string.accessibility_no_drinks)
    val dateLabelDesc = stringResource(R.string.accessibility_date_label, state.dateLabel)

    ScreenScaffold(
        scrollState = scrollState,
        edgeButton = {
            EdgeButton(
                onClick = onNavigateToAddDrink,
                buttonSize = EdgeButtonSize.Small,
                modifier = Modifier.semantics {
                    contentDescription = addDrinkDesc
                    role = Role.Button
                }
            ) {
                Text(text = "+")
            }
        }
    ) { contentPadding ->
        TransformingLazyColumn(
            state = scrollState,
            contentPadding = contentPadding,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.Top)
        ) {
            item {
                ListHeader {
                    Text(
                        text = stringResource(R.string.today_caffeine),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.semantics {
                            contentDescription = todayCaffeineDesc
                        }
                    )
                }
            }

            // Date label
            item {
                Text(
                    text = state.dateLabel,
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .semantics { 
                            contentDescription = dateLabelDesc
                        }
                )
            }

            // Coffee cup indicator (animated fill)
            item {
                CoffeeCupIndicator(
                    total = state.totalCaffeineMg,
                    progress = state.progress,
                    exceeded = state.isLimitExceeded,
                    safeLimit = state.safeLimitMg
                )
            }

            // Action buttons
            item {
                Button(
                    onClick = onNavigateToHistory,
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .semantics { contentDescription = historyDesc }
                ) {
                    Text(stringResource(R.string.history))
                }
            }

            item {
                Button(
                    onClick = onNavigateToManageDrinks,
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .semantics { contentDescription = manageDrinksDesc }
                ) {
                    Text(stringResource(R.string.manage_drinks))
                }
            }

            item {
                OutlinedButton(
                    onClick = onNavigateToSettings,
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .semantics { contentDescription = settingsDesc }
                ) {
                    Text(stringResource(R.string.settings))
                }
            }

            if (state.dateIntakes.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.no_drinks_today),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .semantics { contentDescription = noDrinksDesc }
                    )
                }
            } else {
                // Animated list items
                items(state.dateIntakes, key = { it.id }) { intake ->
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn() + slideInVertically { -it / 4 },
                        exit = fadeOut() + slideOutVertically { it / 4 }
                    ) {
                        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(intake.timestamp))
                        Button(
                            onClick = { onEditIntake(intake.id) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier
                                .padding(horizontal = 8.dp)
                                .semantics {
                                    contentDescription = "${intake.drinkName} ${intake.caffeineMg} mg, $time"
                                }
                        ) {
                            Text(
                                text = "$time  ${intake.drinkName}  ${intake.caffeineMg} mg",
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
