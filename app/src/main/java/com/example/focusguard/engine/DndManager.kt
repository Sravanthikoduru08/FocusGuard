package com.example.focusguard.engine

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.provider.Settings

object DndManager {

    /**
     * Checks if the app has permission to modify notification policy (DND).
     */
    fun hasDndPermission(context: Context): Boolean {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return notificationManager.isNotificationPolicyAccessGranted
    }

    /**
     * Opens the system settings screen to grant DND permission.
     */
    fun requestDndPermission(context: Context) {
        val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
        context.startActivity(intent)
    }

    /**
     * Enables Do Not Disturb mode.
     */
    fun enableDnd(context: Context) {
        if (hasDndPermission(context)) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
        }
    }

    /**
     * Disables Do Not Disturb mode (sets to allow all).
     */
    fun disableDnd(context: Context) {
        if (hasDndPermission(context)) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
        }
    }
}
