package com.manganew.reader.designsystem.theme

import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/**
 * Observes Android's animator and transition scale settings.
 *
 * A zero scale is treated as the platform's "remove animations" preference.
 */
@Composable
internal fun rememberSystemReduceMotion(): Boolean {
    val context = LocalContext.current
    var reduceMotion by remember { mutableStateOf(context.readSystemReduceMotion()) }

    DisposableEffect(context) {
        val resolver = context.contentResolver
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                reduceMotion = context.readSystemReduceMotion()
            }
        }
        val animatorScaleUri =
            Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE)
        val transitionScaleUri =
            Settings.Global.getUriFor(Settings.Global.TRANSITION_ANIMATION_SCALE)

        resolver.registerContentObserver(animatorScaleUri, false, observer)
        resolver.registerContentObserver(transitionScaleUri, false, observer)

        onDispose {
            resolver.unregisterContentObserver(observer)
        }
    }

    return reduceMotion
}

internal fun isSystemReduceMotionEnabled(
    animatorDurationScale: Float,
    transitionAnimationScale: Float,
): Boolean = animatorDurationScale <= 0f || transitionAnimationScale <= 0f

private fun android.content.Context.readSystemReduceMotion(): Boolean =
    isSystemReduceMotionEnabled(
        animatorDurationScale = readGlobalFloat(Settings.Global.ANIMATOR_DURATION_SCALE),
        transitionAnimationScale = readGlobalFloat(Settings.Global.TRANSITION_ANIMATION_SCALE),
    )

private fun android.content.Context.readGlobalFloat(key: String): Float =
    try {
        Settings.Global.getFloat(contentResolver, key)
    } catch (_: Settings.SettingNotFoundException) {
        1f
    }
