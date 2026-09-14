package com.maksimowiczm.foodyou.app.ui.home.shared

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maksimowiczm.foodyou.common.domain.date.DateProvider
import com.maksimowiczm.foodyou.common.extension.now
import com.valentinilk.shimmer.Shimmer
import com.valentinilk.shimmer.ShimmerBounds
import com.valentinilk.shimmer.rememberShimmer
import kotlinx.datetime.LocalDate
import org.koin.compose.koinInject

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

    return homeState
}

@Stable
internal class HomeState(initialSelectedDate: LocalDate, initialToday: LocalDate, val shimmer: Shimmer) {
    var selectedDate by mutableStateOf(initialSelectedDate)
        private set

    /** The most recently observed "today" date, used to detect a midnight rollover. */
    var lastKnownToday by mutableStateOf(initialToday)
        private set

    fun selectDate(date: LocalDate) {
        selectedDate = date
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
