package com.kaapstorm.remindmeagain

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.kaapstorm.remindmeagain.ui.navigation.AppNavigation
import com.kaapstorm.remindmeagain.ui.theme.RemindMeAgainTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RemindMeAgainTheme {
                AppNavigation()
            }
        }
    }
}
