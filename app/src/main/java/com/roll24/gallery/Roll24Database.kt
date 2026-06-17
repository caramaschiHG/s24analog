package com.roll24.gallery

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [CaptureRecord::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(CaptureMetadataConverter::class)
abstract class Roll24Database : RoomDatabase() {
    abstract fun captureDao(): CaptureDao

    companion object {
        @Volatile
        private var instance: Roll24Database? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE captures ADD COLUMN metadata TEXT")
            }
        }

        fun get(context: Context): Roll24Database {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    Roll24Database::class.java,
                    "roll24_gallery.db"
                )
                .addMigrations(MIGRATION_1_2)
                .fallbackToDestructiveMigration()
                .build().also { instance = it }
            }
        }
    }
}
