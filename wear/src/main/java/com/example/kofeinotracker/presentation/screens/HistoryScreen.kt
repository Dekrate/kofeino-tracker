package com.example.kofeinotracker.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.example.kofeinotracker.R
import com.example.kofeinotracker.presentation.viewmodel.CaffeineViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun HistoryScreen(viewModel: CaffeineViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

    ScreenScaffold {
        TransformingLazyColumn(
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

            item {
                Text(
                    text = stringResource(R.string.total_today) + ": ${state.totalCaffeineMg} mg",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
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
                    Text(
                        text = "$time  ${intake.drinkName}  ${intake.caffeineMg} mg",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}
