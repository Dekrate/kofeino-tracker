package pl.dekrate.kofeino.tracker.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import pl.dekrate.kofeino.tracker.R
import pl.dekrate.kofeino.common.domain.model.CaffeineIntake
import pl.dekrate.kofeino.tracker.presentation.viewmodel.HistoryUiState
import pl.dekrate.kofeino.tracker.presentation.viewmodel.HistoryViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onEditIntake: (Long) -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Pre-resolve strings for semantics (are not @Composable)
    val previousDayDesc = stringResource(R.string.previous_day)
    val nextDayDesc = stringResource(R.string.next_day)
    val goToTodayDesc = stringResource(R.string.go_to_today)

    // Localized date label — ViewModel returns raw formatted date, UI resolves relative labels
    val dateLabel = when {
        viewModel.isToday() -> stringResource(R.string.today)
        viewModel.isYesterday() -> stringResource(R.string.yesterday)
        else -> state.dateLabel
    }

    // Show error in snackbar
    LaunchedEffect(state.error) {
        state.error?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.history_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            HistoryContent(
                state = state,
                dateLabel = dateLabel,
                onPreviousDay = { viewModel.previousDay() },
                onNextDay = { viewModel.nextDay() },
                onGoToToday = { viewModel.goToToday() },
                showTodayButton = !viewModel.isToday(),
                onIntakeClick = onEditIntake,
                previousDayDesc = previousDayDesc,
                nextDayDesc = nextDayDesc,
                goToTodayDesc = goToTodayDesc,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

@Composable
private fun HistoryContent(
    state: HistoryUiState,
    dateLabel: String,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onGoToToday: () -> Unit,
    showTodayButton: Boolean,
    onIntakeClick: (Long) -> Unit,
    previousDayDesc: String,
    nextDayDesc: String,
    goToTodayDesc: String,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Date navigation row
        item(key = "date_navigation") {
            DateNavigationRow(
                dateLabel = dateLabel,
                onPreviousDay = onPreviousDay,
                onNextDay = onNextDay,
                onGoToToday = onGoToToday,
                showTodayButton = showTodayButton,
                previousDayDesc = previousDayDesc,
                nextDayDesc = nextDayDesc,
                goToTodayDesc = goToTodayDesc
            )
        }

        // Daily total
        item(key = "daily_total") {
            DailyTotalCard(totalCaffeineMg = state.totalCaffeineMg)
        }

        // Intake list or empty state
        if (state.dateIntakes.isEmpty()) {
            item(key = "empty_state") {
                EmptyHistoryState()
            }
        } else {
            items(state.dateIntakes, key = { it.id }) { intake ->
                IntakeItem(
                    intake = intake,
                    onClick = { onIntakeClick(intake.id) }
                )
            }
        }
    }
}

@Composable
private fun DateNavigationRow(
    dateLabel: String,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onGoToToday: () -> Unit,
    showTodayButton: Boolean,
    previousDayDesc: String,
    nextDayDesc: String,
    goToTodayDesc: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = dateLabel,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics {
                contentDescription = "Selected date: $dateLabel"
            }
        )

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Previous day button
            Box(
                modifier = Modifier
                    .semantics { contentDescription = previousDayDesc }
                    .clickable(onClick = onPreviousDay)
                    .padding(12.dp)
            ) {
                Text(
                    text = "\u25C0",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (showTodayButton) {
                Button(
                    onClick = onGoToToday,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .semantics { contentDescription = goToTodayDesc },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                ) {
                    Text(
                        text = stringResource(R.string.today),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            } else {
                // Placeholder to keep arrows centered when no today button
                Spacer(Modifier.height(1.dp))
            }

            // Next day button
            Box(
                modifier = Modifier
                    .semantics { contentDescription = nextDayDesc }
                    .clickable(onClick = onNextDay)
                    .padding(12.dp)
            ) {
                Text(
                    text = "\u25B6",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun DailyTotalCard(
    totalCaffeineMg: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.daily_total),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${totalCaffeineMg} mg",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.semantics {
                    contentDescription = "Total caffeine: ${totalCaffeineMg} mg"
                }
            )
        }
    }
}

@Composable
private fun EmptyHistoryState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.no_intakes_for_date),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun IntakeItem(
    intake: CaffeineIntake,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val time = remember(intake.timestamp) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(intake.timestamp))
    }

    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "${intake.drinkName} ${intake.caffeineMg} mg, $time"
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = intake.drinkName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = time,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "${intake.caffeineMg} mg",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
