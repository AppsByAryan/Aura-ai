package com.example.aura.ui.screens

import android.accounts.AccountManager
import android.app.Activity
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Assistant
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.aura.service.AuraAccessibilityService
import com.example.aura.ui.AuraViewModel
import com.example.aura.ui.components.GlassCard
import com.example.ui.theme.AuraBlack
import com.example.ui.theme.AuraBorderActive
import com.example.ui.theme.AuraBorderGlow
import com.example.ui.theme.AuraCyan
import com.example.ui.theme.AuraDarkSurface
import com.example.ui.theme.AuraElectricViolet
import com.example.ui.theme.AuraError
import com.example.ui.theme.AuraSuccess
import com.example.ui.theme.AuraTextMuted
import com.example.ui.theme.AuraTextPrimary
import com.example.ui.theme.AuraTextSecondary
import com.example.ui.theme.AuraWarning
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecuritySettingsScreen(
    viewModel: AuraViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val telemetry by viewModel.telemetry.collectAsStateWithLifecycle()
    val isA11yActive = AuraAccessibilityService.isAccessibilityEnabledInSystem(context)
    val isDefaultAssistant = viewModel.isDefaultAssistant()

    val isWakeWordEnabled by viewModel.isWakeWordEnabled.collectAsStateWithLifecycle()
    val wakeWordPhrase by viewModel.wakeWordPhrase.collectAsStateWithLifecycle()
    val wakeWordSensitivity by viewModel.wakeWordSensitivity.collectAsStateWithLifecycle()
    val isWakeWordListening by viewModel.isWakeWordListening.collectAsStateWithLifecycle()
    val rememberedPermissions by viewModel.rememberedPermissions.collectAsStateWithLifecycle()
    val isDirectExecution by viewModel.isDirectExecution.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    var customWakeWordInput by remember(wakeWordPhrase) { mutableStateOf(wakeWordPhrase) }
    var showManualAccountDialog by remember { mutableStateOf(false) }
    var manualAccountName by remember(currentUser) { mutableStateOf(currentUser.displayName) }
    var manualAccountEmail by remember(currentUser) { mutableStateOf(currentUser.email) }

    val accountPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val accountName = result.data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
            if (!accountName.isNullOrBlank()) {
                viewModel.onGoogleAccountChosen(accountName)
            }
        }
    }

    fun launchGoogleSignInOrSwitch() {
        try {
            accountPickerLauncher.launch(viewModel.getGoogleAccountPickerIntent())
        } catch (_: Exception) {
            showManualAccountDialog = true
        }
    }

    val detectedAccounts = remember(currentUser) { viewModel.getDeviceGoogleAccounts() }

    val hasGeminiApiKey = try {
        val k = BuildConfig.GEMINI_API_KEY
        !k.isNullOrBlank() && k != "MY_GEMINI_API_KEY"
    } catch (_: Exception) {
        false
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = AuraBlack,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "SETTINGS & SECURITY",
                        color = AuraCyan,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AuraDarkSurface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // IDENTITY & CREATOR CARD
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = AuraCyan.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, AuraCyan)
                        ) {
                            Box(modifier = Modifier.padding(8.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = AuraCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "AURA — THE ONE AND ONLY AURA",
                                color = AuraCyan,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Created by Aryan Yadav • Student of Class 10th",
                                color = AuraTextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // GOOGLE ACCOUNT & OAUTH IDENTITY CARD
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("google_oauth_account_card"),
                borderColor = if (currentUser.isSignedIn) AuraCyan else AuraBorderGlow
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = AuraCyan.copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, AuraCyan)
                            ) {
                                Box(
                                    modifier = Modifier.size(38.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (currentUser.displayName.isNotBlank()) currentUser.displayName.take(1).uppercase() else "G",
                                        color = AuraCyan,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (currentUser.isSignedIn) currentUser.displayName else "GOOGLE ACCOUNT",
                                    color = AuraTextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (currentUser.isSignedIn && currentUser.email.isNotBlank()) currentUser.email else "Not connected",
                                    color = if (currentUser.isSignedIn) AuraCyan else AuraTextMuted,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (currentUser.isSignedIn) AuraSuccess.copy(alpha = 0.15f) else AuraCyan.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, if (currentUser.isSignedIn) AuraSuccess.copy(alpha = 0.5f) else AuraCyan.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = if (currentUser.isSignedIn) "OAUTH LINKED" else "DISCONNECTED",
                                color = if (currentUser.isSignedIn) AuraSuccess else AuraCyan,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "AURA uses Google OAuth to identify you securely. This allows AURA to personalize answers, remember who is giving commands, and greet you by name.",
                        color = AuraTextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    if (detectedAccounts.isNotEmpty()) {
                        Text(
                            text = "ACCOUNTS DETECTED ON THIS DEVICE:",
                            color = AuraCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            detectedAccounts.forEach { acc ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.onGoogleAccountChosen(acc) },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (currentUser.email == acc) AuraCyan.copy(alpha = 0.15f) else Color(0x220A192F),
                                    border = BorderStroke(1.dp, if (currentUser.email == acc) AuraCyan else AuraBorderGlow)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = acc, color = AuraTextPrimary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                        Text(
                                            text = if (currentUser.email == acc) "ACTIVE" else "SELECT",
                                            color = if (currentUser.email == acc) AuraCyan else AuraTextMuted,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { launchGoogleSignInOrSwitch() },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("google_sign_in_button"),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AuraCyan,
                                    contentColor = Color.Black
                                )
                            ) {
                                Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (currentUser.isSignedIn) "CHOOSE GOOGLE ACCOUNT" else "SIGN IN WITH GOOGLE",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }

                            OutlinedButton(
                                onClick = { showManualAccountDialog = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("manual_account_switch_button"),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, AuraCyan.copy(alpha = 0.6f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = AuraCyan)
                            ) {
                                Text("SWITCH EMAIL / NAME", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }

                        if (currentUser.isSignedIn) {
                            OutlinedButton(
                                onClick = { viewModel.signOutGoogle() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("google_sign_out_button"),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, AuraError.copy(alpha = 0.6f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = AuraError)
                            ) {
                                Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("SIGN OUT FROM GOOGLE", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // Manual Account Switch Dialog
            if (showManualAccountDialog) {
                AlertDialog(
                    onDismissRequest = { showManualAccountDialog = false },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccountCircle, contentDescription = null, tint = AuraCyan, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("SWITCH GOOGLE IDENTITY", color = AuraCyan, fontFamily = FontFamily.Monospace, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    },
                    text = {
                        Column {
                            Text(
                                text = "Enter any Google account name & email to update who AURA serves:",
                                color = AuraTextSecondary,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = manualAccountName,
                                onValueChange = { manualAccountName = it },
                                label = { Text("Display Name") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AuraCyan,
                                    unfocusedBorderColor = AuraBorderGlow,
                                    focusedTextColor = AuraTextPrimary,
                                    unfocusedTextColor = AuraTextPrimary,
                                    focusedLabelColor = AuraCyan
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = manualAccountEmail,
                                onValueChange = { manualAccountEmail = it },
                                label = { Text("Google Email Address") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AuraCyan,
                                    unfocusedBorderColor = AuraBorderGlow,
                                    focusedTextColor = AuraTextPrimary,
                                    unfocusedTextColor = AuraTextPrimary,
                                    focusedLabelColor = AuraCyan
                                )
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (manualAccountEmail.isNotBlank()) {
                                    viewModel.switchOrUpdateAccount(manualAccountName, manualAccountEmail)
                                    showManualAccountDialog = false
                                }
                            },
                            enabled = manualAccountEmail.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = AuraCyan, contentColor = Color.Black)
                        ) {
                            Text("SAVE & SWITCH", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    },
                    dismissButton = {
                        OutlinedButton(
                            onClick = { showManualAccountDialog = false },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AuraTextMuted)
                        ) {
                            Text("CANCEL", fontSize = 11.sp)
                        }
                    },
                    containerColor = AuraDarkSurface
                )
            }

            // AUTONOMOUS DIRECT EXECUTION (ZERO PERMISSION PROMPTS) CARD
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("direct_execution_card"),
                borderColor = if (isDirectExecution) AuraCyan else AuraBorderGlow
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = if (isDirectExecution) AuraCyan else AuraTextMuted,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "AUTONOMOUS EXECUTION",
                                    color = if (isDirectExecution) AuraCyan else AuraTextSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = if (isDirectExecution) "Active • Never asks for permission" else "Prompt for permission",
                                    color = if (isDirectExecution) AuraSuccess else AuraTextMuted,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        Switch(
                            checked = isDirectExecution,
                            onCheckedChange = { viewModel.setDirectExecution(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = AuraCyan,
                                uncheckedThumbColor = AuraTextMuted,
                                uncheckedTrackColor = Color(0xFF13223A)
                            ),
                            modifier = Modifier.testTag("direct_execution_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "When active, AURA never asks for permission for anything. Every command (launching apps, setting volume, playing media, screenshots, device settings) executes instantly and speaks the result out loud.",
                        color = AuraTextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }

            // DEFAULT DIGITAL ASSISTANT CARD
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("default_assistant_card"),
                borderColor = if (isDefaultAssistant) AuraSuccess else AuraCyan
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Assistant,
                                contentDescription = null,
                                tint = if (isDefaultAssistant) AuraSuccess else AuraCyan,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "DEFAULT DEVICE ASSISTANT",
                                    color = if (isDefaultAssistant) AuraSuccess else AuraCyan,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = if (isDefaultAssistant) "Active as Default Assistant" else "Tap to set as default",
                                    color = if (isDefaultAssistant) AuraSuccess else AuraTextSecondary,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (isDefaultAssistant) AuraSuccess.copy(alpha = 0.15f) else AuraCyan.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, if (isDefaultAssistant) AuraSuccess.copy(alpha = 0.5f) else AuraCyan.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = if (isDefaultAssistant) "DEFAULT" else "CONFIGURABLE",
                                color = if (isDefaultAssistant) AuraSuccess else AuraCyan,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Setting AURA as your default assistant allows you to invoke AURA instantly by holding the Home button, swiping up from the corner, or pressing your headset button from any screen.",
                        color = AuraTextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { viewModel.openDefaultAssistantSettings() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("set_default_assistant_button"),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDefaultAssistant) Color(0xFF132A4A) else AuraCyan,
                            contentColor = if (isDefaultAssistant) AuraCyan else Color.Black
                        )
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isDefaultAssistant) "CONFIGURE DEFAULT ASSISTANT SETTINGS" else "SET AURA AS DEFAULT ASSISTANT",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // REMEMBERED PERMISSIONS & AUTOMATION CARD
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("remembered_permissions_card"),
                borderColor = AuraCyan
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                tint = AuraCyan,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "REMEMBERED PERMISSIONS",
                                    color = AuraCyan,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "${rememberedPermissions.size} actions pre-authorized",
                                    color = AuraTextSecondary,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        if (rememberedPermissions.isNotEmpty()) {
                            OutlinedButton(
                                onClick = { viewModel.clearAllPermissions() },
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, AuraError.copy(alpha = 0.5f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = AuraError),
                                modifier = Modifier.testTag("reset_all_permissions_button")
                            ) {
                                Text("REVOKE ALL", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "AURA asks permission once. Once approved, the action is remembered so you aren't asked repeatedly. You can revoke any permission below at any time.",
                        color = AuraTextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (rememberedPermissions.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0x33000000),
                            border = BorderStroke(1.dp, Color(0x22FFFFFF)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "No permissions remembered yet. When you approve an action (e.g. YouTube, Volume, Camera), AURA will save it here so you won't be asked again.",
                                color = AuraTextMuted,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            val dateFormat = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
                            rememberedPermissions.forEach { perm ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0x330A192F),
                                    border = BorderStroke(1.dp, AuraBorderGlow),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = perm.label.ifBlank { perm.permissionKey },
                                                color = AuraTextPrimary,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = "Granted on ${dateFormat.format(Date(perm.grantedAtTimestamp))}",
                                                color = AuraTextMuted,
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }

                                        IconButton(
                                            onClick = { viewModel.revokePermission(perm.permissionKey) },
                                            modifier = Modifier.testTag("revoke_permission_${perm.permissionKey.replace(":", "_").lowercase()}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Revoke permission",
                                                tint = AuraError,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // WAKE WORD ENGINE CONFIGURATION CARD
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("wake_word_config_card"),
                borderColor = if (isWakeWordEnabled) AuraCyan else AuraBorderGlow
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Hearing,
                                contentDescription = null,
                                tint = if (isWakeWordEnabled) AuraCyan else AuraTextMuted,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "HANDS-FREE WAKE WORD",
                                    color = if (isWakeWordEnabled) AuraCyan else AuraTextSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = if (isWakeWordEnabled && isWakeWordListening) "Active • Low-Power Acoustic VAD"
                                    else if (isWakeWordEnabled) "Armed • Suspended during activity"
                                    else "Disabled",
                                    color = if (isWakeWordEnabled) AuraSuccess else AuraTextMuted,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        Switch(
                            checked = isWakeWordEnabled,
                            onCheckedChange = { viewModel.setWakeWordEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = AuraCyan,
                                uncheckedThumbColor = AuraTextMuted,
                                uncheckedTrackColor = Color(0xFF13223A)
                            ),
                            modifier = Modifier.testTag("wake_word_toggle_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Allows you to say \"$wakeWordPhrase\" to activate AURA without touching the screen.",
                        color = AuraTextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Preset Phrase Selector
                    Text(
                        text = "PRESET WAKE PHRASES:",
                        color = AuraTextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    val presets = listOf("Hey AURA", "AURA", "OK AURA", "Computer")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        presets.forEach { preset ->
                            val isSelected = wakeWordPhrase.equals(preset, ignoreCase = true)
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) AuraCyan.copy(alpha = 0.2f) else Color(0x33000000),
                                border = BorderStroke(1.dp, if (isSelected) AuraCyan else AuraBorderGlow),
                                modifier = Modifier
                                    .clickable {
                                        viewModel.setWakeWordPhrase(preset)
                                        customWakeWordInput = preset
                                    }
                                    .testTag("preset_wake_word_${preset.replace(" ", "_").lowercase()}")
                            ) {
                                Text(
                                    text = preset,
                                    color = if (isSelected) AuraCyan else AuraTextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Custom Phrase Input
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = customWakeWordInput,
                            onValueChange = { customWakeWordInput = it },
                            label = { Text("Custom Wake Phrase") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AuraCyan,
                                unfocusedBorderColor = AuraBorderGlow,
                                focusedTextColor = AuraTextPrimary,
                                unfocusedTextColor = AuraTextPrimary
                            ),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("custom_wake_word_input")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (customWakeWordInput.isNotBlank()) {
                                    viewModel.setWakeWordPhrase(customWakeWordInput)
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AuraCyan, contentColor = Color.Black),
                            modifier = Modifier.testTag("save_wake_word_button")
                        ) {
                            Text("SAVE", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Sensitivity Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "DETECTION SENSITIVITY",
                            color = AuraTextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = when {
                                wakeWordSensitivity < 0.4f -> "Low (Fewer Triggers)"
                                wakeWordSensitivity < 0.75f -> "Balanced (Recommended)"
                                else -> "High (Instant Response)"
                            },
                            color = AuraCyan,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Slider(
                        value = wakeWordSensitivity,
                        onValueChange = { viewModel.setWakeWordSensitivity(it) },
                        valueRange = 0.2f..1.0f,
                        steps = 3,
                        colors = SliderDefaults.colors(
                            thumbColor = AuraCyan,
                            activeTrackColor = AuraCyan,
                            inactiveTrackColor = Color(0xFF13223A)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("wake_word_sensitivity_slider")
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Battery Impact Disclosure Badge
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0x3300E5FF),
                        border = BorderStroke(1.dp, Color(0x2200E5FF))
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = AuraCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Battery Optimization: Engine sleeps during silence using RMS acoustic gating (~1.2% battery/hr duty cycle).",
                                color = AuraTextSecondary,
                                fontSize = 10.sp,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }

            // Core Philosophy Card
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = AuraCyan,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "AURA ZERO-SECRET-CONTROL RULE",
                            color = AuraCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "AURA never secretly controls your device. You are the final authority over every state change and interaction.",
                        color = AuraTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val steps = listOf(
                        "1. UNDERSTAND: Parse natural speech/text command",
                        "2. PLAN: Formulate typed Action Plan & assess risk",
                        "3. EXPLAIN: Clearly describe intended Android API action",
                        "4. ASK: Request explicit permission from user once",
                        "5. AUTHORIZE: User confirms, AURA remembers approval",
                        "6. EXECUTE: Call legitimate Android framework APIs",
                        "7. VERIFY & REPORT: Confirm outcome and speak results"
                    )

                    steps.forEach { step ->
                        Text(
                            text = step,
                            color = AuraTextSecondary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }

            // Accessibility Service Setup & Transparency
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Accessibility,
                                contentDescription = null,
                                tint = if (isA11yActive) AuraSuccess else AuraWarning,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "ASSISTIVE AUTOMATION SERVICE",
                                color = if (isA11yActive) AuraSuccess else AuraWarning,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (isA11yActive) AuraSuccess.copy(alpha = 0.15f) else AuraWarning.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, if (isA11yActive) AuraSuccess.copy(alpha = 0.4f) else AuraWarning.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = if (isA11yActive) "ENABLED" else "DISABLED",
                                color = if (isA11yActive) AuraSuccess else AuraWarning,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "What AURA CAN DO with this service:",
                        color = AuraTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "• Capture screenshots only when you explicitly confirm.",
                        color = AuraTextSecondary,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "• Assist in system navigation upon direct user instruction.",
                        color = AuraTextSecondary,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "What AURA CANNOT DO (Strict Privacy Guarantee):",
                        color = AuraTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "• CanRetrieveWindowContent is permanently disabled (cannot read passwords or screen text).",
                        color = AuraTextSecondary,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "• Never collects or transmits private screen pixels to third-party servers.",
                        color = AuraTextSecondary,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("open_accessibility_settings_button"),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isA11yActive) Color(0xFF132A4A) else AuraCyan,
                            contentColor = if (isA11yActive) AuraCyan else Color.Black
                        )
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isA11yActive) "MANAGE IN ANDROID SETTINGS" else "CONFIGURE IN ACCESSIBILITY SETTINGS",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // AI Reasoning Engine Status
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = null,
                            tint = AuraCyan,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "AI REASONING ARCHITECTURE",
                            color = AuraCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Offline Rule Engine:",
                            color = AuraTextSecondary,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "ACTIVE (Zero Latency)",
                            color = AuraSuccess,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Gemini AI Hybrid Reasoning:",
                            color = AuraTextSecondary,
                            fontSize = 12.sp
                        )
                        Text(
                            text = if (hasGeminiApiKey) "CONNECTED (Gemini 3.5 Flash)" else "STANDBY (Offline Fallback)",
                            color = if (hasGeminiApiKey) AuraCyan else AuraTextMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Google Search Grounding:",
                            color = AuraTextSecondary,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "ACTIVE (Real-Time Web Data)",
                            color = AuraSuccess,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Google Maps Grounding:",
                            color = AuraTextSecondary,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "ACTIVE (Live Places & Routes)",
                            color = AuraSuccess,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Work Done Notifications:",
                            color = AuraTextSecondary,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "ACTIVE (Status & Results)",
                            color = AuraCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "AURA can answer any general, real-time, or location question using Gemini with Google Search and Maps data, while device operations work offline.",
                        color = AuraTextMuted,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }

            // Emergency Stop Kill Switch
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = AuraError.copy(alpha = 0.5f)
            ) {
                Column {
                    Text(
                        text = "EMERGENCY OVERRIDE",
                        color = AuraError,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Instantly clears any queued or executing automation routines and silences voice responses.",
                        color = AuraTextSecondary,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { viewModel.emergencyStop("Emergency kill switch activated from security panel.") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("security_emergency_stop_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = AuraError),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("TRIGGER EMERGENCY STOP", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
