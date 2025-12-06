package com.stremio.app

import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer

class PlayerActivity : AppCompatActivity(), SurfaceHolder.Callback {
    
    companion object {
        private const val TAG = "PlayerActivity"
        const val EXTRA_STREAM_URL = "stream_url"
        const val EXTRA_YOUTUBE_ID = "youtube_id"
        const val EXTRA_ITEM_ID = "item_id"
        const val EXTRA_INTRO_FROM = "intro_from"
        const val EXTRA_INTRO_TO = "intro_to"
    }
    
    private lateinit var surfaceView: SurfaceView
    private lateinit var youtubePlayerView: YouTubePlayerView
    private lateinit var skipIntroButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var timeText: TextView
    private lateinit var seekBar: SeekBar
    private lateinit var controlsContainer: View
    
    private var libVLC: LibVLC? = null
    private var mediaPlayer: MediaPlayer? = null
    private var youtubePlayer: YouTubePlayer? = null
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
        setContentView(R.layout.activity_player)
        
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
                Log.d(TAG, "Loaded intro data from intent: from=$introFrom, to=$introTo")
            }
            
            if (!streamUrl.isNullOrEmpty()) {
                playStream(streamUrl)
            }
        }
    }
    
    private fun initViews() {
        surfaceView = findViewById(R.id.surface)
        youtubePlayerView = findViewById(R.id.youtube_player_view)
        skipIntroButton = findViewById(R.id.skip_intro_button)
        progressBar = findViewById(R.id.progress_bar)
        timeText = findViewById(R.id.time_text)
        seekBar = findViewById(R.id.seek_bar)
        controlsContainer = findViewById(R.id.controls_container)
        
        if (isYoutubeStream) {
            surfaceView.visibility = View.GONE
            youtubePlayerView.visibility = View.VISIBLE
            // Hide VLC-specific controls
            controlsContainer.visibility = View.GONE
            timeText.visibility = View.GONE
            seekBar.visibility = View.GONE
            skipIntroButton.visibility = View.GONE
            
            // Do not attach surface holder or set click listener for YouTube
            surfaceView.setOnClickListener(null)
        } else {
            youtubePlayerView.visibility = View.GONE
            surfaceView.visibility = View.VISIBLE
            surfaceView.holder.addCallback(this)
            
            surfaceView.setOnClickListener {
                toggleControls()
            }
            
            seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
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
        
        skipIntroButton.setOnClickListener {
            handleSkipIntro()
        }
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
                this@PlayerActivity.youtubePlayer = youTubePlayer
                // Note: The YouTube player has its own UI controls on mobile.
                youTubePlayer.loadVideo(videoId, 0f) // Start playing from 0 seconds
            }
            
            override fun onStateChange(youTubePlayer: YouTubePlayer, state: com.pierfrancescosoffritti.androidyoutubeplayer.core.player.utils.YouTubePlayerTracker.State) {
                super.onStateChange(youTubePlayer, state)
                runOnUiThread {
                    // Update the loading indicator based on player state
                    if (state == com.pierfrancescosoffritti.androidyoutubeplayer.core.player.utils.YouTubePlayerTracker.State.PLAYING) {
                        progressBar.visibility = View.GONE
                    } else if (state == com.pierfrancescosoffritti.androidyoutubeplayer.core.player.utils.YouTubePlayerTracker.State.BUFFERING) {
                        progressBar.visibility = View.VISIBLE
                    }
                    
                    if (state == com.pierfrancescosoffritti.androidyoutubeplayer.core.player.utils.YouTubePlayerTracker.State.ENDED) {
                        finish()
                    }
                }
            }
        })
    }
    
    private fun loadIntroDataFromCore(duration: Long) {
        val id = itemId
        if (id.isNullOrEmpty()) {
            Log.d(TAG, "No item ID available, cannot load intro data from core")
            return
        }
        
        Thread {
            try {
                val loaded = skipIntroHandler?.loadIntroDataFromCore(id, duration) ?: false
                if (loaded) {
                    hasLoadedIntroData = true
                    Log.i(TAG, "Successfully loaded intro data from core for item $id")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load intro data from core: ${e.message}")
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
        if (!isYoutubeStream && skipIntroHandler?.shouldShowSkipButton(currentTimeMs) == true) {
            if (skipIntroButton.visibility != View.VISIBLE) {
                skipIntroButton.visibility = View.VISIBLE
                Log.d(TAG, "Showing skip intro button at time $currentTimeMs")
            }
        } else {
            if (skipIntroButton.visibility == View.VISIBLE) {
                skipIntroButton.visibility = View.GONE
            }
        }
    }
    
    private fun handleSkipIntro() {
        if (isYoutubeStream) return
        
        val endTime = skipIntroHandler?.getSkipToTime()
        if (endTime != null && endTime > 0) {
            Log.i(TAG, "Skipping intro to time $endTime")
            mediaPlayer?.time = endTime
            skipIntroButton.visibility = View.GONE
            skipIntroHandler?.clearIntroData()
            StremioCore.dispatchSkipIntro()
        }
    }
    
    private fun toggleControls() {
        if (isYoutubeStream) return
        
        if (controlsVisible) {
            hideControls()
        } else {
            showControls()
        }
    }
    
    private fun showControls() {
        if (isYoutubeStream) return
        
        controlsContainer.visibility = View.VISIBLE
        controlsVisible = true
        scheduleHideControls()
    }
    
    private fun hideControls() {
        if (isYoutubeStream) return
        
        controlsContainer.visibility = View.GONE
        controlsVisible = false
    }
    
    private fun scheduleHideControls() {
        if (isYoutubeStream) return
        
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
            // YouTube Player Cleanup
            youtubePlayerView.release()
        }
        
        youtubePlayer = null
    }
}
