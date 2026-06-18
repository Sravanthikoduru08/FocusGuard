package com.example.focusguard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.focusguard.databinding.FragmentAnalyticsBinding

import androidx.fragment.app.activityViewModels

class AnalyticsFragment : Fragment() {

    private var _binding: FragmentAnalyticsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: FocusViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAnalyticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.focusLevel.observe(viewLifecycleOwner) {
            binding.tvFocusPercentage.text = "$it%"
        }

        viewModel.dopamineHistory.observe(viewLifecycleOwner) {
            // We can keep a small bar chart for dopamine or use it elsewhere
        }

        viewModel.focusHistory.observe(viewLifecycleOwner) { history ->
            updateFocusHeatmap(history)
        }

        binding.btnManageBlocks.setOnClickListener {
            findNavController().navigate(R.id.action_global_AppSelectionFragment)
        }

        binding.btnTeacherReport.setOnClickListener {
            findNavController().navigate(R.id.action_global_TeacherReportFragment)
        }
    }

    private fun updateFocusHeatmap(history: List<Int>) {
        binding.heatmapGrid.removeAllViews()
        
        // Heatmap expects 84 entries for 7x12 grid
        // If history is shorter, pad with zeros at the beginning
        val paddedHistory = if (history.size < 84) {
            List(84 - history.size) { 0 } + history
        } else {
            history.takeLast(84)
        }

        paddedHistory.forEach { value ->
            val square = View(requireContext())
            val size = 12.dpToPx()
            val params = android.widget.GridLayout.LayoutParams()
            params.width = size
            params.height = size
            params.setMargins(2.dpToPx(), 2.dpToPx(), 2.dpToPx(), 2.dpToPx())
            square.layoutParams = params
            
            // Color based on focus intensity
            val colorRes = when {
                value == 0 -> R.color.card_dark // No data/inactive
                value > 80 -> R.color.cyan_glow
                value > 50 -> R.color.purple_glow
                else -> R.color.magenta_alert // Low focus
            }
            
            square.setBackgroundResource(R.drawable.heatmap_square_bg)
            square.backgroundTintList = android.content.res.ColorStateList.valueOf(requireContext().getColor(colorRes))
            
            // Alpha for depth
            square.alpha = if (value == 0) 0.1f else (value / 100f).coerceAtLeast(0.4f)
            
            binding.heatmapGrid.addView(square)
        }
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
