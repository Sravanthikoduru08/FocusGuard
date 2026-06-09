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

    private val _focusLevel = MutableLiveData<Int>(85)
    val focusLevel: LiveData<Int> = _focusLevel

    private val _dopamineLevel = MutableLiveData<Int>(70)
    val dopamineLevel: LiveData<Int> = _dopamineLevel

    private val _streak = MutableLiveData<Int>(20)
    val streak: LiveData<Int> = _streak

    private val _xp = MutableLiveData<Int>(1250)
    val xp: LiveData<Int> = _xp

    // Analytics Data — ArrayDeque gives O(1) add/removeFirst without copying the whole list
    private val focusHistoryDeque = ArrayDeque<Int>()
    private val dopamineHistoryDeque = ArrayDeque<Int>()

    private val _focusHistory = MutableLiveData<List<Int>>(emptyList())
    val focusHistory: LiveData<List<Int>> = _focusHistory

    private val _dopamineHistory = MutableLiveData<List<Int>>(emptyList())
    val dopamineHistory: LiveData<List<Int>> = _dopamineHistory

    // Emotional Anchor
    private val prefs = application.getSharedPreferences("FocusGuardPrefs", android.content.Context.MODE_PRIVATE)

    private val _emotionalAnchorQuote = MutableLiveData<String>(prefs.getString("anchor_quote", "I build for my family's better tomorrow.") ?: "")
    val emotionalAnchorQuote: LiveData<String> = _emotionalAnchorQuote

    private val _emotionalAnchorPhotoUri = MutableLiveData<String?>(prefs.getString("anchor_photo", null))
    val emotionalAnchorPhotoUri: LiveData<String?> = _emotionalAnchorPhotoUri

    private val _isOnboardingComplete = MutableLiveData<Boolean>(prefs.getBoolean("onboarding_complete", false))
    val isOnboardingComplete: LiveData<Boolean> = _isOnboardingComplete

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

    // AI Configuration - Suggesting user to add their key
    private val generativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = "YOUR_API_KEY", // Replace with actual API Key
        systemInstruction = content { text("You are a focus assistant. Generate one challenging question based on the study topic provided. Keep it concise.") }
    )

    init {
        startRealTimeUpdates()
    }

    private fun startRealTimeUpdates() {
        viewModelScope.launch {
            // Observe cognitive state changes
            com.example.focusguard.engine.CognitiveStateEngine.currentStatus.collect { state ->
                when(state) {
                    com.example.focusguard.engine.BrainState.CALM -> {
                        // Passive focus growth
                        if (_isStudyActive.value == false) {
                            _xp.postValue((_xp.value ?: 0) + (1 * xpMultiplier))
                        }
                    }
                    com.example.focusguard.engine.BrainState.FRAGMENTED -> {
                        // Penalty for fragmentation
                        _focusLevel.postValue((_focusLevel.value!! - 2).coerceAtLeast(0))
                        _dopamineLevel.postValue((_dopamineLevel.value!! + 5).coerceAtMost(100))
                    }
                    else -> {}
                }
            }
        }

        viewModelScope.launch {
            while (true) {
                delay(2000)
                val currentState = com.example.focusguard.engine.CognitiveStateEngine.currentStatus.value
                
                if (_isStudyActive.value == true) {
                    _focusLevel.postValue((_focusLevel.value!! + 1).coerceIn(0, 100))
                    _xp.postValue((_xp.value ?: 0) + (2 * xpMultiplier))
                } else {
                    when (currentState) {
                        com.example.focusguard.engine.BrainState.CALM -> {
                            _focusLevel.postValue((_focusLevel.value!! + 1).coerceIn(0, 100))
                            _dopamineLevel.postValue((_dopamineLevel.value!! - 1).coerceIn(0, 100))
                        }
                        com.example.focusguard.engine.BrainState.BUSY, 
                        com.example.focusguard.engine.BrainState.FRAGMENTED -> {
                            _focusLevel.postValue((_focusLevel.value!! - 1).coerceIn(0, 100))
                            _dopamineLevel.postValue((_dopamineLevel.value!! + 2).coerceIn(0, 100))
                        }
                        com.example.focusguard.engine.BrainState.RECOVERY -> {
                            _focusLevel.postValue((_focusLevel.value!! + 2).coerceIn(0, 100))
                            _dopamineLevel.postValue((_dopamineLevel.value!! - 3).coerceIn(0, 100))
                        }
                    }
                }

                // Update Analytics History
                focusHistoryDeque.addLast(_focusLevel.value!!)
                if (focusHistoryDeque.size > 20) focusHistoryDeque.removeFirst()
                _focusHistory.postValue(focusHistoryDeque.toList())

                dopamineHistoryDeque.addLast(_dopamineLevel.value!!)
                if (dopamineHistoryDeque.size > 20) dopamineHistoryDeque.removeFirst()
                _dopamineHistory.postValue(dopamineHistoryDeque.toList())
            }
        }
    }

    fun purchaseItem(item: ShopItem): Boolean {
        val currentXP = _xp.value ?: 0
        if (currentXP >= item.cost) {
            _xp.value = currentXP - item.cost
            applyBooster(item.id)
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
        viewModelScope.launch {
            try {
                val response = generativeModel.generateContent("The topic is: $topic. Ask me a question to verify my understanding.")
                _aiQuestion.postValue(response.text ?: "What did you learn about $topic?")
            } catch (e: Exception) {
                _aiQuestion.postValue("Explain the most important part of $topic.")
            }
        }
    }

    fun completeStudySession(answeredCorrectly: Boolean) {
        if (answeredCorrectly) {
            _xp.value = (_xp.value ?: 0) + 50
            increaseFocus(15)
        } else {
            deductXP(0.10)
        }
        _isStudyActive.value = false
        com.example.focusguard.engine.CognitiveStateEngine.setStudyModeActive(false)
        getApplication<Application>().sendBroadcast(Intent("com.example.focusguard.STUDY_STATE_CHANGED"))
        _studyTopic.value = ""
    }

    fun abortStudySession() {
        deductXP(0.10)
        _isStudyActive.value = false
        com.example.focusguard.engine.CognitiveStateEngine.setStudyModeActive(false)
        getApplication<Application>().sendBroadcast(Intent("com.example.focusguard.STUDY_STATE_CHANGED"))
        _studyTopic.value = ""
    }

    private fun deductXP(percentage: Double) {
        val currentXP = _xp.value ?: 0
        val deduction = (currentXP * percentage).toInt()
        _xp.value = (currentXP - deduction).coerceAtLeast(0)
    }
}
