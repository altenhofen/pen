package io.github.altenhofen.pen.ui.theme

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.toArgb

data class InkPalette(val paper: Int, val ink: Int, val muted: Int, val key: Int)

fun inkPalette(context: Context): InkPalette {
    val night =
        (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (night) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        night -> darkColorScheme(
            primary = Purple80,
            secondary = PurpleGrey80,
            tertiary = Pink80,
        )
        else -> lightColorScheme(
            primary = Purple40,
            secondary = PurpleGrey40,
            tertiary = Pink40,
        )
    }
    return InkPalette(
        paper = colorScheme.surface.toArgb(),
        ink = colorScheme.onSurface.toArgb(),
        muted = colorScheme.onSurfaceVariant.toArgb(),
        key = colorScheme.surfaceContainerHigh.toArgb(),
    )
}
