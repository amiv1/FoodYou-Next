package com.maksimowiczm.foodyou.app.ui.food.diary.search

import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LunchDining
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maksimowiczm.foodyou.app.ui.common.component.ArrowBackIconButton
import com.maksimowiczm.foodyou.app.ui.food.search.FoodSearchApp
import com.maksimowiczm.foodyou.common.compose.extension.LaunchedCollectWithLifecycle
import com.maksimowiczm.foodyou.common.compose.extension.toDp
import com.maksimowiczm.foodyou.common.compose.utility.LocalDateFormatter
import com.maksimowiczm.foodyou.common.domain.measurement.Measurement
import com.maksimowiczm.foodyou.food.domain.entity.FoodId
import com.valentinilk.shimmer.shimmer
import foodyou.app.generated.resources.*
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun DiaryFoodSearchScreen(
    onBack: () -> Unit,
    onCreateRecipe: () -> Unit,
    onCreateProduct: () -> Unit,
    onMeasure: (FoodId, Measurement) -> Unit,
    onUpdateUsdaApiKey: () -> Unit,
    onUpdateOpenFoodFactsCredentials: () -> Unit,
    date: LocalDate,
    mealId: Long,
    modifier: Modifier = Modifier,
) {
    val dateFormatter = LocalDateFormatter.current

    val viewModel: DiaryFoodSearchViewModel = koinViewModel { parametersOf(mealId) }
    val meal = viewModel.meal.collectAsStateWithLifecycle().value

    val snackBarHostState = remember { SnackbarHostState() }
    val message = stringResource(Res.string.neutral_measurement_added)

    LaunchedCollectWithLifecycle(viewModel.newEntryEvents) {
        snackBarHostState.showSnackbar(message)
    }

    var createMenuExpanded by rememberSaveable { mutableStateOf(false) }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val topBar =
        @Composable {
            TopAppBar(
                title = {
                    updateTransition(meal).Crossfade(contentKey = { it?.toString() }) {
                        if (meal == null) {
                            Spacer(
                                modifier =
                                    Modifier.height(LocalTextStyle.current.toDp() - 4.dp)
                                        .width(100.dp)
                                        .padding(bottom = 4.dp)
                                        .shimmer()
                                        .clip(MaterialTheme.shapes.medium)
                                        .background(
                                            MaterialTheme.colorScheme.surfaceContainerHighest
                                        )
                            )
                        } else {
                            Text(meal.name)
                        }
                    }
                },
                subtitle = { Text(dateFormatter.formatDate(date)) },
                titleHorizontalAlignment = Alignment.CenterHorizontally,
                navigationIcon = { ArrowBackIconButton(onBack) },
                actions = {
                    Box {
                        IconButton(onClick = { createMenuExpanded = true }) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = stringResource(Res.string.action_create),
                            )
                        }
                        DropdownMenu(
                            expanded = createMenuExpanded,
                            onDismissRequest = { createMenuExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.headline_product)) },
                                leadingIcon = { Icon(Icons.Default.LunchDining, null) },
                                onClick = {
                                    createMenuExpanded = false
                                    onCreateProduct()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.headline_recipe)) },
                                leadingIcon = {
                                    Icon(
                                        painter =
                                            painterResource(Res.drawable.ic_skillet_filled),
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp),
                                    )
                                },
                                onClick = {
                                    createMenuExpanded = false
                                    onCreateRecipe()
                                },
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        }
    val content: @Composable (PaddingValues) -> Unit =
        @Composable { paddingValues ->
            FoodSearchApp(
                onFoodClick = { model, measurement -> onMeasure(model.id, measurement) },
                onUpdateUsdaApiKey = onUpdateUsdaApiKey,
                onUpdateOpenFoodFactsCredentials = onUpdateOpenFoodFactsCredentials,
                modifier =
                    Modifier.padding(paddingValues)
                        .consumeWindowInsets(paddingValues)
                        .nestedScroll(scrollBehavior.nestedScrollConnection),
            )
        }

    Scaffold(
        modifier = modifier,
        topBar = topBar,
        snackbarHost = { SnackbarHost(snackBarHostState) },
        content = content,
    )
}

