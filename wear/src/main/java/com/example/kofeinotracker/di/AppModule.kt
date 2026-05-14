package com.example.kofeinotracker.di

import com.example.kofeinotracker.R
import com.example.kofeinotracker.domain.model.CaffeineDrink
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun providePredefinedDrinks(): List<CaffeineDrink> = listOf(
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
