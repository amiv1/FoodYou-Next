package com.maksimowiczm.foodyou.app.ui.common.theme

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color

@Immutable
data class NutrientsPalette(
    val proteinsOnSurfaceContainer: Color = Color.Unspecified,
    val carbohydratesOnSurfaceContainer: Color = Color.Unspecified,
    val fatsOnSurfaceContainer: Color = Color.Unspecified,
)

val DarkNutrientsPalette =
    NutrientsPalette(
        proteinsOnSurfaceContainer = Color(0xFF0B88AB),
        carbohydratesOnSurfaceContainer = Color(0xFF4046D1),
        fatsOnSurfaceContainer = Color(0xFFDC9528),
    )

val LightNutrientsPalette =
    NutrientsPalette(
        proteinsOnSurfaceContainer = Color(0xFF0B88AB),
        carbohydratesOnSurfaceContainer = Color(0xFF4046D1),
        fatsOnSurfaceContainer = Color(0xFFDC9528),
    )

val LocalNutrientsPalette = staticCompositionLocalOf { NutrientsPalette() }
