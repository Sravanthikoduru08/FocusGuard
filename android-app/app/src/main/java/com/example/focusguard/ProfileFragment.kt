package com.example.focusguard

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.focusguard.databinding.FragmentProfileBinding
import com.example.focusguard.engine.CognitiveStateEngine

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Load existing settings
        val prefs = requireContext().getSharedPreferences("FocusGuardPrefs", android.content.Context.MODE_PRIVATE)
        binding.etApiKey.setText(prefs.getString("gemini_api_key", ""))
        binding.etBlockMessage.setText(CognitiveStateEngine.customBlockMessage.value)
        updateBlockedAppsList()

        binding.btnBrowseApps.setOnClickListener {
            findNavController().navigate(R.id.action_global_AppSelectionFragment)
        }

        binding.btnClearApps.setOnClickListener {
            CognitiveStateEngine.clearBlockedApps(requireContext())
            updateBlockedAppsList()
            Toast.makeText(requireContext(), "Restricted list cleared", Toast.LENGTH_SHORT).show()
        }

        binding.btnSaveSettings.setOnClickListener {
            val newMessage = binding.etBlockMessage.text.toString().trim()
            val apiKey = binding.etApiKey.text.toString().trim()
            
            requireContext().getSharedPreferences("FocusGuardPrefs", android.content.Context.MODE_PRIVATE)
                .edit()
                .putString("gemini_api_key", apiKey)
                .apply()
                
            if (newMessage.isNotEmpty()) {
                CognitiveStateEngine.setCustomBlockMessage(newMessage)
            }
            Toast.makeText(requireContext(), "Neural configuration saved!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateBlockedAppsList() {
        val apps = CognitiveStateEngine.blockedApps.value
        val pm = requireContext().packageManager
        
        if (apps.isEmpty()) {
            binding.tvBlockedAppsList.text = "No restricted apps configured."
        } else {
            val displayList = apps.map { pkg ->
                try {
                    val appInfo = pm.getApplicationInfo(pkg, 0)
                    val name = pm.getApplicationLabel(appInfo).toString()
                    "• $name"
                } catch (e: PackageManager.NameNotFoundException) {
                    "• $pkg"
                }
            }
            binding.tvBlockedAppsList.text = "Currently Restricted:\n" + displayList.joinToString("\n")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
