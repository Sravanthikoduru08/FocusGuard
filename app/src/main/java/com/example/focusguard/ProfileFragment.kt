package com.example.focusguard

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
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
        binding.etBlockMessage.setText(CognitiveStateEngine.customBlockMessage.value)
        updateBlockedAppsList()

        binding.btnBrowseApps.setOnClickListener {
            showAppPickerDialog()
        }

        binding.btnClearApps.setOnClickListener {
            CognitiveStateEngine.clearBlockedApps()
            updateBlockedAppsList()
            Toast.makeText(requireContext(), "Restricted list cleared", Toast.LENGTH_SHORT).show()
        }

        binding.btnSaveSettings.setOnClickListener {
            val newMessage = binding.etBlockMessage.text.toString().trim()
            if (newMessage.isNotEmpty()) {
                CognitiveStateEngine.setCustomBlockMessage(newMessage)
                Toast.makeText(requireContext(), "Neural configuration saved!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showAppPickerDialog() {
        val pm = requireContext().packageManager
        val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        
        val appData = mutableListOf<Pair<String, String>>() // Name to Package
        
        for (appInfo in packages) {
            if (pm.getLaunchIntentForPackage(appInfo.packageName) != null) {
                val name = pm.getApplicationLabel(appInfo).toString()
                appData.add(name to appInfo.packageName)
            }
        }
        
        appData.sortBy { it.first.lowercase() }
        
        val names = appData.map { it.first }.toTypedArray()
        
        AlertDialog.Builder(requireContext(), R.style.Theme_FocusGuard)
            .setTitle("Select App to Block")
            .setItems(names) { _, which ->
                val selectedApp = appData[which]
                CognitiveStateEngine.addBlockedApp(selectedApp.second)
                updateBlockedAppsList()
                Toast.makeText(requireContext(), "${selectedApp.first} restricted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
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
