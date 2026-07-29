# Implementation Plan: Relational Product Catalog — Data Layer (Phase 1)

## Overview

Introduce four new Room entities, their DAOs, two domain models, two repositories, a
`SelectionType` enum, and bump `AppDatabase` to version 2. All code is Kotlin and
follows the Clean Architecture patterns already in use by the existing
`MenuItemEntity` / `MenuItemDao` / `MenuRepository` stack. No UI is touched.

---

## Tasks

- [x] 1. Add `SelectionType` enum
  - [x] 1.1 Create `data/local/SelectionType.kt`
    - Define `MULTIPLE_CHECKBOXES("multiple_checkboxes")` and `SINGLE_OPTION("single_option")` entries
    - Add `companion object` with `fromValue(raw: String): SelectionType?` using `entries.associateBy { it.value }`
    - _Requirements: 3.3, 9.8_

  - [x]* 1.2 Write unit tests for `SelectionType.fromValue`
    - Test every valid value returns the correct enum constant
    - Test any unknown string (including empty string, null-coerced value, whitespace) returns `null`
    - _Requirements: 3.3_

- [x] 2. Implement `CategoryEntity`, `CategoryDao`, and `Category` domain model
  - [x] 2.1 Create `data/local/CategoryEntity.kt`
    - `@Entity(tableName = "categories")` with FK on `associatedMenuId` → `menu_items.id`, `onDelete = CASCADE`
    - Add `@Index("associatedMenuId")` as required by Room for non-PK FK columns
    - Fields: `id: String` (PK), `name: String`, `associatedMenuId: String`
    - _Requirements: 1.1, 1.2_

  - [x] 2.2 Create `data/local/CategoryDao.kt`
    - `@Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(category: CategoryEntity)`
    - `@Query("SELECT * FROM categories WHERE associatedMenuId = :menuId") fun getCategoriesByMenu(menuId: String): Flow<List<CategoryEntity>>`
    - `@Query("DELETE FROM categories WHERE id = :id") suspend fun deleteById(id: String)`
    - _Requirements: 1.3, 1.4, 1.5, 1.6_

  - [x] 2.3 Create `data/model/Category.kt`
    - `data class Category(val id: String, val name: String, val associatedMenuId: String)`
    - No Room or Android imports — usable in pure JVM tests
    - _Requirements: 7.1_

- [x] 3. Implement `ProductEntity`, `ProductDao`, and `Product` domain model
  - [x] 3.1 Create `data/local/ProductEntity.kt`
    - `@Entity(tableName = "products")` with FK on `categoryId` → `categories.id`, `onDelete = CASCADE`
    - Add `@Index("categoryId")`
    - Fields: `id: String` (PK), `emoji: String`, `name: String`, `description: String`, `basePrice: Double`, `isActive: Boolean`, `categoryId: String`
    - _Requirements: 2.1, 2.2_

  - [x] 3.2 Create `data/local/ProductDao.kt`
    - `@Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(product: ProductEntity)`
    - `@Query("SELECT * FROM products WHERE categoryId = :categoryId") fun getProductsByCategory(categoryId: String): Flow<List<ProductEntity>>`
    - `@Query("SELECT * FROM products WHERE categoryId = :categoryId AND isActive = 1") fun getActiveProductsByCategory(categoryId: String): Flow<List<ProductEntity>>`
    - `@Query("DELETE FROM products WHERE id = :id") suspend fun deleteById(id: String)`
    - _Requirements: 2.3, 2.4, 2.5, 2.6, 2.7_

  - [x] 3.3 Create `data/model/Product.kt`
    - `data class Product(val id: String, val emoji: String, val name: String, val description: String, val basePrice: Double, val isActive: Boolean, val categoryId: String)`
    - No Room or Android imports — usable in pure JVM tests
    - _Requirements: 6.6_

- [x] 4. Implement `CustomizationGroupEntity` and `CustomizationGroupDao`
  - [x] 4.1 Create `data/local/CustomizationGroupEntity.kt`
    - `@Entity(tableName = "customization_groups")` with FK on `productId` → `products.id`, `onDelete = CASCADE`
    - Add `@Index("productId")`
    - Fields: `id: String` (PK), `productId: String`, `groupName: String`, `selectionType: String`
    - _Requirements: 3.1, 3.2_

  - [x] 4.2 Create `data/local/CustomizationGroupDao.kt`
    - Internal insert: `@Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertInternal(group: CustomizationGroupEntity)`
    - `@Query("SELECT * FROM customization_groups WHERE productId = :productId") fun getGroupsByProduct(productId: String): Flow<List<CustomizationGroupEntity>>`
    - `@Query("DELETE FROM customization_groups WHERE id = :id") suspend fun deleteById(id: String)`
    - Note: public `insert` with `selectionType` validation lives in `ProductRepository` (see task 6)
    - _Requirements: 3.4, 3.5, 3.6_

- [x] 5. Implement `CustomizationOptionEntity` and `CustomizationOptionDao`
  - [x] 5.1 Create `data/local/CustomizationOptionEntity.kt`
    - `@Entity(tableName = "customization_options")` with FK on `groupId` → `customization_groups.id`, `onDelete = CASCADE`
    - Add `@Index("groupId")`
    - Fields: `id: String` (PK), `groupId: String`, `optionName: String`, `extraPrice: Double`
    - _Requirements: 4.1, 4.2_

  - [x] 5.2 Create `data/local/CustomizationOptionDao.kt`
    - `@Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(option: CustomizationOptionEntity)`
    - `@Query("SELECT * FROM customization_options WHERE groupId = :groupId") fun getOptionsByGroup(groupId: String): Flow<List<CustomizationOptionEntity>>`
    - `@Query("DELETE FROM customization_options WHERE id = :id") suspend fun deleteById(id: String)`
    - Note: `extraPrice ≥ 0.0` validation lives in `ProductRepository` (see task 6)
    - _Requirements: 4.4, 4.5, 4.6_

- [x] 6. Update `AppDatabase` to version 2
  - [x] 6.1 Modify `data/local/AppDatabase.kt`
    - Change `version = 1` → `version = 2` in `@Database` annotation
    - Add `CategoryEntity::class`, `ProductEntity::class`, `CustomizationGroupEntity::class`, `CustomizationOptionEntity::class` to the `entities` array
    - Add four abstract DAO accessors: `categoryDao()`, `productDao()`, `customizationGroupDao()`, `customizationOptionDao()`
    - Add `.setForeignKeyConstraintsEnabled(true)` to the `Room.databaseBuilder` chain (required for CASCADE to work at runtime)
    - Confirm `fallbackToDestructiveMigration()` is already present
    - _Requirements: 5.1, 5.2, 5.3, 5.4_

- [x] 7. Checkpoint — compile and verify entity schema
  - Ensure all entities compile without KSP errors (Room validates FKs, indices, and column types at compile time)
  - Ensure all tests pass, ask the user if questions arise.

- [x] 8. Implement `CategoryRepository`
  - [x] 8.1 Create `data/repository/CategoryRepository.kt`
    - Constructor: `class CategoryRepository(private val dao: CategoryDao)`
    - `fun getCategoriesByMenu(menuId: String): Flow<List<Category>>` — maps entities via private `CategoryEntity.toDomain()`
    - `suspend fun insert(category: Category)` — maps via private `Category.toEntity()` then delegates to `dao.insert`
    - `suspend fun deleteById(id: String)` — delegates to `dao.deleteById`
    - Private mapping helpers: `CategoryEntity.toDomain()` and `Category.toEntity()` (field-by-field, lossless)
    - _Requirements: 7.1, 7.2, 7.3, 7.4_

  - [x]* 8.2 Write property test for `CategoryRepository` — Property 2: Category mapping round-trip
    - **Property 2: Category mapping round-trip**
    - Use Kotest `checkAll(100, Arb.string(1..36), Arb.string(1..120), Arb.string(1..36))` to generate arbitrary `(id, name, associatedMenuId)` triples
    - Assert `Category(id, name, menuId).toEntity().toDomain() == Category(id, name, menuId)`
    - **Validates: Requirements 7.4, 9.2**

  - [x]* 8.3 Write unit tests for `CategoryRepository` — query delegation and no-op delete
    - Use a fake `CategoryDao` stub (no Room needed)
    - Test `getCategoriesByMenu` maps `CategoryEntity` list → `Category` list correctly
    - Test `deleteById` with non-existent id performs no-op (0 exceptions, 0 rows affected)
    - _Requirements: 7.1, 7.3_

- [x] 9. Implement `ProductRepository`
  - [x] 9.1 Create `data/repository/ProductRepository.kt`
    - Constructor: `class ProductRepository(private val productDao: ProductDao, private val groupDao: CustomizationGroupDao, private val optionDao: CustomizationOptionDao)`
    - `fun getProductsByCategory(categoryId: String): Flow<List<Product>>` — maps via `ProductEntity.toDomain()`
    - `fun getActiveProductsByCategory(categoryId: String): Flow<List<Product>>` — maps active entities only
    - `suspend fun insert(product: Product)` — maps via `Product.toEntity()`, delegates to `productDao.insert`; FK exceptions propagate to caller
    - `suspend fun deleteById(id: String)` — delegates to `productDao.deleteById`
    - `suspend fun insertGroup(group: CustomizationGroupEntity)` — calls `SelectionType.fromValue(group.selectionType)`, throws `IllegalArgumentException` if null, else delegates to `groupDao.insertInternal`
    - `suspend fun insertOption(option: CustomizationOptionEntity)` — requires `option.extraPrice >= 0.0`, throws `IllegalArgumentException` if negative, else delegates to `optionDao.insert`
    - Private mapping helpers: `ProductEntity.toDomain()` and `Product.toEntity()` (field-by-field, lossless)
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5, 6.6, 3.3, 4.3_

  - [x]* 9.2 Write property test for `ProductRepository` — Property 1: Product mapping round-trip
    - **Property 1: Product mapping round-trip**
    - Use `checkAll(100, Arb.string(1..36), Arb.string(1..8), Arb.string(1..120), Arb.string(1..500), Arb.double(0.0..9999.99), Arb.boolean(), Arb.string(1..36))`
    - Assert `Product(id, emoji, name, desc, price, active, catId).toEntity().toDomain() == original`
    - **Validates: Requirements 6.6, 9.1**

  - [x]* 9.3 Write property test for `ProductRepository` — Property 8: Invalid `selectionType` is rejected
    - **Property 8: Invalid selectionType is rejected**
    - Use `checkAll(100, Arb.string())` with `assume(raw !in setOf("multiple_checkboxes", "single_option"))`
    - Assert `insertGroup` throws `IllegalArgumentException` and does NOT call `groupDao.insertInternal`
    - **Validates: Requirements 3.3, 9.8**

  - [x]* 9.4 Write property test for `ProductRepository` — Property 9: Negative `extraPrice` is rejected
    - **Property 9: Negative extraPrice is rejected**
    - Use `checkAll(100, Arb.double(Double.MIN_VALUE..-0.001))`
    - Assert `insertOption` throws `IllegalArgumentException` and does NOT call `optionDao.insert`
    - **Validates: Requirements 4.3**

  - [x]* 9.5 Write unit tests for `ProductRepository` — FK propagation and active filter
    - Use fake DAO stubs
    - Test `insert(product)` where `categoryId` references no row propagates `SQLiteConstraintException` without swallowing
    - Test `getActiveProductsByCategory` with mixed active/inactive entities returns only active ones
    - _Requirements: 6.2, 6.5_

- [x] 10. Checkpoint — run unit and property-based tests
  - Run `./gradlew :app:test` (pure JVM tests)
  - Ensure all tests pass, ask the user if questions arise.

- [x] 11. Write instrumented DAO tests (`androidTest`)
  - [x] 11.1 Create `androidTest/data/local/CategoryDaoTest.kt`
    - Use `Room.inMemoryDatabaseBuilder` with `setForeignKeyConstraintsEnabled(true)`
    - **Property 4: Count invariant — `getCategoriesByMenu`**: insert N distinct `CategoryEntity` rows under same `associatedMenuId`, assert `getCategoriesByMenu` emits list of size N; N=0 emits empty list
    - **Property 7 (Category): Idempotent upsert** — insert same `CategoryEntity` twice with different `name`, assert only one row with second `name`
    - **Property 10 (Category): Query filter isolation** — insert categories under two `associatedMenuId` values, assert each query returns only its own rows
    - _Requirements: 1.3, 1.4, 1.5, 1.6, 9.4_

  - [x] 11.2 Create `androidTest/data/local/ProductDaoTest.kt`
    - **Property 3: Count invariant — `getProductsByCategory`**: insert N distinct `ProductEntity` rows under same `categoryId`, assert emitted list size equals N
    - **Property 6: DAO insert/retrieve field preservation** — insert one `ProductEntity`, assert all 7 fields equal in retrieved row
    - **Property 7 (Product): Idempotent upsert** — same `id`, different `name`, second insert wins
    - **Property 11: Active product filter** — insert active and inactive rows, assert `getActiveProductsByCategory` returns only active rows
    - _Requirements: 2.3, 2.4, 2.5, 2.6, 2.7, 9.3, 9.6_

  - [x] 11.3 Create `androidTest/data/local/CustomizationGroupDaoTest.kt`
    - **Property 7 (Group): Idempotent upsert** via `insertInternal`
    - **Property 10 (Group): Query filter isolation** for `getGroupsByProduct`
    - _Requirements: 3.4, 3.5, 3.6_

  - [x] 11.4 Create `androidTest/data/local/CustomizationOptionDaoTest.kt`
    - **Property 5: Count invariant — `getOptionsByGroup`**: insert N rows under same `groupId`, assert emitted list size equals N
    - **Property 10 (Option): Query filter isolation** for `getOptionsByGroup`
    - _Requirements: 4.4, 4.5, 4.6, 9.5_

  - [x] 11.5 Create `androidTest/data/local/CascadeDeletionTest.kt`
    - Build full 5-level hierarchy: `MenuItemEntity → CategoryEntity → ProductEntity → CustomizationGroupEntity → CustomizationOptionEntity`
    - Test delete `MenuItemEntity` → assert all four descendant tables empty (Requirement 8.1)
    - Test delete `CategoryEntity` → assert products, groups, options empty (Requirement 8.3)
    - Test delete `ProductEntity` → assert groups and options empty (Requirement 8.4)
    - Test delete `CustomizationGroupEntity` → assert options empty (Requirement 8.5)
    - Test FK violation on `ProductEntity.insert` with unknown `categoryId` throws constraint exception (Requirement 6.5)
    - _Requirements: 8.1, 8.2, 8.3, 8.4, 8.5_

- [x] 12. Final checkpoint — full test suite
  - Run `./gradlew :app:test` (unit + PBT)
  - Run `./gradlew :app:connectedAndroidTest` (instrumented DAO and cascade tests)
  - Ensure all tests pass, ask the user if questions arise.

---

## Notes

- Tasks marked with `*` are optional and can be skipped for a faster MVP
- All Kotlin files go under `app/src/main/java/com/example/puntodeventa/`
- Unit/PBT tests go under `app/src/test/`, instrumented tests under `app/src/androidTest/`
- `kotest-property` is already declared in `app/build.gradle.kts`; no new Gradle changes needed
- PBT tasks use Kotest's `checkAll` with a minimum of 100 iterations per property
- `setForeignKeyConstraintsEnabled(true)` is **mandatory** for cascade behavior — must be set in both the app builder (task 6.1) and the in-memory test builder (task 11.x)
- `MenuItemEntity`, `MenuItemDao`, and `MenuRepository` are **never modified**

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "2.1", "2.3", "3.3"] },
    { "id": 1, "tasks": ["1.2", "2.2", "3.1"] },
    { "id": 2, "tasks": ["3.2", "4.1"] },
    { "id": 3, "tasks": ["4.2", "5.1"] },
    { "id": 4, "tasks": ["5.2", "6.1"] },
    { "id": 5, "tasks": ["8.1", "9.1"] },
    { "id": 6, "tasks": ["8.2", "8.3", "9.2", "9.3", "9.4", "9.5"] },
    { "id": 7, "tasks": ["11.1", "11.2", "11.3", "11.4", "11.5"] }
  ]
}
```
