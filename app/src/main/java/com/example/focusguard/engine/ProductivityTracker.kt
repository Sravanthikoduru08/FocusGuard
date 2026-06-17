package com.example.focusguard.engine

import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.Timestamp
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

data class ProductivityStats(
    val appOpenCount: Int,
    val studySessionsStarted: Int,
    val studySessionsCompleted: Int,
    val studySessionsAborted: Int,
    val totalStudyTimeMs: Long,
    val blockedAppAttempts: Int,
    val recoverySessions: Int,
    val topicsStudied: List<String>,
    val currentStreak: Int,
    val totalXp: Int,
    val avgFocusScore: Int,
    val dailyLogs: List<DailyLog>
)

data class DailyLog(
    val date: String,
    val studyMinutes: Int,
    val sessionsCompleted: Int,
    val avgFocus: Int,
    val blockedAttempts: Int
)

object ProductivityTracker {
    private const val PREFS_NAME = "FocusGuardProductivity"

    // ─── File location ───
    // Expected path: /sdcard/Android/data/com.example.focusguard/files/focusguard_data.txt
    private fun getDataFile(context: Context): File {
        val externalDir = context.getExternalFilesDir(null)
        val dir = if (externalDir != null && externalDir.exists()) {
            externalDir
        } else {
            Log.w("ProductivityTracker", "External storage unavailable, falling back to internal storage")
            context.filesDir
        }
        val file = File(dir, "focusguard_data.txt")
        Log.d("ProductivityTracker", "Data file path: ${file.absolutePath}")
        return file
    }

    // ─── Auto-saves readable .txt every time data changes ───
    fun saveToTextFile(context: Context) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val focusPrefs = context.getSharedPreferences("FocusGuardPrefs", Context.MODE_PRIVATE)

            val appOpens = prefs.getInt("app_open_count", 0)
            val started = prefs.getInt("sessions_started", 0)
            val completed = prefs.getInt("sessions_completed", 0)
            val aborted = prefs.getInt("sessions_aborted", 0)
            val totalStudyMs = prefs.getLong("total_study_time_ms", 0L)
            val blocked = prefs.getInt("blocked_attempts", 0)
            val recovery = prefs.getInt("recovery_sessions", 0)
            val topics = (prefs.getStringSet("topics_studied", emptySet()) ?: emptySet()).toList().sorted()
            val streak = focusPrefs.getInt("streak", 0)
            val xp = focusPrefs.getInt("xp", 0)
            val focusReadings = prefs.getInt("focus_readings_count", 1).coerceAtLeast(1)
            val focusTotal = prefs.getLong("focus_score_total", 75L)
            val avgFocus = (focusTotal / focusReadings).toInt().coerceIn(0, 100)
            val completionRate = if (started > 0) (completed * 100 / started) else 0
            val studyHours = totalStudyMs / 3600000.0

            val logsJson = prefs.getString("daily_logs", "[]") ?: "[]"
            val logsArray = JSONArray(logsJson)

            val now = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
            val sb = StringBuilder()

            sb.appendLine("================================================")
            sb.appendLine("        FocusGuard - User Productivity Data")
            sb.appendLine("================================================")
            sb.appendLine("Last Updated : $now")
            sb.appendLine()
            sb.appendLine("--- General Stats ---")
            sb.appendLine("App Opens              : $appOpens times")
            sb.appendLine("Total Study Time       : ${String.format("%.2f", studyHours)} hours")
            sb.appendLine("Current Streak         : $streak days")
            sb.appendLine("Total XP Earned        : $xp XP")
            sb.appendLine("Average Focus Score    : $avgFocus%")
            sb.appendLine()
            sb.appendLine("--- Study Sessions ---")
            sb.appendLine("Sessions Started       : $started")
            sb.appendLine("Sessions Completed     : $completed")
            sb.appendLine("Sessions Aborted       : $aborted")
            sb.appendLine("Completion Rate        : $completionRate%")
            sb.appendLine()
            sb.appendLine("--- Distraction Control ---")
            sb.appendLine("Blocked App Attempts   : $blocked times")
            sb.appendLine("Recovery Sessions Done : $recovery")
            sb.appendLine()
            sb.appendLine("--- Topics Studied ---")
            if (topics.isEmpty()) {
                sb.appendLine("  (No sessions recorded yet)")
            } else {
                topics.forEachIndexed { i, t -> sb.appendLine("  ${i + 1}. $t") }
            }
            sb.appendLine()
            sb.appendLine("--- Daily Log (Last 10 Days) ---")
            sb.appendLine(String.format("%-12s | %-10s | %-10s | %-9s | %-10s",
                "Date", "Study Min", "Sessions", "Focus%", "Blocked"))
            sb.appendLine("--------------------------------------------------------------")
            if (logsArray.length() == 0) {
                sb.appendLine("  (No daily data yet)")
            } else {
                for (i in 0 until logsArray.length()) {
                    val log = logsArray.getJSONObject(i)
                    val date = log.getString("date")
                    val studyMin = log.getInt("study_minutes")
                    val sessComp = log.getInt("sessions_completed")
                    val focus = log.optInt("avg_focus", 0)
                    val blockedDay = log.getInt("blocked_attempts")
                    sb.appendLine(String.format("%-12s | %-10d | %-10d | %-9d | %-10d",
                        date, studyMin, sessComp, focus, blockedDay))
                }
            }
            sb.appendLine()
            sb.appendLine("================================================")
            sb.appendLine("File Location: ${getDataFile(context).absolutePath}")
            sb.appendLine("================================================")

            val dataFile = getDataFile(context)
            FileWriter(dataFile, false).use { it.write(sb.toString()) }
            Log.d("ProductivityTracker", "File written successfully: ${dataFile.absolutePath} (${dataFile.length()} bytes)")
            // Notify MediaStore so file managers can see the file immediately
            try {
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(dataFile.absolutePath),
                    arrayOf("text/plain")
                ) { path, uri ->
                    Log.d("ProductivityTracker", "MediaStore scan complete: $path -> $uri")
                }
            } catch (scanEx: Exception) {
                Log.w("ProductivityTracker", "MediaStore scan failed (non-critical): ${scanEx.message}")
            }

            // Sync with Firebase
            val stats = getStats(context, avgFocus, streak, xp)
            syncToFirebase(context, stats)
        } catch (e: Exception) {
            Log.e("ProductivityTracker", "Failed to write data file: ${e.message}")
            e.printStackTrace()
        }
    }

    // ─── Public API ───

    fun incrementAppOpen(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val count = prefs.getInt("app_open_count", 0) + 1
        prefs.edit().putInt("app_open_count", count).apply()
        saveToTextFile(context)
    }

    fun recordStudySessionStart(context: Context, topic: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val started = prefs.getInt("sessions_started", 0) + 1
        val topics = (prefs.getStringSet("topics_studied", mutableSetOf()) ?: mutableSetOf()).toMutableSet()
        topics.add(topic)
        prefs.edit()
            .putInt("sessions_started", started)
            .putStringSet("topics_studied", topics)
            .putLong("current_session_start", System.currentTimeMillis())
            .apply()
        saveToTextFile(context)
    }

    fun recordStudySessionComplete(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val completed = prefs.getInt("sessions_completed", 0) + 1
        val startTime = prefs.getLong("current_session_start", System.currentTimeMillis())
        val sessionDurationMs = System.currentTimeMillis() - startTime
        val totalStudyTime = prefs.getLong("total_study_time_ms", 0L) + sessionDurationMs
        prefs.edit()
            .putInt("sessions_completed", completed)
            .putLong("total_study_time_ms", totalStudyTime)
            .apply()
        updateDailyLog(context, sessionDurationMs, completedSession = true, blockedDelta = 0)
        saveToTextFile(context)
    }

    fun recordStudySessionAbort(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val aborted = prefs.getInt("sessions_aborted", 0) + 1
        prefs.edit().putInt("sessions_aborted", aborted).apply()
        saveToTextFile(context)
    }

    fun recordBlockedAppAttempt(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val attempts = prefs.getInt("blocked_attempts", 0) + 1
        prefs.edit().putInt("blocked_attempts", attempts).apply()
        updateDailyLog(context, 0L, completedSession = false, blockedDelta = 1)
        saveToTextFile(context)
    }

    fun recordRecoverySession(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val sessions = prefs.getInt("recovery_sessions", 0) + 1
        prefs.edit().putInt("recovery_sessions", sessions).apply()
        saveToTextFile(context)
    }

    fun updateFocusScoreSnapshot(context: Context, focusScore: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val count = prefs.getInt("focus_readings_count", 0) + 1
        val total = prefs.getLong("focus_score_total", 0L) + focusScore
        prefs.edit()
            .putInt("focus_readings_count", count)
            .putLong("focus_score_total", total)
            .apply()
        updateTodayFocusAvg(context, focusScore)
        // Save txt only occasionally (every 10 readings) to avoid too-frequent writes
        if (count % 10 == 0) saveToTextFile(context)
    }

    private fun updateTodayFocusAvg(context: Context, focusScore: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val logsJson = prefs.getString("daily_logs", "[]") ?: "[]"
        val logsArray = JSONArray(logsJson)
        for (i in 0 until logsArray.length()) {
            val log = logsArray.getJSONObject(i)
            if (log.getString("date") == today) {
                val oldAvg = log.optInt("avg_focus", focusScore)
                val readings = log.optInt("focus_readings", 0) + 1
                val newAvg = ((oldAvg * (readings - 1)) + focusScore) / readings
                log.put("avg_focus", newAvg)
                log.put("focus_readings", readings)
                prefs.edit().putString("daily_logs", logsArray.toString()).apply()
                return
            }
        }
    }

    private fun updateDailyLog(context: Context, studyDurationMs: Long, completedSession: Boolean, blockedDelta: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val logsJson = prefs.getString("daily_logs", "[]") ?: "[]"
        val logsArray = JSONArray(logsJson)

        var todayLog: JSONObject? = null
        var todayIndex = -1
        for (i in 0 until logsArray.length()) {
            val log = logsArray.getJSONObject(i)
            if (log.getString("date") == today) {
                todayLog = log; todayIndex = i; break
            }
        }
        if (todayLog == null) {
            todayLog = JSONObject().apply {
                put("date", today); put("study_minutes", 0)
                put("sessions_completed", 0); put("avg_focus", 75)
                put("focus_readings", 0); put("blocked_attempts", 0)
            }
            logsArray.put(todayLog)
        }
        todayLog.put("study_minutes", todayLog.getInt("study_minutes") + (studyDurationMs / 60000).toInt())
        if (completedSession) todayLog.put("sessions_completed", todayLog.getInt("sessions_completed") + 1)
        todayLog.put("blocked_attempts", todayLog.getInt("blocked_attempts") + blockedDelta)
        if (todayIndex >= 0) logsArray.remove(todayIndex)
        logsArray.put(todayLog)

        val trimmedArray = JSONArray()
        val startIdx = if (logsArray.length() > 10) logsArray.length() - 10 else 0
        for (i in startIdx until logsArray.length()) trimmedArray.put(logsArray.get(i))
        prefs.edit().putString("daily_logs", trimmedArray.toString()).apply()
    }

    fun getStats(context: Context, focusLevel: Int, streak: Int, xp: Int): ProductivityStats {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val logsJson = prefs.getString("daily_logs", "[]") ?: "[]"
        val logsArray = JSONArray(logsJson)
        val dailyLogs = mutableListOf<DailyLog>()
        for (i in 0 until logsArray.length()) {
            val log = logsArray.getJSONObject(i)
            dailyLogs.add(DailyLog(
                date = log.getString("date"),
                studyMinutes = log.getInt("study_minutes"),
                sessionsCompleted = log.getInt("sessions_completed"),
                avgFocus = log.optInt("avg_focus", 75),
                blockedAttempts = log.getInt("blocked_attempts")
            ))
        }
        val totalReadings = prefs.getInt("focus_readings_count", 1).coerceAtLeast(1)
        val totalScore = prefs.getLong("focus_score_total", focusLevel.toLong())
        return ProductivityStats(
            appOpenCount = prefs.getInt("app_open_count", 0),
            studySessionsStarted = prefs.getInt("sessions_started", 0),
            studySessionsCompleted = prefs.getInt("sessions_completed", 0),
            studySessionsAborted = prefs.getInt("sessions_aborted", 0),
            totalStudyTimeMs = prefs.getLong("total_study_time_ms", 0L),
            blockedAppAttempts = prefs.getInt("blocked_attempts", 0),
            recoverySessions = prefs.getInt("recovery_sessions", 0),
            topicsStudied = ((prefs.getStringSet("topics_studied", emptySet()) ?: emptySet())).toList(),
            currentStreak = streak,
            totalXp = xp,
            avgFocusScore = (totalScore / totalReadings).toInt().coerceIn(0, 100),
            dailyLogs = dailyLogs
        )
    }

    /**
     * Returns the path of the auto-saved .txt file so the UI can display it.
     */
    fun getTextFilePath(context: Context): String = getDataFile(context).absolutePath

    /**
     * Exports all productivity data as a JSON file to the Downloads folder.
     */
    fun exportToJson(context: Context, stats: ProductivityStats, studentName: String): File? {
        return try {
            val completionRate = if (stats.studySessionsStarted > 0)
                (stats.studySessionsCompleted * 100 / stats.studySessionsStarted) else 0
            val json = JSONObject().apply {
                put("report_title", "FocusGuard Productivity Report")
                put("student_name", studentName)
                put("exported_at", SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date()))
                put("app_open_count", stats.appOpenCount)
                put("study_sessions_started", stats.studySessionsStarted)
                put("study_sessions_completed", stats.studySessionsCompleted)
                put("study_sessions_aborted", stats.studySessionsAborted)
                put("completion_rate_percent", completionRate)
                put("total_study_hours", String.format("%.1f", stats.totalStudyTimeMs / 3600000.0))
                put("blocked_app_attempts", stats.blockedAppAttempts)
                put("recovery_sessions", stats.recoverySessions)
                put("current_streak_days", stats.currentStreak)
                put("total_xp", stats.totalXp)
                put("avg_focus_score", stats.avgFocusScore)
                put("topics_studied", JSONArray(stats.topicsStudied))
                val dailyArray = JSONArray()
                stats.dailyLogs.forEach { log ->
                    dailyArray.put(JSONObject().apply {
                        put("date", log.date); put("study_minutes", log.studyMinutes)
                        put("sessions_completed", log.sessionsCompleted)
                        put("avg_focus", log.avgFocus); put("blocked_attempts", log.blockedAttempts)
                    })
                }
                put("daily_logs", dailyArray)
            }
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val fileName = "FocusGuard_Report_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())}.json"
            val file = File(downloadsDir, fileName)
            FileWriter(file).use { it.write(json.toString(2)) }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun syncToFirebase(context: Context, stats: ProductivityStats) {
        try {
            val db = FirebaseFirestore.getInstance()
            val focusPrefs = context.getSharedPreferences("FocusGuardPrefs", Context.MODE_PRIVATE)
            val studentName = focusPrefs.getString("student_name", "Student") ?: "Student"
            val deviceId = android.provider.Settings.Secure.getString(context.contentResolver, android.provider.Settings.Secure.ANDROID_ID) ?: "unknown_device"

            val payload = hashMapOf(
                "student_name" to studentName,
                "device_id" to deviceId,
                "last_updated" to Timestamp.now(),
                "app_open_count" to stats.appOpenCount,
                "study_sessions_started" to stats.studySessionsStarted,
                "study_sessions_completed" to stats.studySessionsCompleted,
                "study_sessions_aborted" to stats.studySessionsAborted,
                "total_study_time_ms" to stats.totalStudyTimeMs,
                "blocked_app_attempts" to stats.blockedAppAttempts,
                "recovery_sessions" to stats.recoverySessions,
                "current_streak" to stats.currentStreak,
                "total_xp" to stats.totalXp,
                "avg_focus_score" to stats.avgFocusScore,
                "topics_studied" to stats.topicsStudied,
                "daily_logs" to stats.dailyLogs.map { log ->
                    mapOf(
                        "date" to log.date,
                        "study_minutes" to log.studyMinutes,
                        "sessions_completed" to log.sessionsCompleted,
                        "avg_focus" to log.avgFocus,
                        "blocked_attempts" to log.blockedAttempts
                    )
                }
            )

            db.collection("productivity_stats")
                .document(deviceId)
                .set(payload, SetOptions.merge())
                .addOnSuccessListener {
                    Log.d("ProductivityTracker", "Firebase sync successful for device: $deviceId")
                }
                .addOnFailureListener { e ->
                    Log.e("ProductivityTracker", "Firebase sync failed: ${e.message}")
                }
        } catch (e: Exception) {
            Log.e("ProductivityTracker", "Failed to sync with Firebase: ${e.message}")
        }
    }
}
