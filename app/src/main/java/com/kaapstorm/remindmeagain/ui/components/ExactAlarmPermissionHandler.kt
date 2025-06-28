package com.kaapstorm.remindmeagain.ui.components

import android.content.Context
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.kaapstorm.remindmeagain.permissions.ExactAlarmPermissionManager

@Composable
fun ExactAlarmPermissionHandler(
    context: Context,
    exactAlarmPermissionManager: ExactAlarmPermissionManager,
    onPermissionGranted: () -> Unit = {},
    onPermissionDenied: () -> Unit = {}
) {
    var showPermissionDialog by remember { mutableStateOf(false) }

    // Check permission status on composition
    LaunchedEffect(Unit) {
        exactAlarmPermissionManager.logExactAlarmPermissionStatus()
        
        if (!exactAlarmPermissionManager.canScheduleExactAlarms()) {
            showPermissionDialog = true
        } else {
            onPermissionGranted()
        }
    }

    // Activity result launcher for opening settings
    val settingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // Check permission again after returning from settings
        if (exactAlarmPermissionManager.canScheduleExactAlarms()) {
            Log.d("ExactAlarmPermission", "Permission granted after settings")
            onPermissionGranted()
        } else {
            Log.w("ExactAlarmPermission", "Permission still not granted after settings")
            onPermissionDenied()
        }
    }

    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = {
                showPermissionDialog = false
                onPermissionDenied()
            },
            title = {
                Text("Exact Alarm Permission Required")
            },
            text = {
                Text(
                    "This app needs permission to schedule exact alarms for snooze functionality to work properly. " +
                    "Please grant this permission in the settings."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPermissionDialog = false
                        try {
                            val settingsIntent = exactAlarmPermissionManager.getExactAlarmSettingsIntent()
                            settingsLauncher.launch(settingsIntent)
                        } catch (e: Exception) {
                            Log.e("ExactAlarmPermission", "Failed to launch settings", e)
                            onPermissionDenied()
                        }
                    }
                ) {
                    Text("Open Settings")
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        showPermissionDialog = false
                        onPermissionDenied()
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
} 