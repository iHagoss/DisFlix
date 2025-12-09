package com.stremio.app

import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.smoothstreaming.SsMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView

@UnstableApi
class ExoPlayerActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "ExoPlayerActivity"
        const val EXTRA_STREAM_URL = "stream_url"
        const val EXTRA_ITEM_ID = "item_id"
        const val EXTRA_ITEM_TITLE = "item_title"
        const val EXTRA_INTRO_FROM = "intro_from"
        const val EXTRA_INTRO_TO = "intro_to"
    }
    
    private lateinit var playerView: PlayerView
    private lateinit var skipIntroButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var titleText: TextView
    
    private var exoPlayer: ExoPlayer? = null
    private var skipIntroHandler: SkipIntroHandler? = null
    
    private val handler = Handler(Looper.getMainLooper())
    private var itemId: String? = null
    private var hasLoadedIntroData = false
    private var introFromMs: Long = -1
    private var introToMs: Long = -1
    
    private val updateProgressRunnable = object : Runnable {
        override fun run() {
            checkSkipIntroVisibility()
            handler.postDelayed(this, 1000)
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exo_player)
        
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        
        val streamUrl = intent.getStringExtra(EXTRA_STREAM_URL)
        itemId = intent.getStringExtra(EXTRA_ITEM_ID)
        val title = intent.getStringExtra(EXTRA_ITEM_TITLE)
        introFromMs = intent.getLongExtra(EXTRA_INTRO_FROM, -1)
        introToMs = intent.getLongExtra(EXTRA_INTRO_TO, -1)
        
        initViews()
        initSkipIntroHandler()
        initPlayer()
        
        if (introFromMs >= 0 && introToMs > introFromMs) {
            skipIntroHandler?.setIntroData(introFromMs, introToMs)
            hasLoadedIntroData = true
        }
        
        title?.let { titleText.text = it }
        
        if (!streamUrl.isNullOrEmpty()) {
            playStream(streamUrl)
        }
    }
    
    private fun initViews() {
        playerView = findViewById(R.id.player_view)
        skipIntroButton = findViewById(R.id.skip_intro_button)
        progressBar = findViewById(R.id.progress_bar)
        titleText = findViewById(R.id.title_text)
        
        skipIntroButton.setOnClickListener {
            handleSkipIntro()
        }
    }
    
    private fun initSkipIntroHandler() {
        skipIntroHandler = SkipIntroHandler(this)
    }
    
    private fun initPlayer() {
        exoPlayer = ExoPlayer.Builder(this)
            .build()
            .also { player ->
                playerView.player = player
                
                player.addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        when (playbackState) {
                            Player.STATE_BUFFERING -> {
                                progressBar.visibility = View.VISIBLE
                            }
                            Player.STATE_READY -> {
                                progressBar.visibility = View.GONE
                                handler.post(updateProgressRunnable)
                                
                                if (!hasLoadedIntroData) {
                                    val duration = player.duration
                                    if (duration > 0) {
                                        loadIntroDataFromCore(duration)
                                    }
                                }
                            }
                            Player.STATE_ENDED -> {
                                finish()
                            }
                            Player.STATE_IDLE -> {
                                progressBar.visibility = View.GONE
                            }
                        }
                    }
                    
                    override fun onPlayerError(error: PlaybackException) {
                        Log.e(TAG, "Player error: ${error.message}")
                        progressBar.visibility = View.GONE
                    }
                })
            }
    }
    
    private fun playStream(url: String) {
        progressBar.visibility = View.VISIBLE
        
        val dataSourceFactory = DefaultDataSource.Factory(
            this,
            DefaultHttpDataSource.Factory()
                .setAllowCrossProtocolRedirects(true)
                .setConnectTimeoutMs(15000)
                .setReadTimeoutMs(15000)
        )
        
        val uri = Uri.parse(url)
        val mediaSource = createMediaSource(uri, dataSourceFactory)
        
        exoPlayer?.apply {
            setMediaSource(mediaSource)
            prepare()
            playWhenReady = true
        }
    }
    
    private fun createMediaSource(uri: Uri, dataSourceFactory: DefaultDataSource.Factory): MediaSource {
        val path = uri.path?.lowercase() ?: ""
        val lastPathSegment = uri.lastPathSegment?.lowercase() ?: ""
        
        return when {
            path.endsWith(".m3u8") || lastPathSegment.endsWith(".m3u8") -> {
                HlsMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(MediaItem.fromUri(uri))
            }
            path.endsWith(".mpd") || lastPathSegment.endsWith(".mpd") -> {
                DashMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(MediaItem.fromUri(uri))
            }
            path.endsWith(".ism") || path.contains("/manifest") -> {
                SsMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(MediaItem.fromUri(uri))
            }
            else -> {
                ProgressiveMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(MediaItem.fromUri(uri))
            }
        }
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
    
    private fun checkSkipIntroVisibility() {
        val currentPosition = exoPlayer?.currentPosition ?: 0
        
        if (skipIntroHandler?.shouldShowSkipButton(currentPosition) == true) {
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
            Log.i(TAG, "Skipping intro to time $endTime")
            exoPlayer?.seekTo(endTime)
            skipIntroButton.visibility = View.GONE
            skipIntroHandler?.clearIntroData()
            StremioCore.dispatchSkipIntro()
        }
    }
    
    override fun onPause() {
        super.onPause()
        exoPlayer?.pause()
    }
    
    override fun onResume() {
        super.onResume()
        exoPlayer?.play()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateProgressRunnable)
        exoPlayer?.release()
        exoPlayer = null
    }
}
