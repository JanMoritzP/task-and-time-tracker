# TaskAndTimeManager - Comprehensive Requirements Document

## Project Overview
A Kotlin/Jetpack Compose Android application that manages daily tasks, tracks app usage time, and implements app blocking based on purchased time limits. Users earn coins through task completion and can spend coins to purchase usage time for restricted apps.

**Package Name:** `com.example.taskandtimemanager`
**Min SDK:** 28
**Target SDK:** 35
**Compile SDK:** 35
**AGP Version:** 8.4.0
**Kotlin Compiler Extension Version:** 1.5.8

---

## Core Architecture

### Technology Stack
- **Database:** Room (SQLite) with Kotlin Coroutines
- **UI Framework:** Jetpack Compose (Material3)
- **Navigation:** Bottom Navigation Bar (4 tabs)
- **Device Admin:** DevicePolicyManager for app blocking/hiding
- **Usage Tracking:** Android UsageStatsManager for real app usage
- **State Management:** ViewModel-independent scope-based coroutines

### Project Structure
```
src/main/java/com/example/taskandtimemanager/
├── MainActivity.kt (Entry point, 4-tab navigation)
├── AdminReceiver.kt (Device admin receiver)
├── AppBlockerService.kt (Background service for app blocking)
│
├── data/
│   ├── AppDatabase.kt (Room database)
│   ├── DataStore.kt (Data access layer - wrapper around DAOs)
│   ├── TaskDao.kt
│   ├── RewardDao.kt
│   ├── AppDataDao.kt
│   └── UsageTracker.kt (UsageStatsManager wrapper)
│
├── model/
│   ├── Task.kt (@Entity)
│   ├── OneTimeReward.kt (@Entity)
│   └── AppData.kt (@Entity)
│
└── ui/
    ├── DashboardScreen.kt (Home screen - quick task completion)
    ├── TasksScreen.kt (Create/edit/delete tasks)
    ├── CostsScreen.kt (Set app time limits)
    └── SettingsScreen.kt (Rewards + CSV export)

res/xml/
└── device_admin.xml (Device admin policy)
```

---

## Data Models

### Task (@Entity tableName: "task")
```kotlin
@PrimaryKey val id: String
val name: String
val category: String = ""
val completed: Boolean = false
val mandatory: Boolean = false
val reward: Int = 0 // Coins earned on completion
val infinite: Boolean = false // Repeatable task?
val infiniteName: String = "" // Name for each repeat
val infiniteReward: Int = 0 // Coins per repeat
val infiniteCount: Int = 0 // Number of times completed
```

**Features:**
- One-time completion tasks with fixed coin rewards
- Infinite/repeatable tasks with per-repeat rewards
- Mandatory tasks (marked with warning)
- Task categories for organization
- Track completion status

---

### OneTimeReward (@Entity tableName: "one_time_reward")
```kotlin
@PrimaryKey val id: String
val name: String = ""
val description: String = ""
val coins: Int = 0
val claimedDate: String = ""
val claimedTime: String = ""
```

**Features:**
- Create custom rewards to claim
- Track claimed vs unclaimed rewards
- Store claim timestamp

---

### AppData (@Entity tableName: "app_data")
```kotlin
@PrimaryKey val id: String
val name: String
val packageName: String
val costPerMinute: Float = 0f
val purchasedMinutes: Long = 0L // Total minutes allocated
val usedMinutes: Long = 0L // Minutes consumed
val isBlocked: Boolean = false // Current block status
```

**Features:**
- Track purchased vs used time per app
- Automatic calculation of remaining time
- Real-time block status tracking
- Support for cost/value conversions

---

## Screen Specifications

### 1. Dashboard Screen
**Purpose:** Quick overview and fast task completion

**Components:**
- Title: "📊 Dashboard"
- Stats Cards:
  - "X/Y Tasks Done" (completed out of total)
  - "Z Infinite Done" (total repeats completed)
- Pending Tasks List:
  - Show only non-completed tasks
  - Each task card displays:
    - Task name
    - Mandatory warning (if applicable)
    - Repeat count (if infinite)
    - Coin reward amount
  - Quick Complete/Increment button
- Auto-refresh on task state changes

**Functionality:**
- Tap "Complete" on one-time tasks → marks completed, updates dashboard
- Tap "✓" on infinite tasks → increments count, stays in list
- Real-time stat updates

---

### 2. Tasks Screen
**Purpose:** Create, edit, and delete tasks

**Components:**
- "➕ Add Task" button
- Tasks grouped by category
- Each task card:
  - Task name
  - Category label
  - Repeat count (if infinite)
  - Mandatory warning (if applicable)
  - "Edit" button (small)
  - "Delete" button (red)

**Functionality:**
- Add new task dialog:
  - Name (text)
  - Category (text)
  - Mandatory (checkbox)
  - Reward coins (number, hidden if infinite)
  - Infinite (checkbox, toggles reward fields)
  - If infinite:
    - Per-repeat name
    - Per-repeat reward
- Edit task dialog:
  - Same as add dialog
  - Pre-populated with current values
  - Preserves completion status and infinite count
- Delete task:
  - Removes from database
- GroupBy category for visual organization

---

### 3. Costs Screen (App Limits)
**Purpose:** Manage app usage time purchases and tracking

**Components:**
- Title: "⚙️ App Limits"
- For each app card:
  - App name
  - Three stat boxes:
    - **Purchased:** Total minutes allocated
    - **Used:** Minutes consumed (from AppData.usedMinutes)
    - **Remaining:** Purchased - Used (red if ≤ 0)
  - Edit fields:
    - Purchase (minutes) text input
    - Used (minutes) text input
    - Coins/min cost text input
  - "Save" button

**Functionality:**
- Set how many minutes to allocate to each app
- Track consumed minutes manually
- View remaining time at a glance
- Remaining time turns red when ≤ 0 (warning)
- Changes persist to Room database

---

### 4. Settings Screen
**Purpose:** Manage rewards and export data

**Tab 1: Rewards**
- "➕ Add Reward" button
- Two sections:
  - **Unclaimed:** List of unclaimed rewards
    - Reward card with name, description, coins
    - "Claim" button
  - **Claimed:** List of claimed rewards
    - Show claim date & time
    - Read-only display
- Add Reward dialog:
  - Name (text)
  - Description (text)
  - Coins (number)
  - "Add" / "Cancel" buttons

**Tab 2: Export**
- "📊 Export to CSV" button
- Generates CSV with:
  - Type (Task/Reward)
  - Name
  - Status (Done/Pending/Claimed)
  - Coins
  - Date
- Saves to: `app_files_dir/TaskTimeManager_YYYY-MM-DD.csv`
- Status message: "Data exported to app files"

---

## Navigation

### Bottom Navigation Bar (4 Tabs)
```
Tab 0: Dashboard (📊)  - DashboardScreen()
Tab 1: Tasks (📋)      - TasksScreen()
Tab 2: Costs (⚙️)      - CostsScreen()
Tab 3: Settings (⚙️)   - SettingsScreen()
```

---

## Core Functionality

### Usage Tracking (Real App Usage)
**Class:** `UsageTracker`
- **Method:** `getDailyUsageTime(packageName: String): Long`
  - Returns milliseconds of foreground time in last 24h
  - Uses `UsageStatsManager.INTERVAL_DAILY`
  
- **Method:** `getWeeklyUsageTime(packageName: String): Long`
  - Returns milliseconds of foreground time in last 7 days
  - Uses `UsageStatsManager.INTERVAL_WEEKLY`
  
- **Method:** `getAllAppsUsage(): Map<String, Long>`
  - Returns map of all apps → usage time in ms

**Permission Required:**
```xml
<uses-permission android:name="android.permission.PACKAGE_USAGE_STATS" />
```

---

### App Blocking Service
**Class:** `AppBlockerService` (extends Service)

**Logic:**
1. Runs every 60 seconds (60000ms)
2. For each registered app:
   - Get remaining time: `purchasedMinutes - usedMinutes`
   - If `remainingMinutes <= 0`:
     - Block app using `DevicePolicyManager.setApplicationHidden()`
     - Update `AppData.isBlocked = true` in database
   - Else:
     - Unblock app
     - Update `AppData.isBlocked = false`
3. Continues scheduling checks every 60s

**Setup:**
- AndroidManifest.xml:
  ```xml
  <service android:name=".AppBlockerService" android:enabled="true" android:exported="false" />
  ```
- Requires Device Admin permission setup
- AdminReceiver handles device admin lifecycle

---

### Device Admin Setup
**Class:** `AdminReceiver` (extends DeviceAdminReceiver)

**Methods:**
- `onEnabled()` - Called when admin is enabled
- `onDisabled()` - Called when admin is disabled

**Manifest Registration:**
```xml
<receiver android:name=".AdminReceiver" android:permission="android.permission.BIND_DEVICE_ADMIN">
    <intent-filter>
        <action android:name="android.app.action.DEVICE_ADMIN_ENABLED" />
        <action android:name="android.app.action.DEVICE_ADMIN_DISABLE_REQUESTED" />
        <action android:name="android.app.action.DEVICE_ADMIN_DISABLED" />
    </intent-filter>
    <meta-data android:name="android.app.device_admin" android:resource="@xml/device_admin" />
</receiver>
```

**res/xml/device_admin.xml:**
```xml
<?xml version="1.0" encoding="utf-8"?>
<device-admin xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-policies>
        <hide-application />
    </uses-policies>
</device-admin>
```

---

## Data Access Layer (DataStore)

**Responsibilities:** Wrapper around Room DAOs to provide clean API

### Task Operations
- `getTasks(): List<Task>` - All tasks
- `addTask(task: Task)` - Insert new task
- `updateTask(task: Task)` - Update existing task
- `completeTask(taskId: String, reward: Int)` - Mark complete
- `incrementInfiniteTask(taskId: String)` - Increment repeat counter
- `deleteTask(taskId: String)` - Remove task

### Reward Operations
- `getOneTimeRewards(): List<OneTimeReward>` - All rewards
- `addOneTimeReward(reward: OneTimeReward)` - Create reward
- `claimOneTimeReward(rewardId: String)` - Mark as claimed with timestamp

### App Operations
- `getApps(): List<AppData>` - All apps
- `addApp(app: AppData)` - Register new app
- `updateApp(app: AppData)` - Update app limits/status
- `setApps(apps: List<AppData>)` - Bulk insert
- `getAppTime(appId: String): Long` - Get time spent

### Export
- `exportToCSV(): String` - Generate CSV data

---

## Room Database Setup

**Database Class:** `AppDatabase`
```kotlin
@Database(entities = [Task::class, OneTimeReward::class, AppData::class], version = 1)
abstract class AppDatabase : RoomDatabase()
```

**DAOs:** TaskDao, RewardDao, AppDataDao
- All support: insert (replace on conflict), update, delete, getAll()

**Build Dependencies:**
```gradle
implementation("androidx.room:room-runtime:2.5.2")
implementation("androidx.room:room-ktx:2.5.2")
kapt("androidx.room:room-compiler:2.5.2")
```

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
<uses-permission android:name="android.permission.MANAGE_APP_TOKENS" />
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
        <!-- ... intent-filter and meta-data as shown above -->
    </receiver>
</application>
```

---

## Key Implementation Details

### State Management
- No ViewModel used initially
- Use `rememberCoroutineScope()` for launches
- `remember { mutableStateOf() }` for local Compose state
- `LaunchedEffect(Unit)` for data loading
- Scope-based coroutines with `Dispatchers.Main`

### Error Handling
- Try-catch blocks in background services
- Graceful fallbacks for optional operations
- No hard crashes on permission denials

### Data Persistence
- All data survives app reinstalls (Room database)
- Database stored in app's private directory
- Version 1 database (no migrations defined yet)

### UI/UX
- Material3 color scheme (lightColorScheme())
- Proper spacing (16dp padding, 8dp gaps)
- Responsive layouts (weight modifiers for flexible sizing)
- Real-time updates via state changes
- Visual feedback (color changes for warnings)

---

## Future Enhancements (Not Required)
- Dark mode support
- Cloud sync (Firebase)
- App usage graph visualization
- Time-based blocking schedules
- Multiple user profiles
- Push notifications for limits
- Advanced analytics dashboard

---

## Testing Checklist

- [ ] App compiles and runs without crashes
- [ ] Room database creates successfully
- [ ] Tasks can be added/edited/deleted
- [ ] Task completion updates dashboard stats
- [ ] Infinite tasks increment properly
- [ ] Rewards can be created and claimed
- [ ] App limits can be set and saved
- [ ] UsageTracker reads real app usage (requires usage stats permission)
- [ ] AppBlockerService blocks apps when limit reached
- [ ] CSV export generates correct file
- [ ] Navigation between 4 tabs works smoothly
- [ ] Data persists across app restart
- [ ] Device admin is properly set up and functional

---

## Notes
- Package name is case-sensitive: `com.example.taskandtimemanager`
- All files should be individual Kotlin files in respective directories
- No shared ViewModel across screens
- Coroutines should use `scope.launch { }` from MainActivity's rememberCoroutineScope()
- Each screen is a @Composable function taking DataStore and scope as parameters
