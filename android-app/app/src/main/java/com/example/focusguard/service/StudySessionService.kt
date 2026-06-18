package com.example.focusguard.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.focusguard.MainActivity
import com.example.focusguard.R
import com.example.focusguard.engine.CognitiveStateEngine
import com.example.focusguard.engine.DndManager
import java.util.Locale

class StudySessionService : Service() {

    companion object {
        var remainingMillis: Long = 0
        var isRunning: Boolean = false
        var currentTopic: String = ""
    }

    private var timer: CountDownTimer? = null
    private val CHANNEL_ID = "StudySessionChannel"
    private val NOTIFICATION_ID = 1001

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == "PAUSE") {
            pauseTimer()
            return START_NOT_STICKY
        } else if (action == "RESUME") {
            resumeTimer()
            return START_NOT_STICKY
        }

        val duration = intent?.getLongExtra("duration", 30 * 60 * 1000L) ?: (30 * 60 * 1000L)
        val topic = intent?.getStringExtra("topic") ?: "Study Session"
        
        currentTopic = topic
        remainingMillis = duration
        isRunning = true

        android.widget.Toast.makeText(this, "Study Protocol Initiated", android.widget.Toast.LENGTH_SHORT).show()

        DndManager.enableDnd(this)
        startForeground(NOTIFICATION_ID, createNotification("Starting...", topic))
        startTimer(duration, topic)

        return START_NOT_STICKY
    }

    private fun startTimer(duration: Long, topic: String) {
        timer?.cancel()
        timer = object : CountDownTimer(duration, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                remainingMillis = millisUntilFinished
                val minutes = (millisUntilFinished / 1000) / 60
                val seconds = (millisUntilFinished / 1000) % 60
                val timeStr = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
                updateNotification(timeStr, topic)
                
                // Broadcast for UI sync - Make it explicit to this app
                val intent = Intent("com.example.focusguard.TIMER_TICK")
                intent.putExtra("remaining", millisUntilFinished)
                intent.setPackage(packageName)
                sendBroadcast(intent)
            }

            override fun onFinish() {
                isRunning = false
                remainingMillis = 0
                CognitiveStateEngine.setStudyModeActive(false)
                DndManager.disableDnd(this@StudySessionService)
                updateNotification("Session Complete! Return to app.", topic)
                
                val intent = Intent("com.example.focusguard.STUDY_TIMER_FINISHED")
                intent.setPackage(packageName)
                sendBroadcast(intent)
                
                stopForeground(true)
                stopSelf()
            }
        }.start()
    }

    fun pauseTimer() {
        timer?.cancel()
        isRunning = false
        CognitiveStateEngine.setStudyModeActive(false) 
        DndManager.disableDnd(this)
        updateNotification("Session Paused", currentTopic)
        
        val intent = Intent("com.example.focusguard.TIMER_PAUSED")
        intent.setPackage(packageName)
        sendBroadcast(intent)
    }

    fun resumeTimer() {
        if (!isRunning && remainingMillis > 0) {
            isRunning = true
            CognitiveStateEngine.setStudyModeActive(true)
            DndManager.enableDnd(this)
            startTimer(remainingMillis, currentTopic)
            
            val intent = Intent("com.example.focusguard.TIMER_RESUMED")
            intent.setPackage(packageName)
            sendBroadcast(intent)
        }
    }

    private fun createNotification(content: String, topic: String): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Studying: $topic")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(content: String, topic: String) {
        val notification = createNotification(content, topic)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Study Session Timer",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
        DndManager.disableDnd(this)
        isRunning = false
    }
}
