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
public final class LauncherAppsRepository_Factory implements Factory<LauncherAppsRepository> {
  private final Provider<Context> contextProvider;

  private LauncherAppsRepository_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public LauncherAppsRepository get() {
    return newInstance(contextProvider.get());
  }

  public static LauncherAppsRepository_Factory create(Provider<Context> contextProvider) {
    return new LauncherAppsRepository_Factory(contextProvider);
  }

  public static LauncherAppsRepository newInstance(Context context) {
    return new LauncherAppsRepository(context);
  }
}
