package com.maksimowiczm.foodyou.app.ui.food.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.maksimowiczm.foodyou.app.ui.common.form.FormField
import com.maksimowiczm.foodyou.app.ui.common.form.nullableFloatParser
import com.maksimowiczm.foodyou.app.ui.common.form.positiveFloatValidator
import com.maksimowiczm.foodyou.app.ui.common.form.rememberFormField
import com.maksimowiczm.foodyou.app.ui.common.utility.Saver
import com.maksimowiczm.foodyou.app.ui.common.utility.stringResource
import com.maksimowiczm.foodyou.common.compose.utility.formatClipZeros
import com.maksimowiczm.foodyou.common.domain.measurement.Measurement
import com.maksimowiczm.foodyou.common.domain.measurement.MeasurementType
import com.maksimowiczm.foodyou.common.domain.measurement.from
import com.maksimowiczm.foodyou.common.domain.measurement.rawValue
import com.maksimowiczm.foodyou.common.domain.measurement.type

@Composable
fun MeasurementPicker(state: MeasurementPickerState, modifier: Modifier = Modifier) {
    val latestState by rememberUpdatedState(state)
    LaunchedEffect(state.inputField.value, state.type) {
        val value = state.inputField.value ?: return@LaunchedEffect
        val measurement = Measurement.from(state.type, value.toDouble())
        latestState.measurement = measurement
    }

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        MeasurementScrubber(
            measurement = state.measurement,
            onMeasurementChange = { newMeasurement ->
                state.type = newMeasurement.type
                state.inputField.textFieldState.setTextAndPlaceCursorAtEnd(
                    newMeasurement.rawValue.formatClipZeros()
                )
            },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(8.dp))

        MeasurementTypeSelector(
            type = state.type,
            types = state.possibleTypes,
            onSelect = { state.type = it },
        )

        Spacer(Modifier.height(8.dp))

        FlowRow(
            modifier = Modifier.padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            state.suggestions.forEach { measurement ->
                SuggestionChip(
                    onClick = {
                        state.inputField.textFieldState.setTextAndPlaceCursorAtEnd(
                            text = measurement.rawValue.formatClipZeros()
                        )
                        state.type = measurement.type
                    },
                    label = { Text(measurement.stringResource()) },
                )
            }
        }
    }
}

@Composable
private fun MeasurementTypeSelector(
    type: MeasurementType,
    types: List<MeasurementType>,
    onSelect: (MeasurementType) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Box(modifier = modifier) {
        Surface(
            onClick = { expanded = true },
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shape = RoundedCornerShape(16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            ) {
                Text(text = type.stringResource())
                Icon(imageVector = Icons.Outlined.KeyboardArrowDown, contentDescription = null)
            }
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            types.forEach {
                DropdownMenuItem(
                    text = { Text(it.stringResource()) },
                    onClick = {
                        onSelect(it)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
fun rememberMeasurementPickerState(
    suggestions: List<Measurement>,
    possibleTypes: List<MeasurementType>,
    selectedMeasurement: Measurement,
): MeasurementPickerState {
    val inputField =
        rememberFormField(
            initialValue = selectedMeasurement.rawValue.toFloat(),
            parser = nullableFloatParser(onNotANumber = { "Invalid number format" }),
            validator =
                positiveFloatValidator(
                    onNotPositive = { "Value must be positive" },
                    onNull = { "Value cannot be empty" },
                ),
            textFieldState = rememberTextFieldState(selectedMeasurement.rawValue.formatClipZeros()),
            validateFirst = true,
        )
    val typeState = rememberSaveable { mutableStateOf(selectedMeasurement.type) }
    val measurementState =
        rememberSaveable(selectedMeasurement, stateSaver = Measurement.Saver) {
            mutableStateOf(selectedMeasurement)
        }

    return remember(suggestions, possibleTypes, inputField, typeState, measurementState) {
        MeasurementPickerState(
            suggestions = suggestions,
            possibleTypes = possibleTypes,
            inputField = inputField,
            measurementState = measurementState,
            typeState = typeState,
        )
    }
}

class MeasurementPickerState(
    val suggestions: List<Measurement>,
    val possibleTypes: List<MeasurementType>,
    val inputField: FormField<Float?, String>,
    measurementState: MutableState<Measurement>,
    typeState: MutableState<MeasurementType>,
) {
    var measurement by measurementState
    var type by typeState
}
