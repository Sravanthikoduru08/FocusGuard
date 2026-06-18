package com.example.focusguard

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.focusguard.databinding.FragmentTeacherReportBinding
import com.example.focusguard.engine.ProductivityTracker
import java.text.SimpleDateFormat
import java.util.*

class LocalUserStorageFragment : Fragment() {

    private var _binding: FragmentTeacherReportBinding? = null
    private val binding get() = _binding!!
    private val viewModel: FocusViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTeacherReportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Observe focus level, streak and XP to build stats
        viewModel.focusLevel.observe(viewLifecycleOwner) { focus ->
            viewModel.streak.value?.let { streak ->
                viewModel.xp.value?.let { xp ->
                    ProductivityTracker.updateFocusScoreSnapshot(requireContext(), focus)
                    loadAndDisplayStats(focus, streak, xp)
                }
            }
        }

        binding.tvExportDate.text = "Generated: ${SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())}"

        // Get student name from prefs
        val prefs = requireContext().getSharedPreferences("FocusGuardPrefs", android.content.Context.MODE_PRIVATE)
        val anchor = prefs.getString("anchor_quote", "") ?: ""
        binding.tvStudentName.text = if (anchor.isNotBlank()) "Student Report" else "Student Report"

        binding.btnExportReport.setOnClickListener {
            exportReport()
        }

        binding.btnShareReport.setOnClickListener {
            shareReportAsText()
        }
    }

    private fun loadAndDisplayStats(focus: Int, streak: Int, xp: Int) {
        val stats = ProductivityTracker.getStats(requireContext(), focus, streak, xp)

        // Stat cards
        binding.tvAppOpenCount.text = stats.appOpenCount.toString()
        binding.tvTotalStudyHours.text = String.format("%.1fh", stats.totalStudyTimeMs / 3600000.0)
        binding.tvStreakDays.text = stats.currentStreak.toString()
        binding.tvTotalXp.text = stats.totalXp.toString()
        binding.tvBlockedAttempts.text = stats.blockedAppAttempts.toString()
        binding.tvRecoverySessions.text = stats.recoverySessions.toString()

        // Completion Rate
        val completionRate = if (stats.studySessionsStarted > 0)
            (stats.studySessionsCompleted * 100 / stats.studySessionsStarted) else 0
        binding.tvCompletionRatePct.text = "$completionRate%"
        binding.pbCompletionRate.progress = completionRate
        binding.tvSessionsStarted.text = "Started: ${stats.studySessionsStarted}"
        binding.tvSessionsCompleted.text = "Done: ${stats.studySessionsCompleted}"
        binding.tvSessionsAborted.text = "Quit: ${stats.studySessionsAborted}"

        // Avg Focus
        binding.tvAvgFocusPct.text = "${stats.avgFocusScore}%"
        binding.pbAvgFocus.progress = stats.avgFocusScore
        binding.tvFocusLabel.text = when {
            stats.avgFocusScore >= 80 -> "Excellent focus 🌟 — High performance student"
            stats.avgFocusScore >= 60 -> "Good focus 👍 — Consistent effort"
            stats.avgFocusScore >= 40 -> "Moderate focus ⚠️ — Room to improve"
            else -> "Low focus 🔴 — Needs attention"
        }

        // 7-Day Bar Chart
        buildBarChart(stats.dailyLogs)

        // Topics
        buildTopicsList(stats.topicsStudied)
    }

    private fun buildBarChart(dailyLogs: List<com.example.focusguard.engine.DailyLog>) {
        binding.llBarChart.removeAllViews()
        binding.llBarLabels.removeAllViews()

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val labelSdf = SimpleDateFormat("EEE", Locale.getDefault())
        val days = (6 downTo 0).map {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -it)
            cal.time
        }

        val maxMinutes = dailyLogs.maxOfOrNull { it.studyMinutes }?.coerceAtLeast(1) ?: 1

        days.forEach { day ->
            val dateKey = sdf.format(day)
            val log = dailyLogs.find { it.date == dateKey }
            val minutes = log?.studyMinutes ?: 0
            val heightFraction = if (maxMinutes > 0) minutes.toFloat() / maxMinutes else 0f

            val barContainer = LinearLayout(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f).also {
                    it.setMargins(4, 0, 4, 0)
                }
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.BOTTOM
            }

            val spacer = View(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f - heightFraction
                )
            }

            val bar = View(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 0,
                    heightFraction.coerceAtLeast(0.02f)
                )
                val color = when {
                    minutes == 0 -> 0x221D1D2E.toInt()
                    minutes >= 30 -> 0xFF00E5FF.toInt()
                    minutes >= 15 -> 0xFFA78BFA.toInt()
                    else -> 0xFF4A4A8A.toInt()
                }
                setBackgroundColor(color)
            }

            barContainer.addView(spacer)
            barContainer.addView(bar)
            binding.llBarChart.addView(barContainer)

            val label = TextView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                text = labelSdf.format(day).take(2)
                textSize = 10f
                setTextColor(0xFF7A7A8C.toInt())
                gravity = Gravity.CENTER
            }
            binding.llBarLabels.addView(label)
        }
    }

    private fun buildTopicsList(topics: List<String>) {
        binding.llTopicsContainer.removeAllViews()
        if (topics.isEmpty()) {
            binding.tvNoTopics.visibility = View.VISIBLE
            return
        }
        binding.tvNoTopics.visibility = View.GONE
        topics.forEachIndexed { i, topic ->
            val tv = TextView(requireContext()).apply {
                text = "${i + 1}.  $topic"
                textSize = 13f
                setTextColor(0xFFE0E0FF.toInt())
                setPadding(0, 6, 0, 6)
            }
            binding.llTopicsContainer.addView(tv)

            if (i < topics.lastIndex) {
                val div = View(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1)
                    setBackgroundColor(0xFF2A2A3E.toInt())
                }
                binding.llTopicsContainer.addView(div)
            }
        }
    }

    private fun exportReport() {
        val focus = viewModel.focusLevel.value ?: 75
        val streak = viewModel.streak.value ?: 0
        val xp = viewModel.xp.value ?: 0
        val stats = ProductivityTracker.getStats(requireContext(), focus, streak, xp)
        val file = ProductivityTracker.exportToJson(requireContext(), stats, "Student")
        if (file != null) {
            Toast.makeText(requireContext(), "✅ Saved to Downloads:\n${file.name}", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(requireContext(), "❌ Export failed. Check storage permission.", Toast.LENGTH_LONG).show()
        }
    }

    private fun shareReportAsText() {
        val focus = viewModel.focusLevel.value ?: 75
        val streak = viewModel.streak.value ?: 0
        val xp = viewModel.xp.value ?: 0
        val stats = ProductivityTracker.getStats(requireContext(), focus, streak, xp)
        val completionRate = if (stats.studySessionsStarted > 0)
            (stats.studySessionsCompleted * 100 / stats.studySessionsStarted) else 0
        val date = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())

        val text = """
📊 *FocusGuard Productivity Report*
📅 Date: $date

━━━━━━━━━━━━━━━━━━━━
📱 App Opens: ${stats.appOpenCount}
⏱️ Total Study Time: ${String.format("%.1f", stats.totalStudyTimeMs / 3600000.0)} hours
🔥 Current Streak: ${stats.currentStreak} days
⚡ Total XP Earned: ${stats.totalXp}

━━━━━━━━━━━━━━━━━━━━
🎯 Session Completion Rate: $completionRate%
   • Sessions Started: ${stats.studySessionsStarted}
   • Sessions Completed: ${stats.studySessionsCompleted}
   • Sessions Aborted: ${stats.studySessionsAborted}

━━━━━━━━━━━━━━━━━━━━
🧠 Average Focus Score: ${stats.avgFocusScore}%
🚫 Distraction Attempts Blocked: ${stats.blockedAppAttempts}
💚 Recovery Sessions: ${stats.recoverySessions}

━━━━━━━━━━━━━━━━━━━━
📚 Topics Studied:
${stats.topicsStudied.mapIndexed { i, t -> "${i + 1}. $t" }.joinToString("\n").ifEmpty { "None yet" }}

_Sent from FocusGuard Cognitive OS_
        """.trimIndent()

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            putExtra(Intent.EXTRA_SUBJECT, "FocusGuard Report - $date")
        }
        startActivity(Intent.createChooser(intent, "Share report via..."))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
