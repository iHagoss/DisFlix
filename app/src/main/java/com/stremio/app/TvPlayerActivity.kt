package com.stremio.app

import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import kotlin.math.max
import kotlin.math.min

class TvPlayerActivity : FragmentActivity(), SurfaceHolder.Callback {
    
    companion object {
        private const val TAG = "TvPlayerActivity"
        const val EXTRA_STREAM_URL = "stream_url"
        const val EXTRA_YOUTUBE_ID = "youtube_id" // New constant for YouTube stream ID
        const val EXTRA_ITEM_ID = "item_id"
        const val EXTRA_INTRO_FROM = "intro_from"
        const val EXTRA_INTRO_TO = "intro_to"
    }
    
    private lateinit var surfaceView: SurfaceView
    private lateinit var youtubePlayerView: YouTubePlayerView // New view for YouTube
    private lateinit var skipIntroButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var timeText: TextView
    private lateinit var seekBar: SeekBar
    private lateinit var controlsContainer: View
    
    private var libVLC: LibVLC? = null
    private var mediaPlayer: MediaPlayer? = null
    private var youtubePlayer: YouTubePlayer? = null // Stored instance of YouTube player
    private var skipIntroHandler: SkipIntroHandler? = null
    
    private val handler = Handler(Looper.getMainLooper())
    private var controlsVisible = true
    private var isPlaying = false
    private var itemId: String? = null
    private var hasLoadedIntroData = false
    private var youtubeId: String? = null
    private var isYoutubeStream = false
    
    private val hideControlsRunnable = Runnable {
        hideControls()
    }
    
    private val updateProgressRunnable = object : Runnable {
        override fun run() {
            if (!isYoutubeStream) {
                updateProgress()
                handler.postDelayed(this, 1000)
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tv_player)
        
        // StremioCore init is handled by MainActivity Router
        
        // Get intent data first
        val streamUrl = intent.getStringExtra(EXTRA_STREAM_URL)
        youtubeId = intent.getStringExtra(EXTRA_YOUTUBE_ID)
        itemId = intent.getStringExtra(EXTRA_ITEM_ID)
        
        isYoutubeStream = !youtubeId.isNullOrEmpty()
        
        initViews()
        initSkipIntroHandler()
        
        if (isYoutubeStream) {
            initYouTubePlayer(youtubeId!!)
        } else {
            initVLC()
            
            val introFrom = intent.getLongExtra(EXTRA_INTRO_FROM, -1)
            val introTo = intent.getLongExtra(EXTRA_INTRO_TO, -1)
            
            if (introFrom >= 0 && introTo > introFrom) {
                skipIntroHandler?.setIntroData(introFrom, introTo)
                hasLoadedIntroData = true
            }
            
            if (!streamUrl.isNullOrEmpty()) {
                playStream(streamUrl)
            }
        }
    }
    
    private fun initViews() {
        surfaceView = findViewById(R.id.surface)
        youtubePlayerView = findViewById(R.id.youtube_player_view) // Assuming this ID is in activity_tv_player.xml
        skipIntroButton = findViewById(R.id.skip_intro_button)
        progressBar = findViewById(R.id.progress_bar)
        timeText = findViewById(R.id.time_text)
        seekBar = findViewById(R.id.seek_bar)
        controlsContainer = findViewById(R.id.controls_container)
        
        // Conditional visibility and setup
        if (isYoutubeStream) {
            surfaceView.visibility = View.GONE
            youtubePlayerView.visibility = View.VISIBLE
        } else {
            youtubePlayerView.visibility = View.GONE
            surfaceView.visibility = View.VISIBLE
            surfaceView.holder.addCallback(this)
        }
        
        skipIntroButton.setOnClickListener {
            handleSkipIntro()
        }
        
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (!isYoutubeStream && fromUser) { // Only for VLC stream
                    val duration = mediaPlayer?.length ?: 0
                    val newTime = (progress * duration / 100).toLong()
                    mediaPlayer?.time = newTime
                }
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                handler.removeCallbacks(hideControlsRunnable)
            }
            
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                scheduleHideControls()
            }
        })
    }
    
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                togglePlayPause()
                return true
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                seekBackward()
                return true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                seekForward()
                return true
            }
            KeyEvent.KEYCODE_BACK -> {
                if (controlsVisible) {
                    hideControls()
                    return true
                }
            }
        }
        showControls()
        return super.onKeyDown(keyCode, event)
    }
    
    private fun togglePlayPause() {
        if (isYoutubeStream) {
            // Rely on the YouTube player's built-in controls for play/pause on TV.
            // This method will rely on the default behavior of the YouTubePlayerView on TV.
            // Returning early prevents the VLC logic from running.
            return
        }
        
        if (isPlaying) {
            mediaPlayer?.pause()
        } else {
            mediaPlayer?.play()
        }
    }
    
    private fun seekBackward() {
        if (isYoutubeStream) {
            // No simple API for relative seek. Rely on YouTube player's native controls for TV.
            return
        }
        val currentTime = mediaPlayer?.time ?: 0
        mediaPlayer?.time = max(0, currentTime - 10000)
    }
    
    private fun seekForward() {
        if (isYoutubeStream) {
            // No simple API for relative seek. Rely on YouTube player's native controls for TV.
            return
        }
        val currentTime = mediaPlayer?.time ?: 0
        val duration = mediaPlayer?.length ?: Long.MAX_VALUE
        mediaPlayer?.time = min(duration, currentTime + 10000)
    }
    
    private fun initVLC() {
        try {
            val options = ArrayList<String>()
            options.add("--aout=opensles")
            options.add("--audio-time-stretch")
            options.add("-vvv")
            
            libVLC = LibVLC(this, options)
            mediaPlayer = MediaPlayer(libVLC)
            
            mediaPlayer?.setEventListener { event ->
                when (event.type) {
                    MediaPlayer.Event.Playing -> {
                        runOnUiThread {
                            isPlaying = true
                            progressBar.visibility = View.GONE
                            handler.post(updateProgressRunnable)
                        }
                    }
                    MediaPlayer.Event.Paused -> {
                        runOnUiThread {
                            isPlaying = false
                        }
                    }
                    MediaPlayer.Event.EndReached -> {
                        runOnUiThread {
                            finish()
                        }
                    }
                    MediaPlayer.Event.Buffering -> {
                        runOnUiThread {
                            if (event.buffering < 100f) {
                                progressBar.visibility = View.VISIBLE
                            } else {
                                progressBar.visibility = View.GONE
                            }
                        }
                    }
                    MediaPlayer.Event.LengthChanged -> {
                        runOnUiThread {
                            val duration = event.lengthChanged
                            if (duration > 0 && !hasLoadedIntroData) {
                                loadIntroDataFromCore(duration)
                            }
                        }
                    }
                    MediaPlayer.Event.TimeChanged -> {
                        runOnUiThread {
                            checkSkipIntroVisibility(event.timeChanged)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize VLC: ${e.message}")
        }
    }
    
    private fun initYouTubePlayer(videoId: String) {
        // Add the YouTubePlayerView as a lifecycle observer
        lifecycle.addObserver(youtubePlayerView)
        
        progressBar.visibility = View.VISIBLE // Show initial loading progress bar
        
        youtubePlayerView.addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
            override fun onReady(youTubePlayer: YouTubePlayer) {
                this@TvPlayerActivity.youtubePlayer = youTubePlayer
                youTubePlayer.loadVideo(videoId, 0f) // Start playing from 0 seconds
            }
            
            override fun onStateChange(youTubePlayer: YouTubePlayer, state: com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants.PlayerState) {
                super.onStateChange(youTubePlayer, state)
                runOnUiThread {
                    // Update the loading indicator based on player state
                    if (state == com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants.PlayerState.PLAYING) {
                        progressBar.visibility = View.GONE
                    } else if (state == com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants.PlayerState.BUFFERING) {
                        progressBar.visibility = View.VISIBLE
                    }
                    
                    if (state == com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants.PlayerState.ENDED) {
                        finish()
                    }
                }
            }
        })
        
        // Hide VLC-specific controls as they are not easily integrated with YouTube player
        controlsContainer.visibility = View.GONE
        timeText.visibility = View.GONE
        seekBar.visibility = View.GONE
        skipIntroButton.visibility = View.GONE
    }
    
    private fun loadIntroDataFromCore(duration: Long) {
        val id = itemId
        if (id.isNullOrEmpty()) return
        
        Thread {
            try {
                val loaded = skipIntroHandler?.loadIntroDataFromCore(id, duration) ?: false
                if (loaded) {
                    hasLoadedIntroData = true
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load intro data: ${e.message}")
            }
        }.start()
    }
    
    private fun initSkipIntroHandler() {
        skipIntroHandler = SkipIntroHandler(this)
    }
    
    private fun playStream(url: String) {
        try {
            progressBar.visibility = View.VISIBLE
            
            val media = Media(libVLC, Uri.parse(url))
            media.setHWDecoderEnabled(true, false)
            
            mediaPlayer?.media = media
            media.release()
            
            mediaPlayer?.play()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play stream: ${e.message}")
        }
    }
    
    private fun updateProgress() {
        val time = mediaPlayer?.time ?: 0
        val duration = mediaPlayer?.length ?: 1
        
        val progress = (time * 100 / duration).toInt()
        seekBar.progress = progress
        
        timeText.text = "${formatTime(time)} / ${formatTime(duration)}"
    }
    
    private fun formatTime(timeMs: Long): String {
        val seconds = (timeMs / 1000) % 60
        val minutes = (timeMs / 1000 / 60) % 60
        val hours = timeMs / 1000 / 60 / 60
        
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }
    
    private fun checkSkipIntroVisibility(currentTimeMs: Long) {
        if (skipIntroHandler?.shouldShowSkipButton(currentTimeMs) == true) {
            if (skipIntroButton.visibility != View.VISIBLE) {
                skipIntroButton.visibility = View.VISIBLE
            }
        } else {
            if (skipIntroButton.visibility == View.VISIBLE) {
                skipIntroButton.visibility = View.GONE
            }
        }
    }
    
    private fun handleSkipIntro() {
        val endTime = skipIntroHandler?.getSkipToTime()
        if (endTime != null && endTime > 0) {
            mediaPlayer?.time = endTime
            skipIntroButton.visibility = View.GONE
            skipIntroHandler?.clearIntroData()
            StremioCore.dispatchSkipIntro()
        }
    }
    
    private fun showControls() {
        if (isYoutubeStream) {
            // YouTube player has its own UI controls, do not show VLC ones.
            return
        }
        
        controlsContainer.visibility = View.VISIBLE
        controlsVisible = true
        scheduleHideControls()
    }
    
    private fun hideControls() {
        if (isYoutubeStream) {
            return
        }
        controlsContainer.visibility = View.GONE
        controlsVisible = false
    }
    
    private fun scheduleHideControls() {
        handler.removeCallbacks(hideControlsRunnable)
        handler.postDelayed(hideControlsRunnable, 5000)
    }
    
    override fun surfaceCreated(holder: SurfaceHolder) {
        if (!isYoutubeStream) {
            mediaPlayer?.vlcVout?.apply {
                setVideoSurface(holder.surface, holder)
                attachViews()
            }
        }
    }
    
    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        if (!isYoutubeStream) {
            mediaPlayer?.vlcVout?.setWindowSize(width, height)
        }
    }
    
    override fun surfaceDestroyed(holder: SurfaceHolder) {
        if (!isYoutubeStream) {
            mediaPlayer?.vlcVout?.detachViews()
        }
    }
    
    override fun onPause() {
        super.onPause()
        if (!isYoutubeStream) {
            mediaPlayer?.pause()
        }
        // YouTube player's onPause is handled by the lifecycle observer
    }
    
    override fun onResume() {
        super.onResume()
        if (!isYoutubeStream) {
            if (isPlaying) {
                mediaPlayer?.play()
            }
        }
        // YouTube player's onResume is handled by the lifecycle observer
    }
    
    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateProgressRunnable)
        handler.removeCallbacks(hideControlsRunnable)
        
        if (!isYoutubeStream) {
            // VLC Cleanup
            mediaPlayer?.stop()
            mediaPlayer?.detachViews()
            mediaPlayer?.release()
            libVLC?.release()
            
            mediaPlayer = null
            libVLC = null
        } else {
            // YouTube Player Cleanup (redundant due to lifecycle observer, but good practice)
            youtubePlayerView.release()
        }
        
        youtubePlayer = null
        
        // StremioCore shutdown is handled by MainActivity Router
    }
}
