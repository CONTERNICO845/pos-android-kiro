# Requirements Document

## Introduction

Currently, menu items created in PuntoDeVenta exist only in memory via `HomeViewModel`
and are lost when the app is closed. This feature introduces local data persistence using
Room Database so that menu items survive app restarts. The implementation adds a Room
Entity, a DAO, a Database class, and a Repository layer, then rewires `HomeViewModel` to
read and write through the Repository instead of an in-memory list. The UI (card grid in
`HomeScreen`) continues to observe a `StateFlow` and requires no structural changes.

---

## Glossary

- **Room**: Android Jetpack library that provides an abstraction layer over SQLite.
- **Entity**: A Kotlin data class annotated with `@Entity` that maps to a database table.
- **DAO** (Data Access Object): An interface annotated with `@Dao` that declares database
  operations as suspending or `Flow`-returning functions.
- **Database**: A class annotated with `@Database` that is the main access point to the
  SQLite database; instantiated as a singleton.
- **Repository**: A class that owns all data-access logic and exposes coroutine-safe
  functions and `Flow` streams to the ViewModel.
- **MenuItemEntity**: The Room `@Entity` class representing a persisted menu item row.
- **MenuItemDao**: The `@Dao` interface for `MenuItemEntity` CRUD operations.
- **AppDatabase**: The `@Database` singleton that holds a reference to `MenuItemDao`.
- **MenuRepository**: The Repository that wraps `MenuItemDao` and converts entities to
  domain `MenuItem` objects.
- **HomeViewModel**: The existing ViewModel that will be refactored to depend on
  `MenuRepository` instead of an in-memory list.
- **MenuItem**: The existing domain data class (`id: String`, `emoji: String`,
  `name: String`) used throughout the UI layer.
- **UUID**: A universally unique identifier string generated once at item creation and
  stored as the primary key.
- **Flow**: Kotlin coroutines reactive stream type used to expose live database queries.
- **ViewModelFactory**: A factory class that provides constructor-injected dependencies
  (i.e., `MenuRepository`) to `HomeViewModel`.

---

## Requirements

### Requirement 1: Room Entity

**User Story:** As an Android developer, I want a Room Entity that mirrors the existing
`MenuItem` domain model, so that menu items can be stored and retrieved from SQLite.

#### Acceptance Criteria

1. THE `MenuItemEntity` SHALL be annotated with `@Entity(tableName = "menu_items")`.
2. THE `MenuItemEntity` SHALL declare a `id: String` field annotated with `@PrimaryKey` that maps to the UUID of the domain `MenuItem`.
3. THE `MenuItemEntity` SHALL declare an `emoji: String` field that stores the menu item emoji.
4. THE `MenuItemEntity` SHALL declare a `name: String` field that stores the menu item name.
5. WHEN a `MenuItemEntity` is created with a given `id`, `emoji`, and `name`, THE `MenuItemEntity` SHALL preserve all three values without modification.
6. THE `MenuItemEntity` SHALL be placed in the `data/local/` package under `com.example.puntodeventa`.

---

### Requirement 2: Data Access Object (DAO)

**User Story:** As an Android developer, I want a DAO that exposes insert, query-all,
and delete operations, so that the Repository can perform CRUD operations on
`menu_items` without writing raw SQL in business logic.

#### Acceptance Criteria

1. THE `MenuItemDao` SHALL be annotated with `@Dao`.
2. THE `MenuItemDao` SHALL declare a `suspend fun insert(item: MenuItemEntity)` function annotated with `@Insert(onConflict = OnConflictStrategy.REPLACE)`.
3. THE `MenuItemDao` SHALL declare a `fun getAllMenuItems(): Flow<List<MenuItemEntity>>` function annotated with `@Query("SELECT * FROM menu_items")`.
4. THE `MenuItemDao` SHALL declare a `suspend fun deleteById(id: String)` function annotated with `@Query("DELETE FROM menu_items WHERE id = :id")`.
5. WHEN `insert` is called with a `MenuItemEntity` whose `id` already exists in the table, THE `MenuItemDao` SHALL replace the existing row with the new data.
6. WHEN `deleteById` is called with an `id` that does not exist in the table, THE `MenuItemDao` SHALL complete without error and without modifying any existing row.
7. WHEN `getAllMenuItems` is called, THE `MenuItemDao` SHALL return a `Flow` that emits a new list every time the `menu_items` table changes.

---

### Requirement 3: Room Database Singleton

**User Story:** As an Android developer, I want a single Room Database instance for the
app, so that all components share one connection to the SQLite file.

#### Acceptance Criteria

1. THE `AppDatabase` SHALL be annotated with `@Database(entities = [MenuItemEntity::class], version = 1, exportSchema = false)`.
2. THE `AppDatabase` SHALL expose an abstract function `menuItemDao(): MenuItemDao`.
3. THE `AppDatabase` SHALL be instantiated as a singleton using `Room.databaseBuilder` with the database name `"punto_de_venta_db"`.
4. WHILE the app process is running, THE `AppDatabase` SHALL return the same instance on every access.
5. IF `AppDatabase` is accessed from multiple coroutines simultaneously, THEN THE `AppDatabase` SHALL remain consistent and thread-safe by using `@Volatile` and a `synchronized` block in its companion object.

---

### Requirement 4: Repository Layer

**User Story:** As an Android developer, I want a Repository that abstracts database
operations behind a clean API, so that `HomeViewModel` is decoupled from Room
implementation details.

#### Acceptance Criteria

1. THE `MenuRepository` SHALL accept a `MenuItemDao` parameter in its constructor.
2. THE `MenuRepository` SHALL expose a `val menuItems: Flow<List<MenuItem>>` property that maps each `MenuItemEntity` to the domain `MenuItem` by converting `id`, `emoji`, and `name` fields.
3. THE `MenuRepository` SHALL expose a `suspend fun insert(item: MenuItem)` function that converts the domain `MenuItem` to `MenuItemEntity` and calls `MenuItemDao.insert`.
4. THE `MenuRepository` SHALL expose a `suspend fun deleteById(id: String)` function that calls `MenuItemDao.deleteById` with the given `id`.
5. WHEN `MenuRepository.insert` is called with a `MenuItem`, THE `MenuRepository` SHALL produce a `MenuItemEntity` with identical `id`, `emoji`, and `name` values.
6. WHEN `MenuRepository.menuItems` emits a list, THE `MenuRepository` SHALL ensure each emitted `MenuItem` has the same field values as the corresponding `MenuItemEntity` row.

---

### Requirement 5: ViewModel Integration

**User Story:** As an Android developer, I want `HomeViewModel` to use `MenuRepository`
instead of an in-memory list, so that the UI automatically reflects persisted data and
survives app restarts.

#### Acceptance Criteria

1. THE `HomeViewModel` SHALL accept a `MenuRepository` parameter in its constructor.
2. THE `HomeViewModel` SHALL derive `menuItems` from `MenuRepository.menuItems` via `stateIn` with an initial value of `emptyList()` and a `SharingStarted.WhileSubscribed(5_000)` strategy.
3. WHEN `saveMenu(emoji, name)` is called with a non-blank `name` and non-blank `emoji`, THE `HomeViewModel` SHALL launch a coroutine in `viewModelScope` to call `MenuRepository.insert` with a `MenuItem` containing a new UUID, the given `emoji`, and the trimmed `name`.
4. WHEN `saveMenu(emoji, name)` is called in edit mode (i.e., `editingItem` is non-null), THE `HomeViewModel` SHALL call `MenuRepository.insert` with a `MenuItem` that preserves the `editingItem.id` and updates `emoji` and `name`.
5. WHEN `deleteMenu(id)` is called, THE `HomeViewModel` SHALL launch a coroutine in `viewModelScope` to call `MenuRepository.deleteById(id)`.
6. IF `saveMenu` is called with a blank `name` or blank `emoji`, THEN THE `HomeViewModel` SHALL perform no database operation.
7. THE `HomeViewModel` SHALL be instantiated via a `ViewModelProvider.Factory` that supplies the required `MenuRepository` dependency.

---

### Requirement 6: UI Observation

**User Story:** As a user, I want the card grid to display the persisted menu items
automatically when the app starts or after I add or delete a menu, so that the UI is
always in sync with the database.

#### Acceptance Criteria

1. WHEN the app is launched, THE `HomeScreen` SHALL display all menu items previously saved to the database.
2. WHEN a new menu item is saved via the dialog, THE `HomeScreen` SHALL add the new card to the grid without requiring a manual refresh.
3. WHEN a menu item is deleted, THE `HomeScreen` SHALL remove the corresponding card from the grid without requiring a manual refresh.
4. WHILE the database is emitting updates via `Flow`, THE `HomeScreen` SHALL reflect each emission through `collectAsStateWithLifecycle()` on `HomeViewModel.uiState`.
5. THE `HomeScreen` SHALL always render the `AddMenuCard` as the last item in the grid, regardless of how many menu items are stored in the database.

---

### Requirement 7: Gradle Dependencies

**User Story:** As an Android developer, I want the Room library configured in the
project's Version Catalog and build files, so that the app compiles with Room support
and KSP annotation processing.

#### Acceptance Criteria

1. THE `libs.versions.toml` SHALL declare a `room` version entry and `room-runtime`, `room-ktx`, and `room-compiler` library aliases pointing to `androidx.room`.
2. THE `libs.versions.toml` SHALL declare a `ksp` plugin version entry and a `ksp` plugin alias pointing to `com.google.devtools.ksp`.
3. THE `app/build.gradle.kts` SHALL apply the `ksp` plugin alias.
4. THE `app/build.gradle.kts` SHALL add `room-runtime` and `room-ktx` as `implementation` dependencies.
5. THE `app/build.gradle.kts` SHALL add `room-compiler` as a `ksp` dependency.
6. WHEN the project is synced and compiled after these changes, THE build system SHALL resolve all Room symbols without errors.

---

### Requirement 8: Database Migration Strategy

**User Story:** As an Android developer, I want a defined strategy for future schema
changes, so that users do not lose their data when the app is updated.

#### Acceptance Criteria

1. THE `AppDatabase` SHALL be built with `fallbackToDestructiveMigration()` for version 1 to simplify initial development.
2. WHERE a schema version bump is required in a future release, THE `AppDatabase` SHALL support the addition of a `Migration` object passed to `addMigrations()` in the builder.
3. THE `AppDatabase` configuration SHALL set `exportSchema = false` to suppress schema export warnings during initial development.
