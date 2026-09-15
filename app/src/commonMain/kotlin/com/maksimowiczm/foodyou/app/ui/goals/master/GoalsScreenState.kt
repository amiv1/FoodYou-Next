package com.maksimowiczm.foodyou.app.ui.goals.master

import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.SelectableDates
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maksimowiczm.foodyou.common.domain.date.DateProvider
import com.maksimowiczm.foodyou.common.domain.userpreferences.UserPreferencesRepository
import com.maksimowiczm.foodyou.common.extension.now
import com.maksimowiczm.foodyou.common.extension.plus
import com.maksimowiczm.foodyou.settings.domain.entity.Settings
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.until
import org.koin.compose.koinInject
import org.koin.core.qualifier.named

/**
 * The maximum number of pages allowed in the pager. This limit is set to 50,000 to ensure
 * reasonable performance and memory usage, while allowing users to select dates far into the past
 * and future.
 */
private const val MAX_PAGER_SIZE = 50_000

@Composable
internal fun rememberGoalsScreenState(selectedDate: LocalDate): GoalsScreenState {
    val dateProvider = koinInject<DateProvider>()
    val today by dateProvider.observeDate().collectAsStateWithLifecycle(LocalDate.now())

    val settingsRepository =
        koinInject<UserPreferencesRepository<Settings>>(named(Settings::class.qualifiedName!!))
    val allowFutureDates by
        settingsRepository.observe().map { it.allowFutureDates }.collectAsStateWithLifecycle(false)

    val zeroDate = LocalDate.fromEpochDays(0)
    val initialPage = (zeroDate.until(selectedDate, DateTimeUnit.DAY)).toInt()

    val maxSize =
        if (allowFutureDates) {
            MAX_PAGER_SIZE
        } else {
            (zeroDate.until(today, DateTimeUnit.DAY).toInt() + 1).coerceAtLeast(1)
        }

    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { maxSize })
    val coroutineScope = rememberCoroutineScope()

    return remember(pagerState, coroutineScope, selectedDate) {
        GoalsScreenState(
            coroutineScope = coroutineScope,
            zeroDate = zeroDate,
            pagerState = pagerState,
        )
    }
}

internal class GoalsScreenState(
    private val coroutineScope: CoroutineScope,
    private val zeroDate: LocalDate,
    val pagerState: PagerState,
) {
    val selectedDate by derivedStateOf { zeroDate.plus((pagerState.currentPage).days) }

    fun goToToday() {
        goToDate(LocalDate.now())
    }

    fun goToDate(date: LocalDate) {
        val page = (zeroDate.until(date, DateTimeUnit.DAY)).toInt()
        coroutineScope.launch { pagerState.animateScrollToPage(page) }
    }

    fun dateForPage(page: Int) = zeroDate.plus(page.days)

    @Composable
    fun rememberDatePickerState(): DatePickerState {
        val lastDate = zeroDate + (pagerState.pageCount - 1).days
        val yearRange = zeroDate.year..lastDate.year

        val initialSelectedDateMillis =
            selectedDate.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds().takeIf { it >= 0 } ?: 0

        val initialDisplayedMonthMillis =
            selectedDate.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds().takeIf { it >= 0 } ?: 0

        return androidx.compose.material3.rememberDatePickerState(
            initialSelectedDateMillis = initialSelectedDateMillis,
            initialDisplayedMonthMillis = initialDisplayedMonthMillis,
            yearRange = yearRange,
            selectableDates =
                object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                        val date =
                            Instant.fromEpochMilliseconds(utcTimeMillis)
                                .toLocalDateTime(TimeZone.UTC)
                                .date
                        return date in zeroDate..lastDate
                    }

                    override fun isSelectableYear(year: Int) = year in yearRange
                },
        )
    }
}
