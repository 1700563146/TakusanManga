package com.manganew.reader

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.view.WindowCompat
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.manganew.reader.designsystem.theme.MangaNewTheme
import com.manganew.reader.designsystem.theme.MangaTheme
import com.manganew.reader.feature.settings.AppSettings
import com.manganew.reader.feature.settings.SettingsAction
import com.manganew.reader.feature.settings.SettingsViewModel
import com.manganew.reader.navigation.MangaNavHost
import com.manganew.reader.navigation.TopLevelDestination
import com.manganew.reader.navigation.isTopLevelRoute

@Composable
internal fun MangaNewApp(
    settingsViewModel: SettingsViewModel = viewModel(),
) {
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    MangaNewTheme(
        themeMode = settings.themeMode,
        reduceMotion = settings.reduceMotion,
    ) {
        SystemBarsEffect()
        MangaAppShell(
            settings = settings,
            onSettingsAction = settingsViewModel::onAction,
        )
    }
}

@Composable
private fun SystemBarsEffect() {
    val view = LocalView.current
    val useDarkIcons = MangaTheme.colors.background.luminance() > 0.5f
    SideEffect {
        val activity = view.context.findActivity() ?: return@SideEffect
        WindowCompat.getInsetsController(activity.window, view).apply {
            isAppearanceLightStatusBars = useDarkIcons
            isAppearanceLightNavigationBars = useDarkIcons
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
internal fun MangaAppShell(
    settings: AppSettings = AppSettings(),
    onSettingsAction: (SettingsAction) -> Unit = {},
    navController: NavHostController = rememberNavController(),
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val showBottomBar = currentDestination == null || isTopLevelRoute(currentDestination.route)
    val motion = MangaTheme.motion

    Scaffold(
        containerColor = MangaTheme.colors.background,
        contentColor = MangaTheme.colors.textPrimary,
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = fadeIn(tween(durationMillis = motion.normalMillis)),
                exit = fadeOut(tween(durationMillis = motion.normalMillis)),
            ) {
                NavigationBar(containerColor = MangaTheme.colors.surface) {
                    TopLevelDestination.entries.forEach { destination ->
                        val label = stringResource(destination.labelRes)
                        val selected = currentDestination?.hierarchy?.any {
                            it.route == destination.route
                        } == true

                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (!selected) {
                                    navController.navigate(destination.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            modifier = Modifier.semantics {
                                contentDescription = label
                            },
                            icon = {
                                Icon(
                                    imageVector = destination.icon,
                                    contentDescription = null,
                                )
                            },
                            label = {
                                Text(
                                    text = label,
                                    style = MangaTheme.typography.caption,
                                )
                            },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        MangaNavHost(
            navController = navController,
            settings = settings,
            onSettingsAction = onSettingsAction,
            modifier = Modifier.padding(innerPadding),
        )
    }
}
