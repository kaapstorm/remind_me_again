package com.kaapstorm.remindmeagain.di

import com.kaapstorm.remindmeagain.domain.service.ReminderSchedulingService
import org.koin.dsl.module

val domainModule = module {
    // Domain services
    single { ReminderSchedulingService() }
} 