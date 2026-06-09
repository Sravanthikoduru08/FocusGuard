package com.example.focusguard.engine

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.util.Log
import java.util.Calendar

object UsageTrackingManager {
    private const val TAG = "UsageTracking"

    /**
     * Retrieves the total time spent on each app for the current day.
     * Returns a map of PackageName -> TimeInForeground (in milliseconds)
     */
    fun getTodayUsageStats(context: Context): Map<String, Long> {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        
        // Set the time range for today (from midnight to now)
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()

        // Query the usage stats
        val stats: List<UsageStats>? = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        )

        if (stats.isNullOrEmpty()) {
            Log.w(TAG, "No usage stats found. Does the app have Usage Access permission?")
            return emptyMap()
        }

        val usageMap = mutableMapOf<String, Long>()
        for (stat in stats) {
            if (stat.totalTimeInForeground > 0) {
                usageMap[stat.packageName] = stat.totalTimeInForeground
            }
        }
        
        return usageMap
    }

    /**
     * Checks if the user has granted the Usage Access permission.
     */
    fun hasUsageAccessPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
        val mode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(android.app.AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName)
        } else {
            appOps.checkOpNoThrow(android.app.AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName)
        }
        return mode == android.app.AppOpsManager.MODE_ALLOWED
    }
}
