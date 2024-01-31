package com.example.musicplayer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class MusicPlayerService : Service() {

    val trackTitleDisplay = MutableLiveData<String>("")
    val artistNameDisplay = MutableLiveData<String>("")
    val trackImageDisplay = MutableLiveData<Int>(R.drawable.ic_launcher_foreground)
    val playTimeDisplay = MutableLiveData<Int>(0)
    val trackTimeDisplay = MutableLiveData<Int>(0)
    val isPlaying = MutableLiveData<Boolean>(false)

    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var mediaPlayer = MediaPlayer()
    private val binder = MusicBinder()

    private val playList = listOf(
        TrackData(
            trackTitle = "Linkin Park - In The End (Mellen Gi & Tommee Profitt Remix)",
            artistName = "TrapMusicHDTV",
            track = R.raw.track_in_the_end,
            trackImage = R.drawable.img_in_the_end
        ),
        TrackData(
            trackTitle = "Malia J - Smells Like Teen Spirit (Original Music Video)",
            artistName = "Malia J.",
            track = R.raw.track_smells_like_teen_spirit,
            trackImage = R.drawable.img_smells_like_teen_spirit
        ),
        TrackData(
            trackTitle = "SOROR - SIX DAY WAR (Colonel Bagshot Cover)",
            artistName = "SOROR",
            track = R.raw.track_six_day_war,
            trackImage = R.drawable.img_six_day_war
        )
    )
    private var currentTrack = playList.first()

    private var notificationId = 333

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> play()
            ACTION_PAUSE -> pause()
            ACTION_NEXT -> setNext()
            ACTION_PREVIOUS -> setPrevious()
        }

        return START_STICKY
    }

    override fun onCreate() {
        setTrack(currentTrack)
        super.onCreate()
        createNotification()
    }

    override fun onDestroy() {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(notificationId)
        isPlaying.value = false
        coroutineScope.cancel()
        mediaPlayer.release()
        super.onDestroy()
    }

    fun stop() {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(notificationId)
        mediaPlayer.stop()
        stopSelf()
    }

    private fun setTrack(track: TrackData) {
        mediaPlayer.release()
        mediaPlayer = MediaPlayer.create(this, track.track).apply {
            setOnCompletionListener {
                setNext()
            }
        }
        if (isPlaying.value == true) play()

        trackTitleDisplay.value = track.trackTitle
        artistNameDisplay.value = track.artistName
        trackImageDisplay.value = track.trackImage
        playTimeDisplay.value = mediaPlayer.currentPosition
        trackTimeDisplay.value = mediaPlayer.duration
        currentTrack = track

    }

    fun play() {
        mediaPlayer.start()
        isPlaying.value = true
        coroutineScope.launch {
            do {
                playTimeDisplay.value = mediaPlayer.currentPosition
                delay(333)
            } while (isPlaying.value == true)
        }
    }

    fun pause() {
        mediaPlayer.pause()
        isPlaying.value = false
    }

    fun setNext() {
        val currentIndex = playList.indexOf(currentTrack)
        val nextIndex =
            if (currentIndex == playList.size - 1) 0
            else currentIndex + 1
        setTrack(playList[nextIndex])
    }

    fun setPrevious() {
        val currentIndex = playList.indexOf(currentTrack)
        val previousIndex =
            if (currentIndex == 0) playList.size - 1
            else currentIndex - 1
        setTrack(playList[previousIndex])
    }

    inner class MusicBinder : Binder() {
        fun getService(): MusicPlayerService = this@MusicPlayerService
    }

    companion object {
        private const val CHANNEL_ID = "music_player_channel_id"
        const val ACTION_PAUSE = "PAUSE"
        const val ACTION_PLAY = "PLAY"
        const val ACTION_NEXT = "NEXT"
        const val ACTION_PREVIOUS = "PREVIOUS"
    }

    private fun createNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setupNotificationChannel()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            displayCustomNotification()
        }
    }

    private fun setupNotificationChannel() {
        val channelName = "Music Player"
        val channelDescription = "Controls for the music player"
        val channelImportance = NotificationManager.IMPORTANCE_DEFAULT

        val notificationChannel =
            NotificationChannel(CHANNEL_ID, channelName, channelImportance).apply {
                description = channelDescription
            }

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(notificationChannel)
    }

    private fun displayCustomNotification() {
        val mainActivityIntent = Intent(this, MainActivity::class.java)
        val mainActivityPendingIntent = PendingIntent
            .getActivity(this, 0, mainActivityIntent, PendingIntent.FLAG_IMMUTABLE)

        val musicPlayerServiceIntent = Intent(this, MusicPlayerService::class.java)
        val previousPendingIntent = PendingIntent
            .getService(
                this,
                0,
                musicPlayerServiceIntent.apply { action = "PREVIOUS" },
                PendingIntent.FLAG_IMMUTABLE
            )
        val playPendingIntent = PendingIntent
            .getService(
                this,
                0,
                musicPlayerServiceIntent.apply { action = "PLAY" },
                PendingIntent.FLAG_IMMUTABLE
            )
        val pausePendingIntent = PendingIntent
            .getService(
                this,
                0,
                musicPlayerServiceIntent.apply { action = "PAUSE" },
                PendingIntent.FLAG_IMMUTABLE
            )
        val nextPendingIntent = PendingIntent
            .getService(
                this,
                0,
                musicPlayerServiceIntent.apply { action = "NEXT" },
                PendingIntent.FLAG_IMMUTABLE
            )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.star_big_on)
            .setLargeIcon(
                BitmapFactory.decodeResource(
                    resources,
                    R.drawable.ic_launcher_foreground
                )
            )
            .addAction(android.R.drawable.ic_media_previous, "Previous", previousPendingIntent)
            .addAction(android.R.drawable.ic_media_play, "Play", playPendingIntent)
            .addAction(android.R.drawable.ic_media_pause, "Pause", pausePendingIntent)
            .addAction(android.R.drawable.ic_media_next, "Next", nextPendingIntent)
            .setStyle(androidx.media.app.NotificationCompat.MediaStyle())
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(mainActivityPendingIntent)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setOngoing(true)
            .build()

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, builder)
    }

}