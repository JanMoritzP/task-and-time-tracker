# TaskAndTimeManager - Comprehensive Requirements Document

## Project Overview
A Kotlin/Jetpack Compose Android application that manages daily tasks, tracks app usage time, and implements app blocking based on purchased time limits. Users earn coins through task completion and can spend coins to purchase usage time for restricted apps.

**Package Name:** `com.example.taskandtimemanager`
**Min SDK:** 28
**Target SDK:** 35
**Compile SDK:** 35
**AGP Version:** 8.4.0
**Kotlin Compiler Extension Version:** 1.5.8

This application is intended for **personal / controlled deployment only** (e.g. side-loaded onto your own device). Using privileged permissions and Device Admin APIs is acceptable because the app will **not** be distributed via the Play Store.

---

## Core Architecture

### Technology Stack
- **Database:** Room (SQLite) with Kotlin Coroutines
- **UI Framework:** Jetpack Compose (Material3)
- **Navigation:** Bottom Navigation Bar (4 tabs)
- **Device Admin:** DevicePolicyManager for app blocking / hiding
- **Usage Tracking:** Android UsageStatsManager (and/or equivalent) for real app usage
- **State Management:** Free choice (ViewModel, rememberCoroutineScope, etc.)

### Architecture Policy
- ViewModels **are allowed** and may be used where they simplify state handling and lifecycle.
- Coroutines, Flows, and other modern Android components can be used as appropriate.
- The implementation should keep data layer, domain logic, and UI logic clearly separated.

### Project Structure
```text
src/main/java/com/example/taskandtimemanager/
├── MainActivity.kt (Entry point, 4-tab navigation)
├── AdminReceiver.kt (Device admin receiver)
├── AppBlockerService.kt (Background service for app blocking)
│
├── data/
│   ├── AppDatabase.kt (Room database)
│   ├── DataStore.kt (Data access layer - wrapper around DAOs and services)
│   ├── TaskDefinitionDao.kt
│   ├── TaskExecutionDao.kt
│   ├── RewardDefinitionDao.kt
│   ├── RewardRedemptionDao.kt
│   ├── TrackedAppDao.kt
│   ├── AppUsageAggregateDao.kt
│   ├── AppUsagePurchaseDao.kt
│   └── UsageTracker.kt (UsageStatsManager wrapper, automatic usage)
│
├── model/
│   ├── TaskDefinition.kt (@Entity)
│   ├── TaskExecution.kt (@Entity)
│   ├── RewardDefinition.kt (@Entity)
│   ├── RewardRedemption.kt (@Entity)
│   ├── TrackedApp.kt (@Entity)
│   ├── AppUsageAggregate.kt (@Entity)
│   ├── AppUsagePurchase.kt (@Entity)
│   ├── UsageRecord.kt (DTO/Serializable)
│   ├── DailyStats.kt (DTO/Serializable)
│   └── FavoriteApp.kt (DTO/Serializable)
│
└── ui/
    ├── DashboardScreen.kt (Home screen - quick task completion + summary)
    ├── TasksScreen.kt (Create/edit/delete task definitions, view per-day history)
    ├── CostsScreen.kt (Configure app costs and review remaining time)
    └── SettingsScreen.kt (Rewards + full export/import)

res/xml/
└── device_admin.xml (Device admin policy)
```

### File Organization Rule
- **One Kotlin class per file** is a **strict requirement**.
- Each `data class`, `class`, `interface`, etc. must live in its own file named after the type (e.g. `TaskDefinition.kt` for `TaskDefinition`).
- Existing combined-class files (e.g. `Model_Classes.kt`) are considered violations and must be refactored.

---

## Domain and Data Models

### 1. Task Domain

#### 1.1 TaskDefinition (@Entity tableName: "task_definition")
Represents the definition of a task (habit, repeated activity, or one-time task).

```kotlin
@PrimaryKey val id: String
val name: String
val category: String = ""
val mandatory: Boolean = false
val rewardCoins: Int = 0          // Coins per completion
val recurrenceType: String = "DAILY" // e.g. "DAILY", "ONE_TIME", "UNLIMITED_PER_DAY", "LIMITED_PER_DAY"
val maxExecutionsPerDay: Int? = null // Used when recurrenceType == "LIMITED_PER_DAY"
val archived: Boolean = false      // Hidden from default lists when true
```

**Notes / Behavior:**
- `TaskDefinition` is **not** directly completed; instead, completions are stored as `TaskExecution` records.
- `mandatory` indicates tasks that should be highlighted/flagged.
- Recurrence rules determine how many possible executions per day exist, but actual history is in `TaskExecution`.

#### 1.2 TaskExecution (@Entity tableName: "task_execution")
Represents a single occurrence (execution) of a task.

```kotlin
@PrimaryKey val id: String
val taskDefinitionId: String
val date: String                  // ISO-8601 local date, e.g. "2025-01-31"
val time: String? = null          // Optional local time, e.g. "08:30:00"
val status: String = "DONE"      // e.g. "DONE", "SKIPPED", "PENDING"
val coinsAwarded: Int = 0
```

**Notes / Behavior:**
- Multiple `TaskExecution` rows can exist per `TaskDefinition` and date (e.g. for unlimited or limited-per-day tasks).
- History views and statistics are based on `TaskExecution`, not on counters inside `TaskDefinition`.
- Coins are awarded per execution and contribute to the global coin balance.

#### 1.3 Task-related Derived Data (DTOs)
The app may define DTOs / view models for convenience, e.g. `DailyStats`, but these are **not** persisted as tables.

---

### 2. Reward Domain

#### 2.1 RewardDefinition (@Entity tableName: "reward_definition")
Represents a configurable one-time reward that can be redeemed with coins.

```kotlin
@PrimaryKey val id: String
val name: String
val description: String = ""
val coinCost: Int = 0
val archived: Boolean = false
```

#### 2.2 RewardRedemption (@Entity tableName: "reward_redemption")
Represents a single redemption of a reward.

```kotlin
@PrimaryKey val id: String
val rewardDefinitionId: String
val redemptionDateTime: String   // ISO-8601 local date-time string
val coinsSpent: Int
```

**Notes / Behavior:**
- Rewards can be redeemed multiple times; each redemption is recorded separately.
- Redeeming a reward reduces the global coin balance by `coinsSpent`.

---

### 3. App Usage Domain

#### 3.1 TrackedApp (@Entity tableName: "tracked_app")
Represents an app that is subject to time tracking and blocking.

```kotlin
@PrimaryKey val id: String
val name: String
val packageName: String
val costPerMinute: Float = 0f    // Coins per minute of usage
val purchasedMinutesTotal: Long = 0L
val isBlocked: Boolean = false
```

**Notes / Behavior:**
- Only apps present in this table are considered for limits and blocking.
- `purchasedMinutesTotal` is the aggregate of all purchases for this app (can also be derived from `AppUsagePurchase`).

#### 3.2 AppUsageAggregate (@Entity tableName: "app_usage_aggregate")
Represents automatic usage metrics for a given app in a given period (at minimum per-day).

```kotlin
@PrimaryKey val id: String
val appId: String                 // Refers to TrackedApp.id
val date: String                  // ISO-8601 local date (if using daily granularity)
val usedMinutesAutomatic: Long    // Derived from UsageStats; user cannot edit
```

**Notes / Behavior:**
- `usedMinutesAutomatic` is **always** derived by the `UsageTracker` service from actual device usage and must not be directly editable from the UI.
- Remaining time is computed using purchases vs. automatic usage.

#### 3.3 AppUsagePurchase (@Entity tableName: "app_usage_purchase")
Represents a purchase of time for a tracked app.

```kotlin
@PrimaryKey val id: String
val appId: String
val minutesPurchased: Long
val coinsSpent: Int
val purchaseDateTime: String      // ISO-8601 local date-time
```

**Notes / Behavior:**
- Each purchase is a separate record, used for history and for deriving `purchasedMinutesTotal`.
- Purchasing minutes reduces the global coin balance.

---

### 4. Statistics & Helper Models (DTOs / Non-Entities)

These models are not necessarily persisted as separate tables but are useful for history and dashboard views.

#### 4.1 UsageRecord

```kotlin
val appId: String
val appName: String
val date: String
val minutesUsed: Int
val timeRange: String // e.g. "09:30-10:45"
```

#### 4.2 DailyStats

```kotlin
val date: String
val coinsEarned: Int = 0
val tasksCompleted: Int = 0
val infiniteTasksCount: Int = 0
val timeSpentOnApps: Int = 0        // total minutes
val usage: List<UsageRecord> = emptyList()
val rewardsRedeemed: Int = 0
```

#### 4.3 FavoriteApp

```kotlin
val appId: String
val name: String
val purchaseCount: Int
```

---

## Screen Specifications

### 1. Dashboard Screen
**Purpose:** Quick overview, fast completion of tasks, and summary of usage and coins.

**Components:**
- Title: "📊 Dashboard".
- Stats cards:
  - "X/Y Tasks Done" (number of task definitions with at least one DONE execution today / total active task definitions).
  - "Z Executions" (total number of task executions today).
  - "Coins: C" (current coin balance derived from executions minus rewards and purchases).
- Pending Tasks List:
  - Shows tasks that are relevant for **today** (based on recurrence) and have no DONE execution yet.
  - Each task card displays:
    - Task name
    - Mandatory warning (if applicable)
    - Recurrence info (e.g. unlimited, limited N per day)
    - Coin reward amount per execution.
  - Quick Complete button (creates a `TaskExecution` with status DONE and rewards coins).

**Behavior:**
- Completing a task adds a `TaskExecution` for the current day and updates the coin balance.
- Dashboard statistics are recomputed from `TaskExecution`, `RewardRedemption`, and `AppUsagePurchase`.
- Infinite / repeatable tasks are shown as long as their recurrence allows more executions for today.

---

### 2. Tasks Screen
**Purpose:** Create, edit, and delete task definitions, and inspect basic execution history.

**Components:**
- "➕ Add Task" button.
- Task definitions grouped by category.
- Each task card:
  - Task name.
  - Category label.
  - Recurrence info (daily/one-time/unlimited/limited per day).
  - Mandatory warning (if applicable).
  - Basic stats (e.g. executions today, coins earned today).
  - "Edit" small button.
  - "Delete" button.
- Add/Edit Task dialog:
  - Name (text).
  - Category (text).
  - Mandatory (checkbox).
  - Reward coins per completion (number).
  - Recurrence selector (combo / radio): DAILY, ONE_TIME, UNLIMITED_PER_DAY, LIMITED_PER_DAY.
  - If LIMITED_PER_DAY: Max executions per day (number).

**Behavior:**
- Adding a task inserts a `TaskDefinition` row.
- Editing a task updates the relevant `TaskDefinition` fields (does not modify existing history).
- Deleting a task either:
  - Marks `archived = true` (preferred) and hides it by default, **or**
  - Hard-deletes the `TaskDefinition` and optionally its `TaskExecution` history (implementation choice, but must be clear and consistent).

---

### 3. Costs Screen (App Limits)
**Purpose:** Configure app coin costs, monitor purchased vs. used time, and initiate purchases.

**Components:**
- Title: "⚙️ App Limits".
- For each tracked app card:
  - App name.
  - Package name.
  - Stat boxes:
    - **Purchased:** Total minutes allocated (derived from all `AppUsagePurchase` for this app).
    - **Used:** Automatically tracked minutes (sum of `AppUsageAggregate.usedMinutesAutomatic` for a relevant period, typically today or recent days).
    - **Remaining:** Purchased - Used (red if ≤ 0).
  - Display-only fields for used time (no direct editing of used minutes).
  - Editable fields:
    - coins/min cost (text input mapped to `TrackedApp.costPerMinute`).
    - Purchase minutes (number input for a new purchase).
  - "Buy Time" button (creates a new `AppUsagePurchase` if enough coins).

**Behavior:**
- `usedMinutesAutomatic` is updated in the background via `UsageTracker` and the UI only displays it.
- When the user uses "Buy Time":
  - Calculate coin cost: `minutesToBuy * costPerMinute`.
  - If the global coin balance is sufficient:
    - Deduct coins.
    - Insert an `AppUsagePurchase` record with `minutesPurchased` and `coinsSpent`.
  - If insufficient coins: show an error.
- Remaining time is recomputed and turns red when ≤ 0.

---

### 4. Settings Screen
**Purpose:** Manage rewards and perform full export/import of state.

#### Tab 1: Rewards
- "➕ Add Reward" button.
- Two sections:
  - **Available:** List of reward definitions.
    - Reward card with name, description, coin cost.
    - "Redeem" button.
  - **History:** List of past redemptions (`RewardRedemption`).
    - Show reward name, coins spent, redemption date & time.

**Behavior:**
- Add Reward dialog:
  - Name (text).
  - Description (text).
  - Coin cost (number).
- Redeem Reward:
  - Check that the global coin balance is sufficient.
  - If so, create `RewardRedemption` with `coinsSpent = coinCost` and reduce coin balance.
  - If not, show an error.

#### Tab 2: Export / Import
- "📤 Export Data" button.
- "📥 Import Data" button.
- Status text area for last export/import result.

**Export Requirements:**
- Export **all** core state into a single file (e.g. JSON or CSV-based custom format):
  - Task definitions and all `TaskExecution` history.
  - Reward definitions and all `RewardRedemption` records.
  - Tracked apps, all `AppUsagePurchase` records, and all `AppUsageAggregate` records (or at least recent ones sufficient to reconstruct remaining time).
- The export **must** be sufficient to reconstruct the entire logical state: coins, tasks, rewards, and app limits.

**Import Requirements:**
- Import parses a previously exported file and restores the above entities into the database.
- Strategy for existing data can be either:
  - Clear existing tables then import, **or**
  - Merge by ID (upsert records).
- Import must handle errors gracefully and report success/failure to the user.

---

## Navigation

### Bottom Navigation Bar (4 Tabs)
```text
Tab 0: Dashboard (📊)  - DashboardScreen()
Tab 1: Tasks (📋)      - TasksScreen()
Tab 2: Costs (⚙️)      - CostsScreen()
Tab 3: Settings (⚙️)   - SettingsScreen()
```

---

## Core Functionality

### Usage Tracking (Real App Usage)
**Class:** `UsageTracker`

Responsibilities:
- Wrap `UsageStatsManager` to obtain real foreground usage times per package.
- Periodically (or on demand) update `AppUsageAggregate.usedMinutesAutomatic` for all tracked apps.

Required methods (conceptual):

```kotlin
fun getDailyUsageMinutes(packageName: String, date: LocalDate): Long
fun getWeeklyUsageMinutes(packageName: String, endDateInclusive: LocalDate): Long
fun getAllAppsDailyUsageMinutes(date: LocalDate): Map<String, Long>
```

Permissions:
```xml
<uses-permission android:name="android.permission.PACKAGE_USAGE_STATS" />
```

The UI and data layer **must not** allow manual editing of `usedMinutesAutomatic`; it is always derived from `UsageTracker`.

---

### App Blocking Service
**Class:** `AppBlockerService` (extends Service)

#### Blocking Logic (High Level)
1. Service periodically checks foreground app and remaining time for that app.
2. For the current foreground app, if it is in `TrackedApp`:
   - Compute remaining minutes: `totalPurchasedMinutes - totalUsedMinutesAutomatic`.
   - If `remainingMinutes <= 0`:
     - Show a **blocking dialog** when the app is opened:
       - Message that no time is left.
       - Display current coin balance.
       - Allow user to **buy more minutes** if there are enough coins.
     - If user buys:
       - Deduct coins.
       - Insert `AppUsagePurchase` for purchased minutes.
       - Allow app to continue (do not hide).
     - If user cancels or has insufficient coins:
       - Block/close the app using `DevicePolicyManager.setApplicationHidden()` or an equivalent mechanism.

#### Implementation Notes
- Service may run every ~60 seconds, or use more precise triggers when possible.
- Requires Device Admin setup and permission:

```xml
<uses-permission android:name="android.permission.BIND_DEVICE_ADMIN" />
```

- AdminReceiver handles device admin lifecycle and is registered via `device_admin.xml`.

---

### Data Access Layer (DataStore)

**Responsibilities:** Wrapper around Room DAOs and services, exposing a coherent API to UI and background services.

#### Task Operations
- `getTaskDefinitions(): List<TaskDefinition>`
- `addTaskDefinition(task: TaskDefinition)`
- `updateTaskDefinition(task: TaskDefinition)`
- `archiveTaskDefinition(taskId: String)`
- `getExecutionsForDate(date: LocalDate): List<TaskExecution>`
- `addTaskExecution(execution: TaskExecution)`

#### Reward Operations
- `getRewardDefinitions(): List<RewardDefinition>`
- `addRewardDefinition(reward: RewardDefinition)`
- `redeemReward(rewardId: String)` (creates a `RewardRedemption` if enough coins).
- `getRewardRedemptions(): List<RewardRedemption>`

#### App Operations
- `getTrackedApps(): List<TrackedApp>`
- `addTrackedApp(app: TrackedApp)`
- `updateTrackedApp(app: TrackedApp)`
- `getAppUsageAggregatesForDate(date: LocalDate): List<AppUsageAggregate>`
- `addOrUpdateAppUsageAggregate(aggregate: AppUsageAggregate)`
- `addAppUsagePurchase(purchase: AppUsagePurchase)`
- `getAppUsagePurchases(appId: String): List<AppUsagePurchase>`

#### Coins / Balance
- `getCoinBalance(): Int` (derived from executions, redemptions, and purchases).
- `recalculateCoinBalance()` (recompute from history).

#### Export / Import
- `exportState(): String` – returns a serialized representation (e.g. JSON) of all core entities:
  - TaskDefinition, TaskExecution, RewardDefinition, RewardRedemption, TrackedApp, AppUsageAggregate, AppUsagePurchase.
- `importState(serialized: String)` – parses and restores the above entities.

Implementation details for the serialization format are flexible, but must support full round-trip of the complete state.

---

## Room Database Setup

**Database Class:** `AppDatabase`

```kotlin
@Database(
    entities = [
        TaskDefinition::class,
        TaskExecution::class,
        RewardDefinition::class,
        RewardRedemption::class,
        TrackedApp::class,
        AppUsageAggregate::class,
        AppUsagePurchase::class
    ],
    version = 1
)
abstract class AppDatabase : RoomDatabase()
```

**DAOs:**
- `TaskDefinitionDao`, `TaskExecutionDao`
- `RewardDefinitionDao`, `RewardRedemptionDao`
- `TrackedAppDao`, `AppUsageAggregateDao`, `AppUsagePurchaseDao`

Each DAO supports at least: insert (replace on conflict), update, delete, `getAll()` or appropriate read queries.

Build dependencies follow the existing Room configuration.

---

## Build Configuration

### build.gradle.kts (App Level)

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    kotlin("plugin.serialization")
    id("kotlin-kapt")
}

android {
    namespace = "com.example.taskandtimemanager"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.taskandtimemanager"
        minSdk = 28
        targetSdk = 35
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
}

dependencies {
    // Compose, Material3, Room, Serialization, Testing...
}
```

---

## AndroidManifest.xml Requirements

### Permissions

```xml
<uses-permission android:name="android.permission.PACKAGE_USAGE_STATS" />
<uses-permission android:name="android.permission.BIND_DEVICE_ADMIN" />
<!-- Other permissions as needed for foreground service / overlays, etc. -->
```

### Application Components

```xml
<application>
    <activity android:name=".MainActivity" android:exported="true">
        <intent-filter>
            <action android:name="android.intent.action.MAIN" />
            <category android:name="android.intent.category.LAUNCHER" />
        </intent-filter>
    </activity>

    <service android:name=".AppBlockerService" android:enabled="true" android:exported="false" />

    <receiver android:name=".AdminReceiver" android:permission="android.permission.BIND_DEVICE_ADMIN">
        <intent-filter>
            <action android:name="android.app.action.DEVICE_ADMIN_ENABLED" />
            <action android:name="android.app.action.DEVICE_ADMIN_DISABLE_REQUESTED" />
            <action android:name="android.app.action.DEVICE_ADMIN_DISABLED" />
        </intent-filter>
        <meta-data android:name="android.app.device_admin" android:resource="@xml/device_admin" />
    </receiver>
</application>
```

---

## Key Implementation Details

### State Management
- ViewModels may be used per screen, or state can be managed via `rememberCoroutineScope` and Compose state.
- Prefer lifecycle-aware scopes (e.g. `viewModelScope` or `lifecycleScope`) for long-running operations.

### Error Handling
- Use try-catch blocks in background services and data layer where needed.
- Provide graceful fallbacks when permissions are missing or denied.
- Avoid hard crashes; surface user-friendly error messages when appropriate.

### Data Persistence
- All data is stored in Room database tables defined above.
- Export/import is the mechanism to survive app reinstalls and device changes.
- Database starts at version 1 (no migrations required initially).

### UI/UX
- Material3 color scheme (lightColorScheme()).
- Proper spacing (16dp padding, 8dp gaps).
- Responsive layouts with `Modifier.weight` where appropriate.
- Real-time updates via state changes and/or Flows.
- Visual feedback (color changes for warnings, remaining time, etc.).

---

## Future Enhancements (Not Required)
- Dark mode support.
- Cloud sync (e.g. Firebase) for export/import instead of local files.
- App usage graph visualization.
- Time-based blocking schedules.
- Multiple user profiles.
- Push notifications for limits.
- Advanced analytics dashboard.

---

## Notes
- Package name is case-sensitive: `com.example.taskandtimemanager`.
- **One Kotlin class per file** is mandatory for this project.
- ViewModels are allowed; architecture must stay simple and maintainable.
- Export/import replaces the previous simple CSV export; if CSV is still used, it must represent the full state described above.
