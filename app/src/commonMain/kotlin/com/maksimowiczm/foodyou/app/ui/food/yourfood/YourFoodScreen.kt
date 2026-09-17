package com.maksimowiczm.foodyou.app.ui.food.yourfood

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LunchDining
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.maksimowiczm.foodyou.app.ui.common.component.ArrowBackIconButton
import com.maksimowiczm.foodyou.app.ui.common.component.FoodListItemSkeleton
import com.maksimowiczm.foodyou.app.ui.food.search.FoodSearchListItem
import com.maksimowiczm.foodyou.app.ui.home.shared.FoodYouHomeCard
import com.maksimowiczm.foodyou.common.compose.utility.TestTags
import com.maksimowiczm.foodyou.food.domain.entity.FoodId
import com.maksimowiczm.foodyou.food.search.domain.FoodSearch
import com.valentinilk.shimmer.ShimmerBounds
import com.valentinilk.shimmer.rememberShimmer
import foodyou.app.generated.resources.*
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun YourFoodScreen(
    onBack: () -> Unit,
    onCreateProduct: () -> Unit,
    onCreateRecipe: () -> Unit,
    onEditFood: (FoodId) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: YourFoodViewModel = koinViewModel()
    val selectedIds by viewModel.selectedIds.collectAsStateWithLifecycle()
    val pages = viewModel.items.collectAsLazyPagingItems()
    val shimmer = rememberShimmer(ShimmerBounds.View)

    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    if (showDeleteDialog) {
        DeleteFoodsDialog(
            count = selectedIds.size,
            onDismissRequest = { showDeleteDialog = false },
            onDelete = {
                showDeleteDialog = false
                viewModel.deleteSelected()
            },
        )
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = modifier,
        topBar = {
            if (selectedIds.isEmpty()) {
                TopAppBar(
                    title = { Text(stringResource(Res.string.headline_your_food)) },
                    navigationIcon = { ArrowBackIconButton(onBack) },
                    scrollBehavior = scrollBehavior,
                )
            } else {
                TopAppBar(
                    title = {
                        Text(
                            pluralStringResource(
                                Res.plurals.neutral_foods_selected,
                                selectedIds.size,
                                selectedIds.size,
                            )
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = viewModel::clearSelection,
                            modifier = Modifier.testTag(TestTags.YourFoodCloseSelectionButton),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(Res.string.action_close),
                            )
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = {
                                val ids = (0 until pages.itemCount).mapNotNull { pages[it]?.id }
                                viewModel.selectAllLoaded(ids)
                            },
                            modifier = Modifier.testTag(TestTags.YourFoodSelectAllButton),
                        ) {
                            Text(stringResource(Res.string.action_select_all))
                        }
                        IconButton(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier.testTag(TestTags.YourFoodDeleteButton),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(Res.string.action_delete),
                            )
                        }
                    },
                    scrollBehavior = scrollBehavior,
                )
            }
        },
        floatingActionButton = {
            if (selectedIds.isEmpty()) {
                YourFoodFab(onCreateProduct = onCreateProduct, onCreateRecipe = onCreateRecipe)
            }
        },
    ) { paddingValues ->
        Box(Modifier.fillMaxSize().padding(paddingValues)) {
            FoodYouHomeCard(
                modifier =
                    Modifier.fillMaxSize().padding(horizontal = 8.dp).padding(vertical = 8.dp)
            ) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(count = pages.itemCount, key = pages.itemKey { it.id.toString() }) { i
                        ->
                        val food = pages[i]

                        when (food) {
                            null -> FoodListItemSkeleton(shimmer)
                            is FoodSearch.Product -> {
                                val measurement = food.suggestedMeasurement
                                FoodSearchListItem(
                                    food = food,
                                    measurement = measurement,
                                    onClick = { onEditFood(food.id) },
                                    leadingContent = {
                                        Checkbox(
                                            checked = food.id in selectedIds,
                                            onCheckedChange = {
                                                viewModel.toggleSelection(food.id)
                                            },
                                            modifier =
                                                Modifier.testTag(
                                                    TestTags.yourFoodCheckbox(
                                                        food.id.toString()
                                                    )
                                                ),
                                        )
                                    },
                                )
                            }

                            is FoodSearch.Recipe -> {
                                val measurement = food.suggestedMeasurement
                                FoodSearchListItem(
                                    food = food,
                                    measurement = measurement,
                                    onClick = { onEditFood(food.id) },
                                    shimmer = shimmer,
                                    leadingContent = {
                                        Checkbox(
                                            checked = food.id in selectedIds,
                                            onCheckedChange = {
                                                viewModel.toggleSelection(food.id)
                                            },
                                            modifier =
                                                Modifier.testTag(
                                                    TestTags.yourFoodCheckbox(
                                                        food.id.toString()
                                                    )
                                                ),
                                        )
                                    },
                                )
                            }
                        }
                    }

                    if (pages.loadState.append is LoadState.Loading) {
                        items(10) { FoodListItemSkeleton(shimmer) }
                    }
                }
            }

            if (pages.itemCount == 0 && pages.loadState.append !is LoadState.Loading) {
                Text(
                    text = stringResource(Res.string.neutral_no_food_found),
                    modifier = Modifier.align(Alignment.Center),
                )
            }

            if (pages.loadState.refresh is LoadState.Loading && pages.itemCount == 0) {
                ContainedLoadingIndicator(modifier = Modifier.align(Alignment.TopCenter))
            }
        }
    }
}

@Composable
private fun YourFoodFab(
    onCreateProduct: () -> Unit,
    onCreateRecipe: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Box(modifier) {
        FloatingActionButton(
            onClick = { expanded = true },
            modifier = Modifier.testTag(TestTags.YourFoodFab),
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = stringResource(Res.string.action_create),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.headline_product)) },
                leadingIcon = { Icon(Icons.Default.LunchDining, null) },
                onClick = {
                    expanded = false
                    onCreateProduct()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.headline_recipe)) },
                leadingIcon = {
                    Icon(
                        painter = painterResource(Res.drawable.ic_skillet_filled),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                    )
                },
                onClick = {
                    expanded = false
                    onCreateRecipe()
                },
            )
        }
    }
}

@Composable
private fun DeleteFoodsDialog(
    count: Int,
    onDismissRequest: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = onDelete) { Text(stringResource(Res.string.action_delete)) }
        },
        modifier = modifier,
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(Res.string.action_cancel))
            }
        },
        icon = { Icon(imageVector = Icons.Default.Delete, contentDescription = null) },
        title = {
            Text(pluralStringResource(Res.plurals.headline_delete_foods, count, count))
        },
        text = {
            Text(pluralStringResource(Res.plurals.description_delete_foods, count, count))
        },
    )
}
