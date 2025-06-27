package com.kaapstorm.remindmeagain.ui.screens.reminderlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaapstorm.remindmeagain.data.repository.ReminderRepository
import com.kaapstorm.remindmeagain.data.model.Reminder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class ReminderListViewModel(
    private val reminderRepository: ReminderRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ReminderListState())
    val state: StateFlow<ReminderListState> = _state.asStateFlow()

    init {
        loadReminders()
    }

    fun handleIntent(intent: ReminderListIntent) {
        when (intent) {
            is ReminderListIntent.LoadReminders -> loadReminders()
            is ReminderListIntent.DeleteReminder -> deleteReminder(intent.reminder)
        }
    }

    private fun loadReminders() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            
            reminderRepository.getAllReminders()
                .catch { throwable ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = throwable.message ?: "Failed to load reminders"
                    )
                }
                .collect { reminders ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        reminders = reminders,
                        error = null
                    )
                }
        }
    }

    private fun deleteReminder(reminder: Reminder) {
        viewModelScope.launch {
            try {
                reminderRepository.deleteReminder(reminder)
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = e.message ?: "Failed to delete reminder"
                )
            }
        }
    }
}

data class ReminderListState(
    val isLoading: Boolean = false,
    val reminders: List<Reminder> = emptyList(),
    val error: String? = null
)

sealed class ReminderListIntent {
    object LoadReminders : ReminderListIntent()
    data class DeleteReminder(val reminder: Reminder) : ReminderListIntent()
}