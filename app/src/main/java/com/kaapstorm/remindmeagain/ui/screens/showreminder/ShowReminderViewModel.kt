package com.kaapstorm.remindmeagain.ui.screens.showreminder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaapstorm.remindmeagain.data.repository.ReminderRepository
import com.kaapstorm.remindmeagain.data.model.CompleteAction
import com.kaapstorm.remindmeagain.data.model.PostponeAction
import com.kaapstorm.remindmeagain.data.model.Reminder
import com.kaapstorm.remindmeagain.data.model.ReminderAction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant

class ShowReminderViewModel(
    private val reminderId: Long,
    private val reminderRepository: ReminderRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ShowReminderState())
    val state: StateFlow<ShowReminderState> = _state.asStateFlow()

    init {
        loadReminder()
    }

    fun handleIntent(intent: ShowReminderIntent) {
        when (intent) {
            is ShowReminderIntent.LoadReminder -> loadReminder()
            is ShowReminderIntent.CompleteReminder -> completeReminder()
            is ShowReminderIntent.PostponeReminder -> postponeReminder(intent.intervalSeconds)
            is ShowReminderIntent.SelectPostponeInterval -> selectPostponeInterval(intent.intervalSeconds)
        }
    }

    private fun loadReminder() {
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(isLoading = true)
                
                reminderRepository.getReminderById(reminderId).collect { reminder ->
                    if (reminder != null) {
                        // For now, just get the most recent complete action
                        reminderRepository.getCompleteActionsForReminder(reminderId).collect { actions ->
                            val lastAction = actions.maxByOrNull { it.timestamp }
                            _state.value = _state.value.copy(
                                isLoading = false,
                                reminder = reminder,
                                lastAction = lastAction,
                                isDue = true // TODO: Implement proper due calculation
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

    private fun completeReminder() {
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(isProcessing = true)
                
                val action = CompleteAction(
                    reminderId = reminderId,
                    timestamp = Instant.now()
                )
                
                reminderRepository.insertCompleteAction(action)
                
                _state.value = _state.value.copy(
                    isProcessing = false,
                    isCompleted = true
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isProcessing = false,
                    error = e.message ?: "Failed to complete reminder"
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
                    timestamp = Instant.now(),
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
}

data class ShowReminderState(
    val isLoading: Boolean = false,
    val reminder: Reminder? = null,
    val lastAction: ReminderAction? = null,
    val isDue: Boolean = false,
    val selectedPostponeInterval: Int = 300, // 5 minutes default
    val isProcessing: Boolean = false,
    val isCompleted: Boolean = false,
    val isPostponed: Boolean = false,
    val error: String? = null
)

sealed class ShowReminderIntent {
    object LoadReminder : ShowReminderIntent()
    object CompleteReminder : ShowReminderIntent()
    data class PostponeReminder(val intervalSeconds: Int) : ShowReminderIntent()
    data class SelectPostponeInterval(val intervalSeconds: Int) : ShowReminderIntent()
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