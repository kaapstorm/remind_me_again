Design: Remind Me Again (Android)
=================================

Overview
--------

Remind Me Again is an Android app for recurring reminders. It allows
users to create, edit, and manage reminders with flexible schedules and
receive timely notifications.

App Navigation & Screen Flow
----------------------------

1. **Reminder List Screen**
   - Shows all reminders.
   - "+" FloatingActionButton to add a new reminder.
   - Tap a reminder to edit.
   - Tap notification to open "Show Reminder" screen.

2. **Add/Edit Reminder Screen**
   - Fields: Name (EditText), Time (TimePicker), Schedule (custom UI).
   - Save/Cancel buttons.

3. **Show Reminder Screen**
   - Shows reminder details.
   - If due: "Stop" and "Postpone" actions.

4. **Notification**
   - Appears when a reminder is due.
   - Shows name, "Stop", and "Later" actions.

![Reminder List Mockup](img/reminder_list.png)
![Add Reminder Mockup](img/add_reminder.png)
![Show Reminder Mockup](img/show_reminder.png)
![Notification Mockup](img/notification.png)

> **Note:** Place mockup images in `doc/img/`.

UI Elements & Layouts
---------------------

- **Reminder List:** RecyclerView, FloatingActionButton.
- **Add/Edit Reminder:** EditText, TimePicker, Spinner/Chips for schedule, Buttons.
- **Show Reminder:** TextView, Buttons, RadioGroup.
- **Notification:** Android Notification with actions.

Data Model
----------

| Class             | Fields                                      |
|-------------------|---------------------------------------------|
| Reminder          | id (int), name (string), time (time), schedule (ReminderSchedule) |
| ReminderAction    | id (int), reminderId (int, FK), lastActionId (int, nullable, FK) |
| StopAction        | id (int, FK), timestamp (datetime)          |
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

- Uses AlarmManager/WorkManager for scheduling.
- Notification includes reminder name, "Stop", and "Later" actions.
- "Later" doubles the postpone interval each time (1, 2, 4, ... minutes).

Permissions & Background Work
----------------------------

- Request `POST_NOTIFICATIONS` (Android 13+).
- Use WorkManager for reliable background scheduling.

Accessibility & Localization
----------------------------

- Support TalkBack and large text.
- Strings in `strings.xml` for localization.

Error Handling & Testing
------------------------

- Show error messages via Snackbar.
- Unit and UI tests for reminder logic and notifications.

Edge Cases
----------

- Handle device reboot (reschedule reminders).
- Handle time zone changes.

---

**To help an AI tool:**
- Add mockup images to `doc/img/`.
- Use clear, unambiguous field names and types.
- Specify navigation and user flows.
- List all screens and UI elements.
- Describe notification logic and background work.
- Note platform-specific requirements (permissions, storage).
