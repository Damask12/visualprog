package com.example.android

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.util.Locale
import java.util.concurrent.TimeUnit
import android.provider.MediaStore

class MPlayer : AppCompatActivity() {
    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var playPauseButton: Button
    private lateinit var nextButton: Button
    private lateinit var prevButton: Button
    private lateinit var volumeSeekBar: SeekBar
    private lateinit var songNameTextView: TextView
    private lateinit var progressSeekBar: SeekBar

    private var currentSongIndex = 0
    private var songsList = mutableListOf<File>()
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mplayer)

        playPauseButton = findViewById(R.id.playPauseButton)
        nextButton = findViewById(R.id.nextButton)
        prevButton = findViewById(R.id.prevButton)
        volumeSeekBar = findViewById(R.id.volumeSeekBar)
        songNameTextView = findViewById(R.id.songNameTextView)
        progressSeekBar = findViewById(R.id.progressSeekBar)

        setupProgressBar()
        checkPermissions()
        setupButtons()
        setupVolumeControl()
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                PERMISSION_REQUEST_CODE
            )
        } else {
            initializePlayer()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            initializePlayer()
        }
    }

    private fun initializePlayer() {
        mediaPlayer = MediaPlayer()
        loadSongs()
    }

    private fun loadSongs() {
        songsList.clear()
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(MediaStore.Audio.Media.DATA)

        contentResolver.query(uri, projection, "${MediaStore.Audio.Media.IS_MUSIC} != 0", null, null)?.use { cursor ->
            val pathIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            while (cursor.moveToNext()) {
                val path = cursor.getString(pathIndex)
                if (path != null && path.endsWith(".mp3", true)) {
                    songsList.add(File(path))
                }
            }
        }

        if (songsList.isNotEmpty()) {
            playSong(currentSongIndex)
        }
    }

    private fun setupButtons() {
        playPauseButton.setOnClickListener {
            if (mediaPlayer.isPlaying) {
                mediaPlayer.pause()
                playPauseButton.setText(R.string.play_button)
            } else {
                mediaPlayer.start()
                playPauseButton.setText(R.string.pause_button)
                updateProgress()
            }
        }

        nextButton.setOnClickListener {
            currentSongIndex = (currentSongIndex + 1) % songsList.size
            playSong(currentSongIndex)
        }

        prevButton.setOnClickListener {
            currentSongIndex = (currentSongIndex - 1 + songsList.size) % songsList.size
            playSong(currentSongIndex)
        }
    }

    private fun setupVolumeControl() {
        val audioManager = getSystemService(AUDIO_SERVICE) as android.media.AudioManager
        volumeSeekBar.max = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
        volumeSeekBar.progress = audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)

        volumeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                audioManager.setStreamVolume(
                    android.media.AudioManager.STREAM_MUSIC,
                    progress,
                    0
                )
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
    }

    private fun setupProgressBar() {
        progressSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mediaPlayer.seekTo(progress)
                    updateSongTimeDisplay()
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                handler.removeCallbacksAndMessages(null)
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                if (mediaPlayer.isPlaying) {
                    updateProgress()
                }
            }
        })
    }

    private fun playSong(index: Int) {
            mediaPlayer.reset()
            mediaPlayer.setDataSource(songsList[index].absolutePath)
            mediaPlayer.prepare()

            progressSeekBar.max = mediaPlayer.duration

            mediaPlayer.start()
            updateProgress()

            mediaPlayer.setOnCompletionListener {
                playNextSong()
            }
    }

    private fun updateProgress() {
        if (mediaPlayer.isPlaying) {
            progressSeekBar.progress = mediaPlayer.currentPosition
            updateSongTimeDisplay()

            handler.postDelayed({ updateProgress() }, 500)
        }
    }

    private fun updateSongTimeDisplay() {
        songNameTextView.text = getString(R.string.song_progress_format,
            getCurrentSongName(),
            formatTime(mediaPlayer.currentPosition),
            formatTime(mediaPlayer.duration))
    }

    private fun playNextSong() {
        currentSongIndex = (currentSongIndex + 1) % songsList.size
        playSong(currentSongIndex)
    }

    private fun getCurrentSongName(): String {
        return songsList[currentSongIndex].name.replace(".mp3", "", true)
    }

    private fun formatTime(millis: Int): String {
        return String.format(
            Locale.getDefault(),
            "%02d:%02d",
            TimeUnit.MILLISECONDS.toMinutes(millis.toLong()),
            TimeUnit.MILLISECONDS.toSeconds(millis.toLong()) % 60
        )
    }

    override fun onDestroy() {
        mediaPlayer.release()
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 1001
    }
}