package com.example.focusguard

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.focusguard.databinding.FragmentOnboardingBinding

class OnboardingFragment : Fragment() {

    private var _binding: FragmentOnboardingBinding? = null
    private val binding get() = _binding!!
    private val viewModel: FocusViewModel by activityViewModels()

    private var selectedPhotoUri: Uri? = null

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            // Take persistable permission
            requireContext().contentResolver.takePersistableUriPermission(
                it, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            selectedPhotoUri = it
            binding.ivAnchorPhoto.setImageURI(it)
            binding.tvUploadHint.visibility = View.GONE
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboardingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Check if onboarding is complete
        viewModel.isOnboardingComplete.observe(viewLifecycleOwner) { isComplete ->
            if (isComplete && findNavController().currentDestination?.id == R.id.OnboardingFragment) {
                findNavController().navigate(R.id.action_OnboardingFragment_to_FirstFragment)
            }
        }

        binding.btnUploadPhoto.setOnClickListener {
            pickImageLauncher.launch(arrayOf("image/*"))
        }

        binding.btnContinue.setOnClickListener {
            val quote = binding.etAnchorQuote.text.toString().takeIf { it.isNotBlank() }
                ?: "I build for my family's better tomorrow."
            viewModel.saveEmotionalAnchor(quote, selectedPhotoUri?.toString())
        }

        binding.btnSkip.setOnClickListener {
            viewModel.saveEmotionalAnchor("I build for my family's better tomorrow.", null)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
