package com.example.kofeinotracker.di;

import com.example.kofeinotracker.data.repository.CaffeineRepository;
import com.example.kofeinotracker.data.repository.CaffeineRepositoryImpl;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation",
    "nullness:initialization.field.uninitialized"
})
public final class DatabaseModule_ProvideCaffeineRepositoryFactory implements Factory<CaffeineRepository> {
  private final Provider<CaffeineRepositoryImpl> implProvider;

  private DatabaseModule_ProvideCaffeineRepositoryFactory(
      Provider<CaffeineRepositoryImpl> implProvider) {
    this.implProvider = implProvider;
  }

  @Override
  public CaffeineRepository get() {
    return provideCaffeineRepository(implProvider.get());
  }

  public static DatabaseModule_ProvideCaffeineRepositoryFactory create(
      Provider<CaffeineRepositoryImpl> implProvider) {
    return new DatabaseModule_ProvideCaffeineRepositoryFactory(implProvider);
  }

  public static CaffeineRepository provideCaffeineRepository(CaffeineRepositoryImpl impl) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideCaffeineRepository(impl));
  }
}
