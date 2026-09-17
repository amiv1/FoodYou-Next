package com.maksimowiczm.foodyou.app.ui.common.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import com.maksimowiczm.foodyou.app.ui.common.theme.LocalNutrientsPalette
import com.maksimowiczm.foodyou.app.ui.common.utility.LocalNutrientsOrder
import com.maksimowiczm.foodyou.settings.domain.entity.NutrientsOrder
import foodyou.app.generated.resources.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun FoodListItem(
    name: @Composable () -> Unit,
    proteins: @Composable () -> Unit,
    carbohydrates: @Composable () -> Unit,
    fats: @Composable () -> Unit,
    calories: @Composable () -> Unit,
    measurement: @Composable () -> Unit,
    isRecipe: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    containerColor: Color = Color.Transparent,
    contentColor: Color = LocalContentColor.current,
    shape: Shape = RectangleShape,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
) {
    val nutrientsPalette = LocalNutrientsPalette.current
    val order = LocalNutrientsOrder.current

    val headlineContent =
        @Composable {
            CompositionLocalProvider(
                LocalTextStyle provides MaterialTheme.typography.titleMediumEmphasized
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    name()
                    if (isRecipe) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_skillet_filled),
                            contentDescription = stringResource(Res.string.headline_recipe),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }

    val supportingContent =
        @Composable {
            CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.bodyMedium) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        CompositionLocalProvider(
                            LocalTextStyle provides MaterialTheme.typography.bodyMedium
                        ) {
                            calories()
                        }

                        order.forEach { field ->
                            when (field) {
                                NutrientsOrder.Proteins ->
                                    CompositionLocalProvider(
                                        LocalContentColor provides
                                            nutrientsPalette.proteinsOnSurfaceContainer,
                                        LocalTextStyle provides MaterialTheme.typography.bodyMedium,
                                    ) {
                                        proteins()
                                    }

                                NutrientsOrder.Fats ->
                                    CompositionLocalProvider(
                                        LocalContentColor provides
                                            nutrientsPalette.fatsOnSurfaceContainer,
                                        LocalTextStyle provides MaterialTheme.typography.bodyMedium,
                                    ) {
                                        fats()
                                    }

                                NutrientsOrder.Carbohydrates ->
                                    CompositionLocalProvider(
                                        LocalContentColor provides
                                            nutrientsPalette.carbohydratesOnSurfaceContainer,
                                        LocalTextStyle provides MaterialTheme.typography.bodyMedium,
                                    ) {
                                        carbohydrates()
                                    }

                                NutrientsOrder.Other,
                                NutrientsOrder.Vitamins,
                                NutrientsOrder.Minerals -> Unit
                            }
                        }
                    }
                }
            }
            CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.bodyMedium) {
                measurement()
            }
        }

    val content =
        @Composable {
            val layoutDirection = LocalLayoutDirection.current
            val startPadding =
                if (leadingContent != null) {
                    2.dp
                } else {
                    contentPadding.calculateStartPadding(layoutDirection)
                }

            Row(
                modifier =
                    Modifier.padding(
                            start = startPadding,
                            end = contentPadding.calculateEndPadding(layoutDirection),
                            top = contentPadding.calculateTopPadding(),
                            bottom = contentPadding.calculateBottomPadding(),
                        )
                        .height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (leadingContent != null) {
                    leadingContent()
                    Spacer(modifier = Modifier.width(2.dp))
                    VerticalDivider(modifier = Modifier.fillMaxHeight(0.6f))
                    Spacer(modifier = Modifier.width(16.dp))
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    headlineContent()
                    supportingContent()
                }

                if (trailingContent != null) {
                    Spacer(modifier = Modifier.width(16.dp))
                    trailingContent()
                }
            }
        }

    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = modifier,
            color = containerColor,
            contentColor = contentColor,
            shape = shape,
            content = content,
        )
    } else {
        Surface(
            modifier = modifier,
            color = containerColor,
            contentColor = contentColor,
            shape = shape,
            content = content,
        )
    }
}
