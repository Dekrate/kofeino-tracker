package pl.dekrate.kofeino.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import pl.dekrate.kofeino.data.repository.CaffeineRepository
import pl.dekrate.kofeino.domain.model.DrinkEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class DrinkViewModel @Inject constructor(
    private val repository: CaffeineRepository
) : ViewModel() {

    private val _allDrinks = MutableStateFlow<List<DrinkEntity>>(emptyList())
    val allDrinks: StateFlow<List<DrinkEntity>> = _allDrinks.asStateFlow()

    init {
        Timber.d("DrinkViewModel initialized")
        repository.getAllDrinks()
            .onEach { drinks ->
                Timber.d("Drinks updated: ${drinks.size} total")
                _allDrinks.value = drinks
            }
            .launchIn(viewModelScope)
    }

    fun addDrink(name: String, caffeineMg: Int, volumeMl: Int) {
        Timber.d("Adding new drink: $name ($caffeineMg mg, ${volumeMl}ml)")
        viewModelScope.launch {
            repository.addDrink(
                DrinkEntity(
                    name = name,
                    caffeineMg = caffeineMg,
                    volumeMl = volumeMl,
                    isDefault = false
                )
            )
        }
    }

    fun updateDrink(drink: DrinkEntity) {
        Timber.d("Updating drink id=${drink.id}: ${drink.name}")
        viewModelScope.launch {
            repository.updateDrink(drink)
        }
    }

    fun deleteDrink(drink: DrinkEntity) {
        Timber.d("Deleting drink id=${drink.id}: ${drink.name}")
        viewModelScope.launch {
            repository.deleteDrink(drink)
        }
    }
}
