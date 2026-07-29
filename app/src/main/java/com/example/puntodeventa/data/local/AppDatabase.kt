package com.example.puntodeventa.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        MenuItemEntity::class,              // version 1 — unchanged
        CategoryEntity::class,              // version 2 — new
        ProductEntity::class,               // version 2 — new
        CustomizationGroupEntity::class,    // version 2 — new
        CustomizationOptionEntity::class,   // version 2 — new
        OrderEntity::class,                 // version 3 — new
        OrderItemEntity::class,             // version 3 — new
        OrderItemCustomizationEntity::class // version 3 — new
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    // ── Version 1 accessors (unchanged) ──────────────────────────────────────
    abstract fun menuItemDao(): MenuItemDao

    // ── Version 2 accessors (new) ─────────────────────────────────────────────
    abstract fun categoryDao(): CategoryDao
    abstract fun productDao(): ProductDao
    abstract fun customizationGroupDao(): CustomizationGroupDao
    abstract fun customizationOptionDao(): CustomizationOptionDao

    // ── Version 3 accessors (new) ─────────────────────────────────────────────
    abstract fun orderDao(): OrderDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /** Callback that enables SQLite foreign-key enforcement on every connection. */
        private val foreignKeyCallback = object : Callback() {
            override fun onOpen(db: SupportSQLiteDatabase) {
                db.execSQL("PRAGMA foreign_keys = ON")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            val existing = INSTANCE
            if (existing != null) return existing
            return synchronized(this) {
                val current = INSTANCE
                if (current != null) {
                    current
                } else {
                    val seeder = DatabaseSeeder()
                    val db = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "punto_de_venta_db"
                    )
                        .fallbackToDestructiveMigration(dropAllTables = true)
                        .addCallback(foreignKeyCallback)   // enables CASCADE at runtime
                        .build()
                    INSTANCE = db
                    // Seed after build: triggers onOpen (FK pragma) then seeds if empty.
                    // runBlocking is safe here because we are on a background/init thread
                    // and the DB is fully built before any DAO access.
                    kotlinx.coroutines.runBlocking(kotlinx.coroutines.Dispatchers.IO) {
                        seeder.seedIfEmpty(db)
                    }
                    db
                }
            }
        }
    }
}
