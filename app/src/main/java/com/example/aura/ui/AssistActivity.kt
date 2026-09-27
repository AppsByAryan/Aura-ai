package com.example.aura.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.example.MainActivity

class AssistActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_START_LISTENING", true)
        }
        startActivity(intent)
        finish()
    }
}
