package com.example.kofeinotracker.di;

import com.example.kofeinotracker.domain.model.CaffeineDrink;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import java.util.List;
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
public final class AppModule_ProvidePredefinedDrinksFactory implements Factory<List<CaffeineDrink>> {
  @Override
  public List<CaffeineDrink> get() {
    return providePredefinedDrinks();
  }

  public static AppModule_ProvidePredefinedDrinksFactory create() {
    return InstanceHolder.INSTANCE;
  }

  public static List<CaffeineDrink> providePredefinedDrinks() {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.providePredefinedDrinks());
  }

  private static final class InstanceHolder {
    static final AppModule_ProvidePredefinedDrinksFactory INSTANCE = new AppModule_ProvidePredefinedDrinksFactory();
  }
}
