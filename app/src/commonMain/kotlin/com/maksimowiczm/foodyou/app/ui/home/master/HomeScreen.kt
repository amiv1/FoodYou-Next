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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
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
import androidx.compose.runtime.snapshotFlow
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
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
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
 * Slides the current day's content off-screen in the direction implied by whether [targetDate]
 * is after or before the currently selected date (content slides left when moving forward, right
 * when moving backward), commits the date change on [homeState], then resets the offset.
 *
 * [targetDate] doesn't have to be the literal adjacent day - the revealed neighbor slot is made to
 * display [targetDate]'s content instead of the real adjacent day's (see [HomeScreen]'s
 * `onJumpWillStart` usage), so jumping to a distant date animates the same one-day-swipe way
 * instead of cutting instantly. Once the animation finishes, the neighbor slot's content already
 * matches [targetDate], so resetting the offset right after the date change lines up exactly with
 * where the content already visually is - no visible jump.
 */
private suspend fun commitJump(
    offsetX: Animatable<Float, AnimationVector1D>,
    homeState: HomeState,
    width: Float,
    targetDate: LocalDate,
    onJumpWillStart: (targetDate: LocalDate, direction: Int) -> Unit = { _, _ -> },
) {
    if (targetDate == homeState.selectedDate) return

    val direction = if (targetDate > homeState.selectedDate) 1 else -1
    onJumpWillStart(targetDate, direction)

    val target = -direction * width
    offsetX.animateTo(target, tween(220))
    homeState.selectDate(targetDate)
    offsetX.snapTo(0f)
}

@Composable
fun HomeScreen(
    onSettings: () -> Unit,
    onYourFood: () -> Unit,
    onDailyGoals: () -> Unit,
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

    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    var containerWidthPx by remember { mutableStateOf(0) }
    val thresholdPx = remember(density) { with(density) { SwipeThreshold.toPx() } }

    val selectedDate = homeState.selectedDate
    val prevDate = selectedDate.minus(1, DateTimeUnit.DAY)
    val nextDate = selectedDate.plus(1, DateTimeUnit.DAY)
    val width = containerWidthPx.toFloat().coerceAtLeast(1f)

    // The date the FAB (and its scroll-collapse state) should react to. Kept in sync with
    // [selectedDate] but updated immediately when a swipe/slide transition *starts* (rather than
    // waiting the ~220ms for the animation to finish and homeState to actually commit the date),
    // so the FAB doesn't lag behind the user's action.
    var fabDisplayDate by remember { mutableStateOf(selectedDate) }
    LaunchedEffect(selectedDate) { fabDisplayDate = selectedDate }

    var hasScrolledSinceDateChange by remember { mutableStateOf(false) }
    LaunchedEffect(fabDisplayDate) { hasScrolledSinceDateChange = false }

    // While a jump to a distant (non-adjacent) date is animating, the revealed neighbor slot
    // (prev or next, depending on [pendingJumpDirection]) displays [pendingJumpDate]'s content
    // instead of the real adjacent day's, so distant jumps slide the same way a one-day swipe
    // does instead of cutting instantly. Cleared once the jump commits.
    var pendingJumpDate by remember { mutableStateOf<LocalDate?>(null) }
    var pendingJumpDirection by remember { mutableStateOf(0) }

    val displayPrevDate =
        if (pendingJumpDirection == -1 && pendingJumpDate != null) pendingJumpDate!! else prevDate
    val displayNextDate =
        if (pendingJumpDirection == 1 && pendingJumpDate != null) pendingJumpDate!! else nextDate
    val showPrevColumn =
        homeState.canSelectPreviousDay || (pendingJumpDirection == -1 && pendingJumpDate != null)
    val showNextColumn =
        homeState.canSelectNextDay || (pendingJumpDirection == 1 && pendingJumpDate != null)

    val onJumpWillStart: (LocalDate, Int) -> Unit = { target, direction ->
        fabDisplayDate = target
        pendingJumpDate = target
        pendingJumpDirection = direction
    }

    val onCalendarDateSelect: (LocalDate) -> Unit = { date ->
        coroutineScope.launch {
            commitJump(
                offsetX,
                homeState,
                width,
                targetDate = date,
                onJumpWillStart = onJumpWillStart,
            )
            pendingJumpDate = null
            pendingJumpDirection = 0
        }
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (fabDisplayDate != homeState.lastKnownToday) {
                ExtendedFloatingActionButton(
                    onClick = { onCalendarDateSelect(homeState.lastKnownToday) },
                    expanded = !hasScrolledSinceDateChange,
                    icon = {
                        Icon(imageVector = Icons.Default.Today, contentDescription = null)
                    },
                    text = { Text(stringResource(Res.string.action_today)) },
                )
            }
        },
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
                            text = { Text(stringResource(Res.string.headline_daily_goals)) },
                            onClick = {
                                showMenu = false
                                onDailyGoals()
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
                        commitJump(
                            offsetX,
                            homeState,
                            width,
                            targetDate = targetDate,
                            onJumpWillStart = onJumpWillStart,
                        )
                        pendingJumpDate = null
                        pendingJumpDirection = 0

                        highlightDate = targetDate
                        highlightMealId = targetMealId
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
                                        commitJump(
                                            offsetX,
                                            homeState,
                                            width,
                                            targetDate =
                                                homeState.selectedDate.plus(1, DateTimeUnit.DAY),
                                            onJumpWillStart = onJumpWillStart,
                                        )

                                    current >= thresholdPx && homeState.canSelectPreviousDay ->
                                        commitJump(
                                            offsetX,
                                            homeState,
                                            width,
                                            targetDate =
                                                homeState.selectedDate.minus(1, DateTimeUnit.DAY),
                                            onJumpWillStart = onJumpWillStart,
                                        )

                                    else ->
                                        offsetX.animateTo(
                                            0f,
                                            spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                        )
                                }
                                pendingJumpDate = null
                                pendingJumpDirection = 0
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
            if (showPrevColumn) {
                key(displayPrevDate) {
                    HomeDayColumn(
                        date = displayPrevDate,
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
                    onScrolled = { hasScrolledSinceDateChange = true },
                    modifier =
                        Modifier.fillMaxSize().offset {
                            IntOffset(offsetX.value.roundToInt(), 0)
                        },
                )
            }

            if (showNextColumn) {
                key(displayNextDate) {
                    HomeDayColumn(
                        date = displayNextDate,
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
    onScrolled: () -> Unit = {},
) {
    val listState = rememberLazyListState()
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .drop(1)
            .first()
        onScrolled()
    }

    LazyColumn(
        state = listState,
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
