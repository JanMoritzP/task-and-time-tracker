package com.example.taskandtimemanager

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.example.taskandtimemanager.ui.NeoButton
import com.example.taskandtimemanager.ui.NeoCard
import com.example.taskandtimemanager.ui.theme.TaskAndTimeManagerTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.taskandtimemanager.data.AppBlockerStatusHolder
import kotlinx.coroutines.launch

/**
 * Full-screen blocking overlay activity shown when a tracked app runs out of time.
 *
 * This Activity is launched as a normal activity (no special overlay/window types)
 * and simply sits on top of the blocked application until the user either buys
 * more time or closes the app.
 */
class BlockingActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val targetPackage = intent.getStringExtra(EXTRA_TARGET_PACKAGE).orEmpty()
        val targetAppName = intent.getStringExtra(EXTRA_TARGET_APP_NAME).orEmpty()

        setContent {
            val scope = rememberCoroutineScope()
            val snackbarHostState = remember { SnackbarHostState() }

            
                        var isTimeExhausted by remember { mutableStateOf(true) }
            
                        // Simple re-check using the status holder; if the remaining minutes become
                        // positive again (e.g. after background purchase), we allow dismiss.
                        LaunchedEffect(Unit) {
                            scope.launch {
                                val status = AppBlockerStatusHolder.get()
                                val remaining = status.lastRemainingMinutes ?: 0L
                                isTimeExhausted = remaining <= 0
                            }
                        }
            
                        TaskAndTimeManagerTheme {
                            Scaffold(
                                modifier = Modifier.fillMaxSize(),
                                snackbarHost = { SnackbarHost(snackbarHostState) },
                            ) { paddingValues ->
                                BlockingScreenContent(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(paddingValues)
                                        .padding(24.dp),
                                    targetAppName = targetAppName.ifEmpty { targetPackage },
                                    onBuyMoreTime = { minutesToBuy ->
                                        scope.launch {
                                            val success = AppBlockerCommands.buyMoreTime(
                                                context = this@BlockingActivity,
                                                targetPackage = targetPackage,
                                                minutesToBuy = minutesToBuy,
                                            )
                                            if (success) {
                                                // After a successful purchase, we finish so the user
                                                // immediately returns to the previously blocked app.
                                                finish()
                                            } else {
                                                snackbarHostState.showSnackbar("Not enough coins to buy more time.")
                                                isTimeExhausted = true
                                            }
                                        }
                                    },
                                    onCloseApp = {
                                        navigateHomeAndFinish()
                                    },
                                )
                    // Disable back if time is still exhausted.
                    BackHandler(enabled = isTimeExhausted) {
                        // Swallow back presses while exhausted.
                    }
                }
            }
        }
    }

    private fun navigateHomeAndFinish() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
        finish()
    }

    companion object {
        private const val EXTRA_TARGET_PACKAGE = "extra_target_package"
        private const val EXTRA_TARGET_APP_NAME = "extra_target_app_name"

        fun createIntent(context: Context, targetPackage: String, targetAppName: String?): Intent {
            return Intent(context, BlockingActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_TARGET_PACKAGE, targetPackage)
                putExtra(EXTRA_TARGET_APP_NAME, targetAppName ?: "")
            }
        }
    }
}

@Composable
private fun BlockingScreenContent(
    modifier: Modifier = Modifier,
    targetAppName: String,
    onBuyMoreTime: (minutesToBuy: Long) -> Unit,
    onCloseApp: () -> Unit,
) {
    var minutesInput by remember { mutableStateOf("") }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        NeoCard(
            modifier = Modifier
                .fillMaxSize(fraction = 0.9f),
            innerPadding = androidx.compose.foundation.layout.PaddingValues(24.dp),
        ) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize(),
            ) {
                Text(
                    text = "Time limit reached",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.padding(8.dp))

                Text(
                    text = "You have reached your limit for $targetAppName.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.padding(16.dp))

                OutlinedTextField(
                    value = minutesInput,
                    onValueChange = { value ->
                        minutesInput = value.filter { ch -> ch.isDigit() }
                    },
                    label = { Text("Minutes to buy") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                    ),
                )

                Spacer(modifier = Modifier.padding(16.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    NeoButton(
                        onClick = {
                            val minutesToBuy = minutesInput.toLongOrNull() ?: 0L
                            onBuyMoreTime(minutesToBuy)
                        },
                        modifier = Modifier.fillMaxSize(fraction = 0.7f),
                    ) {
                        Text("Buy more time")
                    }

                    NeoButton(
                        onClick = onCloseApp,
                        modifier = Modifier.fillMaxSize(fraction = 0.7f),
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

/**
 * Helper object to expose blocking-related operations to Activities/Composables
 * without tightly coupling them to the service implementation.
 */
object AppBlockerCommands {

    /**
     * Launches [BlockingActivity] for the given package/app name.
     */
    fun showBlockingOverlay(context: Context, targetPackage: String, targetAppName: String?) {
        val intent = BlockingActivity.createIntent(context, targetPackage, targetAppName)
        context.startActivity(intent)
    }

    /**
     * Tries to buy a custom amount of minutes for the given package using the
     * same purchase logic as [AppBlockerService]. Returns true on success.
     */
    suspend fun buyMoreTime(context: Context, targetPackage: String, minutesToBuy: Long): Boolean {
        if (minutesToBuy <= 0L) return false

        val db = com.example.taskandtimemanager.data.AppDatabase.getDatabase(context)
        val appUsageManager = com.example.taskandtimemanager.data.AppUsageManager(
            trackedAppDao = db.trackedAppDao(),
            appUsageAggregateDao = db.appUsageAggregateDao(),
            appUsagePurchaseDao = db.appUsagePurchaseDao(),
        )
        val coinManager = com.example.taskandtimemanager.data.CoinManager(
            taskExecutionDao = db.taskExecutionDao(),
            rewardRedemptionDao = db.rewardRedemptionDao(),
            appUsagePurchaseDao = db.appUsagePurchaseDao(),
        )

        val trackedApps = appUsageManager.getTrackedApps()
        val app = trackedApps.firstOrNull { it.packageName == targetPackage } ?: return false

        val coinsRequired = (app.costPerMinute * minutesToBuy).toInt()
        val currentBalance = coinManager.getCoinBalance()

        if (coinsRequired <= 0 || currentBalance < coinsRequired) {
            return false
        }

        val purchase = com.example.taskandtimemanager.model.AppUsagePurchase(
            id = java.util.UUID.randomUUID().toString(),
            appId = app.id,
            minutesPurchased = minutesToBuy,
            coinsSpent = coinsRequired,
            purchaseDateTime = java.time.LocalDateTime.now().toString(),
        )
        appUsageManager.addAppUsagePurchase(purchase)

        // Update diagnostics so the overlay can see that time is no longer exhausted.
        com.example.taskandtimemanager.data.AppBlockerStatusHolder.update { previous ->
            previous.copy(
                lastRemainingMinutes = (previous.lastRemainingMinutes ?: 0L) + minutesToBuy,
            )
        }

        return true
    }
}
