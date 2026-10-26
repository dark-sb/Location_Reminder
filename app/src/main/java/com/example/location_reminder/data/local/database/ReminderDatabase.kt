package com.example.location_reminder.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.location_reminder.data.local.dao.DailyTaskDao
import com.example.location_reminder.data.local.dao.EventHistoryDao
import com.example.location_reminder.data.local.dao.LocationHistoryDao
import com.example.location_reminder.data.local.dao.ReminderDao
import com.example.location_reminder.data.local.dao.TaskDao
import com.example.location_reminder.data.local.dao.TitleHistoryDao
import com.example.location_reminder.data.local.entity.DailyTaskCompletionEntity
import com.example.location_reminder.data.local.entity.DailyTaskEntity
import com.example.location_reminder.data.local.entity.EventHistoryEntity
import com.example.location_reminder.data.local.entity.LocationHistoryEntity
import com.example.location_reminder.data.local.entity.ReminderEntity
import com.example.location_reminder.data.local.entity.TaskEntity
import com.example.location_reminder.data.local.entity.TitleHistoryEntity

@Database(
    entities = [
        ReminderEntity::class,
        TaskEntity::class,
        LocationHistoryEntity::class,
        TitleHistoryEntity::class,
        EventHistoryEntity::class,
        DailyTaskEntity::class,
        DailyTaskCompletionEntity::class
    ],
    version = 9,
    exportSchema = false
)
abstract class ReminderDatabase : RoomDatabase() {

    abstract val reminderDao: ReminderDao
    abstract val taskDao: TaskDao
    abstract val locationHistoryDao: LocationHistoryDao
    abstract val titleHistoryDao: TitleHistoryDao
    abstract val eventHistoryDao: EventHistoryDao
    abstract val dailyTaskDao: DailyTaskDao

    companion object {
        @Volatile
        private var INSTANCE: ReminderDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE reminders ADD COLUMN reminderType TEXT NOT NULL DEFAULT 'LOCATION'")
                db.execSQL("ALTER TABLE reminders ADD COLUMN dueDateTime INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE reminders ADD COLUMN isCompleted INTEGER NOT NULL DEFAULT 0")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS tasks (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        reminderId INTEGER NOT NULL,
                        title TEXT NOT NULL,
                        isCompleted INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY(reminderId) REFERENCES reminders(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_reminderId ON tasks(reminderId)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS location_history (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        latitude REAL NOT NULL,
                        longitude REAL NOT NULL,
                        lastUsedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_location_history_name ON location_history(name)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS title_history (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        lastUsedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_title_history_title ON title_history(title)")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE reminders ADD COLUMN repeatIntervalMinutes INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE reminders ADD COLUMN snoozedUntil INTEGER DEFAULT NULL")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE reminders ADD COLUMN formattedAddress TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE reminders ADD COLUMN placeId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE location_history ADD COLUMN address TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE reminders ADD COLUMN completedAt INTEGER DEFAULT NULL")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS event_history (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        eventName TEXT NOT NULL,
                        lastUsedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_event_history_eventName ON event_history(eventName)")
                try {
                    db.execSQL(
                        """
                        INSERT OR IGNORE INTO event_history (eventName, lastUsedAt)
                        SELECT title, createdAt FROM reminders WHERE reminderType = 'DATE_TIME' ORDER BY createdAt DESC LIMIT 5
                        """.trimIndent()
                    )
                } catch (e: Exception) {
                    // Non-fatal if table empty
                }
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE reminders ADD COLUMN geofenceState TEXT NOT NULL DEFAULT 'UNKNOWN'")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS daily_tasks (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        createdAt INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS daily_task_completions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        taskId INTEGER NOT NULL,
                        date TEXT NOT NULL,
                        isCompleted INTEGER NOT NULL DEFAULT 0,
                        completedAt INTEGER DEFAULT NULL,
                        FOREIGN KEY(taskId) REFERENCES daily_tasks(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_daily_task_completions_taskId ON daily_task_completions(taskId)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_daily_task_completions_taskId_date ON daily_task_completions(taskId, date)")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE daily_tasks ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE daily_tasks ADD COLUMN deletedAt INTEGER DEFAULT NULL")
            }
        }

        fun getDatabase(context: Context): ReminderDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ReminderDatabase::class.java,
                    "location_reminder_database"
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7,
                        MIGRATION_7_8,
                        MIGRATION_8_9
                    )
                    .fallbackToDestructiveMigration(false)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}