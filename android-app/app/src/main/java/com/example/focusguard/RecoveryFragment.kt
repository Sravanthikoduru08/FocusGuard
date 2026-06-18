package com.example.focusguard

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.focusguard.databinding.FragmentRecoveryBinding

import androidx.fragment.app.activityViewModels

class RecoveryFragment : Fragment() {

    private var _binding: FragmentRecoveryBinding? = null
    private val binding get() = _binding!!
    private val viewModel: FocusViewModel by activityViewModels()
    private var animatorSet: AnimatorSet? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecoveryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        com.example.focusguard.engine.CognitiveStateEngine.initiateRecovery()
        startBreathingAnimation()

        binding.btnCompleteRecovery.setOnClickListener {
            stopAnimation()
            com.example.focusguard.engine.CognitiveStateEngine.completeRecovery()
            viewModel.increaseFocus(10) // Increase focus after recovery
            findNavController().navigateUp()
        }
    }

    private fun startBreathingAnimation() {
        val scaleUpX = ObjectAnimator.ofFloat(binding.breathingCircleInner, "scaleX", 1f, 2.5f)
        val scaleUpY = ObjectAnimator.ofFloat(binding.breathingCircleInner, "scaleY", 1f, 2.5f)
        val alphaUp = ObjectAnimator.ofFloat(binding.breathingCircleInner, "alpha", 0.5f, 1f)

        val inhale = AnimatorSet().apply {
            playTogether(scaleUpX, scaleUpY, alphaUp)
            duration = 4000
            interpolator = AccelerateDecelerateInterpolator()
            addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) { _binding?.tvInstruction?.text = "Inhale..." }
                override fun onAnimationEnd(animation: Animator) {}
                override fun onAnimationCancel(animation: Animator) {}
                override fun onAnimationRepeat(animation: Animator) {}
            })
        }

        val scaleDownX = ObjectAnimator.ofFloat(binding.breathingCircleInner, "scaleX", 2.5f, 1f)
        val scaleDownY = ObjectAnimator.ofFloat(binding.breathingCircleInner, "scaleY", 2.5f, 1f)
        val alphaDown = ObjectAnimator.ofFloat(binding.breathingCircleInner, "alpha", 1f, 0.5f)

        val exhale = AnimatorSet().apply {
            playTogether(scaleDownX, scaleDownY, alphaDown)
            duration = 4000
            interpolator = AccelerateDecelerateInterpolator()
            addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) { _binding?.tvInstruction?.text = "Exhale..." }
                override fun onAnimationEnd(animation: Animator) {}
                override fun onAnimationCancel(animation: Animator) {}
                override fun onAnimationRepeat(animation: Animator) {}
            })
        }

        animatorSet = AnimatorSet().apply {
            playSequentially(inhale, exhale)
        }

        animatorSet?.addListener(object : Animator.AnimatorListener {
            override fun onAnimationEnd(animation: Animator) {
                if (isAdded) animatorSet?.start()
            }
            override fun onAnimationStart(animation: Animator) {}
            override fun onAnimationCancel(animation: Animator) {}
            override fun onAnimationRepeat(animation: Animator) {}
        })

        animatorSet?.start()
    }

    private fun stopAnimation() {
        animatorSet?.cancel()
        animatorSet = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopAnimation()
        _binding = null
    }
}
