package com.example.taskandtimemanager

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
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
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * Full-screen activity used as the runtime overlay when time is up.
 *
 * This reuses the same blocking UI / purchase flow but runs inside a normal
 * Activity lifecycle, avoiding the Compose-in-Service issues while still being
 * launched by [AppBlockerOverlayService] on top of other apps.
 */
class BlockingOverlayActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val targetPackage = intent.getStringExtra(EXTRA_TARGET_PACKAGE).orEmpty()
        val targetAppName = intent.getStringExtra(EXTRA_TARGET_APP_NAME).orEmpty()

        setContent {
            val scope = rememberCoroutineScope()
            val snackbarHostState = remember { SnackbarHostState() }

            var isTimeExhausted by remember { mutableStateOf(true) }

            LaunchedEffect(Unit) {
                scope.launch {
                    val status = com.example.taskandtimemanager.data.AppBlockerStatusHolder.get()
                    val remaining = status.lastRemainingMinutes ?: 0L
                    isTimeExhausted = remaining <= 0
                }
            }

            MaterialTheme(colorScheme = lightColorScheme()) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                ) { paddingValues ->
                    BlockingOverlayScreen(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .padding(24.dp),
                        targetAppName = targetAppName.ifEmpty { targetPackage },
                        onBuyMoreTime = {
                            scope.launch {
                                val success = AppBlockerCommands.buyMoreTime(this@BlockingOverlayActivity, targetPackage)
                                if (success) {
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

                    BackHandler(enabled = isTimeExhausted) {
                        // Swallow back while time is still exhausted.
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
            return Intent(context, BlockingOverlayActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS or
                    Intent.FLAG_ACTIVITY_NO_HISTORY
                putExtra(EXTRA_TARGET_PACKAGE, targetPackage)
                putExtra(EXTRA_TARGET_APP_NAME, targetAppName ?: "")
            }
        }
    }
}

@Composable
private fun BlockingOverlayScreen(
    modifier: Modifier = Modifier,
    targetAppName: String,
    onBuyMoreTime: () -> Unit,
    onCloseApp: () -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Time limit reached for $targetAppName",
            style = MaterialTheme.typography.headlineSmall,
        )

        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(16.dp))

        Button(onClick = onBuyMoreTime, modifier = Modifier.fillMaxSize(fraction = 0.6f)) {
            Text("Buy more time")
        }

        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(8.dp))

        Button(onClick = onCloseApp, modifier = Modifier.fillMaxSize(fraction = 0.6f)) {
            Text("Close")
        }
    }
}
