package com.example.kofeinotracker.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.example.kofeinotracker.R
import com.example.kofeinotracker.domain.model.CaffeineDrink
import com.example.kofeinotracker.presentation.viewmodel.CaffeineViewModel
import javax.inject.Inject

@Composable
fun AddDrinkScreen(
    onDrinkAdded: () -> Unit,
    viewModel: CaffeineViewModel = hiltViewModel(),
    drinks: List<CaffeineDrink> = androidx.compose.ui.platform.LocalContext.current.let { ctx ->
        // Predefined drinks are provided via DI but for simplicity we inline here
        // In real app use AssistedInject or pass from ViewModel
        listOf(
            CaffeineDrink("espresso", R.string.espresso, 63, 30),
            CaffeineDrink("double_espresso", R.string.double_espresso, 126, 60),
            CaffeineDrink("black_coffee", R.string.black_coffee, 95, 250),
            CaffeineDrink("cappuccino", R.string.cappuccino, 75, 200),
            CaffeineDrink("latte", R.string.latte, 63, 250),
            CaffeineDrink("tea", R.string.tea, 47, 250),
            CaffeineDrink("green_tea", R.string.green_tea, 28, 250),
            CaffeineDrink("energy_drink", R.string.energy_drink, 80, 250),
            CaffeineDrink("cola", R.string.cola, 34, 330)
        )
    }
) {
    ScreenScaffold {
        TransformingLazyColumn(
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

            items(drinks, key = { it.id }) { drink ->
                Button(
                    onClick = {
                        viewModel.addDrink(drink)
                        onDrinkAdded()
                    },
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Text(
                        text = "${stringResource(drink.nameResId)}  ${drink.caffeineMg} mg",
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
