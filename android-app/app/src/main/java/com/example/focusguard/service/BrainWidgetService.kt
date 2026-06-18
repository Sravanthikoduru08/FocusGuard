package com.example.focusguard.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import com.example.focusguard.R
import com.example.focusguard.engine.BrainState
import com.example.focusguard.engine.CognitiveStateEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

import android.view.animation.Animation
import android.view.animation.AnimationUtils

import android.view.MotionEvent
import android.view.View.OnTouchListener

class BrainWidgetService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: View
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    private var currentAnimation: Animation? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_brain_widget, null)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 100
        }

        windowManager.addView(floatingView, params)

        // Make the brain draggable and clickable
        floatingView.setOnTouchListener(object : OnTouchListener {
            private var initialX: Int = 0
            private var initialY: Int = 0
            private var initialTouchX: Float = 0.0f
            private var initialTouchY: Float = 0.0f
            private var startTime: Long = 0

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        startTime = System.currentTimeMillis()
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = initialX + (event.rawX - initialTouchX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager.updateViewLayout(floatingView, params)
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        val duration = System.currentTimeMillis() - startTime
                        val distance = Math.hypot((event.rawX - initialTouchX).toDouble(), (event.rawY - initialTouchY).toDouble())
                        if (duration < 200 && distance < 10) {
                            showAppUsageDialog()
                        }
                        return true
                    }
                }
                return false
            }
        })

        observeCognitiveState()
        observeMessages()
    }

    private fun Int.dpToPx(context: Context): Int = (this * context.resources.displayMetrics.density).toInt()

    private fun observeCognitiveState() {
        val glowView = floatingView.findViewById<View>(R.id.brain_glow)
        
        CognitiveStateEngine.currentStatus
            .onEach { state ->
                glowView.clearAnimation()
                when (state) {
                    BrainState.CALM -> {
                        glowView.setBackgroundResource(R.drawable.brain_glow_cyan)
                        currentAnimation = AnimationUtils.loadAnimation(this, R.anim.pulse_calm)
                    }
                    BrainState.BUSY -> {
                        glowView.setBackgroundResource(R.drawable.brain_glow_purple)
                        currentAnimation = AnimationUtils.loadAnimation(this, R.anim.pulse_calm)
                    }
                    BrainState.FRAGMENTED -> {
                        glowView.setBackgroundResource(R.drawable.brain_glow_red)
                        currentAnimation = AnimationUtils.loadAnimation(this, R.anim.pulse_aggressive)
                    }
                    BrainState.RECOVERY -> {
                        glowView.setBackgroundResource(R.drawable.brain_glow_green)
                        currentAnimation = AnimationUtils.loadAnimation(this, R.anim.pulse_recovery)
                    }
                }
                currentAnimation?.let { glowView.startAnimation(it) }
            }
            .launchIn(serviceScope)
    }

    private fun observeMessages() {
        val messageTv = floatingView.findViewById<TextView>(R.id.tv_message)
        CognitiveStateEngine.widgetMessage
            .onEach { message ->
                if (message != null) {
                    messageTv.text = message
                    messageTv.visibility = View.VISIBLE
                    
                    // Auto hide after 5 seconds
                    serviceScope.launch {
                        delay(5000)
                        messageTv.visibility = View.GONE
                        CognitiveStateEngine.clearMessage()
                    }
                } else {
                    messageTv.visibility = View.GONE
                }
            }
            .launchIn(serviceScope)
    }

    private fun showAppUsageDialog() {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as android.app.usage.UsageStatsManager
        val endTime = System.currentTimeMillis()
        val startTime = endTime - (24 * 60 * 60 * 1000) // Last 24 hours

        val stats = usageStatsManager.queryUsageStats(
            android.app.usage.UsageStatsManager.INTERVAL_DAILY, startTime, endTime
        )

        if (stats == null || stats.isEmpty()) {
            val messageTv = floatingView.findViewById<TextView>(R.id.tv_message)
            messageTv.text = "Permission required: Please enable 'Usage Access' in Settings."
            messageTv.visibility = View.VISIBLE
            
            val intent = Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            
            serviceScope.launch {
                delay(5000)
                messageTv.visibility = View.GONE
            }
            return
        }

        val pm = packageManager
        val sb = StringBuilder()
        sb.append("Real-time App Usage (Today):\n\n")

        stats.filter { it.totalTimeInForeground > 0 }
            .sortedByDescending { it.totalTimeInForeground }
            .take(5)
            .forEach { usageStat ->
                val pkg = usageStat.packageName
                val name = try {
                    pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0))
                } catch (e: Exception) {
                    pkg
                }
                val timeMs = usageStat.totalTimeInForeground
                val minutes = timeMs / 60000
                val seconds = (timeMs % 60000) / 1000
                sb.append("• $name: ${minutes}m ${seconds}s\n")
            }

        val messageTv = floatingView.findViewById<TextView>(R.id.tv_message)
        messageTv.text = sb.toString()
        messageTv.visibility = View.VISIBLE
        
        serviceScope.launch {
            delay(10000)
            messageTv.visibility = View.GONE
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        if (::floatingView.isInitialized) windowManager.removeView(floatingView)
    }
}
