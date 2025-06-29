package com.kaapstorm.remindmeagain.ui.screens.showreminder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kaapstorm.remindmeagain.R
import com.kaapstorm.remindmeagain.data.model.DismissAction
import com.kaapstorm.remindmeagain.ui.components.ReminderScheduleText
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShowReminderScreen(
    reminderId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (Long) -> Unit = {},
    viewModel: ShowReminderViewModel = koinViewModel { parametersOf(reminderId) }
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.error) {
        state.error?.let { error ->
            snackbarHostState.showSnackbar(error)
        }
    }

    LaunchedEffect(state.isDismissed, state.isPostponed, state.isDeleted) {
        if (state.isDismissed || state.isPostponed || state.isDeleted) {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.reminder_details)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.cancel)
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                state.reminder != null -> {
                    ReminderDetailsContent(
                        state = state,
                        onDismissReminder = { viewModel.handleIntent(ShowReminderIntent.DismissReminder) },
                        onPostponeReminder = { interval ->
                            viewModel.handleIntent(ShowReminderIntent.PostponeReminder(interval))
                        },
                        onSelectPostponeInterval = { interval ->
                            viewModel.handleIntent(ShowReminderIntent.SelectPostponeInterval(interval))
                        },
                        onEditReminder = { onNavigateToEdit(reminderId) },
                        onDeleteReminder = { viewModel.handleIntent(ShowReminderIntent.ShowDeleteDialog) }
                    )
                }
                else -> {
                    Text(
                        text = "Reminder not found",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }

    // Delete confirmation dialog
    if (state.showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.handleIntent(ShowReminderIntent.HideDeleteDialog) },
            title = { Text(stringResource(R.string.delete_reminder)) },
            text = { Text(stringResource(R.string.delete_reminder_confirmation)) },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.handleIntent(ShowReminderIntent.DeleteReminder) }
                ) {
                    Text(
                        text = stringResource(R.string.delete),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.handleIntent(ShowReminderIntent.HideDeleteDialog) }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun ReminderDetailsContent(
    state: ShowReminderState,
    onDismissReminder: () -> Unit,
    onPostponeReminder: (Int) -> Unit,
    onSelectPostponeInterval: (Int) -> Unit,
    onEditReminder: () -> Unit,
    onDeleteReminder: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val reminder = state.reminder!!

        // Reminder Info Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = reminder.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "${reminder.time.format(DateTimeFormatter.ofPattern("HH:mm"))} ${ReminderScheduleText(reminder.schedule)}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Last completed info
                state.lastAction?.let { action ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(
                            R.string.last_completed,
                            action.timestamp.atZone(java.time.ZoneId.systemDefault())
                                .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Due Status Card
        if (state.isDue) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.reminder_is_due),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = onDismissReminder,
                    enabled = !state.isProcessing,
                    modifier = Modifier.weight(1f)
                ) {
                    if (state.isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .height(16.dp)
                                .width(16.dp)
                        )
                    } else {
                        Text(stringResource(R.string.dismiss))
                    }
                }

                OutlinedButton(
                    onClick = { onPostponeReminder(state.selectedPostponeInterval) },
                    enabled = !state.isProcessing,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.postpone))
                }
            }

            // Postpone Options
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.postpone_options),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    PostponeIntervals.ALL.forEach { intervalSeconds ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = state.selectedPostponeInterval == intervalSeconds,
                                    onClick = { onSelectPostponeInterval(intervalSeconds) }
                                )
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = state.selectedPostponeInterval == intervalSeconds,
                                onClick = { onSelectPostponeInterval(intervalSeconds) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = getPostponeIntervalText(intervalSeconds),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }

        // Edit and Delete buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = onEditReminder,
                enabled = !state.isProcessing,
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.edit))
            }

            OutlinedButton(
                onClick = onDeleteReminder,
                enabled = !state.isProcessing,
                modifier = Modifier.weight(1f),
                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(stringResource(R.string.delete))
            }
        }
    }
}

@Composable
private fun getPostponeIntervalText(intervalSeconds: Int): String {
    return when (intervalSeconds) {
        PostponeIntervals.FIVE_MINUTES -> stringResource(R.string.postpone_5_min)
        PostponeIntervals.FIFTEEN_MINUTES -> stringResource(R.string.postpone_15_min)
        PostponeIntervals.ONE_HOUR -> stringResource(R.string.postpone_1_hour)
        PostponeIntervals.FOUR_HOURS -> stringResource(R.string.postpone_4_hours)
        PostponeIntervals.TWELVE_HOURS -> stringResource(R.string.postpone_12_hours)
        else -> "$intervalSeconds seconds"
    }
}