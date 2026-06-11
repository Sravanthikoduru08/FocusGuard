package com.example.focusguard

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.focusguard.databinding.FragmentStudyBinding
import com.example.focusguard.engine.DndManager
import com.example.focusguard.service.StudySessionService
import java.util.Locale

class StudyFragment : Fragment() {

    private var _binding: FragmentStudyBinding? = null
    private val binding get() = _binding!!
    private val viewModel: FocusViewModel by activityViewModels()
    private var selectedDurationMinutes: Long = 30

    private val timerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                "com.example.focusguard.TIMER_TICK" -> {
                    val remaining = intent.getLongExtra("remaining", 0)
                    updateTimerUI(remaining)
                }
                "com.example.focusguard.STUDY_TIMER_FINISHED" -> {
                    viewModel.onTimerFinished()
                }
                "com.example.focusguard.TIMER_PAUSED" -> {
                    updatePlayPauseButton(false)
                }
                "com.example.focusguard.TIMER_RESUMED" -> {
                    updatePlayPauseButton(true)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ) : View {
        _binding = FragmentStudyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val filter = IntentFilter().apply {
            addAction("com.example.focusguard.TIMER_TICK")
            addAction("com.example.focusguard.STUDY_TIMER_FINISHED")
            addAction("com.example.focusguard.TIMER_PAUSED")
            addAction("com.example.focusguard.TIMER_RESUMED")
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireContext().registerReceiver(timerReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            requireContext().registerReceiver(timerReceiver, filter)
        }

        setupDurationButtons()
        setupInitialState()

        viewModel.isQuestionPhase.observe(viewLifecycleOwner) { isPhase ->
            if (isPhase) showQuestionState()
        }

        binding.btnStartStudy.setOnClickListener {
            val topic = binding.etTopic.text.toString()
            if (topic.isNotBlank()) {
                if (DndManager.hasDndPermission(requireContext())) {
                    startStudySession(topic)
                } else {
                    showDndPermissionDialog()
                }
            } else {
                Toast.makeText(requireContext(), "Please enter a topic", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnPlayPause.setOnClickListener {
            val serviceIntent = Intent(requireContext(), StudySessionService::class.java)
            if (StudySessionService.isRunning) {
                serviceIntent.action = "PAUSE"
            } else {
                serviceIntent.action = "RESUME"
            }
            requireContext().startService(serviceIntent)
        }

        binding.btnSubmitAnswer.setOnClickListener {
            val answer = binding.etAnswer.text.toString()
            if (answer.isNotBlank()) {
                viewModel.completeStudySession(true)
                Toast.makeText(requireContext(), "Great! Focus & XP increased.", Toast.LENGTH_LONG).show()
                findNavController().navigateUp()
            } else {
                Toast.makeText(requireContext(), "Please answer the question", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnSkipPenalty.setOnClickListener {
            stopStudyService()
            viewModel.abortStudySession()
            Toast.makeText(requireContext(), "Session aborted. -10% XP penalty applied.", Toast.LENGTH_LONG).show()
            findNavController().navigateUp()
        }

        binding.btnFinishEarly.setOnClickListener {
            stopStudyService()
            viewModel.onTimerFinished()
        }

        viewModel.aiQuestion.observe(viewLifecycleOwner) { question ->
            binding.tvAiQuestion.text = question
        }
    }

    private fun showDndPermissionDialog() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Do Not Disturb Access Required")
            .setMessage("To block notifications during your study session, FocusGuard needs 'Do Not Disturb' access. Please grant it in the next screen.")
            .setPositiveButton("Grant") { _, _ ->
                DndManager.requestDndPermission(requireContext())
            }
            .setNegativeButton("Later") { _, _ ->
                // Still allow session if user denies? 
                // The user requested the feature, but maybe they want to start anyway.
                // Let's prompt them that DND won't work.
                startStudySession(binding.etTopic.text.toString())
            }
            .show()
    }

    private fun setupDurationButtons() {
        binding.btnDuration20.setOnClickListener { selectedDurationMinutes = 20; updateSelectedDurationUI() }
        binding.btnDuration30.setOnClickListener { selectedDurationMinutes = 30; updateSelectedDurationUI() }
        binding.btnDuration45.setOnClickListener { selectedDurationMinutes = 45; updateSelectedDurationUI() }
        updateSelectedDurationUI()
    }

    private fun updateSelectedDurationUI() {
        binding.btnDuration20.alpha = if (selectedDurationMinutes == 20L) 1.0f else 0.5f
        binding.btnDuration30.alpha = if (selectedDurationMinutes == 30L) 1.0f else 0.5f
        binding.btnDuration45.alpha = if (selectedDurationMinutes == 45L) 1.0f else 0.5f
    }

    private fun setupInitialState() {
        if (viewModel.isQuestionPhase.value == true) {
            showQuestionState()
        } else if (StudySessionService.isRunning || StudySessionService.remainingMillis > 0) {
            binding.layoutTopicInput.visibility = View.GONE
            binding.layoutTimer.visibility = View.VISIBLE
            binding.layoutTimerControls.visibility = View.VISIBLE
            binding.tvCurrentTopic.text = "Studying: ${StudySessionService.currentTopic}"
            updateTimerUI(StudySessionService.remainingMillis)
            updatePlayPauseButton(StudySessionService.isRunning)
        } else {
            binding.layoutTopicInput.visibility = View.VISIBLE
            binding.layoutTimer.visibility = View.GONE
            binding.layoutQuestion.visibility = View.GONE
            binding.layoutTimerControls.visibility = View.GONE
        }
    }

    private fun updatePlayPauseButton(running: Boolean) {
        binding.btnPlayPause.text = if (running) "Pause" else "Resume"
    }

    private fun startStudySession(topic: String) {
        viewModel.startStudySession(topic)
        
        val durationMillis = selectedDurationMinutes * 60 * 1000L
        
        val serviceIntent = Intent(requireContext(), StudySessionService::class.java).apply {
            putExtra("duration", durationMillis)
            putExtra("topic", topic)
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            requireContext().startForegroundService(serviceIntent)
        } else {
            requireContext().startService(serviceIntent)
        }

        binding.layoutTopicInput.visibility = View.GONE
        binding.layoutTimer.visibility = View.VISIBLE
        binding.layoutTimerControls.visibility = View.VISIBLE
        binding.tvCurrentTopic.text = "Studying: $topic"
        updatePlayPauseButton(true)
    }

    private fun updateTimerUI(remainingMillis: Long) {
        val minutes = (remainingMillis / 1000) / 60
        val seconds = (remainingMillis / 1000) % 60
        binding.tvTimer.text = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)

        val totalMillis = selectedDurationMinutes * 60 * 1000L
        val progress = if (totalMillis > 0) {
            ((totalMillis - remainingMillis).toFloat() / totalMillis * 100).toInt()
        } else {
            0
        }
        binding.pbSessionProgress.progress = progress
    }

    private fun stopStudyService() {
        val serviceIntent = Intent(requireContext(), StudySessionService::class.java)
        requireContext().stopService(serviceIntent)
    }

    private fun showQuestionState() {
        binding.layoutTopicInput.visibility = View.GONE
        binding.layoutTimer.visibility = View.GONE
        binding.layoutTimerControls.visibility = View.GONE
        binding.layoutQuestion.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        try {
            requireContext().unregisterReceiver(timerReceiver)
        } catch (e: Exception) {}
        _binding = null
    }
}
