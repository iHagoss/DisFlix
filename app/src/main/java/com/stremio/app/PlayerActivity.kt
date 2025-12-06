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
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer

class PlayerActivity : AppCompatActivity(), SurfaceHolder.Callback {
    
    companion object {
        private const val TAG = "PlayerActivity"
        const val EXTRA_STREAM_URL = "stream_url"
        const val EXTRA_ITEM_ID = "item_id"
        const val EXTRA_INTRO_FROM = "intro_from"
        const val EXTRA_INTRO_TO = "intro_to"
    }
    
    private lateinit var surfaceView: SurfaceView
    private lateinit var skipIntroButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var timeText: TextView
    private lateinit var seekBar: SeekBar
    private lateinit var controlsContainer: View
    
    private var libVLC: LibVLC? = null
    private var mediaPlayer: MediaPlayer? = null
    private var skipIntroHandler: SkipIntroHandler? = null
    
    private val handler = Handler(Looper.getMainLooper())
    private var controlsVisible = true
    private var isPlaying = false
    private var itemId: String? = null
    private var hasLoadedIntroData = false
    
    private val hideControlsRunnable = Runnable {
        hideControls()
    }
    
    private val updateProgressRunnable = object : Runnable {
        override fun run() {
            updateProgress()
            handler.postDelayed(this, 1000)
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)
        
        initViews()
        initVLC()
        initSkipIntroHandler()
        
        val streamUrl = intent.getStringExtra(EXTRA_STREAM_URL)
        itemId = intent.getStringExtra(EXTRA_ITEM_ID)
        
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
    
    private fun initViews() {
        surfaceView = findViewById(R.id.surface)
        skipIntroButton = findViewById(R.id.skip_intro_button)
        progressBar = findViewById(R.id.progress_bar)
        timeText = findViewById(R.id.time_text)
        seekBar = findViewById(R.id.seek_bar)
        controlsContainer = findViewById(R.id.controls_container)
        
        surfaceView.holder.addCallback(this)
        
        surfaceView.setOnClickListener {
            toggleControls()
        }
        
        skipIntroButton.setOnClickListener {
            handleSkipIntro()
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
        if (skipIntroHandler?.shouldShowSkipButton(currentTimeMs) == true) {
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
        if (controlsVisible) {
            hideControls()
        } else {
            showControls()
        }
    }
    
    private fun showControls() {
        controlsContainer.visibility = View.VISIBLE
        controlsVisible = true
        scheduleHideControls()
    }
    
    private fun hideControls() {
        controlsContainer.visibility = View.GONE
        controlsVisible = false
    }
    
    private fun scheduleHideControls() {
        handler.removeCallbacks(hideControlsRunnable)
        handler.postDelayed(hideControlsRunnable, 5000)
    }
    
    override fun surfaceCreated(holder: SurfaceHolder) {
        mediaPlayer?.vlcVout?.apply {
            setVideoSurface(holder.surface, holder)
            attachViews()
        }
    }
    
    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        mediaPlayer?.vlcVout?.setWindowSize(width, height)
    }
    
    override fun surfaceDestroyed(holder: SurfaceHolder) {
        mediaPlayer?.vlcVout?.detachViews()
    }
    
    override fun onPause() {
        super.onPause()
        mediaPlayer?.pause()
    }
    
    override fun onResume() {
        super.onResume()
        if (isPlaying) {
            mediaPlayer?.play()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateProgressRunnable)
        handler.removeCallbacks(hideControlsRunnable)
        
        mediaPlayer?.stop()
        mediaPlayer?.detachViews()
        mediaPlayer?.release()
        libVLC?.release()
        
        mediaPlayer = null
        libVLC = null
    }
}
