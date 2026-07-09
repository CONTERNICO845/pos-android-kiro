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
        CustomizationOptionEntity::class    // version 2 — new
    ],
    version = 2,
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
                    val db = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "punto_de_venta_db"
                    )
                        .fallbackToDestructiveMigration(dropAllTables = true)
                        .addCallback(foreignKeyCallback)   // enables CASCADE at runtime
                        .build()
                    INSTANCE = db
                    db
                }
            }
        }
    }
}
