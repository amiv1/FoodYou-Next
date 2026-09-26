package com.maksimowiczm.foodyou.app.ui.food.yourfood

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LunchDining
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.maksimowiczm.foodyou.app.ui.common.component.ArrowBackIconButton
import com.maksimowiczm.foodyou.app.ui.common.component.FoodListItemSkeleton
import com.maksimowiczm.foodyou.app.ui.food.search.FoodSearchListItem
import com.maksimowiczm.foodyou.common.compose.utility.TestTags
import com.maksimowiczm.foodyou.food.domain.entity.FoodId
import com.maksimowiczm.foodyou.food.search.domain.FoodSearch
import com.valentinilk.shimmer.ShimmerBounds
import com.valentinilk.shimmer.rememberShimmer
import foodyou.app.generated.resources.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** Vertical spacing around the floating search field, also used as bottom list clearance. */
private val SearchFieldVerticalPadding = 8.dp

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

    val searchFieldState = rememberTextFieldState()
    LaunchedEffect(viewModel) {
        snapshotFlow { searchFieldState.text.toString() }
            .distinctUntilChanged()
            .collectLatest { query -> viewModel.search(query.ifBlank { null }) }
    }

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

    var createMenuExpanded by rememberSaveable { mutableStateOf(false) }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = modifier,
        topBar = {
            if (selectedIds.isEmpty()) {
                TopAppBar(
                    title = { Text(stringResource(Res.string.headline_your_food)) },
                    navigationIcon = { ArrowBackIconButton(onBack) },
                    actions = {
                        Box {
                            IconButton(
                                onClick = { createMenuExpanded = true },
                                modifier = Modifier.testTag(TestTags.YourFoodCreateButton),
                            ) {
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
            } else {
                TopAppBar(
                    title = { Text(selectedIds.size.toString()) },
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
    ) { paddingValues ->
        Box(Modifier.fillMaxSize().padding(paddingValues)) {
            var searchFieldHeight by remember { mutableIntStateOf(0) }
            val density = LocalDensity.current

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding =
                    PaddingValues(
                        bottom =
                            density.run { searchFieldHeight.toDp() } + SearchFieldVerticalPadding
                    ),
            ) {
                items(count = pages.itemCount, key = pages.itemKey { it.id.toString() }) { i ->
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
                                        onCheckedChange = { viewModel.toggleSelection(food.id) },
                                        modifier =
                                            Modifier.testTag(
                                                TestTags.yourFoodCheckbox(food.id.toString())
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
                                        onCheckedChange = { viewModel.toggleSelection(food.id) },
                                        modifier =
                                            Modifier.testTag(
                                                TestTags.yourFoodCheckbox(food.id.toString())
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

            TextField(
                state = searchFieldState,
                modifier =
                    Modifier.fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .windowInsetsPadding(
                            WindowInsets.systemBars
                                .only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)
                                .add(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal))
                        )
                        .onSizeChanged { searchFieldHeight = it.height }
                        .padding(horizontal = 16.dp)
                        .padding(
                            top = SearchFieldVerticalPadding,
                            bottom = SearchFieldVerticalPadding,
                        )
                        .shadow(2.dp, MaterialTheme.shapes.medium)
                        .testTag(TestTags.YourFoodSearchField),
                placeholder = { Text(stringResource(Res.string.action_search)) },
                leadingIcon = {
                    Icon(imageVector = Icons.Outlined.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (searchFieldState.text.isNotEmpty()) {
                        IconButton(onClick = searchFieldState::clearText) {
                            Icon(
                                imageVector = Icons.Outlined.Clear,
                                contentDescription = stringResource(Res.string.action_clear),
                            )
                        }
                    }
                },
                shape = MaterialTheme.shapes.medium,
                colors =
                    TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        unfocusedContainerColor =
                            MaterialTheme.colorScheme.surfaceContainerHighest,
                        disabledContainerColor =
                            MaterialTheme.colorScheme.surfaceContainerHighest,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                    ),
            )

            if (pages.itemCount == 0 && pages.loadState.append !is LoadState.Loading) {
                Column(
                    modifier = Modifier.padding(top = 32.dp).align(Alignment.TopCenter),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Image(
                        painter = painterResource(Res.drawable.mascot_fork_no_recipe),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
                        modifier = Modifier.size(width = 240.dp, height = 352.dp),
                    )
                    Text(text = stringResource(Res.string.neutral_no_food_found))
                }
            }

            if (pages.loadState.refresh is LoadState.Loading && pages.itemCount == 0) {
                ContainedLoadingIndicator(modifier = Modifier.align(Alignment.TopCenter))
            }
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
