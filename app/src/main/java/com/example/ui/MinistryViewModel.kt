package com.example.ui

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.BibleStudent
import com.example.data.DailyPlan
import com.example.data.MagazinePlacement
import com.example.data.MinistryRepository
import com.example.data.ServiceReport
import com.example.data.UserProfile
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class MinistryViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = MinistryRepository(db)

    // Current Date Context
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1 // 1-indexed (1-12)
    val currentDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)

    // Selection Date for reports/planner
    val selectedYear = MutableStateFlow(currentYear)
    val selectedMonth = MutableStateFlow(currentMonth) // 1-12

    // Profile state
    val profileState: StateFlow<UserProfile> = repository.userProfile
        .combine(MutableStateFlow(UserProfile())) { dbProfile, defaultProfile ->
            dbProfile ?: defaultProfile
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserProfile()
        )

    // Reports of the selected month
    val reportsState: StateFlow<List<ServiceReport>> = combine(
        selectedYear,
        selectedMonth,
        repository.allReports
    ) { year, month, _ ->
        // Room flow triggers update, we filter locally or pull custom flow.
        // Let's pull monthly reports safely
        emptyList<ServiceReport>()
    }.combine(repository.allReports) { _, all ->
        all.filter { it.year == selectedYear.value && it.month == selectedMonth.value }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Daily Plans
    val plansState: StateFlow<List<DailyPlan>> = repository.allPlans
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Magazine placements marked
    val placementsState: StateFlow<List<MagazinePlacement>> = repository.allPlacements
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Bible Students
    val studentsState: StateFlow<List<BibleStudent>> = repository.allStudents
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Timer States
    private var timerJob: Job? = null
    private val _timerRemainingSeconds = MutableStateFlow(0)
    val timerRemainingSeconds: StateFlow<Int> = _timerRemainingSeconds.asStateFlow()

    private val _timerOriginalSeconds = MutableStateFlow(0)
    val timerOriginalSeconds: StateFlow<Int> = _timerOriginalSeconds.asStateFlow()

    private val _isTimerRunning = MutableStateFlow(false)
    val isTimerRunning: StateFlow<Boolean> = _isTimerRunning.asStateFlow()

    private val _showTimerFinishedAlert = MutableStateFlow(false)
    val showTimerFinishedAlert: StateFlow<Boolean> = _showTimerFinishedAlert.asStateFlow()

    // Monthly summary calculation
    val monthlyStats = combine(reportsState, profileState) { reports, profile ->
        val totalHours = reports.sumOf { it.hours }
        val totalMagazines = reports.sumOf { it.magazines }
        val totalVideos = reports.sumOf { it.videos }
        val totalReturnVisits = reports.sumOf { it.returnVisits }
        val targetHours = profile.monthlyGoalHours.toDouble()
        val percentage = if (targetHours > 0.0) {
            (totalHours / targetHours * 100.0).coerceAtMost(100.0)
        } else {
            100.0
        }
        
        MonthlyStats(
            totalHours = totalHours,
            totalMagazines = totalMagazines,
            totalVideos = totalVideos,
            totalReturnVisits = totalReturnVisits,
            targetHours = targetHours,
            progressPercentage = percentage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MonthlyStats()
    )

    // Profile actions
    fun updateProfile(name: String, role: String, goalHours: Int, dueDay: Int) {
        viewModelScope.launch {
            repository.saveProfile(
                UserProfile(
                    name = name,
                    role = role,
                    monthlyGoalHours = goalHours,
                    reportDueDay = dueDay
                )
            )
        }
    }

    // Reports actions
    fun addReport(day: Int, hours: Double, magazines: Int, videos: Int, returnVisits: Int, notes: String) {
        viewModelScope.launch {
            repository.insertReport(
                ServiceReport(
                    year = selectedYear.value,
                    month = selectedMonth.value,
                    day = day,
                    hours = hours,
                    magazines = magazines,
                    videos = videos,
                    returnVisits = returnVisits,
                    notes = notes
                )
            )
        }
    }

    fun deleteReport(id: Int) {
        viewModelScope.launch {
            repository.deleteReportById(id)
        }
    }

    // Plans actions
    fun addPlan(day: Int, activity: String, companion: String, notes: String) {
        viewModelScope.launch {
            repository.insertPlan(
                DailyPlan(
                    year = selectedYear.value,
                    month = selectedMonth.value,
                    day = day,
                    plannedActivity = activity,
                    companion = companion,
                    notes = notes,
                    isCompleted = false
                )
            )
        }
    }

    fun togglePlanCompletion(plan: DailyPlan) {
        viewModelScope.launch {
            repository.insertPlan(plan.copy(isCompleted = !plan.isCompleted))
        }
    }

    fun deletePlan(plan: DailyPlan) {
        viewModelScope.launch {
            repository.deletePlan(plan)
        }
    }

    // Placements actions
    fun addPlacement(personName: String, address: String, publications: String, notes: String) {
        viewModelScope.launch {
            repository.insertPlacement(
                MagazinePlacement(
                    personName = personName,
                    address = address,
                    publications = publications,
                    notes = notes
                )
            )
        }
    }

    fun deletePlacement(placement: MagazinePlacement) {
        viewModelScope.launch {
            repository.deletePlacement(placement)
        }
    }

    // Students actions
    fun addStudent(name: String, book: String, lesson: String, schedule: String, notes: String, isActive: Boolean = true) {
        viewModelScope.launch {
            repository.insertStudent(
                BibleStudent(
                    name = name,
                    bookOrTopic = book,
                    currentLesson = lesson,
                    studySchedule = schedule,
                    notes = notes,
                    isActive = isActive
                )
            )
        }
    }

    fun deleteStudent(student: BibleStudent) {
        viewModelScope.launch {
            repository.deleteStudent(student)
        }
    }

    // Calendar month shifting
    fun changeMonth(by: Int) {
        var nextMonth = selectedMonth.value + by
        var nextYear = selectedYear.value
        if (nextMonth > 12) {
            nextMonth = 1
            nextYear += 1
        } else if (nextMonth < 1) {
            nextMonth = 12
            nextYear -= 1
        }
        selectedMonth.value = nextMonth
        selectedYear.value = nextYear
    }

    // Countdown Timer logic
    fun setTimerDuration(minutes: Int) {
        val seconds = minutes * 60
        _timerRemainingSeconds.value = seconds
        _timerOriginalSeconds.value = seconds
        _isTimerRunning.value = false
        timerJob?.cancel()
    }

    fun startTimer() {
        if (_timerRemainingSeconds.value <= 0) return
        _isTimerRunning.value = true
        _showTimerFinishedAlert.value = false
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_timerRemainingSeconds.value > 0 && _isTimerRunning.value) {
                delay(1000)
                _timerRemainingSeconds.value -= 1
            }
            if (_timerRemainingSeconds.value == 0) {
                _isTimerRunning.value = false
                _showTimerFinishedAlert.value = true
                triggerHapticAndSoundFeedback()
            }
        }
    }

    fun pauseTimer() {
        _isTimerRunning.value = false
        timerJob?.cancel()
    }

    fun resetTimer() {
        _isTimerRunning.value = false
        timerJob?.cancel()
        _timerRemainingSeconds.value = _timerOriginalSeconds.value
        _showTimerFinishedAlert.value = false
    }

    fun clearFinishedAlert() {
        _showTimerFinishedAlert.value = false
    }

    // Trigger vibration haptic feedback on countdown end
    private fun triggerHapticAndSoundFeedback() {
        try {
            val ctx = getApplication<Application>().applicationContext
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            vibrator?.let {
                if (it.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        // Vibrate with custom pattern (heavy alert)
                        val pattern = longArrayOf(0, 500, 200, 500, 200, 500)
                        it.vibrate(VibrationEffect.createWaveform(pattern, -1))
                    } else {
                        @Suppress("DEPRECATION")
                        it.vibrate(1000)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Text formatting for sharing the report via WhatsApp/SMS
    fun generateShareReportText(monthName: String): String {
        val stats = monthlyStats.value
        return buildString {
            append("*📖 Meu Relatório de Serviço de Campo*\n")
            append("📅 *Mês:* $monthName de ${selectedYear.value}\n")
            append("━━━━━━━━━━━━━━━━━━━━\n")
            append("⏱️ *Horas:* ${String.format("%.1f", stats.totalHours)} h\n")
            append("📚 *Publicações:* ${stats.totalMagazines}\n")
            append("🎥 *Vídeos:* ${stats.totalVideos}\n")
            append("♻️ *Revisitas:* ${stats.totalReturnVisits}\n")
            
            // Count active students (isActive)
            val students = studentsState.value.count { it.isActive }
            append("📖 *Estudos Bíblicos:* $students\n")
            append("━━━━━━━━━━━━━━━━━━━━\n")
            if (profileState.value.name.isNotBlank()) {
                append("👤 *Publicador:* ${profileState.value.name}\n")
            }
            append("Enviado através do *Meu Ministério* App ✨")
        }
    }
}

data class MonthlyStats(
    val totalHours: Double = 0.0,
    val totalMagazines: Int = 0,
    val totalVideos: Int = 0,
    val totalReturnVisits: Int = 0,
    val targetHours: Double = 15.0,
    val progressPercentage: Double = 0.0
)
