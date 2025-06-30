package com.kaapstorm.remindmeagain.di

import com.kaapstorm.remindmeagain.ui.screens.reminderlist.ReminderListViewModel
import com.kaapstorm.remindmeagain.ui.screens.addeditreminder.AddEditReminderViewModel
import com.kaapstorm.remindmeagain.ui.screens.showreminder.ShowReminderViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val uiModule = module {
    viewModel { ReminderListViewModel(get()) }
    viewModel { (reminderId: Long?) -> AddEditReminderViewModel(reminderId, get(), get(), get()) }
    viewModel { (reminderId: Long) -> ShowReminderViewModel(reminderId, get(), get(), get(), get()) }
}