package com.example.kofeinotracker.di;

import com.example.kofeinotracker.data.local.CaffeineDatabase;
import com.example.kofeinotracker.data.local.CaffeineIntakeDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class DatabaseModule_ProvideDaoFactory implements Factory<CaffeineIntakeDao> {
  private final Provider<CaffeineDatabase> databaseProvider;

  private DatabaseModule_ProvideDaoFactory(Provider<CaffeineDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public CaffeineIntakeDao get() {
    return provideDao(databaseProvider.get());
  }

  public static DatabaseModule_ProvideDaoFactory create(
      Provider<CaffeineDatabase> databaseProvider) {
    return new DatabaseModule_ProvideDaoFactory(databaseProvider);
  }

  public static CaffeineIntakeDao provideDao(CaffeineDatabase database) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideDao(database));
  }
}
