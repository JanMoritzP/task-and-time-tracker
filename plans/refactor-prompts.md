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

Use each prompt separately in order. After finishing Prompt 12, the project should be structurally aligned with app/requirements.md and ready for manual testing on your device.