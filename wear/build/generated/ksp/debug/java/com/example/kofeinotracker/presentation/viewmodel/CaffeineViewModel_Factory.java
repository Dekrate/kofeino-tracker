package com.example.kofeinotracker.presentation.viewmodel;

import com.example.kofeinotracker.data.repository.CaffeineRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata
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
public final class CaffeineViewModel_Factory implements Factory<CaffeineViewModel> {
  private final Provider<CaffeineRepository> repositoryProvider;

  private CaffeineViewModel_Factory(Provider<CaffeineRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public CaffeineViewModel get() {
    return newInstance(repositoryProvider.get());
  }

  public static CaffeineViewModel_Factory create(Provider<CaffeineRepository> repositoryProvider) {
    return new CaffeineViewModel_Factory(repositoryProvider);
  }

  public static CaffeineViewModel newInstance(CaffeineRepository repository) {
    return new CaffeineViewModel(repository);
  }
}
