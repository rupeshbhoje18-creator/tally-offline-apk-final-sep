package com.example.tallycustomerapp.data

import android.content.Context
import androidx.room.*

@Database(entities = [OfflineCompany::class], version = 2, exportSchema = false)
@TypeConverters(ListTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun companyDao(): CompanyDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tally_customer_db"
                )
                .fallbackToDestructiveMigration()
                .build().also { INSTANCE = it }
            }
    }
}