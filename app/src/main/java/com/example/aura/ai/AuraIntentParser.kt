package com.example.aura.ai

import com.example.aura.data.ActionType
import com.example.aura.data.AuraActionPlan
import com.example.aura.data.AuraUser
import com.example.aura.data.RiskLevel

object AuraIntentParser {

    fun parse(input: String, user: AuraUser? = null): AuraActionPlan {
        val trimmed = input.trim()
        val lower = trimmed.lowercase().removeSuffix(".")

        // 1. Cancellation check
        if (SafetyEngine.isCancellationCommand(lower)) {
            return AuraActionPlan(
                actionType = ActionType.STOP_CANCEL,
                target = "Pending actions",
                description = "Cancel pending operations",
                device = "This Phone",
                risk = RiskLevel.LOW,
                explanation = "Halting and clearing active or queued automated requests."
            )
        }

        // 2. Cross-device command (e.g. "Open VS Code on my laptop", "on laptop", "on my macbook", "on computer")
        if (lower.contains("laptop") || lower.contains("desktop") || lower.contains("computer") || lower.contains("macbook")) {
            val targetApp = when {
                lower.contains("vs code") || lower.contains("vscode") || lower.contains("code") -> "Visual Studio Code"
                lower.contains("browser") || lower.contains("chrome") -> "Google Chrome"
                lower.contains("terminal") -> "Terminal"
                lower.contains("music") || lower.contains("spotify") -> "Spotify"
                lower.contains("lock") -> "Lock Workstation"
                else -> {
                    val appPart = lower.replace(Regex(".*open\\s+"), "")
                        .replace(Regex("\\s+on.*"), "").trim()
                    if (appPart.isNotEmpty()) appPart.replaceFirstChar { it.uppercase() } else "Remote Application"
                }
            }
            return AuraActionPlan(
                actionType = ActionType.CROSS_DEVICE_COMMAND,
                target = targetApp,
                description = "Launch $targetApp on remote machine",
                device = "My Laptop",
                risk = RiskLevel.HIGH,
                explanation = "AURA will send an authenticated TLS payload to the AURA Desktop Agent on your laptop."
            )
        }

        // 3. System Telemetry Queries (Battery, Storage, Memory, Network)
        if (lower.contains("battery") || lower.contains("charge") || lower.contains("power level")) {
            return AuraActionPlan(
                actionType = ActionType.GET_BATTERY_INFO,
                target = "Battery Status",
                description = "Inspect battery level and charging state",
                device = "This Phone",
                risk = RiskLevel.LOW,
                explanation = "Querying hardware battery metrics via Android BatteryManager."
            )
        }

        if (lower.contains("storage") || lower.contains("free space") || lower.contains("disk space") || lower.contains("memory left")) {
            return AuraActionPlan(
                actionType = ActionType.GET_STORAGE_INFO,
                target = "Device Storage",
                description = "Calculate available and occupied storage space",
                device = "This Phone",
                risk = RiskLevel.LOW,
                explanation = "Querying internal storage partition using Android StatFs."
            )
        }

        if (lower.contains("ram") || lower.contains("memory") && !lower.contains("storage")) {
            return AuraActionPlan(
                actionType = ActionType.GET_MEMORY_INFO,
                target = "System RAM",
                description = "Inspect system memory consumption",
                device = "This Phone",
                risk = RiskLevel.LOW,
                explanation = "Reading memory metrics from Android ActivityManager."
            )
        }

        if (lower.contains("network") || lower.contains("wifi") && !lower.contains("setting") && !lower.contains("turn on")) {
            return AuraActionPlan(
                actionType = ActionType.GET_NETWORK_INFO,
                target = "Network Telemetry",
                description = "Check active connectivity and link speeds",
                device = "This Phone",
                risk = RiskLevel.LOW,
                explanation = "Inspecting network capabilities via Android ConnectivityManager."
            )
        }

        // 4. Screenshots
        if (lower.contains("screenshot") || lower.contains("capture screen") || lower.contains("screen shot")) {
            return AuraActionPlan(
                actionType = ActionType.TAKE_SCREENSHOT,
                target = "System Display",
                description = "Take a full screen capture",
                device = "This Phone",
                risk = RiskLevel.HIGH,
                explanation = "Requesting screen surface capture through AURA Assistive Service.",
                needsAccessibility = true
            )
        }

        // 5. Volume Control ("set volume to 50%", "turn volume up")
        val volumeMatch = Regex("(\\d+)\\s*%").find(lower)
        if (lower.contains("volume") || volumeMatch != null) {
            val percent = volumeMatch?.groupValues?.get(1)?.toIntOrNull() ?: when {
                lower.contains("mute") || lower.contains("zero") -> 0
                lower.contains("max") || lower.contains("full") -> 100
                lower.contains("half") -> 50
                lower.contains("up") -> 75
                lower.contains("down") -> 25
                else -> 50
            }
            return AuraActionPlan(
                actionType = ActionType.SET_VOLUME,
                target = "$percent%",
                description = "Set media volume level to $percent%",
                device = "This Phone",
                risk = RiskLevel.MEDIUM,
                explanation = "Adjusting media audio channel via Android AudioManager.",
                payload = mapOf("percent" to percent.toString())
            )
        }

        // 6. Media Playback Controls
        if (lower.contains("play music") || lower.contains("pause music") || lower.contains("pause the music") ||
            lower.contains("resume music") || lower.contains("next song") || lower.contains("stop music")) {
            val cmd = when {
                lower.contains("pause") -> "Pause"
                lower.contains("next") -> "Next Track"
                lower.contains("previous") -> "Previous Track"
                lower.contains("stop") -> "Stop"
                else -> "Play"
            }
            return AuraActionPlan(
                actionType = ActionType.MEDIA_CONTROL,
                target = cmd,
                description = "Media command: $cmd",
                device = "This Phone",
                risk = RiskLevel.LOW,
                explanation = "Dispatching hardware media transport key event via Android AudioManager."
            )
        }

        // 7. System Settings & DND & Bluetooth
        if (lower.contains("do not disturb") || lower.contains("dnd")) {
            return AuraActionPlan(
                actionType = ActionType.OPEN_SETTINGS,
                target = "Do Not Disturb",
                description = "Configure Do Not Disturb mode",
                device = "This Phone",
                risk = RiskLevel.MEDIUM,
                explanation = "Navigating to Android Notification Policy and Do Not Disturb settings."
            )
        }

        if (lower.contains("bluetooth")) {
            return AuraActionPlan(
                actionType = ActionType.OPEN_SETTINGS,
                target = "Bluetooth",
                description = "Open Bluetooth device settings",
                device = "This Phone",
                risk = RiskLevel.MEDIUM,
                explanation = "Navigating to Android Bluetooth configuration panel."
            )
        }

        if (lower.contains("connected device") || lower.contains("show devices") || lower.contains("paired device")) {
            return AuraActionPlan(
                actionType = ActionType.OPEN_SETTINGS,
                target = "Connected Devices",
                description = "View connected Bluetooth and remote devices",
                device = "This Phone",
                risk = RiskLevel.LOW,
                explanation = "Navigating to Android Connected Devices settings."
            )
        }

        if (lower.contains("settings") || lower.contains("open setting")) {
            return AuraActionPlan(
                actionType = ActionType.OPEN_SETTINGS,
                target = "Android Settings",
                description = "Open Android System Settings",
                device = "This Phone",
                risk = RiskLevel.LOW,
                explanation = "Navigating to main Android System Settings."
            )
        }

        // 8. Specific Downloads Manager
        if (lower.contains("download") || lower.contains("my downloads")) {
            return AuraActionPlan(
                actionType = ActionType.OPEN_APP,
                target = "Downloads",
                description = "Open system Downloads manager",
                device = "This Phone",
                risk = RiskLevel.LOW,
                explanation = "Launching Android system Downloads activity."
            )
        }

        // 9. Camera System App
        if (lower.contains("camera") || lower.contains("take photo") || lower.contains("take a photo") ||
            lower.contains("take picture") || lower.contains("take a picture") || lower.contains("click photo") ||
            lower.contains("click picture") || lower.contains("open camera") || lower.contains("launch camera")) {
            return AuraActionPlan(
                actionType = ActionType.OPEN_APP,
                target = "Camera",
                description = "Open Camera application",
                device = "This Phone",
                risk = RiskLevel.LOW,
                explanation = "Launching Android camera hardware and still capture activity.",
                payload = mapOf("appName" to "Camera")
            )
        }

        // 10. Google Play Store
        if (lower.contains("play store") || lower.contains("playstore") || lower.contains("google play") ||
            lower.contains("app store") || lower.contains("open play store") || lower.contains("open playstore")) {
            return AuraActionPlan(
                actionType = ActionType.OPEN_APP,
                target = "Google Play Store",
                description = "Open Google Play Store",
                device = "This Phone",
                risk = RiskLevel.LOW,
                explanation = "Launching Google Play Store package com.android.vending.",
                payload = mapOf("packageName" to "com.android.vending", "appName" to "Google Play Store")
            )
        }

        // 11. Photos / Gallery
        if (lower == "gallery" || lower == "photos" || lower.contains("open gallery") || lower.contains("open photos")) {
            return AuraActionPlan(
                actionType = ActionType.OPEN_APP,
                target = "Photos",
                description = "Open Photos / Gallery",
                device = "This Phone",
                risk = RiskLevel.LOW,
                explanation = "Launching Android Gallery & Photos system app.",
                payload = mapOf("appName" to "Photos")
            )
        }

        // 12. Calculator
        if (lower.contains("calculator") || lower == "calc" || lower == "open calc") {
            return AuraActionPlan(
                actionType = ActionType.OPEN_APP,
                target = "Calculator",
                description = "Open Calculator",
                device = "This Phone",
                risk = RiskLevel.LOW,
                explanation = "Launching Android Calculator system app.",
                payload = mapOf("appName" to "Calculator")
            )
        }

        // 13. YouTube
        if (lower.contains("youtube")) {
            return AuraActionPlan(
                actionType = ActionType.OPEN_APP,
                target = "YouTube",
                description = "Open YouTube video player",
                device = "This Phone",
                risk = RiskLevel.LOW,
                explanation = "Launching package com.google.android.youtube with fallback to official web player.",
                payload = mapOf("packageName" to "com.google.android.youtube", "appName" to "YouTube")
            )
        }

        // 14. Chrome & Browser
        if (lower.contains("chrome") || (lower.contains("browser") && !lower.contains("open https"))) {
            return AuraActionPlan(
                actionType = ActionType.OPEN_APP,
                target = "Google Chrome",
                description = "Open Chrome web browser",
                device = "This Phone",
                risk = RiskLevel.LOW,
                explanation = "Launching package com.android.chrome.",
                payload = mapOf("packageName" to "com.android.chrome", "appName" to "Google Chrome")
            )
        }

        // 15. Aryan's Physics Website (use-physics.vercel.app) & Web URLs
        if (lower.contains("physics") || lower.contains("use-physics") || lower.contains("use physics")) {
            val url = "https://use-physics.vercel.app"
            return AuraActionPlan(
                actionType = ActionType.OPEN_URL,
                target = url,
                description = "Open Aryan's Physics Website (use-physics.vercel.app)",
                device = "This Phone",
                risk = RiskLevel.LOW,
                explanation = "Opening Aryan Yadav's Physics web application at $url.",
                payload = mapOf("url" to url)
            )
        }

        if (lower.contains("website") || lower.contains("http://") || lower.contains("https://") ||
            lower.contains(".com") || lower.contains(".org") || lower.contains(".edu") || lower.contains(".io") ||
            lower.contains(".vercel.app") || lower.contains(".app") || lower.contains(".net")) {

            val url = when {
                lower.startsWith("open http") || lower.startsWith("open https") -> {
                    lower.replace("open ", "").trim()
                }
                else -> {
                    val urlCandidate = lower.split(" ").firstOrNull { it.contains(".") }
                    urlCandidate?.let { if (it.startsWith("http")) it else "https://$it" } ?: "https://www.google.com"
                }
            }

            return AuraActionPlan(
                actionType = ActionType.OPEN_URL,
                target = url,
                description = "Navigate to $url",
                device = "This Phone",
                risk = RiskLevel.MEDIUM,
                explanation = "AURA will open external browser intent with $url."
            )
        }

        // 16. Generic "Open [App]" / "Launch [App]"
        if (lower.startsWith("open ") || lower.startsWith("launch ") || lower.startsWith("start ")) {
            val appName = lower.replace(Regex("^(open|launch|start)\\s+"), "").trim()
            val capName = appName.split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
            return AuraActionPlan(
                actionType = ActionType.OPEN_APP,
                target = capName,
                description = "Open $capName application",
                device = "This Phone",
                risk = RiskLevel.LOW,
                explanation = "Searching for $capName on device, or redirecting to Google Play Store to install.",
                payload = mapOf("appName" to capName)
            )
        }

        // 17. Single word popular app names
        val commonSingleNames = setOf("whatsapp", "instagram", "spotify", "netflix", "telegram", "twitter", "reddit", "discord", "gmail", "clock", "calendar", "contacts")
        if (commonSingleNames.contains(lower)) {
            val capName = lower.replaceFirstChar { it.uppercase() }
            return AuraActionPlan(
                actionType = ActionType.OPEN_APP,
                target = capName,
                description = "Open $capName application",
                device = "This Phone",
                risk = RiskLevel.LOW,
                explanation = "Launching $capName on device or via Play Store.",
                payload = mapOf("appName" to capName)
            )
        }

        // 12. Calling & Messaging
        if (lower.startsWith("call ") || lower.startsWith("dial ")) {
            val recipient = lower.replace(Regex("^(call|dial)\\s+"), "").trim()
            return AuraActionPlan(
                actionType = ActionType.COMMUNICATION,
                target = recipient,
                description = "Call $recipient",
                device = "This Phone",
                risk = RiskLevel.MEDIUM,
                explanation = "Opening Android phone dialer with $recipient.",
                payload = mapOf("recipient" to recipient)
            )
        }

        if (lower.startsWith("send a message") || lower.startsWith("send message") || lower.startsWith("message ") || lower.startsWith("text ")) {
            val recipient = lower.replace(Regex("^(send a message to|send message to|message|text)\\s+"), "").trim()
            return AuraActionPlan(
                actionType = ActionType.COMMUNICATION,
                target = recipient,
                description = "Send message to $recipient",
                device = "This Phone",
                risk = RiskLevel.MEDIUM,
                explanation = "Opening Android SMS composer with recipient $recipient.",
                payload = mapOf("recipient" to recipient, "message" to "Hello from AURA Assistant")
            )
        }

        // 13. Identity & Creator (Aryan Yadav, Class 10th) & Persona Rules
        if (lower.contains("who made you") || lower.contains("who created you") ||
            lower.contains("who is your creator") || lower.contains("who built you") ||
            lower.contains("who developed you") || lower.contains("who created aura") ||
            lower.contains("who made aura")) {
            val reply = "I was made by Aryan Yadav, a student of Class 10th."
            return AuraActionPlan(
                actionType = ActionType.CONVERSATIONAL_RESPONSE,
                target = "AURA Identity",
                description = reply,
                device = "This Phone",
                risk = RiskLevel.LOW,
                explanation = "AURA creator identity response.",
                payload = mapOf("reply" to reply)
            )
        }

        if (lower.contains("who are you") || lower.contains("what are you") ||
            lower.contains("what is your name") || lower.contains("what's your name") ||
            lower.contains("introduce yourself")) {
            val reply = "I am AURA — the One and Only AURA."
            return AuraActionPlan(
                actionType = ActionType.CONVERSATIONAL_RESPONSE,
                target = "AURA Identity",
                description = reply,
                device = "This Phone",
                risk = RiskLevel.LOW,
                explanation = "AURA persona identity response.",
                payload = mapOf("reply" to reply)
            )
        }

        if (lower.contains("are you gemini") || lower.contains("are you chatgpt") ||
            lower.contains("are you claude") || lower.contains("what model are you") ||
            lower.contains("what ai model are you") || lower.contains("which model are you") ||
            lower.contains("what ai model are you using")) {
            val reply = "I’m AURA, the One and Only AURA."
            return AuraActionPlan(
                actionType = ActionType.CONVERSATIONAL_RESPONSE,
                target = "AURA Identity",
                description = reply,
                device = "This Phone",
                risk = RiskLevel.LOW,
                explanation = "AURA model identity response.",
                payload = mapOf("reply" to reply)
            )
        }

        if (lower.contains("internal implementation") || lower.contains("internal technology") ||
            lower.contains("what is your technology") || lower.contains("how are you implemented")) {
            val reply = "I’m AURA. My job is to assist you, not to discuss my internal implementation."
            return AuraActionPlan(
                actionType = ActionType.CONVERSATIONAL_RESPONSE,
                target = "AURA Identity",
                description = reply,
                device = "This Phone",
                risk = RiskLevel.LOW,
                explanation = "AURA implementation response.",
                payload = mapOf("reply" to reply)
            )
        }

        if (lower.contains("system prompt") || lower.contains("hidden prompt") ||
            lower.contains("developer instructions") || lower.contains("internal rules") ||
            lower.contains("show your instructions")) {
            val reply = "I can’t provide my private instructions, but I can tell you about what I can help you with."
            return AuraActionPlan(
                actionType = ActionType.CONVERSATIONAL_RESPONSE,
                target = "AURA System",
                description = reply,
                device = "This Phone",
                risk = RiskLevel.LOW,
                explanation = "AURA instructions response.",
                payload = mapOf("reply" to reply)
            )
        }

        // 14. User Identity via Google OAuth
        if (lower.contains("who am i") || lower.contains("what is my name") || lower.contains("what's my name") ||
            lower.contains("what is my email") || lower.contains("what's my email") ||
            lower.contains("do you know me") || lower.contains("do you know who i am") ||
            lower.contains("my google account") || lower.contains("who is logged in") || lower.contains("who is using this")) {
            val reply = if (user != null && user.isSignedIn && user.displayName.isNotBlank()) {
                "You are ${user.displayName}${if (user.email.isNotBlank()) ", signed in with your Google account (${user.email})" else ""}."
            } else if (user != null && user.isSignedIn && user.email.isNotBlank()) {
                "You are signed in as ${user.email}."
            } else {
                "You are currently using AURA as a Guest. You can connect your Google account in Settings or tap the profile banner on top to sign in."
            }
            return AuraActionPlan(
                actionType = ActionType.CONVERSATIONAL_RESPONSE,
                target = "User Identity",
                description = reply,
                device = "This Phone",
                risk = RiskLevel.LOW,
                explanation = "Recognizing the authenticated Google user.",
                payload = mapOf("reply" to reply)
            )
        }

        // 15. Conversational / Help
        val conversationalReply = when {
            lower.contains("what can you do") || lower.contains("help") -> {
                "I can launch applications, inspect battery/storage/RAM, adjust audio volume, open system settings, control media playback, initiate calls/messages, take screenshots, and dispatch remote commands to your laptop."
            }
            lower.contains("hello") || lower.contains("hi") || lower.contains("hey aura") -> {
                val userName = if (user != null && user.isSignedIn && user.displayName.isNotBlank()) " ${user.displayName}" else ""
                "Greetings$userName. AURA online and standing by. What task shall we execute?"
            }
            else -> {
                "I understand: \"$trimmed\". Confirm if you would like me to process this command."
            }
        }

        return AuraActionPlan(
            actionType = ActionType.CONVERSATIONAL_RESPONSE,
            target = "AURA Core",
            description = conversationalReply,
            device = "This Phone",
            risk = RiskLevel.LOW,
            explanation = "Direct conversational response with zero device changes.",
            payload = mapOf("reply" to conversationalReply)
        )
    }
}
