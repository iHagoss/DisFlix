package com.stremio.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout

class PlayerActivity : AppCompatActivity() {

    companion object {
        const val TAG = "PlayerActivity"
        const val EXTRA_STREAM_URL = "stream_url"
        const val EXTRA_TITLE = "title"
        const val EXTRA_META_ID = "meta_id"
        const val EXTRA_VIDEO_ID = "video_id"
    }

    private var libVLC: LibVLC? = null
    private var mediaPlayer: MediaPlayer? = null
    private var videoLayout: VLCVideoLayout? = null

    private var playPauseButton: ImageButton? = null
    private var seekBar: SeekBar? = null
    private var currentTimeText: TextView? = null
    private var totalTimeText: TextView? = null
    private var titleText: TextView? = null

    private var streamUrl: String? = null
    private var videoTitle: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep screen on during playback
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Get intent extras
        streamUrl = intent.getStringExtra(EXTRA_STREAM_URL)
        videoTitle = intent.getStringExtra(EXTRA_TITLE)

        if (streamUrl == null) {
            Log.e(TAG, "No stream URL provided")
            finish()
            return
        }

        Log.i(TAG, "Starting playback: $videoTitle - $streamUrl")

        setContentView(R.layout.activity_player)

        initializeViews()
        initializePlayer()
        playMedia(streamUrl!!)
    }

    private fun initializeViews() {
        videoLayout = findViewById(R.id.video_layout)
        playPauseButton = findViewById(R.id.play_pause_button)
        seekBar = findViewById(R.id.seek_bar)
        currentTimeText = findViewById(R.id.current_time)
        totalTimeText = findViewById(R.id.total_time)
        titleText = findViewById(R.id.video_title)

        titleText?.text = videoTitle ?: "Playing..."

        playPauseButton?.setOnClickListener {
            togglePlayPause()
        }

        seekBar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mediaPlayer?.time = progress.toLong()
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        findViewById<ImageButton>(R.id.back_button)?.setOnClickListener {
            finish()
        }
    }

    private fun initializePlayer() {
        try {
            val options = ArrayList<String>().apply {
                add("--aout=opensles")
                add("--audio-time-stretch")
                add("-vvv")
            }

            libVLC = LibVLC(this, options)
            mediaPlayer = MediaPlayer(libVLC).apply {
                attachViews(videoLayout, null, false, false)

                // Set event listeners
                setEventListener { event ->
                    when (event.type) {
                        MediaPlayer.Event.Playing -> {
                            Log.d(TAG, "Playing")
                            updatePlayPauseButton(true)
                        }
                        MediaPlayer.Event.Paused -> {
                            Log.d(TAG, "Paused")
                            updatePlayPauseButton(false)
                        }
                        MediaPlayer.Event.EndReached -> {
                            Log.d(TAG, "End reached")
                            finish()
                        }
                        MediaPlayer.Event.EncounteredError -> {
                            Log.e(TAG, "Player error")
                            showError("Playback error occurred")
                        }
                        MediaPlayer.Event.TimeChanged -> {
                            updateProgress()
                        }
                        MediaPlayer.Event.LengthChanged -> {
                            val duration = event.lengthChanged
                            seekBar?.max = duration.toInt()
                            totalTimeText?.text = formatTime(duration)
                        }
                    }
                }
            }

            Log.i(TAG, "VLC player initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize VLC player", e)
            showError("Failed to initialize player: ${e.message}")
        }
    }

    private fun playMedia(url: String) {
        try {
            val media = Media(libVLC, Uri.parse(url))
            media.setHWDecoderEnabled(true, false)
            mediaPlayer?.media = media
            media.release()
            mediaPlayer?.play()
            Log.i(TAG, "Playing: $url")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play media", e)
            showError("Failed to play video: ${e.message}")
        }
    }

    private fun togglePlayPause() {
        mediaPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
            } else {
                player.play()
            }
        }
    }

    private fun updatePlayPauseButton(isPlaying: Boolean) {
        runOnUiThread {
            playPauseButton?.setImageResource(
                if (isPlaying) R.drawable.pause else R.drawable.play
            )
        }
    }

    private fun updateProgress() {
        mediaPlayer?.let { player ->
            runOnUiThread {
                val currentTime = player.time
                seekBar?.progress = currentTime.toInt()
                currentTimeText?.text = formatTime(currentTime)
            }
        }
    }

    private fun formatTime(timeMs: Long): String {
        val seconds = (timeMs / 1000) % 60
        val minutes = (timeMs / (1000 * 60)) % 60
        val hours = timeMs / (1000 * 60 * 60)

        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%d:%02d", minutes, seconds)
        }
    }

    private fun showError(message: String) {
        runOnUiThread {
            android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_LONG).show()
        }
    }

    override fun onStop() {
        super.onStop()
        mediaPlayer?.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.detachViews()
        mediaPlayer?.release()
        libVLC?.release()
        Log.i(TAG, "Player resources released")
    }
}