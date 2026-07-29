package com.example.puntodeventa.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.kotest.common.ExperimentalKotest
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import io.kotest.property.PropTestConfig
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Property-based tests for [DatabaseSeeder].
 *
 * Uses Kotest Property with AndroidJUnit4 runner since Room in-memory databases
 * require Android instrumentation context.
 */
@OptIn(ExperimentalKotest::class)
@RunWith(AndroidJUnit4::class)
class DatabaseSeederPropertyTest {

    private lateinit var db: AppDatabase
    private lateinit var seeder: DatabaseSeeder

    /** Enables SQLite FK enforcement for the in-memory test database. */
    private val foreignKeyCallback = object : RoomDatabase.Callback() {
        override fun onOpen(db: SupportSQLiteDatabase) {
            db.execSQL("PRAGMA foreign_keys = ON")
        }
    }

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .addCallback(foreignKeyCallback)
            .build()
        seeder = DatabaseSeeder()
    }

    @After
    fun tearDown() {
        db.close()
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun queryCount(table: String): Int {
        val cursor = db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM $table")
        return cursor.use {
            it.moveToFirst()
            it.getInt(0)
        }
    }

    private fun queryAllIds(table: String): Set<String> {
        val cursor = db.openHelper.readableDatabase.query("SELECT id FROM $table")
        return cursor.use {
            val ids = mutableSetOf<String>()
            while (it.moveToNext()) {
                ids.add(it.getString(0))
            }
            ids
        }
    }

    private fun createFreshDatabase(): AppDatabase {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .addCallback(foreignKeyCallback)
            .build()
    }

    private val seededTables = listOf(
        "menu_items", "categories", "products",
        "customization_groups", "customization_options"
    )

    // ── Property 1: Idempotency ──────────────────────────────────────────────
    // Feature: 11_database_seeding, Property 1: Idempotency
    /**
     * **Validates: Requirements 1.2, 2.5, 14.1, 14.2**
     *
     * For any database state where at least one row exists in the menu_items table,
     * executing the seeder any number of additional times SHALL leave the row count
     * and content of all five seeded tables unchanged.
     */
    @Test
    fun property1_idempotency_seedingMultipleTimesDoesNotChangeData() = runBlocking {
        // Seed the database once to establish baseline
        seeder.seedIfEmpty(db)

        // Record baseline counts and IDs
        val baselineCounts = seededTables.associateWith { queryCount(it) }
        val baselineIds = seededTables.associateWith { queryAllIds(it) }

        checkAll(PropTestConfig(iterations = 100), Arb.int(1..10)) { additionalRuns ->
            // Run seeder N additional times
            repeat(additionalRuns) {
                seeder.seedIfEmpty(db)
            }

            // Verify all table row counts remain unchanged
            for (table in seededTables) {
                assertEquals(
                    "Table '$table' count should be unchanged after $additionalRuns additional seeds",
                    baselineCounts[table],
                    queryCount(table)
                )
            }

            // Verify all IDs remain the same (content unchanged)
            for (table in seededTables) {
                assertEquals(
                    "Table '$table' IDs should be unchanged after $additionalRuns additional seeds",
                    baselineIds[table],
                    queryAllIds(table)
                )
            }
        }
    }

    // ── Property 2: Atomicity (all-or-nothing) ───────────────────────────────
    // Feature: 11_database_seeding, Property 2: Atomicity (all-or-nothing)
    /**
     * **Validates: Requirements 2.1, 2.4**
     *
     * For any successful seed operation on an empty database, all five seeded tables
     * SHALL have at least one row; conversely, if the database has zero rows in
     * menu_items after a seed attempt, then all other seeded tables SHALL also have
     * zero rows.
     */
    @Test
    fun property2_atomicity_allTablesPopulatedOrAllEmpty() = runBlocking {
        checkAll(PropTestConfig(iterations = 100), Arb.int(1..5)) { _ ->
            // Create a fresh database for each iteration
            val freshDb = createFreshDatabase()
            try {
                val freshSeeder = DatabaseSeeder()

                // Seed the fresh database
                freshSeeder.seedIfEmpty(freshDb)

                // Check menu_items count
                val menuCount = freshDb.openHelper.readableDatabase
                    .query("SELECT COUNT(*) FROM menu_items")
                    .use { it.moveToFirst(); it.getInt(0) }

                if (menuCount > 0) {
                    // All tables must have at least one row
                    for (table in seededTables) {
                        val count = freshDb.openHelper.readableDatabase
                            .query("SELECT COUNT(*) FROM $table")
                            .use { it.moveToFirst(); it.getInt(0) }
                        assertTrue(
                            "If menu_items has rows, '$table' must also have rows (got $count)",
                            count > 0
                        )
                    }
                } else {
                    // If menu_items is empty, all other tables must also be empty
                    for (table in seededTables) {
                        val count = freshDb.openHelper.readableDatabase
                            .query("SELECT COUNT(*) FROM $table")
                            .use { it.moveToFirst(); it.getInt(0) }
                        assertEquals(
                            "If menu_items has 0 rows, '$table' must also have 0 rows",
                            0, count
                        )
                    }
                }
            } finally {
                freshDb.close()
            }
        }
    }

    // ── Property 3: Rollback on failure ──────────────────────────────────────
    // Feature: 11_database_seeding, Property 3: Rollback on failure
    /**
     * **Validates: Requirements 2.2**
     *
     * For any point of failure injected during the seed transaction (before commit),
     * all five seeded tables SHALL contain zero rows after the error propagates.
     */
    @Test
    fun property3_rollbackOnFailure_noPartialDataAfterError() = runBlocking {
        // The seeder inserts in 5 steps:
        // Step 0: menu_items (1 row)
        // Step 1: categories (4 rows)
        // Step 2: products (12 rows)
        // Step 3: customization_groups (10 rows)
        // Step 4: customization_options (40 rows)
        // We inject failure at a random step by using a FailingSeeder wrapper.

        checkAll(PropTestConfig(iterations = 100), Arb.int(0..4)) { failAtStep ->
            val freshDb = createFreshDatabase()
            try {
                // Use a seeder that throws at a specific step
                val failingSeeder = FailingDatabaseSeeder(failAtStep)

                // Attempt to seed — should throw
                var exceptionThrown = false
                try {
                    failingSeeder.seedIfEmpty(freshDb)
                } catch (_: SimulatedInsertionFailure) {
                    exceptionThrown = true
                }

                assertTrue(
                    "Exception should have been thrown at step $failAtStep",
                    exceptionThrown
                )

                // Verify all tables are empty after rollback
                for (table in seededTables) {
                    val count = freshDb.openHelper.readableDatabase
                        .query("SELECT COUNT(*) FROM $table")
                        .use { it.moveToFirst(); it.getInt(0) }
                    assertEquals(
                        "Table '$table' should have 0 rows after failure at step $failAtStep",
                        0, count
                    )
                }
            } finally {
                freshDb.close()
            }
        }
    }

    // ── Property 4: Deterministic ID generation ──────────────────────────────
    // Feature: 11_database_seeding, Property 4: Deterministic ID generation
    /**
     * **Validates: Requirements 3.2, 4.6, 5.7, 6.7, 7.5, 8.5**
     *
     * For any number of independent seed executions on a freshly-created (empty)
     * database, the set of all generated entity IDs SHALL be identical across
     * executions.
     */
    @Test
    fun property4_deterministicIds_allExecutionsProduceSameIds() = runBlocking {
        // Seed a reference database to get the baseline IDs
        val referenceDb = createFreshDatabase()
        val referenceSeeder = DatabaseSeeder()
        referenceSeeder.seedIfEmpty(referenceDb)

        val referenceIds = seededTables.associateWith { table ->
            referenceDb.openHelper.readableDatabase
                .query("SELECT id FROM $table ORDER BY id")
                .use { cursor ->
                    val ids = mutableListOf<String>()
                    while (cursor.moveToNext()) {
                        ids.add(cursor.getString(0))
                    }
                    ids
                }
        }
        referenceDb.close()

        checkAll(PropTestConfig(iterations = 100), Arb.int(1..3)) { _ ->
            val freshDb = createFreshDatabase()
            try {
                val freshSeeder = DatabaseSeeder()
                freshSeeder.seedIfEmpty(freshDb)

                // Collect IDs from this execution
                for (table in seededTables) {
                    val currentIds = freshDb.openHelper.readableDatabase
                        .query("SELECT id FROM $table ORDER BY id")
                        .use { cursor ->
                            val ids = mutableListOf<String>()
                            while (cursor.moveToNext()) {
                                ids.add(cursor.getString(0))
                            }
                            ids
                        }

                    assertEquals(
                        "IDs in '$table' should be identical across independent seed executions",
                        referenceIds[table],
                        currentIds
                    )
                }
            } finally {
                freshDb.close()
            }
        }
    }

    // ── Property 5: UUID format invariant ────────────────────────────────────
    // Feature: 11_database_seeding, Property 5: UUID format invariant
    /**
     * **Validates: Requirements 13.7**
     *
     * For any entity inserted by the seeder, its primary key (id) field SHALL be
     * a non-empty string of exactly 36 characters matching the pattern
     * [0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}.
     */
    @Test
    fun property5_uuidFormat_allIdsMatchUuidPattern() = runBlocking {
        val uuidRegex = Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")

        checkAll(PropTestConfig(iterations = 100), Arb.int(1..3)) { _ ->
            val freshDb = createFreshDatabase()
            try {
                val freshSeeder = DatabaseSeeder()
                freshSeeder.seedIfEmpty(freshDb)

                // Collect all primary key IDs from all seeded tables
                for (table in seededTables) {
                    val ids = freshDb.openHelper.readableDatabase
                        .query("SELECT id FROM $table")
                        .use { cursor ->
                            val idList = mutableListOf<String>()
                            while (cursor.moveToNext()) {
                                idList.add(cursor.getString(0))
                            }
                            idList
                        }

                    for (id in ids) {
                        assertEquals(
                            "ID '$id' in table '$table' should be exactly 36 characters",
                            36, id.length
                        )
                        assertTrue(
                            "ID '$id' in table '$table' should match UUID format pattern",
                            uuidRegex.matches(id)
                        )
                    }
                }
            } finally {
                freshDb.close()
            }
        }
    }
}
