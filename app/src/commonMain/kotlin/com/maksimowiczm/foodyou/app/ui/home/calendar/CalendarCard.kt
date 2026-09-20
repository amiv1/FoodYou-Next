package com.maksimowiczm.foodyou.app.ui.home.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.maksimowiczm.foodyou.app.ui.home.shared.FoodYouHomeCard
import com.maksimowiczm.foodyou.common.compose.utility.LocalDateFormatter
import com.maksimowiczm.foodyou.common.compose.utility.TestTags
import foodyou.app.generated.resources.*
import kotlin.time.Instant
import kotlinx.coroutines.flow.drop
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun CalendarCard(
    date: LocalDate,
    referenceDate: LocalDate,
    onDateSelect: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    maxDate: LocalDate = referenceDate,
) {
    val calendarState =
        rememberCalendarState(referenceDate = referenceDate, maxDate = maxDate, selectedDate = date)

    LaunchedEffect(calendarState.selectedDate) {
        val selected = calendarState.selectedDate

        if (selected != date) {
            onDateSelect(selected)
        }
    }

    CalendarCard(calendarState = calendarState, modifier = modifier)
}

@Composable
private fun CalendarCard(calendarState: CalendarState, modifier: Modifier = Modifier) {
    val dateFormatter = LocalDateFormatter.current
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    if (showDatePicker) {
        CalendarCardDatePickerDialog(
            calendarState = calendarState,
            onDismissRequest = { showDatePicker = false },
        )
    }

    val label =
        if (calendarState.selectedDate == calendarState.referenceDate) {
            stringResource(Res.string.action_today)
        } else {
            dateFormatter.formatMonthDayWeek(calendarState.selectedDate)
        }

    FoodYouHomeCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(
                onClick = { calendarState.selectPreviousDay() },
                modifier = Modifier.testTag(TestTags.CalendarPreviousDayButton),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                    contentDescription = stringResource(Res.string.action_previous_day),
                )
            }

            FilledTonalButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.weight(1f).fillMaxHeight().testTag(TestTags.CalendarDateButton),
                shape = MaterialTheme.shapes.medium,
                contentPadding = ButtonDefaults.TextButtonContentPadding,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                )
            }

            IconButton(
                onClick = { calendarState.selectNextDay() },
                enabled = calendarState.canSelectNextDay,
                modifier = Modifier.testTag(TestTags.CalendarNextDayButton),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = stringResource(Res.string.action_next_day),
                )
            }
        }
    }
}

@Composable
private fun CalendarCardDatePickerDialog(
    calendarState: CalendarState,
    onDismissRequest: () -> Unit,
) {
    val state = calendarState.rememberDatePickerState()

    LaunchedEffect(state) {
        snapshotFlow { state.selectedDateMillis }
            .drop(1)
            .collect { millis ->
                millis?.let {
                    calendarState.onDateSelect(
                        date = Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.UTC).date
                    )
                }
                onDismissRequest()
            }
    }

    DatePickerDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                onClick = {
                    state.selectedDateMillis?.let {
                        calendarState.onDateSelect(
                            date =
                                Instant.fromEpochMilliseconds(it)
                                    .toLocalDateTime(TimeZone.UTC)
                                    .date
                        )
                    }
                    onDismissRequest()
                }
            ) {
                Text(text = stringResource(Res.string.positive_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(Res.string.action_cancel))
            }
        },
    ) {
        DatePicker(
            state = state,
            title = {
                Row(
                    modifier =
                        Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp, top = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    DatePickerDefaults.DatePickerTitle(displayMode = state.displayMode)

                    TextButton(
                        onClick = {
                            calendarState.onDateSelect(date = calendarState.referenceDate)
                            onDismissRequest()
                        }
                    ) {
                        Text(stringResource(Res.string.action_go_to_today))
                    }
                }
            },
            // It won't fit on small screens, so we need to scroll
            modifier = Modifier.verticalScroll(rememberScrollState()),
        )
    }
}
