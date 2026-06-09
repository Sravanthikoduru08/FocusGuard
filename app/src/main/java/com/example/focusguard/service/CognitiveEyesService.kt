package com.example.focusguard.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.example.focusguard.AppBlockActivity
import com.example.focusguard.engine.BrainState
import com.example.focusguard.engine.CognitiveStateEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter

class CognitiveEyesService : AccessibilityService() {

    private var lastScrollTime: Long = 0
    private var scrollCount: Int = 0
    private val TAG = "CognitiveEyes"
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    private var currentBlockedApps = emptySet<String>()

    private val studyStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            // Force a check when study mode starts
            Log.d(TAG, "Study state changed broadcast received. Checking windows...")
            checkAllActiveWindows()
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Cognitive Eyes Connected.")
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(studyStateReceiver, IntentFilter("com.example.focusguard.STUDY_STATE_CHANGED"), Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(studyStateReceiver, IntentFilter("com.example.focusguard.STUDY_STATE_CHANGED"))
        }
        
        // Observe blocked apps changes
        CognitiveStateEngine.blockedApps
            .onEach { currentBlockedApps = it }
            .launchIn(serviceScope)
    }

    private fun checkAllActiveWindows() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            val windows = windows
            for (window in windows) {
                val root = window.root
                if (root != null) {
                    checkAndBlock(root.packageName?.toString())
                    root.recycle()
                }
            }
        } else {
            val rootNode = rootInActiveWindow ?: return
            checkAndBlock(rootNode.packageName?.toString())
        }
    }

    private var lastReportTime: Long = 0
    private var lastPackageName: String? = null
    private var appSwitchCount: Int = 0
    private var lastSwitchTime: Long = 0

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString()
        val currentTime = System.currentTimeMillis()

        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
                handleScrollEvent(packageName)
                checkAndBlock(packageName)
            }
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                // Detect Rapid App Switching (Section 2)
                if (packageName != lastPackageName && packageName != this.packageName) {
                    val timeSinceLastSwitch = currentTime - lastSwitchTime
                    if (timeSinceLastSwitch < 3000) { // Switched apps in less than 3 seconds
                        appSwitchCount++
                        if (appSwitchCount > 3) {
                            Log.i(TAG, "Rapid app switching detected. Focus loop identified.")
                            CognitiveStateEngine.reportActivity(packageName, isMindless = true)
                            appSwitchCount = 0
                        }
                    } else {
                        appSwitchCount = 0
                    }
                    lastSwitchTime = currentTime
                    lastPackageName = packageName
                }

                CognitiveStateEngine.reportActivity(packageName, isMindless = false)
                checkAndBlock(packageName)
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                // Throttle content changes - they fire way too often
                if (currentTime - lastReportTime > 500) {
                    CognitiveStateEngine.reportActivity(packageName, isMindless = false)
                    lastReportTime = currentTime
                }
                checkAndBlock(packageName)
            }
        }
    }

    private fun checkAndBlock(packageName: String?) {
        if (packageName == null || packageName == this.packageName) return
        
        val currentState = CognitiveStateEngine.currentStatus.value
        val isStudyActive = CognitiveStateEngine.isStudyModeActive.value
        
        if (packageName in currentBlockedApps) {
            if (isStudyActive || currentState == BrainState.FRAGMENTED || currentState == BrainState.BUSY) {
                Log.w(TAG, "Blocking $packageName. StudyActive: $isStudyActive, State: $currentState")
                redirectToBlockScreen()
            }
        }
    }

    private fun redirectToBlockScreen() {
        val intent = Intent(this, AppBlockActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
    }

    private fun handleScrollEvent(packageName: String?) {
        val currentTime = System.currentTimeMillis()
        val timeDiff = currentTime - lastScrollTime

        if (timeDiff < 1000) {
            scrollCount++
            if (scrollCount > 5) {
                CognitiveStateEngine.reportActivity(packageName, isMindless = true)
            }
        } else {
            scrollCount = 0
            CognitiveStateEngine.reportActivity(packageName, isMindless = false)
        }

        lastScrollTime = currentTime
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(studyStateReceiver)
        serviceScope.coroutineContext[Job]?.cancel()
    }
}
