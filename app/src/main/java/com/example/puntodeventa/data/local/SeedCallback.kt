package com.example.puntodeventa.data.local

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

/**
 * RoomDatabase.Callback that triggers the DatabaseSeeder on every database open.
 * Uses runBlocking(Dispatchers.IO) to ensure seeding completes before the database
 * instance is made available to other components.
 */
class SeedCallback(
    private val seeder: DatabaseSeeder,
    private val databaseProvider: () -> AppDatabase
) : RoomDatabase.Callback() {

    override fun onOpen(db: SupportSQLiteDatabase) {
        super.onOpen(db)
        runBlocking(Dispatchers.IO) {
            seeder.seedIfEmpty(databaseProvider())
        }
    }
}
