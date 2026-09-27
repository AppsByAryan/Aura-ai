package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aura.ui.AuraApp
import com.example.aura.ui.AuraViewModel
import com.example.ui.theme.AuraBlack
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private var shouldStartListening = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        shouldStartListening = intent?.getBooleanExtra("EXTRA_START_LISTENING", false) == true ||
                intent?.action == Intent.ACTION_ASSIST ||
                intent?.action == Intent.ACTION_VOICE_COMMAND

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = AuraBlack
                ) {
                    val vm: AuraViewModel = viewModel()

                    LaunchedEffect(shouldStartListening) {
                        if (shouldStartListening) {
                            shouldStartListening = false
                            vm.startVoiceListening()
                        }
                    }

                    AuraApp(viewModel = vm)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra("EXTRA_START_LISTENING", false) ||
            intent.action == Intent.ACTION_ASSIST ||
            intent.action == Intent.ACTION_VOICE_COMMAND
        ) {
            shouldStartListening = true
        }
    }
}
