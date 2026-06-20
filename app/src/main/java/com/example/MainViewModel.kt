package com.example

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.util.PrayerTimesCalculator
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

import kotlinx.coroutines.Dispatchers
import java.io.File
import java.net.URL
import java.net.HttpURLConnection

sealed class DownloadState {
    object Idle : DownloadState()
    object Downloading : DownloadState()
    object Success : DownloadState()
    data class Error(val message: String) : DownloadState()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = AppConfigRepository(db.configDao())

    val configState: StateFlow<AppConfig> = repository.configFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppConfig()
        )

    private val _nextPrayerCountdown = MutableStateFlow("00:00:00")
    val nextPrayerCountdown: StateFlow<String> = _nextPrayerCountdown.asStateFlow()

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    private val _nextPrayerName = MutableStateFlow("الفجر")
    val nextPrayerName: StateFlow<String> = _nextPrayerName.asStateFlow()

    private val _nextPrayerTime = MutableStateFlow("12:08")
    val nextPrayerTime: StateFlow<String> = _nextPrayerTime.asStateFlow()

    private val _prayerTimesToday = MutableStateFlow<PrayerTimesCalculator.PrayerTimes?>(null)
    val prayerTimesToday: StateFlow<PrayerTimesCalculator.PrayerTimes?> = _prayerTimesToday.asStateFlow()

    private var clockJob: Job? = null

    init {
        // Enforce populated default settings row in DB on startup
        viewModelScope.launch {
            val existing = db.configDao().getConfig()
            if (existing == null) {
                db.configDao().saveConfig(AppConfig())
            }
            refreshPrayerTimes()
            startCountdownClock()
        }
        downloadAthanIfMissing()
    }

    fun downloadAthanIfMissing() {
        val context = getApplication<Application>()
        val file = File(context.filesDir, "minshawi_athan.mp3")
        if (file.exists() && file.length() > 100000) {
            _downloadState.value = DownloadState.Success
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _downloadState.value = DownloadState.Downloading
            try {
                val url = URL("https://download.quranicaudio.com/adhan/sheikh_muhammad_siddiq_al-minshawi.mp3")
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                connection.connect()

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val tempFile = File(context.cacheDir, "temp_athan.mp3")
                    val inputStream = connection.inputStream
                    val outputStream = tempFile.outputStream()
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                    }
                    outputStream.flush()
                    outputStream.close()
                    inputStream.close()

                    if (tempFile.exists() && tempFile.length() > 0) {
                        tempFile.copyTo(file, overwrite = true)
                        tempFile.delete()
                        _downloadState.value = DownloadState.Success
                    } else {
                        _downloadState.value = DownloadState.Error("تحميل ملف فارغ")
                    }
                } else {
                    _downloadState.value = DownloadState.Error("خطأ تحميل: ${connection.responseCode}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _downloadState.value = DownloadState.Error(e.localizedMessage ?: "حدث خطأ غير معروف")
            }
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            val current = repository.getConfig()
            repository.updateConfig(current.copy(isOnboardingCompleted = true))
        }
    }

    fun updatePinCode(newPin: String) {
        viewModelScope.launch {
            val current = repository.getConfig()
            repository.updateConfig(current.copy(pinCode = newPin))
        }
    }

    fun updateCalculationMethod(method: String) {
        viewModelScope.launch {
            val current = repository.getConfig()
            val updated = current.copy(calculationMethod = method)
            repository.updateConfig(updated)
            refreshPrayerTimes()
            // Reset system alarms to match new methods
            AlMinshawiAlarmReceiver.scheduleAlarms(getApplication())
        }
    }

    fun updateCoords(lat: Double, lng: Double, name: String) {
        viewModelScope.launch {
            val current = repository.getConfig()
            val updated = current.copy(customLatitude = lat, customLongitude = lng, locationName = name)
            repository.updateConfig(updated)
            refreshPrayerTimes()
            AlMinshawiAlarmReceiver.scheduleAlarms(getApplication())
        }
    }

    fun togglePrayerAlarm(prayer: String, isEnabled: Boolean) {
        viewModelScope.launch {
            val current = repository.getConfig()
            val updated = when (prayer) {
                "الفجر" -> current.copy(isFajrEnabled = isEnabled)
                "الظهر" -> current.copy(isDhuhrEnabled = isEnabled)
                "العصر" -> current.copy(isAsrEnabled = isEnabled)
                "المغرب" -> current.copy(isMaghribEnabled = isEnabled)
                "العشاء" -> current.copy(isIshaEnabled = isEnabled)
                else -> current
            }
            repository.updateConfig(updated)
            AlMinshawiAlarmReceiver.scheduleAlarms(getApplication())
        }
    }

    fun toggleCommitmentMode(isEnabled: Boolean) {
        viewModelScope.launch {
            val current = repository.getConfig()
            repository.updateConfig(current.copy(isCommitmentModeEnabled = isEnabled))
        }
    }

    fun refreshPrayerTimes() {
        val config = configState.value
        val calendar = Calendar.getInstance()
        val times = PrayerTimesCalculator.calculate(
            latitude = config.customLatitude,
            longitude = config.customLongitude,
            method = config.calculationMethod,
            calendar = calendar
        )
        _prayerTimesToday.value = times
    }

    private fun startCountdownClock() {
        clockJob?.cancel()
        clockJob = viewModelScope.launch {
            while (true) {
                calculateTimeRemaining()
                delay(1000)
            }
        }
    }

    private fun calculateTimeRemaining() {
        val times = _prayerTimesToday.value ?: return
        val now = Calendar.getInstance()
        val currentTimeMs = now.timeInMillis

        val prayersFormat = SimpleDateFormat("HH:mm", Locale.US)
        val prayersList = listOf(
            "الفجر" to times.fajr,
            "الظهر" to times.dhuhr,
            "العصر" to times.asr,
            "المغرب" to times.maghrib,
            "العشاء" to times.isha
        )

        var nextName = "الفجر"
        var nextTimeStr = times.fajr
        var nextCal = Calendar.getInstance()

        var found = false
        for (pair in prayersList) {
            val (name, timeStr) = pair
            val parts = timeStr.split(":")
            if (parts.size != 2) continue
            val hour = parts[0].toIntOrNull() ?: continue
            val min = parts[1].toIntOrNull() ?: continue

            val pCal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, min)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            if (pCal.timeInMillis > currentTimeMs) {
                nextName = name
                nextTimeStr = timeStr
                nextCal = pCal
                found = true
                break
            }
        }

        // If all prayers for today has passed, the next one is Fajr tomorrow
        if (!found) {
            nextName = "الفجر"
            nextTimeStr = times.fajr
            val parts = times.fajr.split(":")
            if (parts.size == 2) {
                val hour = parts[0].toInt()
                val min = parts[1].toInt()
                nextCal = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, 1)
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, min)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
            }
        }

        _nextPrayerName.value = nextName
        _nextPrayerTime.value = nextTimeStr

        val diffMs = nextCal.timeInMillis - currentTimeMs
        val hours = diffMs / 3600000
        val mins = (diffMs % 3600000) / 60000
        val secs = (diffMs % 60000) / 1000

        _nextPrayerCountdown.value = String.format("%02d:%02d:%02d", hours, mins, secs)
    }
}
