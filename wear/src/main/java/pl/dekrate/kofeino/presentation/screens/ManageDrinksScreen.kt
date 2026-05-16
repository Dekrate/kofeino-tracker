package pl.dekrate.kofeino.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import pl.dekrate.kofeino.R
import pl.dekrate.kofeino.presentation.viewmodel.DrinkViewModel

@Composable
fun ManageDrinksScreen(
    onBack: () -> Unit,
    onOfficialDrinks: () -> Unit = {},
    viewModel: DrinkViewModel = hiltViewModel()
) {
    val drinks by viewModel.allDrinks.collectAsState()
    var showAddForm by remember { mutableStateOf(false) }
    var editingDrinkId by remember { mutableStateOf<Long?>(null) }

    if (showAddForm || editingDrinkId != null) {
        val editingDrink = if (editingDrinkId != null) {
            drinks.find { it.id == editingDrinkId }
        } else null

        ScreenScaffold {
            AddEditDrinkForm(
                initialName = editingDrink?.name ?: "",
                initialCaffeineMg = editingDrink?.caffeineMg ?: 80,
                initialVolumeMl = editingDrink?.volumeMl ?: 250,
                isEditing = editingDrink != null,
                onDismiss = {
                    showAddForm = false
                    editingDrinkId = null
                },
                onSave = { name, caffeineMg, volumeMl ->
                    if (editingDrink != null) {
                        viewModel.updateDrink(
                            editingDrink.copy(name = name, caffeineMg = caffeineMg, volumeMl = volumeMl)
                        )
                    } else {
                        viewModel.addDrink(name, caffeineMg, volumeMl)
                    }
                    showAddForm = false
                    editingDrinkId = null
                },
                onDelete = if (editingDrink != null && !editingDrink.isDefault) {
                    {
                        viewModel.deleteDrink(editingDrink)
                        editingDrinkId = null
                    }
                } else null
            )
        }
        return
    }

    val listScrollState = rememberTransformingLazyColumnState()
    ScreenScaffold(scrollState = listScrollState) { contentPadding ->
        TransformingLazyColumn(
            state = listScrollState,
            contentPadding = contentPadding,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.Top)
        ) {
            item {
                ListHeader {
                    Text(
                        text = stringResource(R.string.manage_drinks),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            item {
                Button(
                    onClick = { showAddForm = true },
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Text(stringResource(R.string.add_new_drink))
                }
            }

            item {
                Button(
                    onClick = onOfficialDrinks,
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Text(stringResource(R.string.official_drinks_button))
                }
            }

            item {
                Button(
                    onClick = onBack,
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Text(stringResource(R.string.back))
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
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn() + slideInVertically { -it / 4 },
                        exit = fadeOut() + slideOutVertically { it / 4 }
                    ) {
                        Button(
                            onClick = { editingDrinkId = drink.id },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Text(
                                text = "${drink.name}  ${drink.caffeineMg} mg · ${drink.volumeMl} ml",
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
