package pl.dekrate.kofeino.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import pl.dekrate.kofeino.common.di.CommonModule
import pl.dekrate.kofeino.common.domain.repository.CaffeineRepository
import pl.dekrate.kofeino.common.domain.repository.OfficialDrinkRepository
import pl.dekrate.kofeino.data.repository.CaffeineRepositoryImpl
import pl.dekrate.kofeino.data.repository.OfficialDrinkRepositoryImpl
import javax.inject.Singleton

@Module(includes = [CommonModule::class])
@InstallIn(SingletonComponent::class)
interface WearRepositoryModule {

    @Binds
    @Singleton
    fun bindCaffeineRepository(
        impl: CaffeineRepositoryImpl
    ): CaffeineRepository

    @Binds
    @Singleton
    fun bindOfficialDrinkRepository(
        impl: OfficialDrinkRepositoryImpl
    ): OfficialDrinkRepository
}
