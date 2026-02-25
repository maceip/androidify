package com.android.developers.androidify.launcher.platform;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class LauncherLayoutStore_Factory implements Factory<LauncherLayoutStore> {
  private final Provider<Context> contextProvider;

  private LauncherLayoutStore_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public LauncherLayoutStore get() {
    return newInstance(contextProvider.get());
  }

  public static LauncherLayoutStore_Factory create(Provider<Context> contextProvider) {
    return new LauncherLayoutStore_Factory(contextProvider);
  }

  public static LauncherLayoutStore newInstance(Context context) {
    return new LauncherLayoutStore(context);
  }
}
