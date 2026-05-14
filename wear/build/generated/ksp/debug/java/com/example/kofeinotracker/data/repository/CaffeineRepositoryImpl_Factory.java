package com.example.kofeinotracker.data.repository;

import com.example.kofeinotracker.data.local.CaffeineIntakeDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class CaffeineRepositoryImpl_Factory implements Factory<CaffeineRepositoryImpl> {
  private final Provider<CaffeineIntakeDao> daoProvider;

  private CaffeineRepositoryImpl_Factory(Provider<CaffeineIntakeDao> daoProvider) {
    this.daoProvider = daoProvider;
  }

  @Override
  public CaffeineRepositoryImpl get() {
    return newInstance(daoProvider.get());
  }

  public static CaffeineRepositoryImpl_Factory create(Provider<CaffeineIntakeDao> daoProvider) {
    return new CaffeineRepositoryImpl_Factory(daoProvider);
  }

  public static CaffeineRepositoryImpl newInstance(CaffeineIntakeDao dao) {
    return new CaffeineRepositoryImpl(dao);
  }
}
