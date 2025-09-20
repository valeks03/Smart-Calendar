package com.example.smartcalendar.data.db


import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.smartcalendar.data.model.Event


@Database(
    entities = [Event::class],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao

    companion object {
        private var INSTANCE: AppDatabase? = null
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE events ADD COLUMN reminderMinutes INTEGER NOT NULL DEFAULT 5")
            }
        }

        fun get(context: Context): AppDatabase =
            INSTANCE ?: Room.databaseBuilder(context, AppDatabase::class.java, "smartcalendar.db")
                .addMigrations(MIGRATION_1_2)
                .build()
                .also { INSTANCE = it }
    }
}