package com.example.taskandtimemanager

import android.app.Service
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import com.example.taskandtimemanager.data.DataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class AppBlockerService : Service() {
    private lateinit var dataStore: DataStore
    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var adminComponent: ComponentName
    private val scope = CoroutineScope(Dispatchers.Main)
    private val handler = Handler(Looper.getMainLooper())
    private val checkInterval = 60000L // 60 seconds

    override fun onCreate() {
        super.onCreate()
        dataStore = DataStore(this)
        devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        adminComponent = ComponentName(this, AdminReceiver::class.java)

        // Start checking blocking status
        scheduleCheck()
    }

    private fun scheduleCheck() {
        handler.postDelayed(::checkAndBlock, checkInterval)
    }

    private fun checkAndBlock() {
        scope.launch {
            try {
                val apps = dataStore.getApps()
                val isAdmin = devicePolicyManager.isAdminActive(adminComponent)

                if (isAdmin) {
                    apps.forEach { app ->
                        val timeSpent = app.timeSpent // in milliseconds
                        val costPerMinute = app.costPerMinute

                        // Calculate if they've spent their time limit
                        // Assuming you want to block after timeSpent minutes
                        val allowedTimeMs = (timeSpent * 60 * 1000).toLong()

                        // For now, we'll block based on timeSpent > some threshold
                        // You can adjust this logic based on your requirements
                        val shouldBlock = timeSpent > 120 // Block after 120 minutes of tracking

                        try {
                            devicePolicyManager.setApplicationHidden(
                                adminComponent,
                                app.name, // Use app name as package identifier if needed
                                shouldBlock
                            )
                        } catch (e: Exception) {
                            // Handle case where app can't be hidden
                            e.printStackTrace()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                scheduleCheck()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }

    override fun onBind(intent: Intent?): IBinder? = null}