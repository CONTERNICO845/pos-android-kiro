# Requirements Document

## Introduction

This feature introduces Phase 1 of the relational product catalog for the PuntoDeVenta Android POS app.
The scope is **data-layer only**: four new Room entities with full foreign-key relationships, their DAOs,
an `AppDatabase` migration to version 2, and repositories exposing reactive Kotlin `Flow` streams.
No UI is built in this phase. The existing `MenuItemEntity` / `MenuItemDao` / `MenuRepository` stack
remains unchanged; the new tables extend it by hanging `CategoryEntity` off `MenuItemEntity`.

---

## Glossary

- **AppDatabase**: The Room `RoomDatabase` singleton that registers all entities and exposes all DAOs.
- **CategoryEntity**: Room entity representing a product category, scoped to one `MenuItemEntity`.
- **CategoryDao**: Room DAO providing CRUD operations for `CategoryEntity`.
- **CategoryRepository**: Repository that wraps `CategoryDao` and exposes domain-mapped `Flow` streams.
- **CustomizationGroupEntity**: Room entity representing a named group of customization options for one product.
- **CustomizationGroupDao**: Room DAO providing CRUD operations for `CustomizationGroupEntity`.
- **CustomizationOptionEntity**: Room entity representing a single selectable customization option within a group.
- **CustomizationOptionDao**: Room DAO providing CRUD operations for `CustomizationOptionEntity`.
- **MenuItemEntity**: Existing Room entity (`menu_items` table) acting as the root of the product hierarchy.
- **ProductEntity**: Room entity representing a saleable product belonging to one category.
- **ProductDao**: Room DAO providing CRUD operations for `ProductEntity`.
- **ProductRepository**: Repository that wraps `ProductDao` and exposes domain-mapped `Flow` streams.
- **SelectionType**: Enumerated string value, either `"multiple_checkboxes"` or `"single_option"`, stored in `CustomizationGroupEntity.selectionType`.
- **FK**: Foreign key constraint enforced by Room at the SQLite level.
- **Destructive Migration**: Room strategy that drops and recreates the database when the schema version increases, used during development.
- **Flow**: Kotlin `kotlinx.coroutines.flow.Flow`; a cold, reactive stream of database query results.

---

## Requirements

### Requirement 1: CategoryEntity and CategoryDao

**User Story:** As a POS developer, I want a `CategoryEntity` table backed by a DAO, so that product categories can be stored, queried, and deleted per menu.

#### Acceptance Criteria

1. THE `CategoryEntity` SHALL define the table `categories` with columns: `id` (String, primary key), `name` (String, non-null), and `associatedMenuId` (String, non-null FK referencing `menu_items.id`).
2. THE `CategoryEntity` SHALL declare a Room `ForeignKey` on `associatedMenuId` referencing `MenuItemEntity` (the parent) with `onDelete = CASCADE`.
3. THE `CategoryDao` SHALL expose a `suspend fun insert(category: CategoryEntity)` function with `OnConflictStrategy.REPLACE`.
4. WHEN `deleteById(id: String)` is called on `CategoryDao` and no row with that `id` exists, THE `CategoryDao` SHALL perform a no-op (0 rows affected, no exception thrown).
5. WHEN `getCategoriesByMenu(menuId: String)` is called on `CategoryDao` and no rows match, THE `CategoryDao` SHALL return a `Flow<List<CategoryEntity>>` that emits an empty list.
6. WHEN `getCategoriesByMenu(menuId: String)` is called on `CategoryDao`, THE `CategoryDao` SHALL return a `Flow<List<CategoryEntity>>` emitting only rows where `associatedMenuId` equals `menuId`.
7. WHEN a `MenuItemEntity` row is deleted from `menu_items`, THE `AppDatabase` SHALL cascade the deletion to all `CategoryEntity` rows whose `associatedMenuId` references that `MenuItemEntity`.

---

### Requirement 2: ProductEntity and ProductDao

**User Story:** As a POS developer, I want a `ProductEntity` table backed by a DAO, so that individual products and their prices can be persisted and queried per category.

#### Acceptance Criteria

1. THE `ProductEntity` SHALL define the table `products` with columns: `id` (String, primary key, max 36 chars), `emoji` (String, non-null, max 8 chars), `name` (String, non-null, max 120 chars), `description` (String, non-null, max 500 chars), `basePrice` (Double, non-null, ≥ 0.0, stored with at most 2 decimal places of precision), `isActive` (Boolean, non-null), and `categoryId` (String, non-null FK referencing `categories.id`).
2. THE `ProductEntity` SHALL declare a Room `ForeignKey` on `categoryId` referencing `CategoryEntity` with `onDelete = CASCADE`.
3. THE `ProductDao` SHALL expose a `suspend fun insert(product: ProductEntity)` function with `OnConflictStrategy.REPLACE`.
4. WHEN `deleteById(id: String)` is called on `ProductDao` and no row with that `id` exists, THE `ProductDao` SHALL perform a no-op (0 rows affected, no exception thrown).
5. WHEN `getProductsByCategory(categoryId: String)` is called on `ProductDao` and no rows match, THE `ProductDao` SHALL return a `Flow<List<ProductEntity>>` that emits an empty list.
6. WHEN `getProductsByCategory(categoryId: String)` is called on `ProductDao`, THE `ProductDao` SHALL return a `Flow<List<ProductEntity>>` emitting only rows where `categoryId` equals the argument.
7. WHEN `getActiveProductsByCategory(categoryId: String)` is called on `ProductDao`, THE `ProductDao` SHALL return a `Flow<List<ProductEntity>>` emitting only rows where `categoryId` equals the argument AND `isActive` is `true`; IF no such rows exist, THEN the emitted list SHALL be empty.
8. WHEN a `CategoryEntity` row is deleted, THE `AppDatabase` SHALL cascade the deletion to all `ProductEntity` rows whose `categoryId` references that `CategoryEntity`, relying solely on the Room-declared `ForeignKey` with `onDelete = CASCADE` database constraint to perform this cascade without any additional application-level deletion logic.

---

### Requirement 3: CustomizationGroupEntity and CustomizationGroupDao

**User Story:** As a POS developer, I want a `CustomizationGroupEntity` table backed by a DAO, so that configurable option groups (e.g. "Ingredients", "Size") can be associated with a product.

#### Acceptance Criteria

1. THE `CustomizationGroupEntity` SHALL define the table `customization_groups` with columns: `id` (String, primary key), `productId` (String, non-null FK referencing `products.id`), `groupName` (String, non-null), and `selectionType` (String, non-null).
2. THE `CustomizationGroupEntity` SHALL declare a Room `ForeignKey` on `productId` referencing `ProductEntity` with `onDelete = CASCADE`.
3. IF a `CustomizationGroupEntity` whose `selectionType` is not `"multiple_checkboxes"` or `"single_option"` is passed to `CustomizationGroupDao.insert`, THEN THE `CustomizationGroupDao` SHALL throw an `IllegalArgumentException` before passing the entity to Room and SHALL NOT persist the row.
4. THE `CustomizationGroupDao` SHALL expose a `suspend fun insert(group: CustomizationGroupEntity)` function with `OnConflictStrategy.REPLACE`.
5. THE `CustomizationGroupDao` SHALL expose a `suspend fun deleteById(id: String)` function that removes the row whose `id` matches the argument; IF no row with that `id` exists, THEN THE `CustomizationGroupDao` SHALL perform a no-op (0 rows affected, no exception thrown).
6. WHEN `getGroupsByProduct(productId: String)` is called on `CustomizationGroupDao`, THE `CustomizationGroupDao` SHALL return a `Flow<List<CustomizationGroupEntity>>` emitting only rows where `productId` equals the argument; IF no rows match, THEN the emitted list SHALL be empty.
7. WHEN a `ProductEntity` row is deleted, THE `AppDatabase` SHALL cascade the deletion to all `CustomizationGroupEntity` rows whose `productId` references that `ProductEntity`.

---

### Requirement 4: CustomizationOptionEntity and CustomizationOptionDao

**User Story:** As a POS developer, I want a `CustomizationOptionEntity` table backed by a DAO, so that individual selectable options (e.g. "No onion", "Extra cheese") with optional price adjustments can be stored per group.

#### Acceptance Criteria

1. THE `CustomizationOptionEntity` SHALL define the table `customization_options` with columns: `id` (String, primary key), `groupId` (String, non-null FK referencing `customization_groups.id`), `optionName` (String, non-null, max 120 chars), and `extraPrice` (Double, non-null, ≥ 0.0; a value of `0.0` encodes "no surcharge").
2. THE `CustomizationOptionEntity` SHALL declare a Room `ForeignKey` on `groupId` referencing `CustomizationGroupEntity` with `onDelete = CASCADE`.
3. IF a `CustomizationOptionEntity` whose `extraPrice` is negative is passed to `CustomizationOptionDao.insert`, THEN THE `CustomizationOptionDao` SHALL throw an `IllegalArgumentException` and SHALL NOT persist the row.
4. THE `CustomizationOptionDao` SHALL expose a `suspend fun insert(option: CustomizationOptionEntity)` function with `OnConflictStrategy.REPLACE`.
5. THE `CustomizationOptionDao` SHALL expose a `suspend fun deleteById(id: String)` function that removes the row whose `id` matches the argument; IF no row with that `id` exists, THEN THE `CustomizationOptionDao` SHALL perform a no-op (0 rows affected, no exception thrown).
6. WHEN `getOptionsByGroup(groupId: String)` is called on `CustomizationOptionDao`, THE `CustomizationOptionDao` SHALL return a `Flow<List<CustomizationOptionEntity>>` emitting only rows where `groupId` equals the argument; IF no rows match, THEN the emitted list SHALL be empty.
7. IF `CustomizationOptionDao.insert` is called with a `CustomizationOptionEntity` whose `groupId` references no existing `CustomizationGroupEntity` row, THEN THE `AppDatabase` SHALL throw a foreign key constraint exception and SHALL NOT persist the row.
8. WHEN a `CustomizationGroupEntity` row is deleted, THE `AppDatabase` SHALL cascade the deletion to all `CustomizationOptionEntity` rows whose `groupId` references that `CustomizationGroupEntity`.

---

### Requirement 5: AppDatabase Migration to Version 2

**User Story:** As a POS developer, I want `AppDatabase` to be updated to version 2 with all four new entities registered, so that the database schema reflects the full product hierarchy without manual SQL migration during development.

#### Acceptance Criteria

1. THE `AppDatabase` SHALL be annotated with `version = 2` and SHALL include `CategoryEntity`, `ProductEntity`, `CustomizationGroupEntity`, and `CustomizationOptionEntity` in its `entities` array alongside `MenuItemEntity`.
2. THE `AppDatabase` SHALL call `fallbackToDestructiveMigration()` in the `Room.databaseBuilder` chain to handle the version 1 → 2 schema change during development.
3. THE `AppDatabase` SHALL expose four abstract DAO accessor functions with the following signatures: `abstract fun categoryDao(): CategoryDao`, `abstract fun productDao(): ProductDao`, `abstract fun customizationGroupDao(): CustomizationGroupDao`, and `abstract fun customizationOptionDao(): CustomizationOptionDao`.
4. WHEN `AppDatabase.getInstance(context)` is called for the first time, THE `AppDatabase` SHALL create and return a new singleton instance; WHEN `AppDatabase.getInstance(context)` is called on any subsequent invocation within the same process, THE `AppDatabase` SHALL return the exact same instance without creating a new one.

---

### Requirement 6: ProductRepository

**User Story:** As a POS developer, I want a `ProductRepository` that wraps `ProductDao`, so that the rest of the app interacts with products through a clean domain layer without direct DAO coupling.

#### Acceptance Criteria

1. THE `ProductRepository` SHALL expose a `getProductsByCategory(categoryId: String): Flow<List<Product>>` function that maps `ProductEntity` rows to `Product` domain objects; IF no rows match the given `categoryId`, THEN the emitted list SHALL be empty.
2. THE `ProductRepository` SHALL expose a `getActiveProductsByCategory(categoryId: String): Flow<List<Product>>` function that maps only active `ProductEntity` rows (where `isActive = true`) to `Product` domain objects; IF no active rows match, THEN the emitted list SHALL be empty.
3. WHEN `insert(product: Product)` is called on `ProductRepository`, THE `ProductRepository` SHALL map the `Product` domain object to a `ProductEntity` and delegate to `ProductDao.insert`.
4. WHEN `deleteById(id: String)` is called on `ProductRepository` and no row with that `id` exists, THE `ProductRepository` SHALL perform a no-op by delegating to `ProductDao.deleteById` (which itself is a no-op).
5. IF `insert(product: Product)` is called on `ProductRepository` where `product.categoryId` references no existing `CategoryEntity`, THEN THE `ProductRepository` SHALL propagate the FK constraint exception thrown by `ProductDao` to the caller without swallowing it.
6. WHEN a `Product` domain object is mapped to `ProductEntity` and back to `Product`, THE `ProductRepository` SHALL produce a `Product` whose `id`, `emoji`, `name`, `description`, `basePrice`, `isActive`, and `categoryId` fields are all identical to those of the original `Product`, regardless of whether the original `Product` contains invalid data such as negative prices — the mapping SHALL preserve the original values exactly.

---

### Requirement 7: CategoryRepository

**User Story:** As a POS developer, I want a `CategoryRepository` that wraps `CategoryDao`, so that the rest of the app interacts with categories through a clean domain layer.

#### Acceptance Criteria

1. THE `CategoryRepository` SHALL expose a `getCategoriesByMenu(menuId: String): Flow<List<Category>>` function that maps `CategoryEntity` rows — each with fields `id` (String), `name` (String), and `associatedMenuId` (String) — to `Category` domain objects with the same three fields; IF no rows match, THEN the emitted list SHALL be empty.
2. WHEN `insert(category: Category)` is called on `CategoryRepository`, THE `CategoryRepository` SHALL map the `Category` domain object's `id`, `name`, and `associatedMenuId` fields to the corresponding `CategoryEntity` fields and delegate to `CategoryDao.insert`.
3. THE `CategoryRepository` SHALL expose a `suspend fun deleteById(id: String)` function that delegates to `CategoryDao.deleteById`; IF no row with that `id` exists, THEN the operation SHALL be a no-op (0 rows affected, no exception thrown).
4. WHEN a `Category` domain object (with fields `id`, `name`, `associatedMenuId`) is mapped to `CategoryEntity` and back to `Category`, THE `CategoryRepository` SHALL produce a `Category` whose `id`, `name`, and `associatedMenuId` are all identical to those of the original.

---

### Requirement 8: Data Integrity — Cascade Deletion Chain

**User Story:** As a POS developer, I want cascading deletes to propagate through the full hierarchy, so that orphaned rows are never left in the database.

#### Acceptance Criteria

1. WHEN a `MenuItemEntity` row is deleted from `menu_items`, THE `AppDatabase` SHALL delete all `CategoryEntity` rows in `categories` whose `associatedMenuId` references that `MenuItemEntity`, which in turn triggers cascades through `products`, `customization_groups`, and `customization_options` as defined in Requirements 2–4.
2. IF any step in the cascade initiated by a `MenuItemEntity` deletion fails, THEN THE `AppDatabase` SHALL roll back the entire delete operation, leaving all rows in their original state even if some deletions succeeded before the failure occurred, propagate the failure as a thrown exception to the caller; IF the rollback itself succeeds but exception propagation fails, THEN that SHALL be treated as a system failure requiring additional error handling or logging mechanisms.
3. WHEN a `CategoryEntity` row is deleted from `categories`, THE `AppDatabase` SHALL delete all `ProductEntity` rows whose `categoryId` references that `CategoryEntity`, which in turn triggers cascades through `customization_groups` and `customization_options`; IF any step fails, THEN THE `AppDatabase` SHALL roll back the entire operation and propagate the failure as a thrown exception.
4. WHEN a `ProductEntity` row is deleted from `products`, THE `AppDatabase` SHALL delete all `CustomizationGroupEntity` rows whose `productId` references that `ProductEntity`, which in turn triggers cascade deletion of all `CustomizationOptionEntity` rows in `customization_options`; IF any step fails, THEN THE `AppDatabase` SHALL roll back the entire operation and propagate the failure as a thrown exception.
5. WHEN a `CustomizationGroupEntity` row is deleted from `customization_groups`, THE `AppDatabase` SHALL delete all `CustomizationOptionEntity` rows whose `groupId` references that group; IF the cascade fails, THEN THE `AppDatabase` SHALL roll back the entire operation, propagate the failure as a thrown exception to the caller, and leave all rows in their original state.

---

### Requirement 9: Correctness Properties for Property-Based Testing

**User Story:** As a POS developer, I want formally stated correctness properties, so that property-based tests can validate the data layer behaves correctly for arbitrary valid inputs.

#### Acceptance Criteria

1. FOR ALL `Product` domain objects `p` where all string fields are non-null and non-empty, `basePrice ≥ 0.0`, and `id` is a non-empty string, THE `ProductRepository` SHALL satisfy the round-trip property: mapping `p` to `ProductEntity` then back to `Product` SHALL yield an object whose `id`, `emoji`, `name`, `description`, `basePrice`, `isActive`, and `categoryId` are all structurally equal to those of `p`.
2. FOR ALL `Category` domain objects `c` where `id`, `name`, and `associatedMenuId` are all non-null and non-empty strings, THE `CategoryRepository` SHALL satisfy the round-trip property: mapping `c` to `CategoryEntity` then back to `Category` SHALL yield an object whose `id`, `name`, and `associatedMenuId` are all structurally equal to those of `c`.
3. WHEN `N` distinct `ProductEntity` rows (with distinct `id` values) are inserted into the `products` table under the same `categoryId`, THE `ProductDao` SHALL emit a list of exactly `N` items from `getProductsByCategory(categoryId)`; IF `N = 0`, THEN the emitted list SHALL be empty.
4. WHEN `N` distinct `CategoryEntity` rows (with distinct `id` values) are inserted into the `categories` table under the same `associatedMenuId`, THE `CategoryDao` SHALL emit a list of exactly `N` items from `getCategoriesByMenu(menuId)`; IF `N = 0`, THEN the emitted list SHALL be empty.
5. WHEN `N` distinct `CustomizationOptionEntity` rows (with distinct `id` values) are inserted into the `customization_options` table under the same `groupId`, THE `CustomizationOptionDao` SHALL emit a list of exactly `N` items from `getOptionsByGroup(groupId)`; IF `N = 0`, THEN the emitted list SHALL be empty.
6. WHEN a `ProductEntity` is inserted and then retrieved by `getProductsByCategory`, THE `ProductDao` SHALL return a list that contains exactly one row whose `id`, `emoji`, `name`, `description`, `basePrice`, `isActive`, and `categoryId` are structurally equal to those of the inserted entity (insert / contains invariant).
7. WHEN the same `ProductEntity` (same `id`) is inserted twice via `ProductDao.insert`, THE `ProductDao` SHALL store exactly one row for that `id`, retaining the field values from the second insert (idempotent upsert via `REPLACE` strategy).
8. IF a `CustomizationGroupEntity` with a `selectionType` value other than `"multiple_checkboxes"` or `"single_option"` is passed to `CustomizationGroupDao.insert`, THEN THE `CustomizationGroupDao` SHALL throw an `IllegalArgumentException` and SHALL NOT persist the row.
