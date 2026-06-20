package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        UserProfile::class,
        ServiceReport::class,
        DailyPlan::class,
        MagazinePlacement::class,
        BibleStudent::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userProfileDao(): UserProfileDao
    abstract fun serviceReportDao(): ServiceReportDao
    abstract fun dailyPlanDao(): DailyPlanDao
    abstract fun magazinePlacementDao(): MagazinePlacementDao
    abstract fun bibleStudentDao(): BibleStudentDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "meu_ministerio_db"
                )
                .fallbackToDestructiveMigration() // safe for simple template apps
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
