package pl.dekrate.kofeino.data.repository

import pl.dekrate.kofeino.common.domain.model.CaffeineIntake as CommonCaffeineIntake
import pl.dekrate.kofeino.common.domain.model.DrinkEntity as CommonDrinkEntity
import pl.dekrate.kofeino.domain.model.CaffeineIntake
import pl.dekrate.kofeino.domain.model.DrinkEntity

// Wear → Common

fun CaffeineIntake.toCommon(): CommonCaffeineIntake = CommonCaffeineIntake(
    id = id,
    drinkId = drinkId,
    drinkName = drinkName,
    caffeineMg = caffeineMg,
    volumeMl = volumeMl,
    timestamp = timestamp,
    lastModifiedTimestamp = lastModifiedTimestamp,
    sourceDeviceId = sourceDeviceId
)

fun DrinkEntity.toCommon(): CommonDrinkEntity = CommonDrinkEntity(
    id = id,
    name = name,
    caffeineMg = caffeineMg,
    volumeMl = volumeMl,
    isDefault = isDefault,
    lastModifiedTimestamp = lastModifiedTimestamp,
    sourceDeviceId = sourceDeviceId
)

// Common → Wear

fun CommonCaffeineIntake.toEntity(): CaffeineIntake = CaffeineIntake(
    id = id,
    drinkId = drinkId,
    drinkName = drinkName,
    caffeineMg = caffeineMg,
    volumeMl = volumeMl,
    timestamp = timestamp,
    lastModifiedTimestamp = lastModifiedTimestamp,
    sourceDeviceId = sourceDeviceId
)

fun CommonDrinkEntity.toEntity(): DrinkEntity = DrinkEntity(
    id = id,
    name = name,
    caffeineMg = caffeineMg,
    volumeMl = volumeMl,
    isDefault = isDefault,
    lastModifiedTimestamp = lastModifiedTimestamp,
    sourceDeviceId = sourceDeviceId
)


