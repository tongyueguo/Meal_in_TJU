package com.example.mealintju.data
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [mealInfo::class], version = 1, exportSchema = false)
abstract class maelInfoDatabase : RoomDatabase() {

    abstract fun mealInfoDao(): mealInfoDao

    companion object {
        @Volatile
        private var INSTANCE: maelInfoDatabase? = null
        fun getDatabase(context: Context): maelInfoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    maelInfoDatabase::class.java,
                    "mealInfoDatabase"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                return instance
            }
        }
    }
}