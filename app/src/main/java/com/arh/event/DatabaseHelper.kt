package com.arh.event.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "arch_event.db"
        private const val DATABASE_VERSION = 1

        const val TABLE_EVENTS = "events"
        const val TABLE_SETTINGS = "user_settings"

        const val COL_EVENT_ID = "id"
        const val COL_TITLE = "title"
        const val COL_DESCRIPTION = "description"
        const val COL_CATEGORY = "category"
        const val COL_LOCATION = "location"
        const val COL_START_TIME = "start_time"

        const val COL_SETTINGS_ID = "id"
        const val COL_NOTIFICATIONS_ENABLED = "notifications_enabled"
        const val COL_NOTIFY_PLAN = "notify_plan"
        const val COL_NOTIFY_EMERGENCY = "notify_emergency"
        const val COL_FREQUENCY = "frequency"
        const val COL_PRIORITY_LEVEL = "priority_level"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_EVENTS (
                $COL_EVENT_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_TITLE TEXT NOT NULL,
                $COL_DESCRIPTION TEXT,
                $COL_CATEGORY TEXT CHECK($COL_CATEGORY IN ('Плановые','Аварийные')),
                $COL_LOCATION TEXT,
                $COL_START_TIME TEXT
            )
            """
        )

        db.execSQL(
            """
            CREATE TABLE $TABLE_SETTINGS (
                $COL_SETTINGS_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_NOTIFICATIONS_ENABLED INTEGER,
                $COL_NOTIFY_PLAN INTEGER,
                $COL_NOTIFY_EMERGENCY INTEGER,
                $COL_FREQUENCY TEXT DEFAULT 'Каждые 5 мин',
                $COL_PRIORITY_LEVEL INTEGER
            )
            """
        )

        db.execSQL(
            """
            INSERT INTO $TABLE_SETTINGS ($COL_NOTIFICATIONS_ENABLED, $COL_NOTIFY_PLAN, $COL_NOTIFY_EMERGENCY, $COL_FREQUENCY, $COL_PRIORITY_LEVEL)
            VALUES (1, 1, 1, 'Каждые 5 мин', 5)
            """
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_EVENTS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_SETTINGS")
        onCreate(db)
    }

    fun insertEvent(
        title: String,
        description: String,
        category: String,
        location: String,
        date: String
    ) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_TITLE, title)
            put(COL_DESCRIPTION, description)
            put(COL_CATEGORY, category)
            put(COL_LOCATION, location)
            put(COL_START_TIME, date)
        }
        db.insert(TABLE_EVENTS, null, values)
    }

    fun getAllEvents(): Cursor {
        val db = readableDatabase
        return db.query(TABLE_EVENTS, null, null, null, null, null, null)
    }

    fun getUserSettings(): Cursor {
        val db = readableDatabase
        return db.query(TABLE_SETTINGS, null, null, null, null, null, null)
    }

    fun updateUserSettings(
        notificationsEnabled: Boolean,
        plan: Boolean,
        emergency: Boolean,
        frequency: String,
        priority: Int
    ) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_NOTIFICATIONS_ENABLED, if (notificationsEnabled) 1 else 0)
            put(COL_NOTIFY_PLAN, if (plan) 1 else 0)
            put(COL_NOTIFY_EMERGENCY, if (emergency) 1 else 0)
            put(COL_FREQUENCY, frequency)
            put(COL_PRIORITY_LEVEL, priority)
        }
        db.update(TABLE_SETTINGS, values, null, null)
    }
}
