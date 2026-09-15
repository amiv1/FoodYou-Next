package com.maksimowiczm.foodyou.app.ui.home.calendar

import androidx.compose.material3.DatePickerState
import androidx.compose.material3.SelectableDates
import androidx.compose.runtime.*
import com.maksimowiczm.foodyou.common.extension.now
import kotlin.time.Instant
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

@Composable
internal fun rememberCalendarState(
    zeroDate: LocalDate = LocalDate.fromEpochDays(0),
    referenceDate: LocalDate = LocalDate.now(),
    maxDate: LocalDate = referenceDate,
    selectedDate: LocalDate = referenceDate,
): CalendarState =
    remember(zeroDate, referenceDate, maxDate, selectedDate) {
        CalendarState(
            zeroDate = zeroDate,
            referenceDate = referenceDate,
            maxDate = maxDate,
            initialSelectedDate = selectedDate,
        )
    }

/**
 * Holds the state for the compact calendar control shown on the home screen: the currently
 * selected date, the reference ("today") date, and the bounds used by the date picker dialog.
 * Navigating to a date later than [maxDate] is never allowed.
 */
@Stable
internal class CalendarState(
    val zeroDate: LocalDate,
    val referenceDate: LocalDate,
    val maxDate: LocalDate = referenceDate,
    initialSelectedDate: LocalDate = referenceDate,
) {
    var selectedDate by mutableStateOf(initialSelectedDate)
        private set

    /** Whether [selectedDate] can move forward, i.e. it hasn't already reached [maxDate]. */
    val canSelectNextDay: Boolean
        get() = selectedDate < maxDate

    fun onDateSelect(date: LocalDate) {
        selectedDate = date.coerceIn(zeroDate, maxDate)
    }

    fun selectPreviousDay() {
        onDateSelect(selectedDate.minus(1, DateTimeUnit.DAY))
    }

    fun selectNextDay() {
        if (canSelectNextDay) {
            onDateSelect(selectedDate.plus(1, DateTimeUnit.DAY))
        }
    }

    @Composable
    fun rememberDatePickerState(): DatePickerState {
        val yearRange = zeroDate.year..maxDate.year

        val initialSelectedDateMillis =
            selectedDate.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds().takeIf { it >= 0 } ?: 0

        return androidx.compose.material3.rememberDatePickerState(
            initialSelectedDateMillis = initialSelectedDateMillis,
            initialDisplayedMonthMillis = initialSelectedDateMillis,
            yearRange = yearRange,
            selectableDates =
                object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                        val date =
                            Instant.fromEpochMilliseconds(utcTimeMillis)
                                .toLocalDateTime(TimeZone.UTC)
                                .date
                        return date in zeroDate..maxDate
                    }

                    override fun isSelectableYear(year: Int) = year in yearRange
                },
        )
    }
}
