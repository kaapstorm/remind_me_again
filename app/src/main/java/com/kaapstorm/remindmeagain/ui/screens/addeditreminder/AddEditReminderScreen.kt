package com.kaapstorm.remindmeagain.ui.screens.addeditreminder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kaapstorm.remindmeagain.R
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditReminderScreen(
    reminderId: Long?,
    onNavigateBack: () -> Unit,
    viewModel: AddEditReminderViewModel = koinViewModel { parametersOf(reminderId) }
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.error) {
        state.error?.let { error ->
            snackbarHostState.showSnackbar(error)
        }
    }

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        if (reminderId == null) stringResource(R.string.add_reminder_title)
                        else stringResource(R.string.edit_reminder_title)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cancel)
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Name Field
            OutlinedTextField(
                value = state.name,
                onValueChange = { viewModel.handleIntent(AddEditReminderIntent.UpdateName(it)) },
                label = { Text(stringResource(R.string.reminder_name)) },
                placeholder = { Text(stringResource(R.string.reminder_name_hint)) },
                isError = state.nameError != null,
                supportingText = state.nameError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                modifier = Modifier.fillMaxWidth()
            )

            // Time Picker
            TimePickerField(
                time = state.time,
                onTimeChange = { viewModel.handleIntent(AddEditReminderIntent.UpdateTime(it)) }
            )

            // Schedule Section
            Text(
                text = stringResource(R.string.reminder_schedule),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )

            ScheduleTypeSelection(
                selectedType = state.scheduleType,
                onTypeSelected = { viewModel.handleIntent(AddEditReminderIntent.UpdateScheduleType(it)) }
            )

            // Schedule-specific options
            when (state.scheduleType) {
                ScheduleType.WEEKLY -> {
                    WeeklyDaySelection(
                        selectedDays = state.weeklySelectedDays,
                        onDaysChanged = { viewModel.handleIntent(AddEditReminderIntent.UpdateWeeklyDays(it)) },
                        error = state.weeklyDaysError
                    )
                }
                ScheduleType.FORTNIGHTLY -> {
                    DatePickerField(
                        label = stringResource(R.string.first_occurrence),
                        selectedDate = state.fortnightlyDate,
                        onDateSelected = { viewModel.handleIntent(AddEditReminderIntent.UpdateFortnightlyDate(it)) },
                        error = state.fortnightlyDateError
                    )
                }
                ScheduleType.MONTHLY -> {
                    DatePickerField(
                        label = stringResource(R.string.first_occurrence),
                        selectedDate = state.monthlyDate,
                        onDateSelected = { viewModel.handleIntent(AddEditReminderIntent.UpdateMonthlyDate(it)) },
                        error = state.monthlyDateError
                    )
                    
                    state.monthlyDate?.let { date ->
                        MonthlyTypeSelection(
                            date = date,
                            selectedType = state.monthlyType,
                            onTypeSelected = { viewModel.handleIntent(AddEditReminderIntent.UpdateMonthlyType(it)) }
                        )
                    }
                }
                ScheduleType.DAILY -> {
                    // No additional options for daily
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.cancel))
                }

                Button(
                    onClick = { viewModel.handleIntent(AddEditReminderIntent.SaveReminder) },
                    enabled = state.isFormValid && !state.isSaving,
                    modifier = Modifier.weight(1f)
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .height(16.dp)
                                .width(16.dp)
                        )
                    } else {
                        Text(stringResource(R.string.save))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerField(
    time: LocalTime,
    onTimeChange: (LocalTime) -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = time.hour,
        initialMinute = time.minute,
        is24Hour = true
    )

    LaunchedEffect(timePickerState.hour, timePickerState.minute) {
        onTimeChange(LocalTime.of(timePickerState.hour, timePickerState.minute))
    }

    Column {
        Text(
            text = stringResource(R.string.reminder_time),
            style = MaterialTheme.typography.labelLarge
        )
        TimeInput(
            state = timePickerState,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun ScheduleTypeSelection(
    selectedType: ScheduleType,
    onTypeSelected: (ScheduleType) -> Unit
) {
    Column {
        ScheduleType.entries.forEach { type ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = selectedType == type,
                        onClick = { onTypeSelected(type) }
                    )
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selectedType == type,
                    onClick = { onTypeSelected(type) }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when (type) {
                        ScheduleType.DAILY -> stringResource(R.string.schedule_daily)
                        ScheduleType.WEEKLY -> stringResource(R.string.schedule_weekly)
                        ScheduleType.FORTNIGHTLY -> stringResource(R.string.schedule_fortnightly)
                        ScheduleType.MONTHLY -> stringResource(R.string.schedule_monthly)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WeeklyDaySelection(
    selectedDays: Set<DayOfWeek>,
    onDaysChanged: (Set<DayOfWeek>) -> Unit,
    error: String?
) {
    Column {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            DayOfWeek.entries.forEach { day ->
                FilterChip(
                    selected = day in selectedDays,
                    onClick = {
                        val newDays = if (day in selectedDays) {
                            selectedDays - day
                        } else {
                            selectedDays + day
                        }
                        onDaysChanged(newDays)
                    },
                    label = {
                        Text(day.getDisplayName(TextStyle.SHORT, Locale.getDefault()))
                    }
                )
            }
        }
        
        error?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerField(
    label: String,
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
    error: String?
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    Column {
        OutlinedTextField(
            value = selectedDate?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) ?: "",
            onValueChange = { },
            label = { Text(label) },
            readOnly = true,
            isError = error != null,
            supportingText = error?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        )
        
        OutlinedButton(
            onClick = { showDatePicker = true },
            modifier = Modifier.padding(top = 4.dp)
        ) {
            Text(stringResource(R.string.select_date))
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val date = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                            onDateSelected(date)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun MonthlyTypeSelection(
    date: LocalDate,
    selectedType: MonthlyType,
    onTypeSelected: (MonthlyType) -> Unit
) {
    Column {
        Text(
            text = "Repeat on:",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        
        // Day of month option
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectable(
                    selected = selectedType == MonthlyType.DAY_OF_MONTH,
                    onClick = { onTypeSelected(MonthlyType.DAY_OF_MONTH) }
                )
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selectedType == MonthlyType.DAY_OF_MONTH,
                onClick = { onTypeSelected(MonthlyType.DAY_OF_MONTH) }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.monthly_day_of_month, getOrdinal(date.dayOfMonth)))
        }
        
        // Nth weekday option
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectable(
                    selected = selectedType == MonthlyType.NTH_WEEKDAY,
                    onClick = { onTypeSelected(MonthlyType.NTH_WEEKDAY) }
                )
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selectedType == MonthlyType.NTH_WEEKDAY,
                onClick = { onTypeSelected(MonthlyType.NTH_WEEKDAY) }
            )
            Spacer(modifier = Modifier.width(8.dp))
            val weekOfMonth = (date.dayOfMonth - 1) / 7 + 1
            val dayName = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
            Text(stringResource(R.string.monthly_nth_weekday, getOrdinal(weekOfMonth), dayName))
        }
    }
}

private fun getOrdinal(number: Int): String {
    return when {
        number % 100 in 11..13 -> "${number}th"
        number % 10 == 1 -> "${number}st"
        number % 10 == 2 -> "${number}nd"
        number % 10 == 3 -> "${number}rd"
        else -> "${number}th"
    }
}