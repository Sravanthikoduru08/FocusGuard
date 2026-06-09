package com.example.focusguard

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

class FocusViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("FocusGuardPrefs", android.content.Context.MODE_PRIVATE)

    private val _focusLevel = MutableLiveData<Int>(prefs.getInt("focus_level", 85))
    val focusLevel: LiveData<Int> = _focusLevel

    private val _dopamineLevel = MutableLiveData<Int>(prefs.getInt("dopamine_level", 70))
    val dopamineLevel: LiveData<Int> = _dopamineLevel

    private val _streak = MutableLiveData<Int>(prefs.getInt("streak", 20))
    val streak: LiveData<Int> = _streak

    private val _xp = MutableLiveData<Int>(prefs.getInt("xp", 1250))
    val xp: LiveData<Int> = _xp

    // Analytics Data
    private val focusHistoryDeque = ArrayDeque<Int>()
    private val dopamineHistoryDeque = ArrayDeque<Int>()

    private val _focusHistory = MutableLiveData<List<Int>>(emptyList())
    val focusHistory: LiveData<List<Int>> = _focusHistory

    private val _dopamineHistory = MutableLiveData<List<Int>>(emptyList())
    val dopamineHistory: LiveData<List<Int>> = _dopamineHistory

    // Emotional Anchor
    private val _emotionalAnchorQuote = MutableLiveData<String>(prefs.getString("anchor_quote", "I build for my family's better tomorrow.") ?: "")
    val emotionalAnchorQuote: LiveData<String> = _emotionalAnchorQuote

    private val _emotionalAnchorPhotoUri = MutableLiveData<String?>(prefs.getString("anchor_photo", null))
    val emotionalAnchorPhotoUri: LiveData<String?> = _emotionalAnchorPhotoUri

    private val _isOnboardingComplete = MutableLiveData<Boolean>(prefs.getBoolean("onboarding_complete", false))
    val isOnboardingComplete: LiveData<Boolean> = _isOnboardingComplete

    private fun persistStats() {
        prefs.edit().apply {
            putInt("focus_level", _focusLevel.value ?: 85)
            putInt("dopamine_level", _dopamineLevel.value ?: 70)
            putInt("streak", _streak.value ?: 0)
            putInt("xp", _xp.value ?: 0)
            apply()
        }
    }

    private fun saveHistory() {
        prefs.edit().apply {
            putString("focus_history", focusHistoryDeque.joinToString(","))
            putString("dopamine_history", dopamineHistoryDeque.joinToString(","))
            apply()
        }
    }

    private fun loadHistory() {
        val fHistory = prefs.getString("focus_history", "") ?: ""
        if (fHistory.isNotEmpty()) {
            fHistory.split(",").mapNotNull { it.toIntOrNull() }.forEach { focusHistoryDeque.addLast(it) }
            _focusHistory.value = focusHistoryDeque.toList()
        }

        val dHistory = prefs.getString("dopamine_history", "") ?: ""
        if (dHistory.isNotEmpty()) {
            dHistory.split(",").mapNotNull { it.toIntOrNull() }.forEach { dopamineHistoryDeque.addLast(it) }
            _dopamineHistory.value = dopamineHistoryDeque.toList()
        }
    }

    fun saveEmotionalAnchor(quote: String, photoUri: String?) {
        prefs.edit().apply {
            putString("anchor_quote", quote)
            putString("anchor_photo", photoUri)
            putBoolean("onboarding_complete", true)
            apply()
        }
        _emotionalAnchorQuote.value = quote
        _emotionalAnchorPhotoUri.value = photoUri
        _isOnboardingComplete.value = true
    }

    // Study Session State
    private val _isStudyActive = MutableLiveData<Boolean>(false)
    val isStudyActive: LiveData<Boolean> = _isStudyActive

    private val _isQuestionPhase = MutableLiveData<Boolean>(false)
    val isQuestionPhase: LiveData<Boolean> = _isQuestionPhase

    private val _studyTopic = MutableLiveData<String>("")
    val studyTopic: LiveData<String> = _studyTopic

    private val _aiQuestion = MutableLiveData<String>("Generating question...")
    val aiQuestion: LiveData<String> = _aiQuestion

    private var xpMultiplier = 1

    private var generativeModel: GenerativeModel? = null

    init {
        loadHistory()
        startRealTimeUpdates()
        registerStatsReceiver()
    }

    private fun registerStatsReceiver() {
        val filter = android.content.IntentFilter("com.example.focusguard.STATS_UPDATED")
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
                reloadStats()
            }
        }
        getApplication<Application>().registerReceiver(receiver, filter, android.content.Context.RECEIVER_NOT_EXPORTED)
    }

    private fun reloadStats() {
        _focusLevel.value = prefs.getInt("focus_level", 85)
        _dopamineLevel.value = prefs.getInt("dopamine_level", 70)
        _streak.value = prefs.getInt("streak", 20)
        _xp.value = prefs.getInt("xp", 1250)
    }

    private fun startRealTimeUpdates() {
        viewModelScope.launch {
            // Observe cognitive state changes
            com.example.focusguard.engine.CognitiveStateEngine.currentStatus.collect { state ->
                when(state) {
                    com.example.focusguard.engine.BrainState.CALM -> {
                        // Passive focus growth
                        if (_isStudyActive.value == false) {
                            _xp.value = (_xp.value ?: 0) + (1 * xpMultiplier)
                        }
                    }
                    com.example.focusguard.engine.BrainState.FRAGMENTED -> {
                        // Penalty for fragmentation
                        _focusLevel.value = (_focusLevel.value!! - 2).coerceAtLeast(0)
                        _dopamineLevel.value = (_dopamineLevel.value!! + 5).coerceAtMost(100)
                    }
                    else -> {}
                }
                persistStats()
            }
        }

        viewModelScope.launch {
            while (true) {
                delay(5000) // Sync every 5 seconds
                com.example.focusguard.engine.CognitiveStateEngine.syncRealUsage(getApplication())
                
                val usage = com.example.focusguard.engine.CognitiveStateEngine.appUsageMap.value
                val currentState = com.example.focusguard.engine.CognitiveStateEngine.currentStatus.value
                
                // Real Focus Calculation: 100 - (Overload / 1.5)
                // We'll still allow focus to grow during study
                if (_isStudyActive.value == true) {
                    _focusLevel.value = (_focusLevel.value!! + 1).coerceIn(0, 100)
                    _xp.value = (_xp.value ?: 0) + (2 * xpMultiplier)
                } else {
                    // Update Focus and Dopamine based on real state
                    val targetFocus = when(currentState) {
                        com.example.focusguard.engine.BrainState.CALM -> 90
                        com.example.focusguard.engine.BrainState.BUSY -> 60
                        com.example.focusguard.engine.BrainState.FRAGMENTED -> 30
                        com.example.focusguard.engine.BrainState.RECOVERY -> 75
                    }
                    
                    val targetDopamine = when(currentState) {
                        com.example.focusguard.engine.BrainState.CALM -> 20
                        com.example.focusguard.engine.BrainState.BUSY -> 60
                        com.example.focusguard.engine.BrainState.FRAGMENTED -> 95
                        com.example.focusguard.engine.BrainState.RECOVERY -> 40
                    }

                    // Smooth transition towards targets
                    val currentF = _focusLevel.value ?: 50
                    val currentD = _dopamineLevel.value ?: 50
                    
                    _focusLevel.value = if (currentF < targetFocus) currentF + 1 else if (currentF > targetFocus) currentF - 1 else currentF
                    _dopamineLevel.value = if (currentD < targetDopamine) currentD + 1 else if (currentD > targetDopamine) currentD - 1 else currentD
                }
                persistStats()

                // Update Analytics History
                focusHistoryDeque.addLast(_focusLevel.value!!)
                if (focusHistoryDeque.size > 84) focusHistoryDeque.removeFirst() // 7 rows * 12 columns
                _focusHistory.value = focusHistoryDeque.toList()

                dopamineHistoryDeque.addLast(_dopamineLevel.value!!)
                if (dopamineHistoryDeque.size > 84) dopamineHistoryDeque.removeFirst()
                _dopamineHistory.value = dopamineHistoryDeque.toList()
                
                saveHistory()
            }
        }
    }

    fun purchaseItem(item: ShopItem): Boolean {
        val currentXP = _xp.value ?: 0
        if (currentXP >= item.cost) {
            _xp.value = currentXP - item.cost
            applyBooster(item.id)
            persistStats()
            return true
        }
        return false
    }

    private fun applyBooster(itemId: String) {
        when (itemId) {
            "sync" -> {
                // Already handled in engine logic as RECOVERY state is faster
                increaseFocus(10)
            }
            "focus" -> {
                increaseFocus(20)
            }
            "flow" -> {
                xpMultiplier = 2
                viewModelScope.launch {
                    delay(10 * 60 * 1000) // 10 minutes
                    xpMultiplier = 1
                }
            }
            "memory" -> {
                // Logic for "Memory Core" could be reducing overload in engine
                // For now, simple boost
                _dopamineLevel.value = (_dopamineLevel.value!! - 30).coerceAtLeast(0)
            }
        }
    }

    fun increaseFocus(amount: Int) {
        _focusLevel.postValue((_focusLevel.value!! + amount).coerceIn(0, 100))
        persistStats()
    }

    fun startStudySession(topic: String) {
        _studyTopic.value = topic
        _isStudyActive.value = true
        _isQuestionPhase.value = false
        com.example.focusguard.engine.CognitiveStateEngine.setStudyModeActive(true)
        getApplication<Application>().sendBroadcast(Intent("com.example.focusguard.STUDY_STATE_CHANGED"))
        generateAIQuestion(topic)
    }

    fun onTimerFinished() {
        // Unlock apps but keep study active for the question phase
        com.example.focusguard.engine.CognitiveStateEngine.setStudyModeActive(false)
        _isQuestionPhase.postValue(true)
    }

    private fun generateAIQuestion(topic: String) {
        val apiKey = prefs.getString("gemini_api_key", "")
        if (apiKey.isNullOrEmpty()) {
            _aiQuestion.postValue("Please configure your Gemini API Key in the Neural Settings to enable AI generated questions.")
            return
        }

        if (generativeModel == null) {
            generativeModel = GenerativeModel(
                modelName = "gemini-1.5-flash",
                apiKey = apiKey,
                systemInstruction = content { text("You are a focus assistant. Generate one challenging question based on the study topic provided. Keep it concise.") }
            )
        }

        viewModelScope.launch {
            try {
                val response = generativeModel!!.generateContent("The topic is: $topic. Ask me a question to verify my understanding.")
                _aiQuestion.postValue(response.text ?: "What did you learn about $topic?")
            } catch (e: Exception) {
                _aiQuestion.postValue("Error generating question. Check your API Key or internet connection.")
            }
        }
    }

    fun completeStudySession(answeredCorrectly: Boolean) {
        if (answeredCorrectly) {
            _xp.value = (_xp.value ?: 0) + 50
            increaseFocus(15)
            _streak.value = (_streak.value ?: 0) + 1
        } else {
            deductXP(0.10)
        }
        persistStats()
        _isStudyActive.value = false
        com.example.focusguard.engine.CognitiveStateEngine.setStudyModeActive(false)
        getApplication<Application>().sendBroadcast(Intent("com.example.focusguard.STUDY_STATE_CHANGED"))
        _studyTopic.value = ""
    }

    fun abortStudySession() {
        deductXP(0.10)
        persistStats()
        _isStudyActive.value = false
        com.example.focusguard.engine.CognitiveStateEngine.setStudyModeActive(false)
        getApplication<Application>().sendBroadcast(Intent("com.example.focusguard.STUDY_STATE_CHANGED"))
        _studyTopic.value = ""
    }

    private fun deductXP(percentage: Double) {
        val currentXP = _xp.value ?: 0
        val deduction = (currentXP * percentage).toInt()
        _xp.value = (currentXP - deduction).coerceAtLeast(0)
        persistStats()
    }
}
