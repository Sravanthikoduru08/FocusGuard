package com.example.focusguard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.focusguard.databinding.FragmentAnalyticsBinding

import androidx.fragment.app.activityViewModels
import android.widget.LinearLayout
import android.view.Gravity

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

        viewModel.dopamineHistory.observe(viewLifecycleOwner) { history ->
            updateDopamineBars(history)
        }
    }

    private fun updateDopamineBars(history: List<Int>) {
        val container = binding.cardHeatmap.getChildAt(0) as? LinearLayout ?: return
        container.removeAllViews()
        
        history.takeLast(15).forEach { value ->
            val bar = View(requireContext())
            val params = LinearLayout.LayoutParams(0, (value * 1.5).toInt().dpToPx())
            params.weight = 1f
            params.setMargins(2.dpToPx(), 0, 2.dpToPx(), 0)
            bar.layoutParams = params
            
            val color = when {
                value > 80 -> R.color.magenta_alert
                value > 50 -> R.color.purple_glow
                else -> R.color.cyan_glow
            }
            bar.setBackgroundColor(requireContext().getColor(color))
            bar.alpha = (value / 100f).coerceAtLeast(0.3f)
            container.addView(bar)
        }
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
