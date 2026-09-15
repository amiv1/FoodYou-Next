package com.maksimowiczm.foodyou.app.ui.home.meals.card

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.maksimowiczm.foodyou.app.ui.food.diary.component.ChipsMealPicker
import com.maksimowiczm.foodyou.app.ui.food.diary.component.rememberChipsMealPickerState
import com.maksimowiczm.foodyou.common.compose.utility.LocalDateFormatter
import com.maksimowiczm.foodyou.common.domain.date.DateProvider
import com.maksimowiczm.foodyou.common.domain.userpreferences.UserPreferencesRepository
import com.maksimowiczm.foodyou.common.extension.now
import com.maksimowiczm.foodyou.settings.domain.entity.Settings
import foodyou.app.generated.resources.*
import kotlin.time.Instant
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.core.qualifier.named

/**
 * Dialog that lets the user pick a target meal and date (never in the future) to copy the
 * currently selected meal's entries into.
 */
@Composable
internal fun CopyMealDialog(
    sourceMealName: String,
    sourceDate: LocalDate,
    meals: List<MealOption>,
    onDismissRequest: () -> Unit,
    onConfirm: (targetMealId: Long, targetDate: LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val zeroDate = remember { LocalDate.fromEpochDays(0) }
    val dateProvider = koinInject<DateProvider>()
    val today = dateProvider.observeDate().collectAsStateWithLifecycle(LocalDate.now()).value
    val settingsRepository =
        koinInject<UserPreferencesRepository<Settings>>(named(Settings::class.qualifiedName!!))
    val allowFutureDates =
        settingsRepository
            .observe()
            .map { it.allowFutureDates }
            .collectAsStateWithLifecycle(false)
            .value
    val maxDate = if (allowFutureDates) today.plus(100, DateTimeUnit.YEAR) else today
    var targetDate by rememberSaveable { mutableStateOf(sourceDate.coerceIn(zeroDate, maxDate)) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    val mealNames = remember(meals) { meals.map { it.name } }
    val mealPickerState =
        rememberChipsMealPickerState(meals = mealNames, selectedMeal = sourceMealName)

    if (showDatePicker) {
        CopyMealDatePickerDialog(
            selectedDate = targetDate,
            zeroDate = zeroDate,
            referenceDate = maxDate,
            onDateSelect = { targetDate = it },
            onDismissRequest = { showDatePicker = false },
        )
    }

    val dateFormatter = LocalDateFormatter.current

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(Res.string.headline_copy_meal)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                FilledTonalButton(onClick = { showDatePicker = true }) {
                    Text(dateFormatter.formatDate(targetDate))
                }

                ChipsMealPicker(state = mealPickerState, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val targetMealId =
                        meals.firstOrNull { it.name == mealPickerState.selectedMeal }?.id
                    if (targetMealId != null) {
                        onConfirm(targetMealId, targetDate)
                    }
                    onDismissRequest()
                },
                enabled = mealPickerState.selectedMeal != null,
            ) {
                Text(stringResource(Res.string.action_copy))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(Res.string.action_cancel))
            }
        },
        modifier = modifier,
    )
}

@Composable
private fun CopyMealDatePickerDialog(
    selectedDate: LocalDate,
    zeroDate: LocalDate,
    referenceDate: LocalDate,
    onDateSelect: (LocalDate) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val state = rememberCopyMealDatePickerState(selectedDate, zeroDate, referenceDate)

    LaunchedEffect(state) {
        snapshotFlow { state.selectedDateMillis }
            .drop(1)
            .collect { millis ->
                millis?.let {
                    onDateSelect(Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.UTC).date)
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
                        onDateSelect(
                            Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.UTC).date
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
            modifier = Modifier.verticalScroll(rememberScrollState()).padding(bottom = 8.dp),
        )
    }
}

@Composable
private fun rememberCopyMealDatePickerState(
    selectedDate: LocalDate,
    zeroDate: LocalDate,
    referenceDate: LocalDate,
): DatePickerState {
    val yearRange = zeroDate.year..referenceDate.year
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
                    return date in zeroDate..referenceDate
                }

                override fun isSelectableYear(year: Int) = year in yearRange
            },
    )
}

internal data class MealOption(val id: Long, val name: String)
