package com.manganew.reader.navigation

import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.manganew.reader.R

internal object AppRoute {
    const val Home = "home"
    const val Sources = "sources"
    const val Search = "search"
    const val Settings = "settings"
}

internal object ReaderDestination {
    const val ChapterIdArgument = "chapterId"
    const val Route = "reader/{$ChapterIdArgument}"

    fun createRoute(chapterId: String): String = "reader/${Uri.encode(chapterId)}"
}

internal object SourceDetailDestination {
    const val SourceIdArgument = "sourceId"
    const val Route = "source/{$SourceIdArgument}"

    fun createRoute(sourceId: String): String = "source/${Uri.encode(sourceId)}"
}

internal object ComicDetailDestination {
    const val ComicIdArgument = "comicId"
    const val Route = "comic/{$ComicIdArgument}"

    fun createRoute(comicId: String): String = "comic/${Uri.encode(comicId)}"
}

internal enum class TopLevelDestination(
    val route: String,
    @param:StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    Home(
        route = AppRoute.Home,
        labelRes = R.string.nav_home,
        icon = Icons.Filled.Home,
    ),
    Sources(
        route = AppRoute.Sources,
        labelRes = R.string.nav_sources,
        icon = Icons.AutoMirrored.Filled.List,
    ),
    Search(
        route = AppRoute.Search,
        labelRes = R.string.nav_search,
        icon = Icons.Filled.Search,
    ),
    Settings(
        route = AppRoute.Settings,
        labelRes = R.string.nav_settings,
        icon = Icons.Filled.Settings,
    ),
}

internal fun isTopLevelRoute(route: String?): Boolean =
    TopLevelDestination.entries.any { it.route == route }
