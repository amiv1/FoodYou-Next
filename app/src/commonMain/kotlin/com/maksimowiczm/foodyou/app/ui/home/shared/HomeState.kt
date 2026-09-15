package com.maksimowiczm.foodyou.app.ui.home.shared

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maksimowiczm.foodyou.common.domain.date.DateProvider
import com.maksimowiczm.foodyou.common.domain.userpreferences.UserPreferencesRepository
import com.maksimowiczm.foodyou.common.extension.now
import com.maksimowiczm.foodyou.settings.domain.entity.Settings
import com.valentinilk.shimmer.Shimmer
import com.valentinilk.shimmer.ShimmerBounds
import com.valentinilk.shimmer.rememberShimmer
import kotlinx.coroutines.flow.map
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import org.koin.compose.koinInject
import org.koin.core.qualifier.named
@Composable
internal fun rememberHomeState(initialSelectedDate: LocalDate = LocalDate.now()): HomeState {
    val shimmer = rememberShimmer(shimmerBounds = ShimmerBounds.Window)

    val homeState =
        rememberSaveable(
            saver =
                Saver(
                    // Save both the selected date and the date that was "today" when we saved it,
                    // so we can tell on restore whether the user was following today or had
                    // explicitly picked a date in the past.
                    save = { listOf(it.selectedDate.toEpochDays(), it.lastKnownToday.toEpochDays()) },
                    restore = { (selectedDateEpochDays, lastKnownTodayEpochDays) ->
                        val restoredSelectedDate = LocalDate.fromEpochDays(selectedDateEpochDays)
                        val restoredLastKnownToday =
                            LocalDate.fromEpochDays(lastKnownTodayEpochDays)
                        val today = LocalDate.now()

                        // If the user was on "today" when we saved, follow the new "today" instead
                        // of staying on the stale, now-outdated date.
                        val selectedDate =
                            if (restoredSelectedDate == restoredLastKnownToday) today
                            else restoredSelectedDate

                        HomeState(
                            initialSelectedDate = selectedDate,
                            initialToday = today,
                            shimmer = shimmer,
                        )
                    },
                )
        ) {
            HomeState(
                initialSelectedDate = initialSelectedDate,
                initialToday = initialSelectedDate,
                shimmer = shimmer,
            )
        }

    val dateProvider = koinInject<DateProvider>()
    val today by dateProvider.observeDate().collectAsStateWithLifecycle(homeState.lastKnownToday)

    LaunchedEffect(today) { homeState.onTodayChanged(today) }

    val settingsRepository =
        koinInject<UserPreferencesRepository<Settings>>(named(Settings::class.qualifiedName!!))
    val allowFutureDates by
        settingsRepository
            .observe()
            .map { it.allowFutureDates }
            .collectAsStateWithLifecycle(homeState.allowFutureDates)

    LaunchedEffect(allowFutureDates) { homeState.applyAllowFutureDates(allowFutureDates) }

    return homeState
}

@Stable
internal class HomeState(initialSelectedDate: LocalDate, initialToday: LocalDate, val shimmer: Shimmer) {
    private val zeroDate = LocalDate.fromEpochDays(0)

    var selectedDate by mutableStateOf(initialSelectedDate)
        private set

    /** The most recently observed "today" date, used to detect a midnight rollover. */
    var lastKnownToday by mutableStateOf(initialToday)
        private set

    /** Whether picking dates after [lastKnownToday] is currently allowed (a user setting). */
    var allowFutureDates by mutableStateOf(false)
        private set

    /**
     * The furthest date that can currently be selected. Equal to [lastKnownToday] unless
     * [allowFutureDates] is enabled, in which case it's a far (but finite) date in the future.
     */
    val maxSelectableDate: LocalDate
        get() =
            if (allowFutureDates) lastKnownToday.plus(100, DateTimeUnit.YEAR) else lastKnownToday

    /** Whether [selectedDate] can move forward, i.e. it hasn't already reached [maxSelectableDate]. */
    val canSelectNextDay: Boolean
        get() = selectedDate < maxSelectableDate

    /** Whether [selectedDate] can move backward, i.e. it hasn't already reached [zeroDate]. */
    val canSelectPreviousDay: Boolean
        get() = selectedDate > zeroDate

    fun selectDate(date: LocalDate) {
        selectedDate = date
    }

    fun selectPreviousDay() {
        if (canSelectPreviousDay) {
            selectDate(selectedDate.minus(1, DateTimeUnit.DAY))
        }
    }

    fun selectNextDay() {
        if (canSelectNextDay) {
            selectDate(selectedDate.plus(1, DateTimeUnit.DAY))
        }
    }

    fun applyAllowFutureDates(value: Boolean) {
        allowFutureDates = value
    }

    /**
     * Called whenever the system date changes (e.g. at midnight). If [selectedDate] was still
     * following the previous "today", it is advanced to the new [today] too.
     */
    fun onTodayChanged(today: LocalDate) {
        if (today == lastKnownToday) return

        if (selectedDate == lastKnownToday) {
            selectedDate = today
        }
        lastKnownToday = today
    }
}
