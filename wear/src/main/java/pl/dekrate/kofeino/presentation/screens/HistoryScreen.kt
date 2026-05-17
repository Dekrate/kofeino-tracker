package pl.dekrate.kofeino.presentation.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
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

    // Pre-resolve strings for accessibility
    val previousDayDesc = stringResource(R.string.previous_day)
    val todayDesc = stringResource(R.string.today)
    val nextDayDesc = stringResource(R.string.next_day)

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
                        text = stringResource(R.string.history),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            // Date navigation
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = state.dateLabel,
                        style = MaterialTheme.typography.labelLarge,
                        textAlign = TextAlign.Center
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = { viewModel.previousDay() },
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 4.dp)
                                .semantics { contentDescription = previousDayDesc }
                        ) {
                            Text("\u25C0", style = MaterialTheme.typography.labelLarge)
                        }
                        if (!viewModel.isToday()) {
                            Button(
                                onClick = { viewModel.goToToday() },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 4.dp)
                                    .semantics { contentDescription = todayDesc }
                            ) {
                                Text(
                                    stringResource(R.string.today),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                        Button(
                            onClick = { viewModel.nextDay() },
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 4.dp)
                                .semantics { contentDescription = nextDayDesc }
                        ) {
                            Text("\u25B6", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }

            item {
                Text(
                    text = stringResource(R.string.total_today) + ": ${state.totalCaffeineMg} mg",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            if (state.dateIntakes.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.no_drinks_today),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            } else {
                items(state.dateIntakes, key = { it.id }) { intake ->
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn() + slideInVertically { -it / 4 },
                        exit = fadeOut() + slideOutVertically { it / 4 }
                    ) {
                        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(intake.timestamp))
                        Button(
                            onClick = { onEditIntake(intake.id) },
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
