# TaskAndTimeManager – Refactor Prompt Script

Use these prompts one by one. Each prompt tells the assistant exactly what to do next on this codebase.

---

## 1. Data Model & Database

**Prompt 1 – Redesign Room entities and DAOs**

> You are Roo‑Code‑Assistant working in this repo. Using app/requirements.md as the source of truth, replace the old entities Task, OneTimeReward, and AppData with the new Room entities TaskDefinition, TaskExecution, RewardDefinition, RewardRedemption, TrackedApp, AppUsageAggregate, and AppUsagePurchase. Create one Kotlin class per file under app/src/main/java/com/example/taskandtimemanager/model. Update AppDatabase and create/adjust DAOs (TaskDefinitionDao, TaskExecutionDao, RewardDefinitionDao, RewardRedemptionDao, TrackedAppDao, AppUsageAggregateDao, AppUsagePurchaseDao). Keep the project compiling as much as possible, but it is fine to add TODOs where UI or services must be updated later.

**Prompt 2 – Remove legacy models and fix imports**

> In this repo, delete or fully migrate any usage of the legacy Task, OneTimeReward, AppData, and Model_Classes.kt so that only the new entities from app/requirements.md are used. Update all imports and Room annotations to match the new model. Do not yet change UI logic significantly; focus on making the data layer internally consistent and buildable.

---

## 2. DataStore, Coins, and Export/Import

**Prompt 3 – Redesign DataStore API and coin balance logic**

> In this repo, redesign DataStore in app/src/main/java/com/example/taskandtimemanager/data/DataStore.kt to work with the new entities and operations defined in app/requirements.md. Implement APIs for TaskDefinition/TaskExecution, RewardDefinition/RewardRedemption, TrackedApp/AppUsageAggregate/AppUsagePurchase, and coin balance computation (coins from task executions minus coins spent on rewards and app time). Add any helper methods you need, and keep one Kotlin class per file by splitting DataStore if it becomes too large (e.g. use separate Manager classes).

**Prompt 4 – Implement full export/import of state**

> In this repo, implement full export/import in the data layer as described in app/requirements.md. Add functions like exportState() and importState(serialized: String) that serialize and restore all core entities (TaskDefinition, TaskExecution, RewardDefinition, RewardRedemption, TrackedApp, AppUsageAggregate, AppUsagePurchase), using a single JSON-based format. Wire these methods into DataStore (or a dedicated ExportImportManager) and ensure they can be called from the UI.

---

## 3. Usage Tracking and App Blocking

**Prompt 5 – Implement UsageTracker and automatic used minutes**

> In this repo, create a UsageTracker class under app/src/main/java/com/example/taskandtimemanager/data that wraps UsageStatsManager as described in app/requirements.md. Implement the methods getDailyUsageMinutes, getWeeklyUsageMinutes, and getAllAppsDailyUsageMinutes, and add logic to periodically update AppUsageAggregate.usedMinutesAutomatic for all TrackedApp entries. Ensure the UI can only read these values, not edit them.

**Prompt 6 – Rewrite AppBlockerService to match requirements**

> In this repo, rewrite AppBlockerService.kt to match the blocking flow described in app/requirements.md: check only TrackedApp entries, compute remaining time from AppUsagePurchase and AppUsageAggregate, and when remainingMinutes <= 0 for the current foreground app, show a blocking dialog that allows buying more minutes with coins or, if the user cancels or has insufficient coins, closes/blocks the app via DevicePolicyManager.setApplicationHidden or an equivalent mechanism. Assume this is a personal, device-admin-enabled app and document any platform limitations with comments.

---

## 4. UI Screens (Dashboard, Tasks, Costs, Settings)

**Prompt 7 – Update DashboardScreen to new model**

> In this repo, refactor DashboardScreen.kt to use TaskDefinition and TaskExecution as specified in app/requirements.md. Show today’s relevant tasks based on recurrence, compute X/Y tasks done and total executions from TaskExecution, and display the current coin balance. Replace the old Task.completed/infinite logic with the new model, creating TaskExecution records on completion.

**Prompt 8 – Update TasksScreen to manage definitions and history**

> In this repo, refactor TasksScreen.kt to manage TaskDefinition records (create/edit/archive/delete) and to display basic execution stats per task (e.g. executions today, coins earned). Update the add/edit dialog to use the new TaskDefinition fields (recurrenceType, maxExecutionsPerDay, mandatory, rewardCoins). Remove any UI that depends on the old Task.infinite/infiniteCount fields.

**Prompt 9 – Update CostsScreen for tracked apps and automatic usage**

> In this repo, refactor CostsScreen.kt to use TrackedApp, AppUsageAggregate, and AppUsagePurchase. Remove any direct editing of usedMinutes/timeSpent; instead, show purchased, used (automatic), and remaining minutes as derived from the data layer. Allow editing costPerMinute and buying additional minutes via a Buy Time flow that deducts coins and creates AppUsagePurchase records.

**Prompt 10 – Update SettingsScreen for rewards and export/import**

> In this repo, refactor SettingsScreen.kt to use RewardDefinition and RewardRedemption as described in app/requirements.md. Implement a Rewards tab where the user can create reward definitions and redeem them (creating RewardRedemption and updating coin balance). Replace the old CSV-only export with a full Export/Import tab that calls DataStore (or ExportImportManager) exportState() and importState(), saving/loading a single JSON file in app-specific storage.

---

## 5. Cleanup and Validation

**Prompt 11 – Enforce one-class-per-file and remove dead code**

> In this repo, scan all Kotlin files and ensure the "one Kotlin class per file" rule is fully enforced. Split any file that contains multiple top-level classes or data classes into separate files named after each class. Remove obsolete or unused code paths that reference the old model or CSV export, replacing them with the new structures from app/requirements.md.

**Prompt 12 – Compile and fix build-time issues**

> In this repo, run a build (Gradle) and fix all compile-time errors arising from the refactor. Adjust imports, types, and function signatures so the project compiles successfully with the new data model, DataStore, services, and UI. Where behavior is not yet fully wired, use TODO comments but keep types consistent.

---

## 6. UI & UX Cleanup – Neomorphism-Inspired Design

**Prompt 13 – Define global visual language and theme**

> In this repo, redesign the Compose Material 3 theme (colors, typography, shapes) to create a clean, low-contrast, neomorphism-inspired visual language: soft background (light grey or desaturated dark), subtle dual shadows for main surfaces, rounded corners (12–20 dp), and minimal emoji usage. Update app/src/main/java/com/example/taskandtimemanager/ui/theme/Color.kt and Theme.kt to use a small, coherent palette (primary, secondary, accent, success/error) and ensure all screens rely on theme colors instead of hard-coded values.

**Prompt 14 – Unify layout structure and spacing**

> In this repo, refactor all major screens (DashboardScreen.kt, TasksScreen.kt, CostsScreen.kt, SettingsScreen.kt) to use a consistent layout scaffold: top title, optional subtitle, main card-like surfaces with uniform padding (16–24 dp), and consistent vertical spacing (8–12 dp). Remove unnecessary emojis from titles, keep copy short and neutral, and make sure all primary actions are visually aligned and use the same button style.

**Prompt 15 – Apply neomorphism-inspired components**

> In this repo, introduce a small set of reusable neomorphism-inspired components under app/src/main/java/com/example/taskandtimemanager/ui (e.g., NeoCard, NeoButton, NeoToggle). Each component should simulate soft extruded surfaces using light and dark shadow colors from the theme, avoid heavy borders, and respect system dark mode where possible. Replace existing Card/Button usages on DashboardScreen, TasksScreen, CostsScreen, and SettingsScreen with these components where it makes sense, without changing the underlying behavior.

**Prompt 16 – Improve readability and focus states**

> In this repo, audit all text and interactive elements for readability and focus/pressed feedback. Ensure text uses a limited set of typography styles (e.g., headline, title, body, label) with sufficient contrast over the new background, and that buttons, inputs, and toggles have clear pressed/selected states (slightly stronger shadows, subtle color shifts) instead of emojis. Update OutlinedTextField and other inputs to match the neomorphism style while keeping accessibility (font size, minimum touch targets) in mind.

**Prompt 17 – Simplify blocking and overlay UI**

> In this repo, restyle BlockingOverlayActivity and the time-limit/mandatory overlay content to match the new neomorphism-inspired design: a single central card, concise message text, and two clear actions (e.g., "Buy more time" and "Close" or, when mandatory tasks are unfinished, only "Close"). Remove emojis from these screens, use theme colors and Neo components, and ensure the mandatory-task message is visually distinct from the normal time-limit message but still consistent with the overall UI.

**Prompt 18 – Polish microcopy and navigation**

> In this repo, go through all visible strings in the UI and adjust the wording for clarity and consistency. Prefer short, descriptive labels (e.g., "Tasks", "Tracked apps", "Rewards", "Export & Import"), avoid emojis in headings, and ensure error/empty states are expressed in simple sentences. Align bottom navigation/tab labels and top-level titles so the app feels coherent from a UX perspective while keeping the neomorphism-inspired visual style.

---

## 7. Weekly coin reset and daily purchase reset (WorkManager)

**Prompt 19 – Add persistent settings for last reset timestamps**

> In this repo, add a small persistence mechanism (e.g., DataStore Preferences or a tiny Room table) to store two timestamps: `lastWeeklyCoinResetDate` and `lastDailyPurchaseResetDate`. Place the code in a dedicated class (for example, LastResetStorage) under app/src/main/java/com/example/taskandtimemanager/data, respecting the one-class-per-file rule. Expose suspend functions to read and update these values so they can be used from workers and the DataStore facade.

**Prompt 20 – Introduce a PeriodicResetWorker with WorkManager**

> In this repo, add a dependency on AndroidX WorkManager in app/build.gradle.kts and implement a PeriodicResetWorker class under app/src/main/java/com/example/taskandtimemanager/data that runs once per day (e.g., using PeriodicWorkRequestBuilder<PeriodicResetWorker>(1, TimeUnit.DAYS)). In doWork(), obtain a DataStore instance and LastResetStorage, then delegate to a helper method that performs: (a) daily cleanup of purchased app time, and (b) weekly coin reset if it is Sunday and coins have not yet been reset this week.

**Prompt 21 – Implement daily reset of purchased app time**

> In this repo, extend AppUsageManager and AppUsagePurchaseDao so that there is a clear operation for clearing all AppUsagePurchase records (e.g., a suspend function clearAllPurchasesTodayOrAll()). Wire this into PeriodicResetWorker so that once per night (every worker run) all purchased minutes are wiped by deleting AppUsagePurchase entries, then recompute any cached values if needed. Ensure DashboardScreen and CostsScreen derive remaining minutes only from the current AppUsagePurchase records so the reset is immediately visible after the next data fetch.

**Prompt 22 – Implement weekly coin reset every Sunday night**

> In this repo, introduce a coin-reset mechanism that is triggered from PeriodicResetWorker: if today is Sunday and lastWeeklyCoinResetDate is before this Sunday, then reset the effective coin balance to 0. Since CoinManager currently derives coins from immutable history, implement this by inserting synthetic neutralizing records, for example by (1) summing the current balance via CoinManager.getCoinBalance(), and (2) creating a negative-compensation RewardRedemption or synthetic TaskExecution that cancels out the current balance. Persist the reset date via LastResetStorage so the reset only happens once per Sunday.

**Prompt 23 – Register and initialize WorkManager scheduling**

> In this repo, wire the PeriodicResetWorker into the app startup. In MainActivity.kt or a small Application subclass (e.g., TaskAndTimeManagerApp), enqueue a unique periodic work request that runs once per day, with reasonable constraints (e.g., requiresBatteryNotLow = true is optional). Make sure multiple enqueues do not create duplicate workers by using enqueueUniquePeriodicWork with KEEP or UPDATE policy.

**Prompt 24 – Add tests and documentation for reset behaviour**

> In this repo, add unit tests (where feasible) around CoinManager / reset helper logic to verify that the weekly reset inserts correct neutralizing records and results in a zero coin balance after reset. Also, document the weekly coin reset and daily app-time reset behaviour in app/requirements.md or a short README section so the behaviour is explicit for future changes.

---

Use each prompt separately in order. After finishing Prompt 24, the project should support weekly coin resets on Sundays and nightly resets of purchased app time using WorkManager, while keeping the architecture clean and respecting the one-class-per-file rule.

---

## 8. On-open reset logic and nightly free time

**Prompt 25 – Replace WorkManager-based resets with on-open resets**

> In this repo, refactor the daily/weekly reset mechanism to run when a tracked app is opened instead of via WorkManager. Extract the reset decision logic from [`PeriodicResetWorker.performResets()`](app/src/main/java/com/example/taskandtimemanager/data/PeriodicResetWorker.kt:39) into a reusable helper class (for example, `ResetCoordinator`) under `app/src/main/java/com/example/taskandtimemanager/data`, keeping one Kotlin class per file. The helper should use [`LastResetStorage`](app/src/main/java/com/example/taskandtimemanager/data/LastResetStorage.kt:18) to determine if (a) a new 6am boundary has been crossed since the last daily purchase reset, and (b) a new Sunday-to-Monday week boundary has been crossed since the last weekly coin reset / weekly coin zero-reset. Call this helper from the app-open logic in the blocker flow (e.g., wherever the foreground app is inspected in `AppBlockerService`), and remove or effectively disable the periodic WorkManager scheduling while keeping the reset semantics identical (daily clear of `AppUsagePurchase` plus weekly synthetic zeroing of coins).

**Prompt 26 – Implement robust nightly free time handling**

> In this repo, fix the nightly free time feature so that enabling the "nightly free time" checkbox in the app limits UI persists correctly and is only cleared after the configured free period ends, instead of being reset immediately when an app is opened. Store the nightly-free-time configuration and current active state in a persistent layer (e.g., a small DataStore Preferences class or dedicated Room table) under `app/src/main/java/com/example/taskandtimemanager/data`, respecting the one-class-per-file rule. Integrate this state with the blocking logic in `AppBlockerService`: when nightly free time is active, bypass blocking and do not decrement purchased minutes; when the free period has passed (based on local time and the configured window), automatically deactivate nightly free time and resume normal blocking behavior. Ensure the UI in `AppConfigScreen` reads and writes this state consistently so the checkbox reflects the actual persisted value.
