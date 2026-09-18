package com.maksimowiczm.foodyou.app.ui.home.master

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maksimowiczm.foodyou.app.ui.home.calendar.CalendarCard
import com.maksimowiczm.foodyou.app.ui.home.goals.GoalsCard
import com.maksimowiczm.foodyou.app.ui.home.meals.card.MealsCards
import com.maksimowiczm.foodyou.app.ui.home.poll.PollsCard
import com.maksimowiczm.foodyou.app.ui.home.shared.HomeState
import com.maksimowiczm.foodyou.app.ui.home.shared.rememberHomeState
import com.maksimowiczm.foodyou.common.compose.utility.TestTags
import com.maksimowiczm.foodyou.settings.domain.entity.HomeCard
import com.valentinilk.shimmer.Shimmer
import foodyou.app.generated.resources.*
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** Minimum horizontal drag distance required to trigger a date change. */
private val SwipeThreshold = 72.dp

/** How much a drag is damped when it's dragged past an allowed bound (rubber-banding). */
private const val RubberBandDamping = 0.35f

/**
 * Animates [offsetX] the rest of the way off-screen in the direction implied by [direction]
 * (`1` = next day, content slides left; `-1` = previous day, content slides right), commits the
 * date change on [homeState], then resets the offset. Since the neighbor day's real content is
 * already rendered at the destination position, resetting the offset right after the date change
 * lines up exactly with where the content already visually is - no visible jump.
 */
private suspend fun commitSwipe(
    offsetX: Animatable<Float, AnimationVector1D>,
    homeState: HomeState,
    width: Float,
    direction: Int,
) {
    val target = -direction * width
    offsetX.animateTo(target, tween(220))
    if (direction > 0) homeState.selectNextDay() else homeState.selectPreviousDay()
    offsetX.snapTo(0f)
}

@Composable
fun HomeScreen(
    onSettings: () -> Unit,
    onYourFood: () -> Unit,
    onTitle: () -> Unit,
    onMealCardLongClick: (mealId: Long) -> Unit,
    onMealCardAddClick: (epochDay: Long, mealId: Long) -> Unit,
    onMealCardQuickAddClick: (epochDay: Long, mealId: Long) -> Unit,
    onGoalsCardLongClick: () -> Unit,
    onGoalsCardClick: (epochDay: Long) -> Unit,
    onEditDiaryEntryClick: (foodEntryId: Long?, manualEntryId: Long?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: HomeViewModel = koinViewModel()
    val order by viewModel.homeOrder.collectAsStateWithLifecycle()
    val homeState = rememberHomeState()
    val snackbarHostState = remember { SnackbarHostState() }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.app_name),
                        modifier =
                            Modifier.clickable(
                                interactionSource = null,
                                indication = null,
                                onClick = onTitle,
                            ),
                    )
                },
                actions = {
                    var showMenu by remember { mutableStateOf(false) }
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.testTag(TestTags.HomeOverflowMenuButton),
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(Res.string.action_show_more),
                        )
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.headline_your_food)) },
                            modifier =
                                Modifier.testTag(TestTags.HomeMyFoodAndRecipesMenuItem),
                            onClick = {
                                showMenu = false
                                onYourFood()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.headline_settings)) },
                            onClick = {
                                showMenu = false
                                onSettings()
                            },
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { paddingValues ->
        val density = LocalDensity.current
        val coroutineScope = rememberCoroutineScope()
        val offsetX = remember { Animatable(0f) }
        var containerWidthPx by remember { mutableStateOf(0) }
        val thresholdPx = remember(density) { with(density) { SwipeThreshold.toPx() } }

        val selectedDate = homeState.selectedDate
        val prevDate = selectedDate.minus(1, DateTimeUnit.DAY)
        val nextDate = selectedDate.plus(1, DateTimeUnit.DAY)
        val width = containerWidthPx.toFloat().coerceAtLeast(1f)

        var highlightMealId by remember { mutableStateOf<Long?>(null) }
        var highlightDate by remember { mutableStateOf<LocalDate?>(null) }
        val viewActionLabel = stringResource(Res.string.action_view)

        // The highlighted MealCard itself requests to be brought into view (via
        // BringIntoViewRequester, which bubbles through every ancestor scrollable) as soon as it
        // composes with highlighted = true, so no manual outer-list scroll is needed here - it
        // would only bring the "Meals" section's own top edge into view, not the specific card
        // nested inside it. This effect just clears the highlight after a short delay.
        LaunchedEffect(highlightMealId, highlightDate, selectedDate) {
            if (highlightMealId != null && highlightDate == selectedDate) {
                delay(2_500)
                highlightMealId = null
                highlightDate = null
            }
        }

        val onCopyCompleted: (message: String, targetDate: LocalDate, targetMealId: Long) -> Unit =
            { message, targetDate, targetMealId ->
                coroutineScope.launch {
                    val result =
                        snackbarHostState.showSnackbar(
                            message = message,
                            actionLabel = viewActionLabel,
                            duration = SnackbarDuration.Long,
                        )

                    if (result == SnackbarResult.ActionPerformed) {
                        val diffDays = targetDate.toEpochDays() - selectedDate.toEpochDays()

                        when {
                            diffDays == 1L && homeState.canSelectNextDay ->
                                commitSwipe(offsetX, homeState, width, direction = 1)

                            diffDays == -1L && homeState.canSelectPreviousDay ->
                                commitSwipe(offsetX, homeState, width, direction = -1)

                            else -> {
                                offsetX.snapTo(0f)
                                homeState.selectDate(targetDate)
                            }
                        }

                        highlightDate = targetDate
                        highlightMealId = targetMealId
                    }
                }
            }

        val onCalendarDateSelect: (LocalDate) -> Unit = { date ->
            coroutineScope.launch {
                val diffDays = date.toEpochDays() - selectedDate.toEpochDays()

                when {
                    diffDays == 1L && homeState.canSelectNextDay ->
                        commitSwipe(offsetX, homeState, width, direction = 1)

                    diffDays == -1L && homeState.canSelectPreviousDay ->
                        commitSwipe(offsetX, homeState, width, direction = -1)

                    else -> {
                        offsetX.snapTo(0f)
                        homeState.selectDate(date)
                    }
                }
            }
        }

        Box(
            modifier =
                Modifier.fillMaxSize()
                    .testTag(TestTags.HomeDateSwipeArea)
                    .onSizeChanged { containerWidthPx = it.width }
                    .pointerInput(homeState) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            coroutineScope.launch {
                                val current = offsetX.value

                                when {
                                    current <= -thresholdPx && homeState.canSelectNextDay ->
                                        commitSwipe(offsetX, homeState, width, direction = 1)

                                    current >= thresholdPx && homeState.canSelectPreviousDay ->
                                        commitSwipe(offsetX, homeState, width, direction = -1)

                                    else ->
                                        offsetX.animateTo(
                                            0f,
                                            spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                        )
                                }
                            }
                        },
                        onDragCancel = {
                            coroutineScope.launch {
                                offsetX.animateTo(
                                    0f,
                                    spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                )
                            }
                        },
                    ) { change, dragAmount ->
                        change.consume()

                        val blockedNext = dragAmount < 0f && !homeState.canSelectNextDay
                        val blockedPrevious = dragAmount > 0f && !homeState.canSelectPreviousDay
                        val dampedAmount =
                            if (blockedNext || blockedPrevious) {
                                dragAmount * RubberBandDamping
                            } else {
                                dragAmount
                            }

                        coroutineScope.launch { offsetX.snapTo(offsetX.value + dampedAmount) }
                    }
                }
        ) {
            if (homeState.canSelectPreviousDay) {
                key(prevDate) {
                    HomeDayColumn(
                        date = prevDate,
                        referenceDate = homeState.lastKnownToday,
                        maxSelectableDate = homeState.maxSelectableDate,
                        shimmer = homeState.shimmer,
                        order = order,
                        onDateSelect = onCalendarDateSelect,
                        onMealCardLongClick = onMealCardLongClick,
                        onMealCardAddClick = onMealCardAddClick,
                        onMealCardQuickAddClick = onMealCardQuickAddClick,
                        onGoalsCardLongClick = onGoalsCardLongClick,
                        onGoalsCardClick = onGoalsCardClick,
                        onEditDiaryEntryClick = onEditDiaryEntryClick,
                        onCopyCompleted = onCopyCompleted,
                        scrollBehavior = scrollBehavior,
                        contentPadding = paddingValues,
                        modifier =
                            Modifier.fillMaxSize().offset {
                                IntOffset((-width + offsetX.value).roundToInt(), 0)
                            },
                    )
                }
            }

            key(selectedDate) {
                HomeDayColumn(
                    date = selectedDate,
                    referenceDate = homeState.lastKnownToday,
                    maxSelectableDate = homeState.maxSelectableDate,
                    shimmer = homeState.shimmer,
                    order = order,
                    onDateSelect = onCalendarDateSelect,
                    onMealCardLongClick = onMealCardLongClick,
                    onMealCardAddClick = onMealCardAddClick,
                    onMealCardQuickAddClick = onMealCardQuickAddClick,
                    onGoalsCardLongClick = onGoalsCardLongClick,
                    onGoalsCardClick = onGoalsCardClick,
                    onEditDiaryEntryClick = onEditDiaryEntryClick,
                    onCopyCompleted = onCopyCompleted,
                    scrollBehavior = scrollBehavior,
                    contentPadding = paddingValues,
                    highlightMealId = if (highlightDate == selectedDate) highlightMealId else null,
                    modifier =
                        Modifier.fillMaxSize().offset {
                            IntOffset(offsetX.value.roundToInt(), 0)
                        },
                )
            }

            if (homeState.canSelectNextDay) {
                key(nextDate) {
                    HomeDayColumn(
                        date = nextDate,
                        referenceDate = homeState.lastKnownToday,
                        maxSelectableDate = homeState.maxSelectableDate,
                        shimmer = homeState.shimmer,
                        order = order,
                        onDateSelect = onCalendarDateSelect,
                        onMealCardLongClick = onMealCardLongClick,
                        onMealCardAddClick = onMealCardAddClick,
                        onMealCardQuickAddClick = onMealCardQuickAddClick,
                        onGoalsCardLongClick = onGoalsCardLongClick,
                        onGoalsCardClick = onGoalsCardClick,
                        onEditDiaryEntryClick = onEditDiaryEntryClick,
                        onCopyCompleted = onCopyCompleted,
                        scrollBehavior = scrollBehavior,
                        contentPadding = paddingValues,
                        modifier =
                            Modifier.fillMaxSize().offset {
                                IntOffset((width + offsetX.value).roundToInt(), 0)
                            },
                    )
                }
            }
        }
    }
}

/**
 * A single day's worth of home-screen content (polls + the user-orderable Calendar/Goals/Meals
 * cards), bound to a fixed [date]. Used to render the previous/current/next day simultaneously
 * side by side so the whole group can be dragged as one during the swipe-to-change-date gesture.
 */
@Composable
private fun HomeDayColumn(
    date: LocalDate,
    referenceDate: LocalDate,
    maxSelectableDate: LocalDate,
    shimmer: Shimmer,
    order: List<HomeCard>,
    onDateSelect: (LocalDate) -> Unit,
    onMealCardLongClick: (mealId: Long) -> Unit,
    onMealCardAddClick: (epochDay: Long, mealId: Long) -> Unit,
    onMealCardQuickAddClick: (epochDay: Long, mealId: Long) -> Unit,
    onGoalsCardLongClick: () -> Unit,
    onGoalsCardClick: (epochDay: Long) -> Unit,
    onEditDiaryEntryClick: (foodEntryId: Long?, manualEntryId: Long?) -> Unit,
    onCopyCompleted: (message: String, targetDate: LocalDate, targetMealId: Long) -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    highlightMealId: Long? = null,
) {
    LazyColumn(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentPadding = contentPadding,
    ) {
        item {
            PollsCard(modifier = Modifier.padding(horizontal = 8.dp).padding(bottom = 8.dp))
        }

        items(order) {
            when (it) {
                HomeCard.Calendar ->
                    CalendarCard(
                        date = date,
                        referenceDate = referenceDate,
                        onDateSelect = onDateSelect,
                        maxDate = maxSelectableDate,
                        modifier = Modifier.padding(horizontal = 8.dp).padding(bottom = 8.dp),
                    )

                HomeCard.Goals ->
                    GoalsCard(
                        date = date,
                        shimmer = shimmer,
                        onClick = onGoalsCardClick,
                        onLongClick = onGoalsCardLongClick,
                        modifier = Modifier.padding(horizontal = 8.dp).padding(bottom = 8.dp),
                    )

                HomeCard.Meals ->
                    MealsCards(
                        date = date,
                        shimmer = shimmer,
                        onAdd = onMealCardAddClick,
                        onQuickAdd = onMealCardQuickAddClick,
                        onEditEntry = onEditDiaryEntryClick,
                        onLongClick = onMealCardLongClick,
                        onCopyCompleted = onCopyCompleted,
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        highlightMealId = highlightMealId,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
            }
        }
    }
}
