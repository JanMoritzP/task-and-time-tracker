package com.example.taskandtimemanager

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import com.example.taskandtimemanager.data.DataStore
import kotlinx.coroutines.launch

/**
 * Foreground service that shows a full-screen overlay using TYPE_APPLICATION_OVERLAY.
 *
 * This reuses the same blocking UI as [BlockingActivity] but is hosted inside a
 * WindowManager view hierarchy so it can appear over other apps like YouTube.
 */
class AppBlockerOverlayService : Service() {

    private val notificationChannelId = "overlay_blocker_channel"
    private val notificationId = 42

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val targetPackage = intent?.getStringExtra(EXTRA_TARGET_PACKAGE).orEmpty()
        val targetAppName = intent?.getStringExtra(EXTRA_TARGET_APP_NAME).orEmpty()
        val action = intent?.action

        if (ACTION_DISMISS == action) {
            removeOverlay()
            stopSelf()
            return START_NOT_STICKY
        }

        if (!Settings.canDrawOverlays(this)) {
            Log.w(TAG, "Overlay permission not granted; cannot show overlay.")
            // We cannot show the overlay; just stop.
            stopSelf()
            return START_NOT_STICKY
        }

        showOverlay(targetPackage = targetPackage, targetAppName = targetAppName)

        // Run as a foreground service on O+ using the declared dataSync type.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForeground(notificationId, buildNotification())
        }

        return START_STICKY
    }

    private fun showOverlay(targetPackage: String, targetAppName: String) {
        // Launch the dedicated overlay Activity which hosts the Compose UI.
        val intent = BlockingOverlayActivity.createIntent(
            context = this,
            targetPackage = targetPackage,
            targetAppName = targetAppName.ifEmpty { targetPackage },
        )
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    private fun removeOverlay() {
        overlayView?.let { view ->
            windowManager?.removeView(view)
            overlayView = null
        }
    }

    private fun navigateHome() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                notificationChannelId,
                "Overlay Blocker",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Shows blocking overlays on top of distracting apps."
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
            .setContentTitle("Blocking overlay active")
            .setContentText("Time limit reached for a tracked app.")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        removeOverlay()
    }

    companion object {
        private const val TAG = "AppBlockerOverlaySvc"

        const val EXTRA_TARGET_PACKAGE = "extra_target_package"
        const val EXTRA_TARGET_APP_NAME = "extra_target_app_name"

        const val ACTION_DISMISS = "com.example.taskandtimemanager.action.DISMISS_OVERLAY"

        fun createStartIntent(
            context: Context,
            targetPackage: String,
            targetAppName: String?,
        ): Intent {
            return Intent(context, AppBlockerOverlayService::class.java).apply {
                putExtra(EXTRA_TARGET_PACKAGE, targetPackage)
                putExtra(EXTRA_TARGET_APP_NAME, targetAppName ?: "")
            }
        }

        fun createDismissIntent(context: Context): Intent {
            return Intent(context, AppBlockerOverlayService::class.java).apply {
                action = ACTION_DISMISS
            }
        }
    }
}

@Composable
private fun OverlayBlockingContent(
    targetAppName: String,
    targetPackage: String,
    service: Service,
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var isTimeExhausted by remember { mutableStateOf(true) }
    var hasEnoughCoinsForOneMinute by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        scope.launch {
            val status = com.example.taskandtimemanager.data.AppBlockerStatusHolder.get()
            val remaining = status.lastRemainingMinutes ?: 0L
            isTimeExhausted = remaining <= 0

            // Also compute if the user has enough coins to buy at least 1 minute.
            val dataStore = DataStore(service.applicationContext)
            val balance = dataStore.getCoinBalance()
            val apps = dataStore.getTrackedApps()
            val app = apps.firstOrNull { it.packageName == targetPackage }
            hasEnoughCoinsForOneMinute = if (app == null || app.costPerMinute <= 0f) {
                false
            } else {
                balance >= app.costPerMinute.toInt()
            }
        }
    }

    MaterialTheme(colorScheme = lightColorScheme()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Time limit reached for $targetAppName",
                    style = MaterialTheme.typography.headlineSmall,
                )

                androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(16.dp))

                if (hasEnoughCoinsForOneMinute) {
                    Button(
                        onClick = {
                            scope.launch {
                                val success = AppBlockerCommands.buyMoreTime(service, targetPackage, 5L)
                                if (success) {
                                    service.stopSelf()
                                } else {
                                    snackbarHostState.showSnackbar("Not enough coins to buy more time.")
                                    isTimeExhausted = true
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize(fraction = 0.6f),
                    ) {
                        Text("Buy more time")
                    }

                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(8.dp))
                }

                Button(
                    onClick = {
                        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                            addCategory(Intent.CATEGORY_HOME)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        service.startActivity(homeIntent)
                        service.stopSelf()
                    },
                    modifier = Modifier.fillMaxSize(fraction = 0.6f),
                ) {
                    Text("Close")
                }
            }

            BackHandler(enabled = isTimeExhausted) {
                // Swallow back presses while exhausted.
            }
        }
    }
}
