package com.example.kofeinotracker.domain.model

import androidx.annotation.StringRes

/** Predefiniowany napoj z zawartoscia kofeiny. */
data class CaffeineDrink(
    val id: String,
    @StringRes val nameResId: Int,
    val caffeineMg: Int,
    val volumeMl: Int
)
