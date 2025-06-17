# Database Schema Documentation

This directory contains the exported Room database schemas. Each version of the schema is stored in a JSON file named `schema_v{version}.json`.

## Schema Versions

### Version 1 (Initial Schema)

The initial schema includes the following tables:

1. `Reminder`
   - `id`: INTEGER PRIMARY KEY AUTOINCREMENT
   - `name`: TEXT NOT NULL
   - `time`: INTEGER NOT NULL (LocalTime stored as seconds of day)
   - `schedule`: TEXT NOT NULL (JSON serialized ReminderSchedule)

2. `StopAction`
   - `id`: INTEGER PRIMARY KEY AUTOINCREMENT
   - `reminderId`: INTEGER NOT NULL (Foreign key to Reminder)
   - `timestamp`: INTEGER NOT NULL (Instant stored as epoch seconds)
   - `lastActionId`: INTEGER (Foreign key to previous action, nullable)

3. `PostponeAction`
   - `id`: INTEGER PRIMARY KEY AUTOINCREMENT
   - `reminderId`: INTEGER NOT NULL (Foreign key to Reminder)
   - `timestamp`: INTEGER NOT NULL (Instant stored as epoch seconds)
   - `intervalSeconds`: INTEGER NOT NULL
   - `lastActionId`: INTEGER (Foreign key to previous action, nullable)

## Type Converters

The database uses the following type converters:

1. `LocalTime` ↔ `Long`: Converts between LocalTime and seconds of day
2. `Instant` ↔ `Long`: Converts between Instant and epoch seconds

## Migrations

Migrations are defined in `AppDatabase.kt` and tested in `AppDatabaseTest.kt`. Each migration should be accompanied by a test case to verify its correctness. 