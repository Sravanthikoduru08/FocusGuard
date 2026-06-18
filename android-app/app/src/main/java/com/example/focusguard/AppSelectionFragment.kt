package com.example.focusguard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.focusguard.databinding.FragmentAppSelectionBinding
import com.example.focusguard.engine.AppListManager
import com.example.focusguard.engine.CognitiveStateEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppSelectionFragment : Fragment() {

    private var _binding: FragmentAppSelectionBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: AppSelectionAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAppSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerViewApps.layoutManager = LinearLayoutManager(requireContext())
        
        adapter = AppSelectionAdapter { appInfo, isChecked ->
            if (isChecked) {
                CognitiveStateEngine.addBlockedApp(requireContext(), appInfo.packageName)
            } else {
                CognitiveStateEngine.removeBlockedApp(requireContext(), appInfo.packageName)
            }
        }
        binding.recyclerViewApps.adapter = adapter

        loadApps()
    }

    private fun loadApps() {
        binding.progressBar.visibility = View.VISIBLE
        binding.recyclerViewApps.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            // Load from engine
            val blockedApps = CognitiveStateEngine.blockedApps.value
            val installedApps = AppListManager.getInstalledApps(requireContext(), blockedApps)

            withContext(Dispatchers.Main) {
                adapter.submitList(installedApps)
                binding.progressBar.visibility = View.GONE
                binding.recyclerViewApps.visibility = View.VISIBLE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
