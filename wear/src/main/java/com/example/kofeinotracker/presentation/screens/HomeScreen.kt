package com.example.kofeinotracker.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.ProgressIndicatorDefaults
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.EdgeButtonSize
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.example.kofeinotracker.R
import com.example.kofeinotracker.presentation.viewmodel.CaffeineViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun HomeScreen(
    onNavigateToAddDrink: () -> Unit,
    onNavigateToHistory: () -> Unit,
    viewModel: CaffeineViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val scrollState = rememberTransformingLazyColumnState()

    ScreenScaffold(
        scrollState = scrollState,
        edgeButton = {
            EdgeButton(
                onClick = onNavigateToAddDrink,
                buttonSize = EdgeButtonSize.Small
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
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            item {
                CaffeineProgress(state.totalCaffeineMg, state.progress, state.isLimitExceeded)
            }

            item {
                Button(
                    onClick = onNavigateToHistory,
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Text(stringResource(R.string.history))
                }
            }

            if (state.todayIntakes.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.no_drinks_today),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            } else {
                items(state.todayIntakes, key = { it.id }) { intake ->
                    val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(intake.timestamp)
                    Button(
                        onClick = { },
                        enabled = false,
                        colors = ButtonDefaults.buttonColors(
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                            disabledContentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp)
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

@Composable
private fun CaffeineProgress(total: Int, progress: Float, exceeded: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier.size(80.dp),
                strokeWidth = 6.dp,
                colors = if (exceeded) {
                    ProgressIndicatorDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.error,
                        trackColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                } else {
                    ProgressIndicatorDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                }
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$total",
                    style = MaterialTheme.typography.displaySmall
                )
                Text(
                    text = "mg",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        if (exceeded) {
            Text(
                text = stringResource(R.string.limit_exceeded),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center
            )
        } else {
            Text(
                text = stringResource(R.string.safe_limit),
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}
