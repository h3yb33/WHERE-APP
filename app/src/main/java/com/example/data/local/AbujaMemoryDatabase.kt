package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        LearnedCorridorEntity::class,
        TripSurveyEntity::class,
        SavedRouteEntity::class,
        SavedPlaceEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AbujaMemoryDatabase : RoomDatabase() {
    abstract fun abujaMemoryDao(): AbujaMemoryDao

    companion object {
        @Volatile
        private var INSTANCE: AbujaMemoryDatabase? = null

        fun getInstance(context: Context): AbujaMemoryDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AbujaMemoryDatabase::class.java,
                    "where_abuja_memory.db"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
