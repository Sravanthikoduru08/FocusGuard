package com.example.focusguard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.focusguard.databinding.FragmentNeuralShopBinding
import android.widget.Toast

class NeuralShopFragment : Fragment() {

    private var _binding: FragmentNeuralShopBinding? = null
    private val binding get() = _binding!!
    private val viewModel: FocusViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNeuralShopBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.xp.observe(viewLifecycleOwner) {
            binding.tvBalanceValue.text = "$it XP"
        }

        val items = listOf(
            ShopItem("sync", "Neural Sync", "Faster recovery after focus sessions.", "250 XP", 250),
            ShopItem("focus", "Deep Focus", "Instant 20% focus restoration.", "500 XP", 500),
            ShopItem("flow", "Flow State", "Double XP gain for 10 minutes.", "750 XP", 750),
            ShopItem("memory", "Memory Core", "Stability boost: -50% overload accumulation.", "1000 XP", 1000)
        )

        binding.rvShopItems.layoutManager = LinearLayoutManager(requireContext())
        binding.rvShopItems.adapter = ShopAdapter(items) { item ->
            if (viewModel.purchaseItem(item)) {
                Toast.makeText(requireContext(), "Neural Protocol Activated: ${item.name}", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Insufficient XP for Neural Sync", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
