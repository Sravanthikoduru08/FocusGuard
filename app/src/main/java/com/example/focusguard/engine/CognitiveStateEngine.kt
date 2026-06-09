package com.example.focusguard.engine

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class BrainState {
    CALM,       // Cyan glow
    BUSY,       // Purple balance
    FRAGMENTED, // Red overload
    RECOVERY    // Green healing
}

object CognitiveStateEngine {
    private val _currentStatus = MutableStateFlow(BrainState.CALM)
    val currentStatus: StateFlow<BrainState> get() = _currentStatus

    private val _widgetMessage = MutableStateFlow<String?>(null)
    val widgetMessage: StateFlow<String?> get() = _widgetMessage

    // User Customizations
    private val _blockedApps = MutableStateFlow<Set<String>>(emptySet())
    val blockedApps: StateFlow<Set<String>> get() = _blockedApps

    fun loadBlockedApps(context: android.content.Context) {
        val prefs = context.getSharedPreferences("FocusGuardPrefs", android.content.Context.MODE_PRIVATE)
        val savedApps = prefs.getStringSet("blocked_apps", setOf(
            "com.instagram.android", 
            "com.facebook.katana", 
            "com.ss.android.ugc.trill", 
            "com.google.android.youtube"
        )) ?: emptySet()
        _blockedApps.value = savedApps
    }

    private fun saveBlockedApps(context: android.content.Context, apps: Set<String>) {
        val prefs = context.getSharedPreferences("FocusGuardPrefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().putStringSet("blocked_apps", apps).apply()
    }

    private val _customBlockMessage = MutableStateFlow("Your mind is currently in a high-focus protocol. This environment may disrupt your stabilization.")
    val customBlockMessage: StateFlow<String> get() = _customBlockMessage

    private val _isStudyModeActive = MutableStateFlow(false)
    val isStudyModeActive: StateFlow<Boolean> get() = _isStudyModeActive

    // App Usage Tracking
    private val _appUsageMap = MutableStateFlow<Map<String, Long>>(emptyMap())
    val appUsageMap: StateFlow<Map<String, Long>> get() = _appUsageMap

    fun syncRealUsage(context: android.content.Context) {
        if (!UsageTrackingManager.hasUsageAccessPermission(context)) return
        
        val realStats = UsageTrackingManager.getTodayUsageStats(context)
        _appUsageMap.value = realStats
        
        // Update overloadScore based on real data
        calculateOverloadFromUsage(realStats)
    }

    private fun calculateOverloadFromUsage(stats: Map<String, Long>) {
        var newOverload = 0
        var totalMindlessMillis = 0L
        
        val blocked = _blockedApps.value
        
        stats.forEach { (pkg, time) ->
            if (pkg in blocked) {
                // For every 5 minutes on a blocked app, add 10 to overload
                val mins = (time / 60000).toInt()
                newOverload += (mins / 5) * 10
                totalMindlessMillis += time
            }
        }
        
        overloadScore = newOverload.coerceIn(0, 150)
        totalMindlessTimeMillis = totalMindlessMillis
        updateBrainState()
    }

    private var overloadScore = 0
    private var totalMindlessTimeMillis = 0L
    private var lastActivityTime = 0L

    // Milestones to avoid repeating the same message
    private var lastMilestoneReached = 0

    private var isRecovering = false

    fun reportActivity(packageName: String?, isMindless: Boolean) {
        val currentTime = System.currentTimeMillis()
        
        // Late-night detection (Section 2)
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        if (hour >= 23 || hour <= 4) {
            if (isMindless && lastMilestoneReached < 100) {
                _widgetMessage.value = "Neural overstimulation detected in nocturnal hours. Stabilization recommended."
                lastMilestoneReached = 100 // Special nocturnal milestone
            }
        }

        if (lastActivityTime != 0L) {
            val delta = currentTime - lastActivityTime
            if (delta < 5000) {
                val pkg = packageName ?: "Other"
                
                // Only update usage map every 2 seconds to avoid flow flooding
                if (currentTime - lastMapUpdateTime > 2000) {
                    val currentUsage = _appUsageMap.value.toMutableMap()
                    currentUsage[pkg] = (currentUsage[pkg] ?: 0L) + (currentTime - lastMapUpdateTime)
                    _appUsageMap.value = currentUsage
                    lastMapUpdateTime = currentTime
                }

                if (isMindless) {
                    totalMindlessTimeMillis += delta
                    
                    val mins = (totalMindlessTimeMillis / 60000).toInt()
                    
                    // Emotional AI Milestones
                    if (mins >= 5 && lastMilestoneReached < 5) {
                        _widgetMessage.value = "Neural activity patterns suggest rising fatigue. Maybe take a breath?"
                        lastMilestoneReached = 5
                    } else if (mins >= 15 && lastMilestoneReached < 15) {
                        _widgetMessage.value = "Attention fragmentation increasing. Consider a neural reset."
                        lastMilestoneReached = 15
                    } else if (mins >= 30 && lastMilestoneReached < 30) {
                        _widgetMessage.value = "Passive consumption detected for 30m. Your goals are waiting for you."
                        lastMilestoneReached = 30
                    } else if (mins >= 40 && lastMilestoneReached < 40) {
                        _widgetMessage.value = "You've been consuming without resting for 40 mins. Shall we stabilize?"
                        lastMilestoneReached = 40
                    }
                }
            }
        }
        lastActivityTime = currentTime

        if (isMindless) {
            overloadScore += 5
            isRecovering = false // Activity breaks recovery
        } else {
            if (overloadScore > 0) overloadScore -= 1
        }

        updateBrainState()
    }

    private var lastMapUpdateTime = 0L

    fun initiateRecovery() {
        isRecovering = true
        _widgetMessage.value = "Initiating neural stabilization. Focus on your breath."
        updateBrainState()
    }

    fun completeRecovery() {
        overloadScore = 0
        totalMindlessTimeMillis = 0
        lastMilestoneReached = 0
        isRecovering = false
        _widgetMessage.value = "Neural pathways stabilized. You are ready."
        updateBrainState()
    }

    fun clearMessage() {
        _widgetMessage.value = null
    }

    fun setCustomBlockMessage(message: String) {
        _customBlockMessage.value = message
    }

    fun addBlockedApp(context: android.content.Context, packageName: String) {
        val newSet = _blockedApps.value + packageName
        _blockedApps.value = newSet
        saveBlockedApps(context, newSet)
    }

    fun removeBlockedApp(context: android.content.Context, packageName: String) {
        val newSet = _blockedApps.value - packageName
        _blockedApps.value = newSet
        saveBlockedApps(context, newSet)
    }

    fun clearBlockedApps(context: android.content.Context) {
        _blockedApps.value = emptySet()
        saveBlockedApps(context, emptySet())
    }

    fun setStudyModeActive(active: Boolean) {
        _isStudyModeActive.value = active
        if (active) {
            // Reset mindless tracking when starting a fresh study session
            totalMindlessTimeMillis = 0
            lastMilestoneReached = 0
            isRecovering = false
        }
    }

    private fun updateBrainState() {
        val newState = when {
            isRecovering -> BrainState.RECOVERY
            overloadScore > 100 -> BrainState.FRAGMENTED
            overloadScore > 50 -> BrainState.BUSY
            else -> BrainState.CALM
        }
        
        if (newState != _currentStatus.value) {
            _currentStatus.value = newState
        }
    }
}
