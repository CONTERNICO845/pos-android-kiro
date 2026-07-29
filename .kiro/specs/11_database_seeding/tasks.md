# Implementation Plan: Database Seeding

## Overview

This plan implements a Database Seeder that populates the Room database with default "Tacos" menu data on first launch or after a destructive migration. The implementation proceeds from the deterministic UUID helper → seed data definitions → seeder logic → callback integration → AppDatabase wiring → tests. All code is in Kotlin targeting the existing `com.example.puntodeventa.data.local` package.

## Tasks

- [x] 1. Create DatabaseSeeder with deterministic UUID helper and SeedData object
  - [x] 1.1 Create DatabaseSeeder class with deterministicId helper and SeedData object
    - Create `DatabaseSeeder.kt` in `data/local/`
    - Implement `private fun deterministicId(namespace: String, name: String): String` using `UUID.nameUUIDFromBytes("$namespace:$name".toByteArray(Charsets.UTF_8)).toString()`
    - Define `private object SeedData` containing all entity instances:
      - 1 `MenuItemEntity` (name="Tacos", emoji="🌮")
      - 4 `CategoryEntity` (Tacos, Tortas, Tacos Dorados, Refrescos) with `associatedMenuId` referencing the menu
      - 12 `ProductEntity` across 4 categories with correct names, basePrices, emojis, description="", isActive=true
      - 10 `CustomizationGroupEntity` (one per non-Refrescos product) with groupName="Remover", selectionType=MULTIPLE_CHECKBOXES
      - 40 `CustomizationOptionEntity` (3 per taco, 5 per torta, 4 per taco dorado) with extraPrice=0.0
    - All IDs generated via `deterministicId` with namespaces: "menu", "category", "product", "group", "option"
    - _Requirements: 3.1, 3.2, 4.1–4.7, 5.1–5.7, 6.1–6.7, 7.1–7.5, 8.1–8.5, 9.1–9.3, 10.1–10.3, 11.1–11.2, 12.1–12.3, 13.7_

  - [x] 1.2 Implement seedIfEmpty suspend function
    - Implement `suspend fun seedIfEmpty(database: AppDatabase)` in `DatabaseSeeder`
    - Query `SELECT COUNT(*) FROM menu_items` to check emptiness
    - If count == 0, execute `database.withTransaction { ... }` inserting in order: MenuItemEntity → CategoryEntity → ProductEntity → CustomizationGroupEntity → CustomizationOptionEntity
    - If count > 0, return immediately (no-op)
    - Let exceptions propagate naturally (Room rolls back transaction on error)
    - _Requirements: 1.1, 1.2, 1.4, 2.1, 2.2, 2.3, 2.4, 2.5, 13.1, 14.1, 14.2, 14.3_

- [x] 2. Create SeedCallback and integrate into AppDatabase
  - [x] 2.1 Create SeedCallback class
    - Create `SeedCallback` class in `data/local/` implementing `RoomDatabase.Callback`
    - Constructor takes `seeder: DatabaseSeeder` and `databaseProvider: () -> AppDatabase`
    - Override `onOpen(db: SupportSQLiteDatabase)` — call `runBlocking(Dispatchers.IO) { seeder.seedIfEmpty(databaseProvider()) }`
    - _Requirements: 1.3, 15.1, 15.2, 15.3_

  - [x] 2.2 Update AppDatabase.getInstance() to add SeedCallback
    - Instantiate `DatabaseSeeder()` inside `getInstance()`
    - Add `.addCallback(SeedCallback(seeder) { INSTANCE!! })` **before** the existing `.addCallback(foreignKeyCallback)` in the Room builder chain
    - Ensure `INSTANCE` is assigned before `.build()` returns (existing pattern handles this)
    - _Requirements: 1.1, 1.3, 2.3_

- [x] 3. Checkpoint - Ensure project compiles and seed runs
  - Ensure all tests pass, ask the user if questions arise.

- [x] 4. Write unit tests for seed data content
  - [x] 4.1 Create DatabaseSeederTest with in-memory Room database setup
    - Create `DatabaseSeederTest.kt` in `test/.../data/local/` (JVM test) or `androidTest/.../data/local/` (instrumented) depending on Room in-memory availability
    - Set up `Room.inMemoryDatabaseBuilder()` with `allowMainThreadQueries()` in `@Before`
    - Close database in `@After`
    - _Requirements: 1.1, 2.1_

  - [x] 4.2 Write tests verifying exact seed data counts and content
    - Test: exactly 1 menu item with name "Tacos" and emoji "🌮"
    - Test: exactly 4 categories with correct names and FK to menu
    - Test: exactly 12 products with correct names, prices, emojis, isActive=true
    - Test: exactly 10 customization groups with groupName "Remover" and selectionType MULTIPLE_CHECKBOXES
    - Test: exactly 40 customization options with correct names and extraPrice 0.0
    - Test: zero customization groups/options for Refrescos products
    - _Requirements: 3.1, 4.1–4.7, 5.1–5.7, 6.1–6.7, 7.1–7.5, 8.1–8.5, 9.1–9.3, 10.1–10.3, 11.1–11.2, 12.1–12.3_

  - [x] 4.3 Write test verifying idempotency (seed twice, counts unchanged)
    - Seed an in-memory database, record row counts, seed again, assert counts identical
    - _Requirements: 14.1, 14.2_

  - [x] 4.4 Write test verifying foreign key integrity (seed with PRAGMA foreign_keys = ON)
    - Enable `PRAGMA foreign_keys = ON` before seeding, verify no constraint errors
    - _Requirements: 13.1–13.6_

- [x] 5. Write property-based tests for correctness properties
  - [x]* 5.1 Write property test for idempotency (Property 1)
    - **Property 1: Idempotency**
    - Seed a database, then run seeder N additional times (N generated by Kotest), verify all table row counts and content remain unchanged after each execution
    - **Validates: Requirements 1.2, 2.5, 14.1, 14.2**

  - [x]* 5.2 Write property test for atomicity (Property 2)
    - **Property 2: Atomicity (all-or-nothing)**
    - Seed an empty in-memory database, verify all 5 tables have at least one row; also verify that if menu_items has 0 rows, all other seeded tables also have 0 rows
    - **Validates: Requirements 2.1, 2.4**

  - [x]* 5.3 Write property test for rollback on failure (Property 3)
    - **Property 3: Rollback on failure**
    - Use MockK or a custom DAO spy to inject failure at a random insertion step (generated by Kotest), verify all 5 tables contain 0 rows after error propagation
    - **Validates: Requirements 2.2**

  - [x]* 5.4 Write property test for deterministic ID generation (Property 4)
    - **Property 4: Deterministic ID generation**
    - Seed N independent in-memory databases (N generated by Kotest), collect all entity ID sets, verify all sets are equal across executions
    - **Validates: Requirements 3.2, 4.6, 5.7, 6.7, 7.5, 8.5**

  - [x]* 5.5 Write property test for UUID format invariant (Property 5)
    - **Property 5: UUID format invariant**
    - Collect all primary key IDs from a seeded database, verify each is exactly 36 characters matching `[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}`
    - **Validates: Requirements 13.7**

- [x] 6. Final checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties from the design document
- Unit tests validate specific examples and edge cases
- The project uses Kotest Property (`io.kotest:kotest-property`) for property-based tests (already in build.gradle.kts)
- Room `inMemoryDatabaseBuilder()` requires Android instrumentation context — tests in task 4 should be placed in `androidTest/` unless Robolectric is set up
- The `SeedData` object is private to `DatabaseSeeder`; tests verify behavior through database queries, not internal state
- Total seed data: 67 rows (1 menu + 4 categories + 12 products + 10 groups + 40 options)

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1"] },
    { "id": 1, "tasks": ["1.2"] },
    { "id": 2, "tasks": ["2.1"] },
    { "id": 3, "tasks": ["2.2"] },
    { "id": 4, "tasks": ["4.1"] },
    { "id": 5, "tasks": ["4.2", "4.3", "4.4"] },
    { "id": 6, "tasks": ["5.1", "5.2", "5.3", "5.4", "5.5"] }
  ]
}
```
