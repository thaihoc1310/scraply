package com.example.scraply.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        StampEntity::class,
        CollectionEntity::class,
        CollectionStampEntity::class,
        ProjectEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class ScraplyDatabase : RoomDatabase() {
    abstract fun stampDao(): StampDao
    abstract fun collectionDao(): CollectionDao
    abstract fun collectionStampDao(): CollectionStampDao
    abstract fun projectDao(): ProjectDao

    companion object {
        @Volatile private var instance: ScraplyDatabase? = null

        fun get(context: Context): ScraplyDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    ScraplyDatabase::class.java,
                    "scraply.db",
                ).fallbackToDestructiveMigration(dropAllTables = true).build().also { instance = it }
            }
    }
}
