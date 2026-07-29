# Requirements Document

## Introduction

The Database Seeder is a mechanism that populates the Room database with a default menu, categories, products, and customization options when the application starts for the first time or detects that the relevant tables are empty. This ensures a ready-to-use experience out of the box for POS operators without requiring manual data entry.

## Glossary

- **Database_Seeder**: The component responsible for detecting an empty database and inserting the default dataset.
- **AppDatabase**: The Room database class (`AppDatabase.kt`) that manages all local persistence for the application.
- **Menu_Item**: An entity in the `menu_items` table representing a top-level menu (e.g., "Tacos").
- **Category**: An entity in the `categories` table representing a grouping of products within a menu.
- **Product**: An entity in the `products` table representing a sellable item with a name, price, and emoji.
- **Customization_Group**: An entity in the `customization_groups` table representing a named group of options attached to a product (e.g., "Remover").
- **Customization_Option**: An entity in the `customization_options` table representing an individual option within a group (e.g., "Sin cilantro").
- **Seed_Transaction**: A single Room database transaction that inserts all seed data atomically.
- **Foreign_Key**: A referential integrity constraint between parent and child tables enforced by SQLite.

## Requirements

### Requirement 1: Trigger Condition

**User Story:** As a POS operator, I want the default menu to be automatically available the first time I open the app, so that I can start taking orders immediately without manual setup.

#### Acceptance Criteria

1. WHEN the AppDatabase instance is accessed and the `menu_items` table contains zero rows, THE Database_Seeder SHALL execute the seed operation.
2. WHEN the AppDatabase instance is accessed and the `menu_items` table contains one or more rows, THE Database_Seeder SHALL skip the seed operation without inserting, updating, or deleting any rows in any table.
3. THE Database_Seeder SHALL complete the emptiness check and, if required, the seed operation before the database instance is made available to any other application component for reading or writing.
4. IF the emptiness check query fails due to a database error, THEN THE Database_Seeder SHALL propagate the error to the caller without inserting any seed data.

### Requirement 2: Atomic Insertion

**User Story:** As a developer, I want all seed data inserted in a single transaction, so that a partial failure leaves the database in a clean empty state rather than a corrupted one.

#### Acceptance Criteria

1. THE Database_Seeder SHALL insert all seed entities within a single Room database transaction (Seed_Transaction).
2. IF any exception is thrown during the Seed_Transaction, THEN THE Database_Seeder SHALL roll back all inserted rows so that every seeded table (menu_items, categories, products, customization_groups, customization_options) contains zero rows after the rollback.
3. THE Seed_Transaction SHALL insert entities in foreign-key-safe order: Menu_Item first, then Category, then Product, then Customization_Group, then Customization_Option.
4. WHEN the Seed_Transaction completes without exception, THE Database_Seeder SHALL have inserted at least one row in each of the five seeded tables (menu_items, categories, products, customization_groups, customization_options).
5. IF the database already contains one or more rows in any seeded table, THEN THE Database_Seeder SHALL skip the Seed_Transaction and leave existing data unchanged.

### Requirement 3: Default Menu Item

**User Story:** As a POS operator, I want a pre-configured "Tacos" menu so that my most common products are ready to sell.

#### Acceptance Criteria

1. THE Database_Seeder SHALL insert exactly one Menu_Item with the name field set to "Tacos" and the emoji field set to "🌮".
2. THE Database_Seeder SHALL generate a deterministic UUID for the Menu_Item so that re-seeding after a database wipe produces the same identifier.
3. IF a Menu_Item with the same id already exists when the Database_Seeder executes, THEN THE Database_Seeder SHALL skip insertion without modifying the existing row.

### Requirement 4: Default Categories

**User Story:** As a POS operator, I want my products organized into categories so that I can find them quickly during order-taking.

#### Acceptance Criteria

1. THE Database_Seeder SHALL insert exactly four Category entities whose `associatedMenuId` matches the primary key of the "Tacos" Menu_Item.
2. THE Database_Seeder SHALL create a Category with the `name` field set to exactly "Tacos" and `associatedMenuId` referencing the "Tacos" Menu_Item.
3. THE Database_Seeder SHALL create a Category with the `name` field set to exactly "Tortas" and `associatedMenuId` referencing the "Tacos" Menu_Item.
4. THE Database_Seeder SHALL create a Category with the `name` field set to exactly "Tacos Dorados" and `associatedMenuId` referencing the "Tacos" Menu_Item.
5. THE Database_Seeder SHALL create a Category with the `name` field set to exactly "Refrescos" and `associatedMenuId` referencing the "Tacos" Menu_Item.
6. THE Database_Seeder SHALL generate a deterministic UUID for each Category `id` so that re-seeding after a database wipe produces the same identifiers.
7. THE Database_Seeder SHALL assign a unique `name` value to each of the four Category entities — no two categories SHALL share the same name within the "Tacos" Menu_Item.

### Requirement 5: Products in Category "Tacos"

**User Story:** As a POS operator, I want my taco products pre-loaded with correct prices so that I can charge customers accurately from the first order.

#### Acceptance Criteria

1. THE Database_Seeder SHALL insert exactly four Product entities whose `categoryId` references the "Tacos" Category.
2. THE Database_Seeder SHALL create a Product named "Taco de Bistec" with basePrice 16.0, emoji "🌮", and description "".
3. THE Database_Seeder SHALL create a Product named "Taco de Chorizo" with basePrice 16.0, emoji "🌮", and description "".
4. THE Database_Seeder SHALL create a Product named "Taco de Tripa" with basePrice 16.0, emoji "🌮", and description "".
5. THE Database_Seeder SHALL create a Product named "Taco de Costilla" with basePrice 18.0, emoji "🌮", and description "".
6. THE Database_Seeder SHALL set isActive to true for all four Products in the "Tacos" Category.
7. THE Database_Seeder SHALL assign a deterministic UUID as the `id` for each Product in the "Tacos" Category so that re-seeding after a database wipe produces the same identifiers.

### Requirement 6: Products in Category "Tortas"

**User Story:** As a POS operator, I want my torta products pre-loaded with correct prices so that I can serve customers without delay.

#### Acceptance Criteria

1. THE Database_Seeder SHALL insert exactly four Product entities whose `categoryId` references the "Tortas" Category.
2. THE Database_Seeder SHALL create a Product named "Torta de Bistec" with basePrice 40.0, emoji "🍔", and description "".
3. THE Database_Seeder SHALL create a Product named "Torta de Chorizo" with basePrice 40.0, emoji "🍔", and description "".
4. THE Database_Seeder SHALL create a Product named "Torta de Tripa" with basePrice 50.0, emoji "🍔", and description "".
5. THE Database_Seeder SHALL create a Product named "Torta de Costilla" with basePrice 50.0, emoji "🍔", and description "".
6. THE Database_Seeder SHALL set isActive to true for all four Products in the "Tortas" Category.
7. THE Database_Seeder SHALL assign a deterministic UUID as the `id` for each Product in the "Tortas" Category so that re-seeding after a database wipe produces the same identifiers.

### Requirement 7: Products in Category "Tacos Dorados"

**User Story:** As a POS operator, I want my tacos dorados products pre-loaded so that I can handle individual and bulk orders.

#### Acceptance Criteria

1. THE Database_Seeder SHALL insert exactly two Product entities whose `categoryId` references the "Tacos Dorados" Category.
2. THE Database_Seeder SHALL create a Product named "Taco Individual" with basePrice 10.0, emoji "🌮", and description "".
3. THE Database_Seeder SHALL create a Product named "Orden de 5" with basePrice 50.0, emoji "🌮", and description "".
4. THE Database_Seeder SHALL set isActive to true for all Products in the "Tacos Dorados" Category.
5. THE Database_Seeder SHALL assign a deterministic UUID as the `id` for each Product in the "Tacos Dorados" Category so that re-seeding after a database wipe produces the same identifiers.

### Requirement 8: Products in Category "Refrescos"

**User Story:** As a POS operator, I want my drink products pre-loaded so that I can add them to orders alongside food items.

#### Acceptance Criteria

1. THE Database_Seeder SHALL insert exactly two Product entities whose `categoryId` references the "Refrescos" Category.
2. THE Database_Seeder SHALL create a Product named "Refresco Pequeño" with basePrice 18.0, emoji "🥤", and description "".
3. THE Database_Seeder SHALL create a Product named "Refresco Grande" with basePrice 23.0, emoji "🥤", and description "".
4. THE Database_Seeder SHALL set isActive to true for all Products in the "Refrescos" Category.
5. THE Database_Seeder SHALL assign a deterministic UUID as the `id` for each Product in the "Refrescos" Category so that re-seeding after a database wipe produces the same identifiers.

### Requirement 9: Customizations for "Tacos" Products

**User Story:** As a POS operator, I want taco customization options pre-loaded so that I can accommodate customer preferences from the first order.

#### Acceptance Criteria

1. THE Database_Seeder SHALL create exactly 4 Customization_Group entities, one for each Product in the "Tacos" Category, each with groupName "Remover", selectionType "multiple_checkboxes", a unique UUID as id, and productId referencing the corresponding Product.
2. THE Database_Seeder SHALL create exactly 3 Customization_Option entities in each "Remover" group (12 total across all 4 Tacos products): "Sin cilantro" (extraPrice 0.0), "Sin cebolla" (extraPrice 0.0), and "Tortilla sin grasa" (extraPrice 0.0), each with a unique UUID as id and groupId referencing its parent Customization_Group.
3. THE Database_Seeder SHALL create the Customization_Group and Customization_Option entities for "Tacos" products only after the "Tacos" Category and its 4 Products have been persisted, ensuring all foreign key constraints (productId → products.id, groupId → customization_groups.id) are satisfied.

### Requirement 10: Customizations for "Tortas" Products

**User Story:** As a POS operator, I want torta customization options pre-loaded so that customers can remove ingredients they dislike.

#### Acceptance Criteria

1. THE Database_Seeder SHALL create exactly 4 Customization_Group entities, one for each Product in the "Tortas" Category, each with groupName "Remover", selectionType "multiple_checkboxes", a unique UUID as id, and productId referencing the corresponding Product.
2. THE Database_Seeder SHALL create exactly 5 Customization_Option entities in each "Remover" group (20 total across all 4 Tortas products): "Cilantro" (extraPrice 0.0), "Cebolla" (extraPrice 0.0), "Crema" (extraPrice 0.0), "Lechuga" (extraPrice 0.0), and "Jitomate" (extraPrice 0.0), each with a unique UUID as id and groupId referencing its parent Customization_Group.
3. THE Database_Seeder SHALL create the Customization_Group and Customization_Option entities for "Tortas" products only after the "Tortas" Category and its 4 Products have been persisted, ensuring all foreign key constraints are satisfied.

### Requirement 11: Customizations for "Tacos Dorados" Products

**User Story:** As a POS operator, I want tacos dorados customization options pre-loaded so that customers can modify their toppings.

#### Acceptance Criteria

1. THE Database_Seeder SHALL create exactly 2 Customization_Group entities (one per Product) named "Remover" with selectionType "multiple_checkboxes" for each of the 2 Products in the "Tacos Dorados" Category ("Taco Individual" and "Orden de 5").
2. THE Database_Seeder SHALL create exactly 4 Customization_Option entities in each "Remover" group (8 total across both groups): "Lechuga" (extraPrice 0.0), "Queso" (extraPrice 0.0), "Jitomate" (extraPrice 0.0), and "Crema" (extraPrice 0.0).

### Requirement 12: No Customizations for "Refrescos"

**User Story:** As a developer, I want the seeder to explicitly not create customizations for drinks, so that the POS UI remains clean for beverage products.

#### Acceptance Criteria

1. THE Database_Seeder SHALL create zero Customization_Group entities for the Product named "Refresco Pequeño" in the "Refrescos" Category.
2. THE Database_Seeder SHALL create zero Customization_Group entities for the Product named "Refresco Grande" in the "Refrescos" Category.
3. THE Database_Seeder SHALL create zero Customization_Option entities associated with any Product in the "Refrescos" Category.

### Requirement 13: Foreign Key Integrity

**User Story:** As a developer, I want all seeded entities to respect foreign key constraints, so that the database maintains referential integrity.

#### Acceptance Criteria

1. THE Database_Seeder SHALL insert entities in hierarchical order: MenuItemEntity first, then CategoryEntity, then ProductEntity, then CustomizationGroupEntity, and finally CustomizationOptionEntity, so that each parent row exists before its children are inserted.
2. THE Database_Seeder SHALL assign each CategoryEntity an `associatedMenuId` value that equals the `id` primary key of an already-inserted MenuItemEntity.
3. THE Database_Seeder SHALL assign each ProductEntity a `categoryId` value that equals the `id` primary key of an already-inserted CategoryEntity.
4. THE Database_Seeder SHALL assign each CustomizationGroupEntity a `productId` value that equals the `id` primary key of an already-inserted ProductEntity.
5. THE Database_Seeder SHALL assign each CustomizationOptionEntity a `groupId` value that equals the `id` primary key of an already-inserted CustomizationGroupEntity.
6. IF the Database_Seeder attempts to insert a child entity whose foreign key value does not match any existing parent primary key, THEN the database SHALL reject the insert by raising a foreign-key constraint error and the entity SHALL NOT be persisted.
7. THE Database_Seeder SHALL assign all primary key (`id`) and foreign key fields as non-empty strings in UUID format (36 characters, pattern xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx).

### Requirement 14: Idempotency

**User Story:** As a POS operator, I want the seeder to be safe to run multiple times without duplicating data, so that app restarts never corrupt my menu.

#### Acceptance Criteria

1. WHEN the Database_Seeder executes and the `menu_items` table contains one or more rows, THE Database_Seeder SHALL not insert, update, or delete any rows in the `menu_items`, `categories`, `products`, `customization_groups`, or `customization_options` tables.
2. WHEN the Database_Seeder completes successfully and is executed a second time, THE Database_Seeder SHALL leave the row count and content of all seeded tables (`menu_items`, `categories`, `products`, `customization_groups`, `customization_options`) identical to their state after the first successful execution.
3. IF a previous Seed_Transaction was rolled back due to an error, THEN THE Database_Seeder SHALL detect the `menu_items` table as empty and execute a new seed operation on the next execution.

### Requirement 15: Coroutine Safety

**User Story:** As a developer, I want the seeding operation to run on a background dispatcher, so that it never blocks the main thread or delays UI rendering.

#### Acceptance Criteria

1. THE Database_Seeder SHALL execute all database operations on Dispatchers.IO (or a coroutine context that dispatches to a background thread pool designated for IO-bound work).
2. THE Database_Seeder SHALL execute zero statements on the main thread during the seed operation, ensuring no frame rendering is delayed.
3. IF the Database_Seeder is triggered from a callback that executes on the main thread (e.g., RoomDatabase.Callback), THEN THE Database_Seeder SHALL switch to Dispatchers.IO via a suspend context switch before performing any database read or write.
