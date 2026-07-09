# Implementation Plan: local-data-persistence

## Overview

Replace the in-memory `List<MenuItem>` in `HomeViewModel` with a Room SQLite database,
following the layered architecture: `Entity → DAO → AppDatabase → Repository → ViewModel`.
The UI layer (`HomeScreen` and all composables) requires no structural changes. Tests
use unit tests, Kotest property-based tests, and instrumented Room in-memory DAO tests.

---

## Tasks

- [x] 1. Configure Gradle dependencies for Room and KSP
  - Add `room` and `ksp` version entries to `gradle/libs.versions.toml`
  - Add `room-runtime`, `room-ktx`, `room-compiler` library aliases to `libs.versions.toml`
  - Add `ksp` plugin alias to `libs.versions.toml`
  - Apply `alias(libs.plugins.ksp)` in `app/build.gradle.kts` plugins block
  - Add `implementation(libs.androidx.room.runtime)` and `implementation(libs.androidx.room.ktx)` to `app/build.gradle.kts` dependencies
  - Add `ksp(libs.androidx.room.compiler)` to `app/build.gradle.kts` dependencies
  - Add `kotest-property` and `kotlinx-coroutines-test` to `[libraries]` in `libs.versions.toml` and as `testImplementation` deps in `app/build.gradle.kts`
  - Add `androidx-room-testing` library alias and `androidTestImplementation` dependency for instrumented DAO tests
  - Sync project to verify all symbols resolve
  - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5, 7.6_

- [x] 2. Implement the `MenuItemEntity` data class
  - [x] 2.1 Create `MenuItemEntity.kt` in `app/src/main/java/com/example/puntodeventa/data/local/`
    - Annotate with `@Entity(tableName = "menu_items")`
    - Declare `@PrimaryKey val id: String`, `val emoji: String`, `val name: String`
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.6_

  - [-]* 2.2 Write property test for entity field preservation (Property 1)
    - **Property 1: Entity field preservation**
    - Use `checkAll(100, Arb.string(), Arb.string(), Arb.string())` to verify that constructing `MenuItemEntity(id, emoji, name)` and reading back its fields always returns the identical values
    - File: `app/src/test/java/com/example/puntodeventa/data/local/MenuItemEntityTest.kt`
    - **Validates: Requirements 1.5**

- [x] 3. Implement `MenuItemDao`
  - [x] 3.1 Create `MenuItemDao.kt` in `app/src/main/java/com/example/puntodeventa/data/local/`
    - Annotate interface with `@Dao`
    - Declare `@Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(item: MenuItemEntity)`
    - Declare `@Query("SELECT * FROM menu_items") fun getAllMenuItems(): Flow<List<MenuItemEntity>>`
    - Declare `@Query("DELETE FROM menu_items WHERE id = :id") suspend fun deleteById(id: String)`
    - _Requirements: 2.1, 2.2, 2.3, 2.4_

  - [ ]* 3.2 Write instrumented DAO tests (Property 2 + example-based)
    - **Property 2: Upsert replaces existing row**
    - Use `Room.inMemoryDatabaseBuilder` in `@Before`; run `checkAll(100, Arb.string(), Arb.string(), Arb.string(), Arb.string())` to verify that inserting a second entity with the same `id` but different `emoji`/`name` results in exactly one row matching the second values
    - Also add example-based cases: insert + `getAllMenuItems` returns item; `deleteById` removes item; `deleteById` with missing id is no-op; Flow emits after insert
    - File: `app/src/androidTest/java/com/example/puntodeventa/data/local/MenuItemDaoTest.kt`
    - **Validates: Requirements 2.5, 2.6, 2.7**

- [x] 4. Implement `AppDatabase`
  - [x] 4.1 Create `AppDatabase.kt` in `app/src/main/java/com/example/puntodeventa/data/local/`
    - Annotate with `@Database(entities = [MenuItemEntity::class], version = 1, exportSchema = false)`
    - Declare `abstract fun menuItemDao(): MenuItemDao`
    - Implement companion object singleton using `@Volatile` + `synchronized` double-checked locking
    - Build with `Room.databaseBuilder(..., "punto_de_venta_db").fallbackToDestructiveMigration()`
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 8.1, 8.3_

  - [ ]* 4.2 Write unit test for AppDatabase singleton invariant (Property 3)
    - **Property 3: AppDatabase singleton invariant**
    - Call `AppDatabase.getInstance(context)` multiple times sequentially and verify referential equality (`assertSame`) across all returned instances
    - File: `app/src/androidTest/java/com/example/puntodeventa/data/local/AppDatabaseTest.kt`
    - **Validates: Requirements 3.3, 3.4, 3.5**

- [x] 5. Checkpoint — verify data layer compiles and DAO tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 6. Implement `MenuRepository`
  - [x] 6.1 Create `MenuRepository.kt` in `app/src/main/java/com/example/puntodeventa/data/repository/`
    - Accept `MenuItemDao` in constructor
    - Expose `val menuItems: Flow<List<MenuItem>>` using `dao.getAllMenuItems().map { entities -> entities.map { it.toDomain() } }`
    - Expose `suspend fun insert(item: MenuItem)` that calls `dao.insert(item.toEntity())`
    - Expose `suspend fun deleteById(id: String)` that calls `dao.deleteById(id)`
    - Add private extension functions `MenuItemEntity.toDomain()` and `MenuItem.toEntity()`
    - _Requirements: 4.1, 4.2, 4.3, 4.4_

  - [ ]* 6.2 Write property test for entity-to-domain mapping round-trip (Property 4)
    - **Property 4: Repository entity-to-domain mapping round-trip**
    - Use `checkAll(100, Arb.string(), Arb.string(), Arb.string())` with a fake DAO that emits a list of one `MenuItemEntity`; collect `menuItems` and verify each `MenuItem` field matches the entity
    - File: `app/src/test/java/com/example/puntodeventa/data/repository/MenuRepositoryTest.kt`
    - **Validates: Requirements 4.2, 4.6**

  - [ ]* 6.3 Write property test for domain-to-entity mapping round-trip (Property 5)
    - **Property 5: Repository domain-to-entity mapping round-trip**
    - Use `checkAll(100, Arb.string(), Arb.string(), Arb.string())` with a capturing fake DAO; call `repo.insert(MenuItem(id, emoji, name))` and assert the captured `MenuItemEntity` has identical `id`, `emoji`, `name`
    - File: `app/src/test/java/com/example/puntodeventa/data/repository/MenuRepositoryTest.kt`
    - **Validates: Requirements 4.3, 4.5**

- [x] 7. Refactor `HomeViewModel` to use `MenuRepository`
  - [x] 7.1 Replace in-memory list logic in `HomeViewModel.kt`
    - Add `MenuRepository` constructor parameter and a `Factory` inner class
    - Add private `DialogState` data class (`isOpen`, `editingItem`)
    - Replace `_uiState` with separate `_dialogState: MutableStateFlow<DialogState>`
    - Derive `val uiState: StateFlow<HomeUiState>` via `combine(repository.menuItems, _dialogState)` with `stateIn(viewModelScope, WhileSubscribed(5_000), HomeUiState())`
    - Rewrite `saveMenu` to launch `repository.insert(item)` in `viewModelScope` and update `_dialogState` only
    - Rewrite `deleteMenu` to launch `repository.deleteById(id)` in `viewModelScope`
    - Keep `openDialog`, `openEditDialog`, `dismissDialog` updating `_dialogState` only
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.7_

  - [ ]* 7.2 Write property test — ViewModel mirrors repository items (Property 6)
    - **Property 6: ViewModel mirrors repository items**
    - Use `checkAll(100, Arb.list(Arb.bind(Arb.string(), Arb.string(), Arb.string()) { id, e, n -> MenuItem(id, e, n) }))` with a `FakeMenuRepository`; emit the list, collect `uiState`, assert `menuItems` matches
    - File: `app/src/test/java/com/example/puntodeventa/ui/home/HomeViewModelTest.kt`
    - **Validates: Requirements 5.2**

  - [ ]* 7.3 Write property test — saveMenu new item (Property 7)
    - **Property 7: saveMenu correctness — new item**
    - Use `checkAll(100, Arb.string(1..20).filter { it.isNotBlank() }, Arb.string(1..50).filter { it.isNotBlank() })` with a `FakeMenuRepository`; call `saveMenu(emoji, name)` in create mode; assert `insert` called once with correct `emoji`, trimmed `name`, and a non-empty id
    - File: `app/src/test/java/com/example/puntodeventa/ui/home/HomeViewModelTest.kt`
    - **Validates: Requirements 5.3**

  - [ ]* 7.4 Write property test — saveMenu preserves id in edit mode (Property 8)
    - **Property 8: saveMenu correctness — edit mode preserves id**
    - Use `checkAll(100, Arb.string(), Arb.string(1..20).filter { it.isNotBlank() }, Arb.string(1..50).filter { it.isNotBlank() })` where the first arg is the existing item's `id`; call `openEditDialog(existingItem)` then `saveMenu(emoji, name)`; assert the `MenuItem` passed to `insert` has `id == existingItem.id`
    - File: `app/src/test/java/com/example/puntodeventa/ui/home/HomeViewModelTest.kt`
    - **Validates: Requirements 5.4**

  - [ ]* 7.5 Write property test — deleteMenu delegates to repository (Property 9)
    - **Property 9: deleteMenu delegation**
    - Use `checkAll(100, Arb.string())` with a `FakeMenuRepository`; call `deleteMenu(id)`; assert `deleteById` was called exactly once with the same `id`
    - File: `app/src/test/java/com/example/puntodeventa/ui/home/HomeViewModelTest.kt`
    - **Validates: Requirements 5.5, 4.4**

  - [ ]* 7.6 Write property test — blank-input validation gate (Property 10)
    - **Property 10: Blank-input validation gate**
    - Use `checkAll(100, Arb.string().filter { it.isBlank() }, Arb.string(1..20))` for blank name; and `checkAll(100, Arb.string(1..20), Arb.string().filter { it.isBlank() })` for blank emoji; assert `FakeMenuRepository.insertCallCount == 0` after each call
    - File: `app/src/test/java/com/example/puntodeventa/ui/home/HomeViewModelTest.kt`
    - **Validates: Requirements 5.6**

- [x] 8. Wire `AppDatabase` and `MenuRepository` into `MainActivity`
  - [x] 8.1 Instantiate `AppDatabase` and `MenuRepository` in `MainActivity.kt`
    - In `onCreate`, call `AppDatabase.getInstance(this).menuItemDao()` to obtain the DAO
    - Construct `MenuRepository(dao)` and pass it to `HomeViewModel.Factory`
    - Pass `factory = HomeViewModel.Factory(repository)` to the `viewModel()` call site where `HomeScreen` is composed (or wherever the ViewModel is first created)
    - _Requirements: 5.7, 6.1, 6.2, 6.3, 6.4_

- [x] 9. Final checkpoint — all tests pass and app builds cleanly
  - Ensure all unit, property, and instrumented tests pass; ensure a debug build compiles without errors; ask the user if questions arise.

---

## Notes

- Tasks marked with `*` are optional and can be skipped for a faster MVP; all 10 correctness properties will be left unverified if skipped
- Each property test uses a minimum of **100 iterations** via Kotest's `checkAll`
- Fake/stub implementations (`FakeMenuRepository`, `FakeMenuItemDao`) must be created as test-only helpers inside the respective test files or a shared `test/fakes/` package
- DAO instrumented tests run on an Android device/emulator using `Room.inMemoryDatabaseBuilder` — no real file I/O
- `HomeScreen` and all composables below it require **zero structural changes**; only the ViewModel factory injection in `MainActivity` changes at the UI-wiring level
- `fallbackToDestructiveMigration()` is intentional for v1; migrate to `addMigrations()` when schema version is bumped (Req 8.2)

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1"] },
    { "id": 1, "tasks": ["2.1", "3.1", "4.1"] },
    { "id": 2, "tasks": ["2.2", "3.2", "4.2", "6.1"] },
    { "id": 3, "tasks": ["6.2", "6.3", "7.1"] },
    { "id": 4, "tasks": ["7.2", "7.3", "7.4", "7.5", "7.6"] },
    { "id": 5, "tasks": ["8.1"] }
  ]
}
```
