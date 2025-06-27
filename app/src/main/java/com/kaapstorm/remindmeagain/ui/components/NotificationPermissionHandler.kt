package com.kaapstorm.remindmeagain.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.kaapstorm.remindmeagain.permissions.NotificationPermissionManager

@Composable
fun NotificationPermissionHandler(
    onPermissionResult: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val permissionManager = remember { NotificationPermissionManager(context) }
    var hasRequestedPermission by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        onPermissionResult(isGranted)
    }

    LaunchedEffect(Unit) {
        if (!hasRequestedPermission) {
            val permission = permissionManager.getNotificationPermission()
            if (permission != null && !permissionManager.hasNotificationPermission()) {
                hasRequestedPermission = true
                permissionLauncher.launch(permission)
            }
        }
    }
}