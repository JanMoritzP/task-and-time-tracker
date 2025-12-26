# Requirements Clarification Summary and Plan

## Interpreted Decisions from Your Answers

1. **System permissions and deployment target**
   - You are fine using system-level permissions and device-admin style capabilities because this APK is only for your own device, not Play Store distribution.
   - Requirements should explicitly say this is intended for personal / controlled deployment, so using privileged permissions is acceptable.

2. **usedMinutes source of truth**
   - `usedMinutes` must be **derived automatically** from actual app usage.
   - Users must **not** be able to change `usedMinutes` manually.
   - UsageStats (or an equivalent tracking mechanism) is the canonical source; UI can display usage but not edit it.

3. **Persistence and history**
   - If data cannot survive uninstalls automatically, you need **export/import of the entire state**:
     - All tasks
     - All apps and their settings / limits
     - Complete usage and completion **history**
   - Requirements must define a complete backup/restore mechanism (not just CSV for a subset of data).

4. **Task and history model**
   - The current model is "most likely bad" and should be redesigned.
   - You want to **see each completed task in a history**, not just aggregate counters.
   - Conceptually, you want a separation like:
     - A **TaskDefinition**: what is supposed to be done (habit / repeated thing).
     - A **TaskInstance** or **TaskExecution**: one concrete occurrence ("do task X on this day" or specific completion event).
   - Each day, new instances are created or at least new executions are recorded when you do the work.

5. **Architecture policy (ViewModel)**
   - You do not care; we should specify that the implementation is free to use whichever is best (ViewModel or not), and not keep "no ViewModel" as a hard constraint.

6. **Apps to control and blocking behavior**
   - a1: Only apps that you explicitly administer/track in the app (i.e. only those in the AppData table / configured list) are considered for limits and blocking.
   - For the previous "b" part: you clarified functional behavior when time expires:
     - If no time is available for an app (remaining <= 0):
       - Show a **warning dialog** when the app is opened.
       - Allow purchasing more time **if there are enough coins**.
       - If the user does not buy more time in that dialog **or** has no coins, **close/block** the app.
   - We will reflect this as the core UX of limit enforcement.

7. **Testing checklist**
   - You consider this a hobby project and do not want the formal test checklist inside requirements.
   - We will **remove the Testing Checklist section** from requirements.md.

8. **Strict one-class-per-file rule**
   - This rule is **very strict**.
   - Existing files that violate it (e.g. Model_Classes.kt) should be treated as violations and must be split in future implementation work.
   - Requirements.md should clearly state this rule as a hard constraint.

---

## Proposed Updated Domain Model (High Level)

This is the conceptual model I will encode into requirements.md. It stays abstract but concrete enough to guide implementation.

### 1. Task domain

- **TaskDefinition** (what can be done)
  - Id
  - Name
  - Category
  - Mandatory flag
  - Reward coins per completion
  - Recurrence characteristics (e.g. daily, one-time, unlimited times per day, limited times per day)

- **TaskInstance / TaskExecution** (what actually happened)
  - Id
  - TaskDefinitionId
  - Date (and optional time)
  - Status (Done / Skipped / Pending)
  - Coins awarded for this execution

**Behavioral rules:**
- For each day and each TaskDefinition, the app can either:
  - Dynamically create TaskInstance when you mark the task as done, or
  - Pre-generate a TaskInstance for each day for recurring tasks.
- History view is based on TaskInstance / TaskExecution, not just a counter.
- Infinite / repeatable tasks can be represented as multiple TaskExecution records for the same TaskDefinition on the same day.

### 2. Reward domain

- **OneTimeRewardDefinition**
  - Id
  - Name
  - Description
  - Coin cost

- **RewardRedemption**
  - Id
  - RewardDefinitionId
  - RedemptionDateTime
  - Coins spent

This replaces the implicit design where reward and claim are collapsed into one record.

### 3. App usage domain

- **AppConfig / TrackedApp**
  - Id
  - Name
  - PackageName
  - CostPerMinute (coins per minute of usage)
  - PurchasedMinutesTotal (sum of all purchases)

- **AppUsageAggregate**
  - AppId
  - Range (day / week / month) or Date
  - UsedMinutesAutomatic (derived from system usage)

- **AppUsagePurchase**
  - Id
  - AppId
  - MinutesPurchased
  - CoinsSpent
  - DateTime

**Behavioral rules:**
- `usedMinutesAutomatic` is derived by a UsageTracker service from actual usage; the user cannot edit it directly.
- Remaining time for an app is a function of purchases versus automatic usage.

---

## Updated Behavioral Requirements (High Level)

These will be integrated into app/requirements.md in the appropriate sections.

1. **Usage tracking and blocking**
   - usedMinutes must come from real device usage (UsageStatsManager or a similar mechanism).
   - When the user attempts to open a tracked app and remaining time <= 0:
     - Show a blocking dialog:
       - Display that no time is left.
       - Offer to buy more minutes using coins if the user has enough.
     - If user buys:
       - Deduct coins.
       - Increase purchased minutes.
       - Allow the app to continue.
     - If user cancels or has insufficient coins:
       - Block or immediately close the app.

2. **State export/import**
   - There must be a full export function that includes:
     - Task definitions and all execution history.
     - Rewards and all redemptions.
     - App configurations, purchases, and usage statistics.
   - There must be an import function that can restore from that export.
   - This is the mechanism to survive reinstalls and device changes.

3. **Architecture and file structure**
   - The project is allowed to use ViewModels, coroutines, etc., as needed.
   - The architecture section will no longer forbid ViewModels.
   - The file organization will explicitly state: "One Kotlin class per file" as a strict requirement; existing combined-class files are to be refactored.

4. **Testing section**
   - The Testing Checklist at the end of requirements.md will be removed to avoid mixing test cases with requirements.

---

## Next Steps I Will Take

1. Rewrite and extend data model sections in [`app/requirements.md`](app/requirements.md) to:
   - Introduce explicit TaskDefinition and TaskExecution / TaskInstance concepts with history.
   - Introduce clearer AppConfig, AppUsage, and Reward models.
   - Clarify that usedMinutes is automatic and non-editable by the user.
2. Update behavior sections for:
   - Costs / App Limits screen to remove manual editing of used minutes and reflect automatic usage.
   - AppBlockerService to include the warning dialog and coin-based extension behavior.
3. Add a new section describing export/import of the entire state instead of the current CSV-only approach, or explicitly repurpose CSV into a full-state export format.
4. Remove the Testing Checklist section and the hard "no ViewModel" constraints.
5. Clarify and harden the "one Kotlin class per file" rule.

Once this plan is accepted, the next mode (Code / Architect) can update [`app/requirements.md`](app/requirements.md) accordingly and then review the existing project implementation against the new requirements.
