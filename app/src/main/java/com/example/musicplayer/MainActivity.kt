package com.example.musicplayer

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.example.musicplayer.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var musicService: MusicPlayerService

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as MusicPlayerService.MusicBinder
            musicService = binder.getService()

            musicService.trackTitleDisplay.observe(this@MainActivity) {
                binding.tvTrackTitle.text = it
            }
            musicService.artistNameDisplay.observe(this@MainActivity) {
                binding.tvArtistName.text = it
            }
            musicService.trackImageDisplay.observe(this@MainActivity) {
                binding.ivTrack.setImageResource(it)
            }
            musicService.trackTimeDisplay.observe(this@MainActivity) {
                binding.tvTrackTime.text = formatTime(it)
                binding.pbTrack.max = it
            }
            musicService.playTimeDisplay.observe(this@MainActivity) {
                binding.tvPlayTime.text = formatTime(it)
                binding.pbTrack.progress = it
            }
            musicService.isPlaying.observe(this@MainActivity) {
                if (it) {
                    binding.btnPlay.visibility = View.INVISIBLE
                    binding.btnPause.visibility = View.VISIBLE
                } else {
                    binding.btnPlay.visibility = View.VISIBLE
                    binding.btnPause.visibility = View.INVISIBLE
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !Manifest.permission.POST_NOTIFICATIONS.checkPermissionGranted()) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
        }

        val intentBind = Intent(this, MusicPlayerService::class.java)
        bindService(intentBind, connection, Context.BIND_AUTO_CREATE)

        binding.btnPlay.setOnClickListener() {
            musicService.play()
        }
        binding.btnPause.setOnClickListener() {
            musicService.pause()
        }
        binding.btnNext.setOnClickListener() {
            musicService.setNext()
        }
        binding.btnPrevious.setOnClickListener() {
            musicService.setPrevious()
        }

        setContentView(view)
    }

    override fun onDestroy() {
        musicService.stop()
        unbindService(connection)
        super.onDestroy()
    }

    private fun String.checkPermissionGranted(): Boolean {
        return ActivityCompat.checkSelfPermission(this@MainActivity, this) == PackageManager.PERMISSION_GRANTED
    }

    private fun formatTime(millis: Int): String {
        val minutes = millis / (1000 * 60)
        val seconds = (millis / 1000) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }
}
