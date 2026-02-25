package com.android.developers.androidify.launcher;

import android.content.Context;
import com.android.developers.androidify.launcher.platform.LauncherAppsRepository;
import com.android.developers.androidify.launcher.platform.LauncherLayoutStore;
import com.android.developers.androidify.launcher.platform.LauncherWidgetHostController;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata
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
public final class LauncherViewModel_Factory implements Factory<LauncherViewModel> {
  private final Provider<Context> contextProvider;

  private final Provider<LauncherAppsRepository> launcherAppsRepositoryProvider;

  private final Provider<LauncherLayoutStore> launcherLayoutStoreProvider;

  private final Provider<LauncherWidgetHostController> launcherWidgetHostControllerProvider;

  private LauncherViewModel_Factory(Provider<Context> contextProvider,
      Provider<LauncherAppsRepository> launcherAppsRepositoryProvider,
      Provider<LauncherLayoutStore> launcherLayoutStoreProvider,
      Provider<LauncherWidgetHostController> launcherWidgetHostControllerProvider) {
    this.contextProvider = contextProvider;
    this.launcherAppsRepositoryProvider = launcherAppsRepositoryProvider;
    this.launcherLayoutStoreProvider = launcherLayoutStoreProvider;
    this.launcherWidgetHostControllerProvider = launcherWidgetHostControllerProvider;
  }

  @Override
  public LauncherViewModel get() {
    return newInstance(contextProvider.get(), launcherAppsRepositoryProvider.get(), launcherLayoutStoreProvider.get(), launcherWidgetHostControllerProvider.get());
  }

  public static LauncherViewModel_Factory create(Provider<Context> contextProvider,
      Provider<LauncherAppsRepository> launcherAppsRepositoryProvider,
      Provider<LauncherLayoutStore> launcherLayoutStoreProvider,
      Provider<LauncherWidgetHostController> launcherWidgetHostControllerProvider) {
    return new LauncherViewModel_Factory(contextProvider, launcherAppsRepositoryProvider, launcherLayoutStoreProvider, launcherWidgetHostControllerProvider);
  }

  public static LauncherViewModel newInstance(Context context,
      LauncherAppsRepository launcherAppsRepository, LauncherLayoutStore launcherLayoutStore,
      LauncherWidgetHostController launcherWidgetHostController) {
    return new LauncherViewModel(context, launcherAppsRepository, launcherLayoutStore, launcherWidgetHostController);
  }
}
