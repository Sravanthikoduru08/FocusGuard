package com.example.focusguard.engine

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Seeds 10 days of realistic sample productivity data into SharedPreferences
 * on first app launch. This ensures the Teacher Report and focusguard_data.txt
 * have meaningful data to show immediately.
 *
 * Only runs ONCE — controlled by the "sample_data_seeded" flag.
 */
object SampleDataSeeder {

    fun seedIfNeeded(context: Context) {
        val prefs = context.getSharedPreferences("FocusGuardProductivity", Context.MODE_PRIVATE)
        val alreadySeeded = prefs.getBoolean("sample_data_seeded", false)
        if (alreadySeeded) return  // Never seed twice

        // ── Sample Daily Logs (last 10 days) ──
        val dailyLogs = JSONArray()

        val days = listOf(
            // date,        studyMin, sessionsCompleted, avgFocus, blockedAttempts
            Triple("2026-06-08", mapOf("study_minutes" to 0,  "sessions_completed" to 0, "avg_focus" to 0,  "focus_readings" to 0, "blocked_attempts" to 0)),
            Triple("2026-06-09", mapOf("study_minutes" to 35, "sessions_completed" to 1, "avg_focus" to 70, "focus_readings" to 7, "blocked_attempts" to 3)),
            Triple("2026-06-10", mapOf("study_minutes" to 50, "sessions_completed" to 2, "avg_focus" to 74, "focus_readings" to 10,"blocked_attempts" to 2)),
            Triple("2026-06-11", mapOf("study_minutes" to 20, "sessions_completed" to 1, "avg_focus" to 68, "focus_readings" to 4, "blocked_attempts" to 4)),
            Triple("2026-06-12", mapOf("study_minutes" to 65, "sessions_completed" to 2, "avg_focus" to 79, "focus_readings" to 13,"blocked_attempts" to 1)),
            Triple("2026-06-13", mapOf("study_minutes" to 0,  "sessions_completed" to 0, "avg_focus" to 0,  "focus_readings" to 0, "blocked_attempts" to 0)),
            Triple("2026-06-14", mapOf("study_minutes" to 45, "sessions_completed" to 2, "avg_focus" to 75, "focus_readings" to 9, "blocked_attempts" to 2)),
            Triple("2026-06-15", mapOf("study_minutes" to 90, "sessions_completed" to 3, "avg_focus" to 82, "focus_readings" to 18,"blocked_attempts" to 3)),
            Triple("2026-06-16", mapOf("study_minutes" to 75, "sessions_completed" to 3, "avg_focus" to 80, "focus_readings" to 15,"blocked_attempts" to 5)),
            Triple("2026-06-17", mapOf("study_minutes" to 55, "sessions_completed" to 2, "avg_focus" to 78, "focus_readings" to 11,"blocked_attempts" to 3))
        )

        days.forEach { (date, data) ->
            val obj = JSONObject()
            obj.put("date", date)
            data.forEach { (k, v) -> obj.put(k, v) }
            dailyLogs.put(obj)
        }

        // ── Aggregate totals from daily logs ──
        val totalStudyMs     = (35 + 50 + 20 + 65 + 45 + 90 + 75 + 55).toLong() * 60 * 1000  // in ms
        val totalSessions    = 1 + 2 + 1 + 2 + 2 + 3 + 3 + 2  // 16 completed
        val totalStarted     = totalSessions + 2                 // 18 started (2 aborted)
        val totalAborted     = 2
        val totalBlocked     = 3 + 2 + 4 + 1 + 2 + 3 + 5 + 3   // 23
        val totalFocusTotal  = (70*7 + 74*10 + 68*4 + 79*13 + 75*9 + 82*18 + 80*15 + 78*11).toLong()
        val totalFocusCount  = 7 + 10 + 4 + 13 + 9 + 18 + 15 + 11  // 87 readings

        val topics = setOf(
            "Python Basics",
            "Data Structures",
            "Machine Learning",
            "Operating Systems",
            "Database Management",
            "Artificial Intelligence",
            "Software Engineering"
        )

        prefs.edit()
            .putInt("app_open_count", 28)
            .putInt("sessions_started", totalStarted)
            .putInt("sessions_completed", totalSessions)
            .putInt("sessions_aborted", totalAborted)
            .putLong("total_study_time_ms", totalStudyMs)
            .putInt("blocked_attempts", totalBlocked)
            .putInt("recovery_sessions", 5)
            .putStringSet("topics_studied", topics)
            .putInt("focus_readings_count", totalFocusCount)
            .putLong("focus_score_total", totalFocusTotal)
            .putString("daily_logs", dailyLogs.toString())
            .putBoolean("sample_data_seeded", true)
            .apply()

        // Also seed streak + XP into FocusGuardPrefs
        val focusPrefs = context.getSharedPreferences("FocusGuardPrefs", Context.MODE_PRIVATE)
        if (focusPrefs.getInt("streak", 0) == 0) {
            focusPrefs.edit()
                .putInt("streak", 8)
                .putInt("xp", 3250)
                .putInt("focus_level", 78)
                .putInt("dopamine_level", 35)
                .apply()
        }

        // Write the .txt file immediately so it's ready to show
        ProductivityTracker.updateFocusScoreSnapshot(context, 78)
    }
}
