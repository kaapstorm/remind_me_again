package com.kaapstorm.remindmeagain.ui.screens.showreminder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaapstorm.remindmeagain.data.repository.ReminderRepository
import com.kaapstorm.remindmeagain.data.model.PostponeAction
import com.kaapstorm.remindmeagain.data.model.Reminder
import com.kaapstorm.remindmeagain.data.model.DismissAction
import com.kaapstorm.remindmeagain.data.model.ReminderSchedule
import com.kaapstorm.remindmeagain.domain.service.ReminderSchedulingService
import com.kaapstorm.remindmeagain.domain.service.NextOccurrenceCalculator
import com.kaapstorm.remindmeagain.notifications.ReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class ShowReminderViewModel(
    private val reminderId: Long,
    private val reminderRepository: ReminderRepository,
    private val schedulingService: ReminderSchedulingService,
    private val reminderScheduler: ReminderScheduler,
    private val nextOccurrenceCalculator: NextOccurrenceCalculator,
    private val clock: java.time.Clock = java.time.Clock.systemDefaultZone()
) : ViewModel() {

    private val _state = MutableStateFlow(ShowReminderState())
    val state: StateFlow<ShowReminderState> = _state.asStateFlow()

    init {
        loadReminder()
    }

    fun handleIntent(intent: ShowReminderIntent) {
        when (intent) {
            is ShowReminderIntent.LoadReminder -> loadReminder()
            is ShowReminderIntent.DismissReminder -> dismissReminder()
            is ShowReminderIntent.PostponeReminder -> postponeReminder(intent.intervalSeconds)
            is ShowReminderIntent.SelectPostponeInterval -> selectPostponeInterval(intent.intervalSeconds)
            is ShowReminderIntent.ShowDeleteDialog -> showDeleteDialog()
            is ShowReminderIntent.HideDeleteDialog -> hideDeleteDialog()
            is ShowReminderIntent.DeleteReminder -> deleteReminder()
        }
    }

    private fun loadReminder() {
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(isLoading = true)
                
                // Load reminder and dismiss actions in parallel
                val reminderFlow = reminderRepository.getReminderById(reminderId)
                val dismissActionsFlow = reminderRepository.getDismissActionsForReminder(reminderId)
                
                // Combine the flows to get both reminder and dismiss actions
                reminderFlow.collect { reminder ->
                    if (reminder != null) {
                        dismissActionsFlow.collect { actions ->
                            val lastDismissAction = actions.maxByOrNull { it.timestamp }
                            
                            // Calculate if reminder is due - use a more flexible approach
                            val currentTime = Instant.now(clock).atZone(ZoneId.systemDefault()).toLocalDateTime()
                            val isDue = isReminderDue(reminder, currentTime, lastDismissAction)
                            
                            // Calculate next due time
                            val nextDueTimestamp = nextOccurrenceCalculator.getNextMainOccurrenceTimestamp(reminder, currentTime)
                            val nextDueTime = if (nextDueTimestamp != Long.MAX_VALUE) {
                                Instant.ofEpochMilli(nextDueTimestamp).atZone(ZoneId.systemDefault()).toLocalDateTime()
                            } else {
                                null
                            }
                            
                            _state.value = _state.value.copy(
                                isLoading = false,
                                reminder = reminder,
                                lastDismissAction = lastDismissAction,
                                isDue = isDue,
                                nextDueTime = nextDueTime
                            )
                        }
                    } else {
                        _state.value = _state.value.copy(
                            isLoading = false,
                            error = "Reminder not found"
                        )
                    }
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load reminder"
                )
            }
        }
    }

    /**
     * Determines if a reminder is due for the Show Reminder Screen.
     * This is more flexible than isReminderActive - it considers a reminder "due"
     * if it should be active within a reasonable time window (e.g., within the next hour).
     * Also checks if the reminder has been dismissed within that time window.
     */
    private fun isReminderDue(reminder: Reminder, currentTime: LocalDateTime, lastDismissAction: DismissAction?): Boolean {
        val schedule = reminder.schedule
        val reminderTime = reminder.time
        
        // Check if the reminder should be active today or very soon
        val isDue = when (schedule) {
            is ReminderSchedule.Daily -> {
                // For daily reminders, check if it's due within the next hour
                val todayReminderTime = currentTime.toLocalDate().atTime(reminderTime)
                val timeUntilReminder = java.time.Duration.between(currentTime, todayReminderTime)
                timeUntilReminder.toMinutes() >= 0 && timeUntilReminder.toMinutes() <= 60
            }
            
            is ReminderSchedule.Weekly -> {
                // For weekly reminders, check if it's due today or within the next few days
                schedule.days.any { dayOfWeek ->
                    val nextOccurrence = getNextOccurrenceForDayOfWeek(currentTime, dayOfWeek, reminderTime)
                    val timeUntilReminder = java.time.Duration.between(currentTime, nextOccurrence)
                    timeUntilReminder.toMinutes() >= 0 && timeUntilReminder.toMinutes() <= 1440 // Within 24 hours
                }
            }
            
            is ReminderSchedule.Fortnightly -> {
                // For fortnightly reminders, check if it's due within the next couple of days
                val nextOccurrence = getNextFortnightlyOccurrence(currentTime, schedule, reminderTime)
                val timeUntilReminder = java.time.Duration.between(currentTime, nextOccurrence)
                timeUntilReminder.toMinutes() >= 0 && timeUntilReminder.toMinutes() <= 2880 // Within 2 days
            }
            
            is ReminderSchedule.Monthly -> {
                // For monthly reminders, check if it's due within the next few days
                val nextOccurrence = getNextMonthlyOccurrence(currentTime, schedule, reminderTime)
                val timeUntilReminder = java.time.Duration.between(currentTime, nextOccurrence)
                timeUntilReminder.toMinutes() >= 0 && timeUntilReminder.toMinutes() <= 4320 // Within 3 days
            }
        }
        
        // If not due based on schedule, return false
        if (!isDue) return false
        
        // Check if the reminder has been dismissed within the time window
        return !isReminderDismissedWithinTimeWindow(reminder, currentTime, lastDismissAction)
    }
    
    /**
     * Checks if a reminder has been dismissed within the time window when it would be due.
     * If dismissed recently, the reminder is no longer considered due.
     */
    private fun isReminderDismissedWithinTimeWindow(reminder: Reminder, currentTime: LocalDateTime, lastDismissAction: DismissAction?): Boolean {
        if (lastDismissAction == null) return false
        val schedule = reminder.schedule
        val reminderTime = reminder.time
        return when (schedule) {
            is ReminderSchedule.Daily -> {
                val dueTime = currentTime.toLocalDate().atTime(reminderTime)
                val windowStart = dueTime.minusMinutes(60)
                val dismissTime = lastDismissAction.timestamp.atZone(ZoneId.systemDefault()).toLocalDateTime()
                dismissTime.isAfter(windowStart) && dismissTime.isBefore(dueTime)
            }
            is ReminderSchedule.Weekly -> {
                schedule.days.any { dayOfWeek ->
                    val dueTime = getNextOccurrenceForDayOfWeek(currentTime, dayOfWeek, reminderTime)
                    val windowStart = dueTime.minusMinutes(60)
                    val dismissTime = lastDismissAction.timestamp.atZone(ZoneId.systemDefault()).toLocalDateTime()
                    dismissTime.isAfter(windowStart) && dismissTime.isBefore(dueTime)
                }
            }
            is ReminderSchedule.Fortnightly -> {
                val dueTime = getNextFortnightlyOccurrence(currentTime, schedule, reminderTime)
                val windowStart = dueTime.minusMinutes(60)
                val dismissTime = lastDismissAction.timestamp.atZone(ZoneId.systemDefault()).toLocalDateTime()
                dismissTime.isAfter(windowStart) && dismissTime.isBefore(dueTime)
            }
            is ReminderSchedule.Monthly -> {
                val dueTime = getNextMonthlyOccurrence(currentTime, schedule, reminderTime)
                val windowStart = dueTime.minusMinutes(60)
                val dismissTime = lastDismissAction.timestamp.atZone(ZoneId.systemDefault()).toLocalDateTime()
                dismissTime.isAfter(windowStart) && dismissTime.isBefore(dueTime)
            }
        }
    }

    private fun getNextOccurrenceForDayOfWeek(currentTime: LocalDateTime, dayOfWeek: java.time.DayOfWeek, reminderTime: LocalTime): LocalDateTime {
        var nextDate = currentTime.toLocalDate()
        while (nextDate.dayOfWeek != dayOfWeek) {
            nextDate = nextDate.plusDays(1)
        }
        return LocalDateTime.of(nextDate, reminderTime)
    }

    private fun getNextFortnightlyOccurrence(currentTime: LocalDateTime, schedule: ReminderSchedule.Fortnightly, reminderTime: LocalTime): LocalDateTime {
        var nextDate = currentTime.toLocalDate()
        while (nextDate.dayOfWeek != schedule.date.dayOfWeek) {
            nextDate = nextDate.plusDays(1)
        }
        
        // Check if this is the correct fortnight
        val daysSinceReference = nextDate.toEpochDay() - schedule.date.toEpochDay()
        val weeksSinceReference = daysSinceReference / 7
        if (weeksSinceReference % 2L != 0L) {
            nextDate = nextDate.plusWeeks(1)
        }
        
        return LocalDateTime.of(nextDate, reminderTime)
    }

    private fun getNextMonthlyOccurrence(currentTime: LocalDateTime, schedule: ReminderSchedule.Monthly, reminderTime: LocalTime): LocalDateTime {
        return when {
            schedule.dayOfWeek != null && schedule.weekOfMonth != null -> {
                // Find the next occurrence of the nth day of week
                var nextDate = currentTime.toLocalDate()
                while (true) {
                    if (nextDate.dayOfWeek == schedule.dayOfWeek) {
                        val weekOfMonth = ((nextDate.dayOfMonth - 1) / 7) + 1
                        if (weekOfMonth == schedule.weekOfMonth) {
                            return LocalDateTime.of(nextDate, reminderTime)
                        }
                    }
                    nextDate = nextDate.plusDays(1)
                }
                LocalDateTime.of(nextDate, reminderTime) // This should never be reached
            }
            schedule.dayOfMonth != null -> {
                // Find the next occurrence of the day of month
                var nextDate = currentTime.toLocalDate()
                while (nextDate.dayOfMonth != schedule.dayOfMonth) {
                    nextDate = nextDate.plusDays(1)
                }
                return LocalDateTime.of(nextDate, reminderTime)
            }
            else -> LocalDateTime.of(currentTime.toLocalDate().plusDays(1), reminderTime) // Fallback
        }
    }

    private fun dismissReminder() {
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(isProcessing = true)
                
                val action = DismissAction(
                    reminderId = reminderId,
                    timestamp = Instant.now(clock)
                )
                
                reminderRepository.insertDismissAction(action)
                
                _state.value = _state.value.copy(
                    isProcessing = false,
                    isDismissed = true
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isProcessing = false,
                    error = e.message ?: "Failed to dismiss reminder"
                )
            }
        }
    }

    private fun postponeReminder(intervalSeconds: Int) {
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(isProcessing = true)
                
                val action = PostponeAction(
                    reminderId = reminderId,
                    timestamp = Instant.now(clock),
                    intervalSeconds = intervalSeconds
                )
                
                reminderRepository.insertPostponeAction(action)
                
                _state.value = _state.value.copy(
                    isProcessing = false,
                    isPostponed = true
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isProcessing = false,
                    error = e.message ?: "Failed to postpone reminder"
                )
            }
        }
    }

    private fun selectPostponeInterval(intervalSeconds: Int) {
        _state.value = _state.value.copy(selectedPostponeInterval = intervalSeconds)
    }

    private fun showDeleteDialog() {
        _state.value = _state.value.copy(showDeleteDialog = true)
    }

    private fun hideDeleteDialog() {
        _state.value = _state.value.copy(showDeleteDialog = false)
    }

    private fun deleteReminder() {
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(isProcessing = true, showDeleteDialog = false)
                
                reminderRepository.deleteReminder(reminderId)
                reminderScheduler.cancelReminder(reminderId)
                
                _state.value = _state.value.copy(
                    isProcessing = false,
                    isDeleted = true
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isProcessing = false,
                    error = e.message ?: "Failed to delete reminder"
                )
            }
        }
    }
}

data class ShowReminderState(
    val isLoading: Boolean = false,
    val reminder: Reminder? = null,
    val lastDismissAction: DismissAction? = null,
    val isDue: Boolean = false,
    val nextDueTime: LocalDateTime? = null,
    val selectedPostponeInterval: Int = 300, // 5 minutes default
    val isProcessing: Boolean = false,
    val isDismissed: Boolean = false,
    val isPostponed: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val isDeleted: Boolean = false,
    val error: String? = null
)

sealed class ShowReminderIntent {
    object LoadReminder : ShowReminderIntent()
    object DismissReminder : ShowReminderIntent()
    data class PostponeReminder(val intervalSeconds: Int) : ShowReminderIntent()
    data class SelectPostponeInterval(val intervalSeconds: Int) : ShowReminderIntent()
    object ShowDeleteDialog : ShowReminderIntent()
    object HideDeleteDialog : ShowReminderIntent()
    object DeleteReminder : ShowReminderIntent()
}

object PostponeIntervals {
    const val FIVE_MINUTES = 300
    const val FIFTEEN_MINUTES = 900
    const val ONE_HOUR = 3600
    const val FOUR_HOURS = 14400
    const val TWELVE_HOURS = 43200
    
    val ALL = listOf(
        FIVE_MINUTES,
        FIFTEEN_MINUTES,
        ONE_HOUR,
        FOUR_HOURS,
        TWELVE_HOURS
    )
}