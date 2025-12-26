package com.example.taskandtimemanager

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.admin.DevicePolicyManager
import android.app.usage.UsageStatsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import com.example.taskandtimemanager.data.AppDatabase
import com.example.taskandtimemanager.data.AppUsageManager
import com.example.taskandtimemanager.data.CoinManager
import com.example.taskandtimemanager.data.TrackedAppDao
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Background service that periodically checks the current foreground app and
 * blocks it once the purchased time is exhausted.
 *
 * IMPORTANT PLATFORM NOTE:
 * - There is no public, fully reliable API to programmatically close another
 *   app on modern Android. This service uses [DevicePolicyManager.setApplicationHidden]
 *   for personal / device-owner style deployments as outlined in requirements.
 * - Foreground app detection requires `PACKAGE_USAGE_STATS` and may be delayed
 *   by the system's batching behaviour.
 */
class AppBlockerService : Service() {

    private val scope = CoroutineScope(Dispatchers.Default)
    private val handler = Handler(Looper.getMainLooper())
    private val checkIntervalMillis: Long = 5_000 // ~5 seconds for more responsive blocking

    private val notificationChannelId = "app_blocker_channel"
    private val notificationId = 1

    // Data / persistence
    private lateinit var appUsageManager: AppUsageManager
    private lateinit var coinManager: CoinManager
    private lateinit var trackedAppDao: TrackedAppDao

    // System services
    private lateinit var usageStatsManager: UsageStatsManager
    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var adminComponent: ComponentName

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()
        startForeground(notificationId, buildNotification())

        val db = AppDatabase.getDatabase(this)
        trackedAppDao = db.trackedAppDao()
        appUsageManager = AppUsageManager(
            trackedAppDao = db.trackedAppDao(),
            appUsageAggregateDao = db.appUsageAggregateDao(),
            appUsagePurchaseDao = db.appUsagePurchaseDao(),
        )
        coinManager = CoinManager(
            taskExecutionDao = db.taskExecutionDao(),
            rewardRedemptionDao = db.rewardRedemptionDao(),
            appUsagePurchaseDao = db.appUsagePurchaseDao(),
        )

        usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        adminComponent = ComponentName(this, AdminReceiver::class.java)

        scheduleNextCheck()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Ensure periodic checks continue as long as the service is running.
        scheduleNextCheck()
        return START_STICKY
    }

    private fun scheduleNextCheck() {
        handler.removeCallbacks(checkRunnable)
        handler.postDelayed(checkRunnable, checkIntervalMillis)
    }

    private val checkRunnable = Runnable {
        scope.launch {
            try {
                checkCurrentForegroundAppAndBlockIfNeeded()
            } finally {
                // Always schedule the next run even if something failed.
                scheduleNextCheck()
            }
        }
    }

    private val TAG = "AppBlockerService"

    /**
     * Core blocking flow:
     *  1. Determine current foreground package.
     *  2. If it matches a [TrackedApp], compute remaining minutes from purchases
     *     vs automatic usage.
     *  3. When remainingMinutes <= 0, show a blocking dialog that lets the user
     *     buy more minutes using coins, or block the app when declined / not enough coins.
     */
    private suspend fun checkCurrentForegroundAppAndBlockIfNeeded() {
        val packageName = getCurrentForegroundPackageName()
        Log.d(TAG, "checkCurrentForegroundAppAndBlockIfNeeded() - foreground package = $packageName")
        if (packageName == null) {
            Log.d(TAG, "No foreground package (or no usage access). Skipping.")
            return
        }

        val trackedApps = appUsageManager.getTrackedApps()
        Log.d(TAG, "Tracked apps: count=${trackedApps.size}")
        val trackedApp = trackedApps.firstOrNull { it.packageName == packageName }
        if (trackedApp == null) {
            Log.d(TAG, "Foreground app $packageName is not a tracked app. Skipping.")
            return
        }
        Log.d(TAG, "Matched tracked app: id=${trackedApp.id}, name=${trackedApp.name}, costPerMinute=${trackedApp.costPerMinute}, purchasedMinutesTotal=${trackedApp.purchasedMinutesTotal}")

        val today = LocalDate.now()
        val aggregatesToday = appUsageManager.getAppUsageAggregatesForDate(today)
        val usedMinutesAutomatic: Long = aggregatesToday
            .filter { it.appId == trackedApp.id }
            .sumOf { it.usedMinutesAutomatic }
        Log.d(TAG, "Used minutes today (automatic) for appId=${trackedApp.id}: $usedMinutesAutomatic")

        val purchases = appUsageManager.getAppUsagePurchases(trackedApp.id)
        val totalPurchasedMinutes: Long = purchases.sumOf { it.minutesPurchased }
        Log.d(TAG, "Total purchased minutes for appId=${trackedApp.id}: $totalPurchasedMinutes (purchases count=${purchases.size})")

        // If a hard cap is configured, interpret purchasedMinutesTotal as that cap,
        // otherwise fall back to purchased minutes.
        val effectiveLimitMinutes: Long = if (trackedApp.purchasedMinutesTotal > 0L) {
            trackedApp.purchasedMinutesTotal
        } else {
            totalPurchasedMinutes
        }
        Log.d(TAG, "Effective limit minutes for appId=${trackedApp.id}: $effectiveLimitMinutes")

        val remainingMinutes: Long = effectiveLimitMinutes - usedMinutesAutomatic
        Log.d(TAG, "Remaining minutes for package=$packageName: $remainingMinutes")
        if (remainingMinutes > 0) {
            Log.d(TAG, "Still have remaining time. Not blocking.")
            return
        }

        Log.d(TAG, "No remaining time. Triggering blocking dialog for $packageName")
        // We're out of time for this app.
        showBlockingDialog(trackedApp.packageName)
    }

    /**
     * Attempts to determine the foreground app using UsageStatsManager.
     * This is inherently best-effort and may lag behind real time.
     */
    private fun getCurrentForegroundPackageName(): String? {
        // Ensure the user has granted the "Usage Access" permission.
        if (!hasUsageAccessPermission()) {
            Log.d(TAG, "Usage access permission not granted.")
            return null
        }

        val endTime = System.currentTimeMillis()
        val startTime = endTime - 60_000 // last 60 seconds
        Log.d(TAG, "Querying usage stats from $startTime to $endTime")

        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime,
        )
        if (stats == null) {
            Log.d(TAG, "queryUsageStats returned null")
            return null
        }
        if (stats.isEmpty()) {
            Log.d(TAG, "queryUsageStats returned empty list")
            return null
        }

        val recent = stats.maxByOrNull { it.lastTimeUsed }
        if (recent == null) {
            Log.d(TAG, "No recent usage stats item found")
            return null
        }
        Log.d(TAG, "Most recently used package=${recent.packageName}, lastTimeUsed=${recent.lastTimeUsed}")
        return recent.packageName
    }

    private fun hasUsageAccessPermission(): Boolean {
        return try {
            val enabled = Settings.Secure.getInt(
                contentResolver,
                "usage_stats_enabled",
                0,
            ) == 1
            Log.d(TAG, "hasUsageAccessPermission() = $enabled")
            enabled
        } catch (e: Exception) {
            Log.e(TAG, "Error checking usage access permission", e)
            false
        }
    }

    /**
     * Shows a modal dialog on top of the current app informing the user that no
     * time is left and allowing them to buy more minutes.
     *
     * NOTE: Displaying UI from a Service requires a SYSTEM_ALERT_WINDOW /
     * overlay-style permission for full-screen overlays on modern Android. For a
     * personal device-admin deployment this may be acceptable, but Play Store
     * apps should not rely on this approach.
     */
    private fun showBlockingDialog(targetPackage: String) {
        handler.post {
            // We use the application context with a system alert window type so
            // the dialog appears above the blocked app. This is best-effort and
            // may behave differently across OEMs.
            val builder = AlertDialog.Builder(this@AppBlockerService).apply {
                setTitle("Time limit reached")
                setMessage("You have no remaining time for this app. Buy more minutes with coins?")
                setCancelable(false)
                setPositiveButton("Buy 10 minutes") { dialog, _ ->
                    dialog.dismiss()
                    scope.launch { handlePurchaseOrBlock(targetPackage, minutesToBuy = 10L) }
                }
                setNegativeButton("Block now") { dialog, _ ->
                    dialog.dismiss()
                    scope.launch { hideApplication(targetPackage) }
                }
            }

            val dialog = builder.create()
            if (dialog.window != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    dialog.window!!.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
                } else {
                    @Suppress("DEPRECATION")
                    dialog.window!!.setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT)
                }
            }
            dialog.show()
        }
    }

    /**
     * Handles the "buy more minutes" flow from the blocking dialog. When there
     * are insufficient coins, the target app is blocked instead.
     */
    private suspend fun handlePurchaseOrBlock(targetPackage: String, minutesToBuy: Long) {
        Log.d(TAG, "handlePurchaseOrBlock(targetPackage=$targetPackage, minutesToBuy=$minutesToBuy)")

        // We need to translate from package name back to TrackedApp id.
        val trackedApps = appUsageManager.getTrackedApps()
        val app = trackedApps.firstOrNull { it.packageName == targetPackage }
        if (app == null) {
            Log.w(TAG, "No TrackedApp found for package=$targetPackage. Hiding app directly.")
            hideApplication(targetPackage)
            return
        }
        Log.d(TAG, "Found TrackedApp for purchase: id=${app.id}, costPerMinute=${app.costPerMinute}")

        // Compute cost and check coin balance.
        val coinsRequired = (app.costPerMinute * minutesToBuy).toInt()
        val currentBalance = coinManager.getCoinBalance()
        Log.d(TAG, "coinsRequired=$coinsRequired, currentBalance=$currentBalance")

        if (coinsRequired <= 0 || currentBalance < coinsRequired) {
            Log.d(TAG, "Not enough coins or zero cost. Blocking app. coinsRequired=$coinsRequired, balance=$currentBalance")
            // Not enough coins – enforce blocking.
            hideApplication(targetPackage)
            return
        }

        val purchase = com.example.taskandtimemanager.model.AppUsagePurchase(
            id = UUID.randomUUID().toString(),
            appId = app.id,
            minutesPurchased = minutesToBuy,
            coinsSpent = coinsRequired,
            purchaseDateTime = LocalDateTime.now().toString(),
        )
        Log.d(TAG, "Recording purchase: $purchase")
        appUsageManager.addAppUsagePurchase(purchase)

        // After successful purchase we simply allow the app to continue.
        Log.d(TAG, "Purchase successful. Allowing app to continue.")
    }

    /**
     * Hides (effectively blocks) the given application package using the
     * DevicePolicyManager. This requires that the app is an active device admin
     * / device owner, which is acceptable for personal deployments but not for
     * general Play Store apps.
     */
    private suspend fun hideApplication(packageName: String) {
        if (!devicePolicyManager.isAdminActive(adminComponent)) {
            return
        }
        try {
            devicePolicyManager.setApplicationHidden(adminComponent, packageName, true)
        } catch (_: SecurityException) {
            // Some devices / OEMs may restrict this even for device admins.
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                notificationChannelId,
                "App Blocker",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Keeps the app blocker running to monitor and block distracting apps."
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, notificationChannelId)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }

        return builder
            .setContentTitle("App blocker running")
            .setContentText("Monitoring distracting apps and enforcing limits.")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
