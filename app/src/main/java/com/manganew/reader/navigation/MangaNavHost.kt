package com.manganew.reader.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.manganew.reader.designsystem.theme.MangaMotion
import com.manganew.reader.designsystem.theme.MangaTheme
import com.manganew.reader.feature.home.HomeRoute
import com.manganew.reader.feature.reader.ReaderScreen
import com.manganew.reader.feature.search.SearchScreen
import com.manganew.reader.feature.settings.SettingsScreen
import com.manganew.reader.feature.source.SourceDetailRoute
import com.manganew.reader.feature.source.SourceListRoute

@Composable
internal fun MangaNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    val motion = MangaTheme.motion
    val enterTransition = motion.navigationEnterTransition()
    val exitTransition = motion.navigationExitTransition()
    val popEnterTransition = motion.navigationPopEnterTransition()
    val popExitTransition = motion.navigationPopExitTransition()

    NavHost(
        navController = navController,
        startDestination = AppRoute.Home,
        modifier = modifier,
        enterTransition = { enterTransition },
        exitTransition = { exitTransition },
        popEnterTransition = { popEnterTransition },
        popExitTransition = { popExitTransition },
    ) {
        composable(AppRoute.Home) {
            HomeRoute(
                onOpenReader = {
                    navController.navigate(ReaderDestination.createRoute("demo-chapter"))
                },
                onOpenSource = { sourceId ->
                    navController.navigate(SourceDetailDestination.createRoute(sourceId))
                },
            )
        }
        composable(AppRoute.Sources) {
            SourceListRoute(
                onOpenSource = { sourceId ->
                    navController.navigate(SourceDetailDestination.createRoute(sourceId))
                },
            )
        }
        composable(AppRoute.Search) {
            SearchScreen()
        }
        composable(AppRoute.Settings) {
            SettingsScreen()
        }
        composable(
            route = SourceDetailDestination.Route,
            arguments = listOf(
                navArgument(SourceDetailDestination.SourceIdArgument) {
                    type = NavType.StringType
                },
            ),
        ) { backStackEntry ->
            val sourceId = backStackEntry.arguments
                ?.getString(SourceDetailDestination.SourceIdArgument)
                .orEmpty()
            SourceDetailRoute(
                sourceId = sourceId,
                onBack = navController::navigateUp,
            )
        }
        composable(
            route = ReaderDestination.Route,
            arguments = listOf(
                navArgument(ReaderDestination.ChapterIdArgument) {
                    type = NavType.StringType
                },
            ),
        ) { backStackEntry ->
            ReaderScreen(
                chapterId = backStackEntry.arguments
                    ?.getString(ReaderDestination.ChapterIdArgument)
                    .orEmpty(),
                onBack = navController::navigateUp,
            )
        }
    }
}

private fun MangaMotion.navigationEnterTransition(): EnterTransition =
    if (reduceMotion) {
        EnterTransition.None
    } else {
        fadeIn(
            animationSpec = tween(
                durationMillis = emphasisMillis,
                easing = emphasizedEasing,
            ),
        ) + slideInHorizontally(
            animationSpec = tween(
                durationMillis = emphasisMillis,
                easing = emphasizedEasing,
            ),
            initialOffsetX = { it / 10 },
        )
    }

private fun MangaMotion.navigationExitTransition(): ExitTransition =
    if (reduceMotion) {
        ExitTransition.None
    } else {
        fadeOut(
            animationSpec = tween(
                durationMillis = normalMillis,
                easing = accelerateEasing,
            ),
        ) + slideOutHorizontally(
            animationSpec = tween(
                durationMillis = normalMillis,
                easing = accelerateEasing,
            ),
            targetOffsetX = { -it / 20 },
        )
    }

private fun MangaMotion.navigationPopEnterTransition(): EnterTransition =
    if (reduceMotion) {
        EnterTransition.None
    } else {
        fadeIn(
            animationSpec = tween(
                durationMillis = normalMillis,
                easing = decelerateEasing,
            ),
        ) + slideInHorizontally(
            animationSpec = tween(
                durationMillis = normalMillis,
                easing = decelerateEasing,
            ),
            initialOffsetX = { -it / 20 },
        )
    }

private fun MangaMotion.navigationPopExitTransition(): ExitTransition =
    if (reduceMotion) {
        ExitTransition.None
    } else {
        fadeOut(
            animationSpec = tween(
                durationMillis = emphasisMillis,
                easing = emphasizedEasing,
            ),
        ) + slideOutHorizontally(
            animationSpec = tween(
                durationMillis = emphasisMillis,
                easing = emphasizedEasing,
            ),
            targetOffsetX = { it / 10 },
        )
    }
