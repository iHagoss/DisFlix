package com.stremio.app

import android.content.Context
import android.util.Log
import android.webkit.WebView
import org.json.JSONObject

data class IntroData(
    val from: Long,
    val to: Long
)

data class ParsedSkipIntroData(
    val accuracy: String,
    val intros: Map<Long, IntroData>
)

class SkipIntroHandler(private val context: Context) {
    
    private var currentIntroData: IntroData? = null
    private var isIntroActive: Boolean = false
    private var videoDuration: Long = 0
    
    companion object {
        private const val TAG = "SkipIntroHandler"
        
        fun parseSkipIntroResponse(json: String): ParsedSkipIntroData? {
            return try {
                val jsonObj = JSONObject(json)
                val accuracy = jsonObj.optString("accuracy", "")
                val introsObj = jsonObj.optJSONObject("intros") ?: return null
                
                val intros = mutableMapOf<Long, IntroData>()
                val keys = introsObj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val duration = key.toLongOrNull() ?: continue
                    val introObj = introsObj.optJSONObject(key) ?: continue
                    val from = introObj.optLong("from", -1)
                    val to = introObj.optLong("to", -1)
                    if (from >= 0 && to > from) {
                        intros[duration] = IntroData(from, to)
                    }
                }
                
                if (intros.isEmpty()) null else ParsedSkipIntroData(accuracy, intros)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse skip intro response: ${e.message}")
                null
            }
        }
        
        fun findMatchingIntro(response: ParsedSkipIntroData, videoDuration: Long, tolerance: Long = 5000): IntroData? {
            if (response.intros.isEmpty()) return null
            
            var bestMatch: IntroData? = null
            var smallestDiff = Long.MAX_VALUE
            
            for ((duration, intro) in response.intros) {
                val diff = kotlin.math.abs(duration - videoDuration)
                if (diff < smallestDiff && diff <= tolerance) {
                    smallestDiff = diff
                    bestMatch = intro
                }
            }
            
            if (bestMatch == null && response.intros.isNotEmpty()) {
                bestMatch = response.intros.values.first()
            }
            
            return bestMatch
        }
    }
    
    fun setVideoDuration(duration: Long) {
        this.videoDuration = duration
    }
    
    fun loadIntroDataFromCore(itemId: String, duration: Long): Boolean {
        try {
            val json = StremioCore.getSkipIntroData(itemId, duration)
            val response = parseSkipIntroResponse(json)
            
            if (response != null) {
                val matchingIntro = findMatchingIntro(response, duration)
                if (matchingIntro != null) {
                    currentIntroData = matchingIntro
                    Log.i(TAG, "Loaded intro data: from=${matchingIntro.from}, to=${matchingIntro.to}")
                    return true
                }
            }
            Log.d(TAG, "No matching intro data found for duration $duration")
            return false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load intro data: ${e.message}")
            return false
        }
    }
    
    fun setIntroData(from: Long, to: Long) {
        if (to > from) {
            currentIntroData = IntroData(from, to)
            Log.i(TAG, "Set intro data: from=$from, to=$to")
        }
    }
    
    fun clearIntroData() {
        currentIntroData = null
        isIntroActive = false
    }
    
    fun isIntroAvailable(): Boolean {
        return currentIntroData != null
    }
    
    fun shouldShowSkipButton(currentTimeMs: Long): Boolean {
        val intro = currentIntroData ?: return false
        return currentTimeMs >= intro.from && currentTimeMs < intro.to
    }
    
    fun handleSkipIntro(introData: IntroData?, webView: WebView?) {
        val intro = introData ?: currentIntroData ?: return
        
        webView?.let { view ->
            val seekTime = intro.to
            val js = """
                (function() {
                    if (window.stremio && window.stremio.player) {
                        window.stremio.player.seek($seekTime);
                    } else if (window.dispatchStremioEvent) {
                        window.dispatchStremioEvent('Player.SkipIntro', { time: $seekTime });
                    } else {
                        var video = document.querySelector('video');
                        if (video) {
                            video.currentTime = $seekTime / 1000;
                        }
                    }
                })();
            """.trimIndent()
            view.evaluateJavascript(js, null)
        }
        
        StremioCore.dispatchSkipIntro()
        isIntroActive = false
    }
    
    fun getSkipToTime(): Long? {
        return currentIntroData?.to
    }
    
    fun getIntroEndTime(): Long? {
        return currentIntroData?.to
    }
    
    fun getIntroStartTime(): Long? {
        return currentIntroData?.from
    }
}
