package com.kaapstorm.remindmeagain.di

import com.kaapstorm.remindmeagain.data.db.AppDatabase
import com.kaapstorm.remindmeagain.data.db.ReminderDao
import com.kaapstorm.remindmeagain.data.db.ReminderActionDao
import com.kaapstorm.remindmeagain.data.repository.ReminderRepository
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val dataModule = module {
    // Database
    single { AppDatabase.getInstance(androidContext()) }
    
    // DAOs
    single { get<AppDatabase>().reminderDao() }
    single { get<AppDatabase>().reminderActionDao() }
    
    // Repository
    single { ReminderRepository(get(), get()) }
} 