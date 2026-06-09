package com.example.focusguard.engine

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable

data class AppInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable,
    var isBlocked: Boolean = false
)

object AppListManager {
    fun getInstalledApps(context: Context, blockedApps: Set<String>): List<AppInfo> {
        val pm = context.packageManager
        val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        
        val appList = mutableListOf<AppInfo>()
        for (packageInfo in packages) {
            // Filter out system apps, only show user-installed apps (or apps that can be launched)
            if (pm.getLaunchIntentForPackage(packageInfo.packageName) != null) {
                val appName = pm.getApplicationLabel(packageInfo).toString()
                val icon = pm.getApplicationIcon(packageInfo)
                val isBlocked = blockedApps.contains(packageInfo.packageName)
                
                appList.add(AppInfo(packageInfo.packageName, appName, icon, isBlocked))
            }
        }
        
        // Sort alphabetically
        return appList.sortedBy { it.appName.lowercase() }
    }
}
