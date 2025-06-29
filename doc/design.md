Design: Remind Me Again (Android)
=================================

Overview
--------

Remind Me Again is an Android app for recurring reminders. It allows
users to create, edit, and manage reminders with flexible schedules and
receive timely notifications.


Guiding Principles
------------------

Remind Me Again is an offline, local-only app.

The UI is clean and minimalist.


Behavior & Logic
----------------

### Use Case Example

#### User requirement

Alice needs to take the recycling out in the evening, every second Thursday, so
that it can be collected.

The last Thursday for collection was June 26, 2025, so she wants to be reminded
to take out the recycling on the evening of Thursday July 10, 2025, and every
fortnight after that.

#### User actions

Alice opens the Remind Me Again app, and adds a new reminder.

She sets the **name** to "Take out the recycling". She sets the **time** to
"21:30". She sets the **schedule** to "Fortnightly". She selects June 26, 2025
as the **date of the first occurrence**. (**Note** that the first occurrence can
be in the past.) She saves the new reminder.

#### App behavior

At 21:30 on Thursday July 10, 2025, the app notifies her: "Take out the
recycling".

Alice ignores the notification. **One minute later** the same notification is
triggered again. And then **one minute after that** the same notification is
triggered again. The app keeps triggering the notification until Alice
interacts with the notification.

Alice is unable to take out the recycling just then, so she taps the button
labeled "Later" on the notification.

**Two minutes later**, the notification is triggered again. The app will
continue to repeat the notification every two minutes.

Alice taps the button labeled "Later" on the notification again.

**Four minutes later**, the notification is triggered again.

This time she takes out the recycling, and taps the button labeled "Dismiss".
The notification for this reminder does not recur.

A fortnight later, at 21:30 on Thursday July 24, 2025, the app notifies her:
"Take out the recycling".

### Logic

The "Later" button on the notification will double the interval every time,
until the "Dismiss" button is tapped.

The interval is reset, so that the next time the user is notified, the first
interval is 60 seconds. The interval is associated with the notification, not
the reminder, so that when the reminder recurs, its notification interval starts
at 60 seconds.

The "Later" interval cannot exceed the time when the reminder is due again. For
example, if Bob has a reminder named "Walk the dog", set for 12:00 midday, and
recurring daily, then if the "Later" interval places the next notification after
12:00 the following day, then the "Later" button should not be shown.

If the app is killed while a reminder is in a "snoozed" (postponed via "Later")
state, then when the app restarts (or the next scheduled check happens), it
should re-notify immediately if the original due time + last snooze delay has
passed.

The app does not need to be resilient to reboots. In-memory storage is
sufficient for managing notification intervals.

WorkManager has a minimum interval of 15 minutes, so when a notification is
snoozed, the app should use `AlarmManager`, with `setExact` to respect
battery-saving measures.


Technical Design Decisions
-------------------------

Targeted devices are Android 13+ (API 33+)

1. **Data Layer & Persistence**
   - Use Kotlin Coroutines for asynchronous database operations
   - RoomDatabase classes are versioned with RoomDatabase.Migration objects
   - All migrations must be covered by migration tests
   - Schema changes are documented using Room's exportSchema = true
   - Repository pattern used to abstract data source

2. **Architecture Pattern**
   - MVI architecture using StateFlow for unidirectional state management

3. **Dependency Injection**
   - Koin for Kotlin-friendly dependency injection

4. **UI Implementation**
   - Jetpack Compose for modern design and UI quality
   - LazyColumn for lists
   - TimePicker: Material3 TimePicker
   - Schedule selection: Material3 Chips without custom animations

5. **Background Work & Scheduling**
   - All reminder and repeat notification scheduling is handled by AlarmManager.
   - AlarmManager is used for exact timing of reminders and for repeating notifications (including snooze/postpone logic).
   - No foreground service required.
   - The app does not attempt to bypass Doze or battery optimizations, and does not use setExactAndAllowWhileIdle.

6. **Testing Strategy**
   - JUnit4 for unit tests
   - Espresso for UI testing
   - Instrumented tests for AlarmManager-based scheduling and notification logic
   - Integration tests for database

7. **Error Handling**
   - No centralized error handling system
   - No network error handling required
   - No retry mechanisms needed

8. **Localization**
   - Initial implementation: English only
   - Time: 24-hour format
   - Dates: ISO 8601 format
   - Days: Three-letter abbreviations (e.g., Tue 2025-06-17 19:30)
   - No RTL support in initial implementation

9. **Accessibility**
   - Basic TalkBack support only
   - No custom accessibility actions required

10. **Edge Cases**
    - Handles device reboots
    - Times are local (e.g., 13:00 remains 13:00 across time zones)
    - Post-update reminder notifications
    - DST handling:
      - Double-occurring times: Notify at first occurrence
      - Skipped times: Notify at next hour

11. **Performance**
    - No pagination required for reminder lists

12. **Analytics & Monitoring**
    - No crash reporting
    - No usage analytics
    - No completion tracking


App Navigation & Screen Flow
----------------------------

1. **Reminder List Screen**
   - Shows all reminders.
   - "+" FloatingActionButton to add a new reminder.
   - Tap a reminder to open "Show Reminder" screen.

   ```plaintext
   +--------------------------------------+
   | Remind Me Again                      |
   | +----------------------------------+ |
   | | + (FAB)                          | |  <-- FloatingActionButton (top left)
   | +----------------------------------+ |
   |                                      |
   | [ Reminder 1              08:00  > ] |
   | [ Reminder 2              21:30  > ] |  <-- LazyColumn list items
   | [ Reminder 3              07:15  > ] |
   |                                      |
   +--------------------------------------+
   ```

2. **Show Reminder Screen**
   - Shows reminder details.
   - If due: "Dismiss" and "Postpone" actions.
   - If the reminder will be due on the same day: "Dismiss" only.
   - If the user taps Delete, they are shown a confirmation dialog. If they
     confirm then the reminder is deleted. Use a standard Material 3
     AlertDialog with "Cancel" and "Delete" buttons.
   - Deleting a reminder takes the user back to the Reminder List Screen
   - Clicking "Edit" takes the user to the Add/Edit Reminder Screen

   ```plaintext
   +--------------------------------------+
   | Reminder Details                     |
   |--------------------------------------|
   | Name: Morning Meds                   |
   | Last done: 2025-06-28 07:45          |
   | Next due: 07:30                      |
   |                                      |
   | [ Reminder is due! ]                 |
   | [ Dismiss ]   [ Postpone > ]         |  <-- Buttons
   |                                      |
   | Postpone:                            |
   |   ( ) 5 min   ( ) 15 min             |  <-- RadioGroup
   |   ( ) 1 hr    ( ) 4 hr   ( ) 12 hr   |
   |                                      |
   | [ Edit ]   [ Delete ]                |  <-- Buttons
   +--------------------------------------+
   ```

3. **Add/Edit Reminder Screen**
   - Fields: Name (EditText), Time (TimePicker), Schedule (custom UI).
   - Save/Cancel buttons.
   - Clicking "Save" when editing takes the user back to the Show Reminder
     Screen. The Show Reminder Screen must refresh to show the updated details.

   ```plaintext
   +--------------------------------------+
   | Add Reminder                         |
   |--------------------------------------|
   | Name: [______________]               |  <-- EditText
   | Time: [ 08:00 AM   ⏰ ]              |  <-- TimePicker
   | Schedule:                            |
   |   ( ) Daily                          |
   |   ( ) Weekly [Mon][Wed][Fri]         |  <-- Chips/Buttons
   |   ( ) Fortnightly [Thu]              |
   |   ( ) Monthly [1st][Thu]             |
   |                                      |
   | [Cancel]         [Save]              |  <-- Buttons
   +--------------------------------------+
   ```

4. **Notification**
   - Appears when a reminder is due.
   - Shows name, "Dismiss", and "Later" actions.
   - Tap name to open "Show Reminder" screen.

   ```plaintext
   +--------------------------------------+
   | 🔔 Reminder: Morning Meds            |
   |--------------------------------------|
   | [ Dismiss ]   [ Later ]              |  <-- Notification actions
   +--------------------------------------+
   ```


UI Elements & Layouts
---------------------

- **Reminder List:** LazyColumn, FloatingActionButton.
- **Add/Edit Reminder:** EditText, TimePicker, Spinner/Chips for schedule, Buttons.
- **Show Reminder:** TextView, Buttons, RadioGroup.
- **Notification:** Android Notification with actions.


Data Model
----------

| Class             | Fields                                      |
|-------------------|---------------------------------------------|
| Reminder          | id (int), name (string), time (time), schedule (ReminderSchedule) |
| ReminderAction    | id (int), reminderId (int, FK), lastActionId (int, nullable, FK) |
| DismissAction     | id (int, FK), timestamp (datetime)          |
| PostponeAction    | id (int, FK), timestamp (datetime), intervalSeconds (int) |

- Use Room for local storage.


ReminderSchedule
----------------

- Daily
- Weekly (one or more days)
- Fortnightly (every two weeks, single day)
- Monthly (by day of month or nth weekday)


Notification Behavior
---------------------

- Uses AlarmManager for all scheduling (initial, repeat, and snooze notifications).
- Notification includes reminder name, "Dismiss", and "Later" actions.
- When a reminder is due, a notification is shown at the exact scheduled time.
- If the user does not interact, the notification is re-posted every minute (or at a doubling interval if "Later" is pressed), until the user taps "Later" or "Dismiss".
- The "Later" button doubles the interval each time (1, 2, 4, ... minutes), up to the next scheduled main occurrence.
- If the app is killed or swiped away, repeat notifications continue (state is persisted with Jetpack DataStore).
- If the user ignores the notification for a long time, repeats continue until the next main reminder instance is due.


Permissions & Background Work
----------------------------

- Request `POST_NOTIFICATIONS` (Android 13+).
- All background scheduling is via AlarmManager.


Accessibility & Localization
----------------------------

- Support TalkBack and large text.
- Strings in `strings.xml` for localization.


Error Handling & Testing
------------------------

- Error messages should identify the problem, and, if possible, indicate to
  the user how to resolve the problem.
- Show field validation errors below the field.
- Show general error messages using Snackbar.
- Unit tests using JUnit5
- UI tests using Espresso
- Integration tests for database
- Database migrations must be covered by tests
- Domain logic must be covered by tests
- UI functionality must be covered by tests
- Write tests as code is added or changes are made so that the changes can be
  verified as work progresses.


Edge Cases
----------

- Handle device reboot (reschedule reminders)
- Handle time zone changes:
  - Times remain local (e.g., 13:00 stays 13:00 across time zones)
  - DST double-occurring times: Notify at first occurrence
  - DST skipped times: Notify at next hour
- Handle app updates (notify immediately after update)


UI Details
----------

### Navigation Pattern

Use **Navigation Compose** with a single activity approach. The 3 main screens
should be composable destinations.

### State Management

Implement "MVI architecture using StateFlow" such that each screen has its own
ViewModel with MVI pattern (Intent → State → Effect).

For the reminder list, show all reminders.

### Schedule Selection UI

When adding or editing a Reminder, the Schedule selection (Daily, Weekly,
Fortnightly or Monthly) should use **radio buttons** so that the user can only
select one.

Once the user has selected a radio button, the user is shown further options:

- Daily: There are no further options.

- Weekly: The user is shown **chips** for the days of the week. The user is able
  to select **multiple**.

- Fortnightly: The user is prompted for the first day. Input uses a Jetpack
  Compose **modal input** date picker, which combines a text field with a modal
  date picker.

- Monthly: The user is prompted for the first day. Input uses a Jetpack
  Compose **modal input** date picker, which combines a text field with a modal
  date picker.

  After the user has chosen a day, they are shown another **radio group** to
  determine whether the Reminder is for the day of the month, or the day of the
  week. For example, if the date is Wednesday 15 January, 2025, the radio button
  labels would be:
  ```
  ( ) 15th day
  ( ) 3rd Wednesday
  ```
  "3rd Wednesday" is because Wednesday 15 January, 2025 is the Wednesday in week
  3 of January 2025.

### Time Format

The time picker should use Jetpack Compose TimeInput. The `is24Hour` parameter
of `TimeInput`'s `state` parameter should be set to `true`.

The time in the reminder list should be in 24-hour format.

### Postpone Options

The postpone intervals (5 min, 15 min, 1 hr, 4 hr, 12 hr) should be fixed.

The "doubling" behavior (1, 2, 4, ... minutes) should be implemented in the
backend.

### Empty States

If there are no reminders in the list, show "You have no reminders."

### Edit vs Add

"Add Reminder" and "Edit Reminder" should be the same screen with different
titles. When editing, the current values should be pre-filled.

### Notification Integration

Notification should have "Dismiss" and "Later" actions, and if the user presses
the name of the reminder, they should be taken to the Reminder Details / Show
Reminder screen.

### Validation

Reminder names must not be empty, and must not be longer than 50 characters.

Reminders can be set in the past. Monthly reminders can also be set on days that
do not occur every month, e.g. the 31st day of the month.

### Theme & Styling

Follow the Material 3 design system. Use the default Material Design 3 theme.
Use dynamic Material You color theming.

### Navigation Structure

Navigation should use just forward/back navigation between screens.

The "Show Reminder" screen (accessed from notifications) should be a modal. The
user should not be taken back to the reminder list.

### Schedule Selection Details

For the **Fortnightly** and **Monthly** date pickers, users should be restricted
to valid dates (e.g. prevented from selecting February 30th).

For the **Monthly** radio group (15th day vs 3rd Wednesday), this choice should
be saved as part of the `ReminderSchedule.Monthly` data.

For example, if "15th day" is selected, then `ReminderSchedule.Monthly` will
have the following values:
```
dayOfMonth = 15
dayOfWeek = null
weekOfMonth = null
```
Alternatively, if "3rd Wednesday" is selected, then `ReminderSchedule.Monthly`
will have the following values:
```
dayOfMonth = null
dayOfWeek = Wednesday
weekOfMonth = 3
```

### Reminder List Display

The reminder list should show the schedule type (e.g., "Daily", "Mon, Wed", "3rd
Tuesday") alongside the time.

### Validation Feedback

When validation fails (empty name, >50 characters):
- Show an error message below the field informing the user how to fix the
  problem. e.g. "Please give the reminder a name", or "The name should not be
  more than 50 characters"
- Disable the Save button

### Time Display Format

In the reminder list, show the time and include the schedule info (e.g.
"21:00 Daily").
