package com.example

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.net.Uri
import android.os.*
import androidx.core.app.NotificationCompat
import com.example.data.AppDatabase
import kotlinx.coroutines.*
import java.io.File

class AthanService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    private lateinit var audioManager: AudioManager
    private var originalVolume = 0
    private var isPlaying = false
    private var volumeLockJob: Job? = null
    private var audioFocusRequest: AudioFocusRequest? = null

    companion object {
        const val CHANNEL_ID = "AlMinshawiAthanChannel"
        const val NOTIFICATION_ID = 1001
        var isServiceRunning = false
        var currentPrayerName = "الصلاة"
    }

    override fun onCreate() {
        super.onCreate()
        isServiceRunning = true
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        originalVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        currentPrayerName = intent?.getStringExtra("PRAYER_NAME") ?: "الصلاة"
        
        // Starts the Foreground service with interactive notifications
        val notificationIntent = Intent(this, MainActivity::class.java).apply {
            putExtra("SHOW_ALARM_SCREEN", true)
            putExtra("PRAYER_NAME", currentPrayerName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("حي على الصلاة - نداء الفلاح")
            .setContentText("حان الآن وقت $currentPrayerName بصوت الشيخ المنشاوي")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(pendingIntent, true)
            .build()

        startForeground(NOTIFICATION_ID, notification)

        playAthan()
        startVolumeLock()

        // Automatically launch the full screen lock activity
        val fullScreenIntent = Intent(applicationContext, AthanLockActivity::class.java).apply {
            putExtra("PRAYER_NAME", currentPrayerName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(fullScreenIntent)

        return START_STICKY
    }

    private fun playAthan() {
        if (isPlaying) return
        isPlaying = true

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
                audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                    .setAudioAttributes(audioAttributes)
                    .setAcceptsDelayedFocusGain(true)
                    .setOnAudioFocusChangeListener { }
                    .build()
                audioFocusRequest?.let { audioManager.requestAudioFocus(it) }
            } else {
                @Suppress("DEPRECATION")
                audioManager.requestAudioFocus(
                    { },
                    AudioManager.STREAM_ALARM,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        serviceScope.launch {
            val db = AppDatabase.getDatabase(this@AthanService)
            val config = db.configDao().getConfig()
            val isCommitment = config?.isCommitmentModeEnabled ?: true

            try {
                mediaPlayer = MediaPlayer()
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    val audioAttributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                    mediaPlayer?.setAudioAttributes(audioAttributes)
                } else {
                    @Suppress("DEPRECATION")
                    mediaPlayer?.setAudioStreamType(AudioManager.STREAM_ALARM)
                }

                val minshawiRealFile = File(filesDir, "minshawi_athan.mp3")
                if (minshawiRealFile.exists() && minshawiRealFile.length() > 100000) {
                    mediaPlayer?.setDataSource(minshawiRealFile.absolutePath)
                } else {
                    val alarmUri = Uri.parse("android.resource://${packageName}/raw/athan_minshawi")
                    try {
                        mediaPlayer?.setDataSource(this@AthanService, alarmUri)
                    } catch (e: Exception) {
                        val fallbackUri = Uri.parse("https://download.quranicaudio.com/adhan/sheikh_muhammad_siddiq_al-minshawi.mp3")
                        mediaPlayer?.setDataSource(this@AthanService, fallbackUri)
                    }
                }

                mediaPlayer?.isLooping = true
                mediaPlayer?.setVolume(1.0f, 1.0f)
                mediaPlayer?.prepareAsync()
                mediaPlayer?.setOnPreparedListener { mp ->
                    mp.start()
                }
                
                mediaPlayer?.setOnErrorListener { _, _, _ ->
                    playSystemFallback()
                    true
                }

            } catch (e: Exception) {
                playSystemFallback()
            }
        }
    }

    private fun playSystemFallback() {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    val attrs = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                    setAudioAttributes(attrs)
                } else {
                    @Suppress("DEPRECATION")
                    setAudioStreamType(AudioManager.STREAM_ALARM)
                }
                setDataSource(this@AthanService, android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI)
                isLooping = true
                setVolume(1.0f, 1.0f)
                prepare()
                start()
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    private fun startVolumeLock() {
        volumeLockJob = serviceScope.launch(Dispatchers.IO) {
            while (isActive) {
                val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, 0)
                val maxRingtone = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxRingtone, 0)
                delay(500)
            }
        }
    }

    fun stopAlarmSilently() {
        volumeLockJob?.cancel()
        isPlaying = false
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
            } else {
                @Suppress("DEPRECATION")
                audioManager.abandonAudioFocus { }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        stopSelf()
    }

    override fun onDestroy() {
        isServiceRunning = false
        volumeLockJob?.cancel()
        serviceJob.cancel()
        mediaPlayer?.release()
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
            } else {
                @Suppress("DEPRECATION")
                audioManager.abandonAudioFocus { }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "المنشاوى - منبه الأذان",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "قناة إشعارات أذان الشيخ المنشاوي بأعلى صوت"
                setBypassDnd(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }
}
