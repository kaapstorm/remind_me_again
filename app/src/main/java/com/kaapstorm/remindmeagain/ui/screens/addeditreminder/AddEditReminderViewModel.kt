package com.kaapstorm.remindmeagain.ui.screens.addeditreminder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaapstorm.remindmeagain.data.repository.ReminderRepository
import com.kaapstorm.remindmeagain.data.model.Reminder
import com.kaapstorm.remindmeagain.data.model.ReminderSchedule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

class AddEditReminderViewModel(
    private val reminderId: Long?,
    private val reminderRepository: ReminderRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AddEditReminderState())
    val state: StateFlow<AddEditReminderState> = _state.asStateFlow()

    init {
        if (reminderId != null) {
            loadReminder(reminderId)
        }
    }

    fun handleIntent(intent: AddEditReminderIntent) {
        when (intent) {
            is AddEditReminderIntent.UpdateName -> updateName(intent.name)
            is AddEditReminderIntent.UpdateTime -> updateTime(intent.time)
            is AddEditReminderIntent.UpdateScheduleType -> updateScheduleType(intent.scheduleType)
            is AddEditReminderIntent.UpdateWeeklyDays -> updateWeeklyDays(intent.days)
            is AddEditReminderIntent.UpdateFortnightlyDate -> updateFortnightlyDate(intent.date)
            is AddEditReminderIntent.UpdateMonthlyDate -> updateMonthlyDate(intent.date)
            is AddEditReminderIntent.UpdateMonthlyType -> updateMonthlyType(intent.type)
            is AddEditReminderIntent.SaveReminder -> saveReminder()
        }
    }

    private fun loadReminder(id: Long) {
        viewModelScope.launch {
            try {
                reminderRepository.getReminderById(id)
                    .collect { reminder ->
                        if (reminder != null) {
                    _state.value = _state.value.copy(
                        name = reminder.name,
                        time = reminder.time,
                        schedule = reminder.schedule,
                        scheduleType = when (reminder.schedule) {
                            is ReminderSchedule.Daily -> ScheduleType.DAILY
                            is ReminderSchedule.Weekly -> ScheduleType.WEEKLY
                            is ReminderSchedule.Fortnightly -> ScheduleType.FORTNIGHTLY
                            is ReminderSchedule.Monthly -> ScheduleType.MONTHLY
                        },
                        weeklySelectedDays = when (reminder.schedule) {
                            is ReminderSchedule.Weekly -> reminder.schedule.days
                            else -> emptySet()
                        },
                        fortnightlyDate = when (reminder.schedule) {
                            is ReminderSchedule.Fortnightly -> reminder.schedule.date
                            else -> null
                        },
                        monthlyDate = when (reminder.schedule) {
                            is ReminderSchedule.Monthly -> {
                                val today = LocalDate.now()
                                when {
                                    reminder.schedule.dayOfMonth != null -> {
                                        // Find next occurrence of this day of month
                                        val targetDay = reminder.schedule.dayOfMonth
                                        if (today.dayOfMonth <= targetDay && today.month.length(today.isLeapYear) >= targetDay) {
                                            today.withDayOfMonth(targetDay)
                                        } else {
                                            today.plusMonths(1).withDayOfMonth(minOf(targetDay, today.plusMonths(1).month.length(today.plusMonths(1).isLeapYear)))
                                        }
                                    }
                                    reminder.schedule.dayOfWeek != null && reminder.schedule.weekOfMonth != null -> {
                                        // Find next occurrence of nth weekday
                                        val targetWeekday = reminder.schedule.dayOfWeek
                                        val targetWeek = reminder.schedule.weekOfMonth
                                        val firstOfMonth = today.withDayOfMonth(1)
                                        val firstTargetWeekday = firstOfMonth.plusDays(
                                            ((targetWeekday.value - firstOfMonth.dayOfWeek.value + 7) % 7).toLong()
                                        )
                                        val targetDate = firstTargetWeekday.plusWeeks((targetWeek - 1).toLong())
                                        if (targetDate.isBefore(today) || targetDate.month != today.month) {
                                            // Move to next month
                                            val nextMonth = today.plusMonths(1)
                                            val firstOfNextMonth = nextMonth.withDayOfMonth(1)
                                            val firstTargetInNext = firstOfNextMonth.plusDays(
                                                ((targetWeekday.value - firstOfNextMonth.dayOfWeek.value + 7) % 7).toLong()
                                            )
                                            firstTargetInNext.plusWeeks((targetWeek - 1).toLong())
                                        } else {
                                            targetDate
                                        }
                                    }
                                    else -> today
                                }
                            }
                            else -> null
                        },
                        monthlyType = when (reminder.schedule) {
                            is ReminderSchedule.Monthly -> {
                                if (reminder.schedule.dayOfMonth != null) MonthlyType.DAY_OF_MONTH
                                else MonthlyType.NTH_WEEKDAY
                            }
                            else -> MonthlyType.DAY_OF_MONTH
                        }
                    )
                        }
                    }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = e.message ?: "Failed to load reminder"
                )
            }
        }
    }

    private fun updateName(name: String) {
        _state.value = _state.value.copy(
            name = name,
            nameError = validateName(name)
        )
    }

    private fun updateTime(time: LocalTime) {
        _state.value = _state.value.copy(time = time)
    }

    private fun updateScheduleType(scheduleType: ScheduleType) {
        _state.value = _state.value.copy(
            scheduleType = scheduleType,
            weeklySelectedDays = if (scheduleType == ScheduleType.WEEKLY) _state.value.weeklySelectedDays else emptySet(),
            fortnightlyDate = if (scheduleType == ScheduleType.FORTNIGHTLY) _state.value.fortnightlyDate else null,
            monthlyDate = if (scheduleType == ScheduleType.MONTHLY) _state.value.monthlyDate else null
        )
    }

    private fun updateWeeklyDays(days: Set<DayOfWeek>) {
        _state.value = _state.value.copy(
            weeklySelectedDays = days,
            weeklyDaysError = if (days.isEmpty()) "Please select at least one day" else null
        )
    }

    private fun updateFortnightlyDate(date: LocalDate) {
        _state.value = _state.value.copy(
            fortnightlyDate = date,
            fortnightlyDateError = null
        )
    }

    private fun updateMonthlyDate(date: LocalDate) {
        _state.value = _state.value.copy(
            monthlyDate = date,
            monthlyDateError = null
        )
    }

    private fun updateMonthlyType(type: MonthlyType) {
        _state.value = _state.value.copy(monthlyType = type)
    }

    private fun validateName(name: String): String? {
        return when {
            name.isBlank() -> "Please give the reminder a name"
            name.length > 50 -> "The name should not be more than 50 characters"
            else -> null
        }
    }

    private fun validateForm(): Boolean {
        val currentState = _state.value
        val nameError = validateName(currentState.name)
        val weeklyDaysError = if (currentState.scheduleType == ScheduleType.WEEKLY && currentState.weeklySelectedDays.isEmpty()) {
            "Please select at least one day"
        } else null
        val fortnightlyDateError = if (currentState.scheduleType == ScheduleType.FORTNIGHTLY && currentState.fortnightlyDate == null) {
            "Please select a date"
        } else null
        val monthlyDateError = if (currentState.scheduleType == ScheduleType.MONTHLY && currentState.monthlyDate == null) {
            "Please select a date"
        } else null

        _state.value = _state.value.copy(
            nameError = nameError,
            weeklyDaysError = weeklyDaysError,
            fortnightlyDateError = fortnightlyDateError,
            monthlyDateError = monthlyDateError
        )

        return nameError == null && weeklyDaysError == null && fortnightlyDateError == null && monthlyDateError == null
    }

    private fun saveReminder() {
        if (!validateForm()) return

        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(isSaving = true)
                
                val schedule = createSchedule()
                val reminder = Reminder(
                    id = reminderId ?: 0,
                    name = _state.value.name,
                    time = _state.value.time,
                    schedule = schedule
                )

                if (reminderId == null) {
                    reminderRepository.insertReminder(reminder)
                } else {
                    reminderRepository.updateReminder(reminder)
                }

                _state.value = _state.value.copy(
                    isSaving = false,
                    isSaved = true
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isSaving = false,
                    error = e.message ?: "Failed to save reminder"
                )
            }
        }
    }

    private fun createSchedule(): ReminderSchedule {
        return when (_state.value.scheduleType) {
            ScheduleType.DAILY -> ReminderSchedule.Daily
            ScheduleType.WEEKLY -> ReminderSchedule.Weekly(_state.value.weeklySelectedDays)
            ScheduleType.FORTNIGHTLY -> ReminderSchedule.Fortnightly(
                date = _state.value.fortnightlyDate!!
            )
            ScheduleType.MONTHLY -> {
                val date = _state.value.monthlyDate!!
                when (_state.value.monthlyType) {
                    MonthlyType.DAY_OF_MONTH -> ReminderSchedule.Monthly(
                        dayOfMonth = date.dayOfMonth
                    )
                    MonthlyType.NTH_WEEKDAY -> {
                        val weekOfMonth = (date.dayOfMonth - 1) / 7 + 1
                        ReminderSchedule.Monthly(
                            dayOfWeek = date.dayOfWeek,
                            weekOfMonth = weekOfMonth
                        )
                    }
                }
            }
        }
    }
}

data class AddEditReminderState(
    val name: String = "",
    val time: LocalTime = LocalTime.of(12, 0),
    val schedule: ReminderSchedule = ReminderSchedule.Daily,
    val scheduleType: ScheduleType = ScheduleType.DAILY,
    val weeklySelectedDays: Set<DayOfWeek> = emptySet(),
    val fortnightlyDate: LocalDate? = null,
    val monthlyDate: LocalDate? = null,
    val monthlyType: MonthlyType = MonthlyType.DAY_OF_MONTH,
    val nameError: String? = null,
    val weeklyDaysError: String? = null,
    val fortnightlyDateError: String? = null,
    val monthlyDateError: String? = null,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null
) {
    val isFormValid: Boolean
        get() = nameError == null && 
                weeklyDaysError == null && 
                fortnightlyDateError == null && 
                monthlyDateError == null &&
                name.isNotBlank()
}

sealed class AddEditReminderIntent {
    data class UpdateName(val name: String) : AddEditReminderIntent()
    data class UpdateTime(val time: LocalTime) : AddEditReminderIntent()
    data class UpdateScheduleType(val scheduleType: ScheduleType) : AddEditReminderIntent()
    data class UpdateWeeklyDays(val days: Set<DayOfWeek>) : AddEditReminderIntent()
    data class UpdateFortnightlyDate(val date: LocalDate) : AddEditReminderIntent()
    data class UpdateMonthlyDate(val date: LocalDate) : AddEditReminderIntent()
    data class UpdateMonthlyType(val type: MonthlyType) : AddEditReminderIntent()
    object SaveReminder : AddEditReminderIntent()
}

enum class ScheduleType {
    DAILY, WEEKLY, FORTNIGHTLY, MONTHLY
}

enum class MonthlyType {
    DAY_OF_MONTH, NTH_WEEKDAY
}