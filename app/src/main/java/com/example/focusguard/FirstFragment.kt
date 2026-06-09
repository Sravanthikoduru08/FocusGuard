package com.example.focusguard

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.example.focusguard.databinding.FragmentFirstBinding

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
import android.view.animation.AnimationUtils
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.launch
import androidx.activity.result.contract.ActivityResultContracts

class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null
    private val binding get() = _binding!!
    private val viewModel: FocusViewModel by activityViewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission granted, continue with initiation if needed
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Auto-navigate if study is active
        if (viewModel.isStudyActive.value == true && findNavController().currentDestination?.id == R.id.FirstFragment) {
            findNavController().navigate(R.id.action_FirstFragment_to_StudyFragment)
            return
        }

        checkAndRequestUsageStatsPermission()

        // Start central brain pulse animation
        val pulse = AnimationUtils.loadAnimation(requireContext(), R.anim.pulse_calm)
        binding.mainBrainGlow.startAnimation(pulse)

        viewModel.focusLevel.observe(viewLifecycleOwner) {
            binding.tvFocusValue.text = "$it%"
        }

        viewModel.dopamineLevel.observe(viewLifecycleOwner) {
            binding.tvDopamineValue.text = "$it%"
        }

        viewModel.streak.observe(viewLifecycleOwner) {
            binding.tvStreakValue.text = "$it"
        }

        viewModel.xp.observe(viewLifecycleOwner) {
            binding.tvXp.text = "$it XP"
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                com.example.focusguard.engine.CognitiveStateEngine.currentStatus.collect { state ->
                    binding.mainBrainGlow.clearAnimation()
                    binding.mainBrainGlowSecondary.clearAnimation()
                    val pulseAnim: Int
                    val glowRes: Int
                    
                    when(state) {
                        com.example.focusguard.engine.BrainState.CALM -> {
                            binding.tvStatus.text = "● Neural state: Stabilized"
                            binding.tvStatus.setTextColor(requireContext().getColor(R.color.cyan_glow))
                            pulseAnim = R.anim.pulse_calm
                            glowRes = R.drawable.brain_glow_cyan
                        }
                        com.example.focusguard.engine.BrainState.BUSY -> {
                            binding.tvStatus.text = "● Attention fragmentation detected"
                            binding.tvStatus.setTextColor(requireContext().getColor(R.color.purple_glow))
                            pulseAnim = R.anim.pulse_calm
                            glowRes = R.drawable.brain_glow_purple
                        }
                        com.example.focusguard.engine.BrainState.FRAGMENTED -> {
                            binding.tvStatus.text = "● Cognitive overload imminent"
                            binding.tvStatus.setTextColor(requireContext().getColor(R.color.magenta_alert))
                            pulseAnim = R.anim.pulse_aggressive
                            glowRes = R.drawable.brain_glow_red
                        }
                        com.example.focusguard.engine.BrainState.RECOVERY -> {
                            binding.tvStatus.text = "● Healing neural pathways"
                            binding.tvStatus.setTextColor(requireContext().getColor(R.color.accent_green))
                            pulseAnim = R.anim.pulse_recovery
                            glowRes = R.drawable.brain_glow_green
                        }
                    }
                    
                    binding.mainBrainGlow.setBackgroundResource(glowRes)
                    binding.mainBrainGlowSecondary.setBackgroundResource(glowRes)
                    
                    /*// Update particles color (Requirement #4)
                    val particleTint = when(state) {
                        com.example.focusguard.engine.BrainState.CALM -> R.color.cyan_glow
                        com.example.focusguard.engine.BrainState.BUSY -> R.color.purple_glow
                        com.example.focusguard.engine.BrainState.FRAGMENTED -> R.color.magenta_alert
                        com.example.focusguard.engine.BrainState.RECOVERY -> R.color.accent_green
                    }
                    binding.neuralParticles.setParticleColor(requireContext().getColor(particleTint))*/

                    val anim = AnimationUtils.loadAnimation(requireContext(), pulseAnim)
                    binding.mainBrainGlow.startAnimation(anim)
                    
                    val secondaryAnim = AnimationUtils.loadAnimation(requireContext(), pulseAnim)
                    secondaryAnim.startOffset = 500 // Offset for depth
                    binding.mainBrainGlowSecondary.startAnimation(secondaryAnim)
                }
            }
        }

        binding.btnInitiate.setOnClickListener {
            // Check for permissions first
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU && 
                androidx.core.content.ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            } else if (!hasOverlayPermission()) {
                requestOverlayPermission()
            } else if (!isAccessibilityServiceEnabled()) {
                requestAccessibilityPermission()
            } else {
                startFocusServices()
                findNavController().navigate(R.id.action_FirstFragment_to_StudyFragment)
            }
        }
    }

    private fun hasOverlayPermission(): Boolean = android.provider.Settings.canDrawOverlays(requireContext())

    private fun isAccessibilityServiceEnabled(): Boolean {
        val service = "${requireContext().packageName}/${com.example.focusguard.service.CognitiveEyesService::class.java.canonicalName}"
        val enabled = android.provider.Settings.Secure.getInt(
            requireContext().contentResolver,
            android.provider.Settings.Secure.ACCESSIBILITY_ENABLED
        )
        if (enabled == 1) {
            val settingValue = android.provider.Settings.Secure.getString(
                requireContext().contentResolver,
                android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            return settingValue?.contains(service) == true
        }
        return false
    }

    private fun requestAccessibilityPermission() {
        val intent = android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
        android.widget.Toast.makeText(requireContext(), "Please enable FocusGuard Accessibility Service", android.widget.Toast.LENGTH_LONG).show()
    }

    private fun requestOverlayPermission() {
        val intent = android.content.Intent(
            android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            android.net.Uri.parse("package:${requireContext().packageName}")
        )
        startActivity(intent)
    }

    private fun startFocusServices() {
        requireContext().startService(android.content.Intent(requireContext(), com.example.focusguard.service.BrainWidgetService::class.java))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun checkAndRequestUsageStatsPermission() {
        val appOps = requireContext().getSystemService(android.content.Context.APP_OPS_SERVICE) as android.app.AppOpsManager
        val mode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(android.app.AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), requireContext().packageName)
        } else {
            appOps.checkOpNoThrow(android.app.AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), requireContext().packageName)
        }
        
        if (mode != android.app.AppOpsManager.MODE_ALLOWED) {
            android.app.AlertDialog.Builder(requireContext())
                .setTitle("Usage Access Required")
                .setMessage("To track how much time you spend on apps, FocusGuard needs 'Usage Access' permission.")
                .setPositiveButton("Grant") { _, _ ->
                    startActivity(android.content.Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS))
                }
                .setNegativeButton("Later", null)
                .show()
        }
    }
}
