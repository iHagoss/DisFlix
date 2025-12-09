
package com.stremio.app

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.View
import android.webkit.JavascriptInterface
import android.widget.Toast
import androidx.core.content.ContextCompat
import org.json.JSONObject

/**
 * WebAppInterface provides the bridge between JavaScript in WebView and Kotlin code
 * All methods are exposed to JavaScript via @JavascriptInterface annotation
 */
class WebAppInterface(private val context: Context) {
    
    companion object {
        private const val TAG = "WebAppInterface"
    }
    
    @JavascriptInterface
    fun showToast(message: String) {
        (context as? Activity)?.runOnUiThread {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
    
    @JavascriptInterface
    fun log(message: String) {
        Log.d(TAG, "[JS] $message")
    }
    
    @JavascriptInterface
    fun onBridgeReady() {
        Log.d(TAG, "Stremio Bridge is ready")
        (context as? MainActivity)?.runOnUiThread {
            (context as? MainActivity)?.onBridgeReady()
        }
    }
    
    @JavascriptInterface
    fun dispatchAction(actionJson: String): String {
        Log.d(TAG, "Dispatching action: $actionJson")
        return try {
            // Forward to StremioCore if available
            if (StremioCore.isLibraryLoaded()) {
                val result = StremioCore.dispatchAction(actionJson, "{}")
                result
            } else {
                "{\"status\":\"received\",\"fallback\":true}"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Action dispatch error", e)
            "{\"error\":\"${e.message}\"}"
        }
    }
    
    @JavascriptInterface
    fun navigate(path: String) {
        Log.d(TAG, "Navigate to: $path")
        (context as? Activity)?.runOnUiThread {
            (context as? MainActivity)?.navigateToPath(path)
        }
    }

    @JavascriptInterface
    fun openPlayer(streamUrl: String, title: String) {
        Log.d(TAG, "Opening player: $title - $streamUrl")
        (context as? Activity)?.runOnUiThread {
            val intent = Intent(context, PlayerActivity::class.java).apply {
                putExtra(PlayerActivity.EXTRA_STREAM_URL, streamUrl)
                putExtra(PlayerActivity.EXTRA_TITLE, title)
            }
            context.startActivity(intent)
        }
    }

    @JavascriptInterface
    fun openExternalPlayer(streamUrl: String) {
        Log.d(TAG, "Opening external player: $streamUrl")
        (context as? Activity)?.runOnUiThread {
            try {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(Uri.parse(streamUrl), "video/*")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(Intent.createChooser(intent, "Play with"))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to open external player", e)
                Toast.makeText(context, "No video player found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @JavascriptInterface
    fun shareUrl(url: String) {
        Log.d(TAG, "Sharing URL: $url")
        (context as? Activity)?.runOnUiThread {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, url)
            }
            context.startActivity(Intent.createChooser(intent, "Share via"))
        }
    }

    @JavascriptInterface
    fun copyToClipboard(text: String) {
        Log.d(TAG, "Copying to clipboard")
        (context as? Activity)?.runOnUiThread {
            val clipboard = ContextCompat.getSystemService(context, ClipboardManager::class.java)
            clipboard?.setPrimaryClip(ClipData.newPlainText("Stremio", text))
            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
        }
    }

    @JavascriptInterface
    fun openUrl(url: String) {
        Log.d(TAG, "Opening URL: $url")
        (context as? Activity)?.runOnUiThread {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to open URL", e)
                Toast.makeText(context, "Cannot open URL", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @JavascriptInterface
    fun getDeviceInfo(): String {
        val deviceInfo = JSONObject().apply {
            put("model", Build.MODEL)
            put("manufacturer", Build.MANUFACTURER)
            put("androidVersion", Build.VERSION.RELEASE)
            put("sdkVersion", Build.VERSION.SDK_INT)
        }
        return deviceInfo.toString()
    }

    @JavascriptInterface
    fun setFullscreen(enabled: Boolean) {
        Log.d(TAG, "Setting fullscreen: $enabled")
        (context as? Activity)?.runOnUiThread {
            val window = (context as? Activity)?.window
            if (enabled) {
                window?.decorView?.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )
            } else {
                window?.decorView?.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
            }
        }
    }

    @JavascriptInterface
    fun vibrate(duration: Int) {
        Log.d(TAG, "Vibrating for ${duration}ms")
        (context as? Activity)?.runOnUiThread {
            val vibrator = ContextCompat.getSystemService(context, Vibrator::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(duration.toLong(), VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(duration.toLong())
            }
        }
    }
    
    @JavascriptInterface
    fun openPlayer(streamUrl: String, metaId: String, videoId: String) {
        Log.d(TAG, "Open player: $streamUrl")
        (context as? Activity)?.runOnUiThread {
            val intent = android.content.Intent(context, PlayerActivity::class.java).apply {
                putExtra(PlayerActivity.EXTRA_STREAM_URL, streamUrl)
                putExtra("metaId", metaId)
                putExtra("videoId", videoId)
            }
            context.startActivity(intent)
        }
    }
    
    @JavascriptInterface
    fun openExternalPlayer(streamUrl: String) {
        Log.d(TAG, "Open external player: $streamUrl")
        (context as? Activity)?.runOnUiThread {
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                setDataAndType(android.net.Uri.parse(streamUrl), "video/*")
            }
            context.startActivity(intent)
        }
    }
    
    @JavascriptInterface
    fun shareUrl(url: String) {
        Log.d(TAG, "Share URL: $url")
        (context as? Activity)?.runOnUiThread {
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(android.content.Intent.EXTRA_TEXT, url)
            }
            context.startActivity(android.content.Intent.createChooser(intent, "Share via"))
        }
    }
    
    @JavascriptInterface
    fun copyToClipboard(text: String) {
        Log.d(TAG, "Copy to clipboard: $text")
        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("Stremio", text)
        clipboard.setPrimaryClip(clip)
        showToast("Copied to clipboard")
    }
    
    @JavascriptInterface
    fun openUrl(url: String) {
        Log.d(TAG, "Open URL: $url")
        (context as? Activity)?.runOnUiThread {
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
            context.startActivity(intent)
        }
    }
    
    @JavascriptInterface
    fun getDeviceInfo(): String {
        return org.json.JSONObject().apply {
            put("platform", "android")
            put("model", android.os.Build.MODEL)
            put("version", android.os.Build.VERSION.RELEASE)
        }.toString()
    }
    
    @JavascriptInterface
    fun setFullscreen(enabled: Boolean) {
        Log.d(TAG, "Set fullscreen: $enabled")
        (context as? Activity)?.runOnUiThread {
            if (enabled) {
                (context as? Activity)?.window?.decorView?.systemUiVisibility = (
                    android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or
                    android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )
            } else {
                (context as? Activity)?.window?.decorView?.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_VISIBLE
            }
        }
    }
    
    @JavascriptInterface
    fun vibrate(durationMs: Int) {
        val vibrator = context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as android.os.Vibrator
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            vibrator.vibrate(android.os.VibrationEffect.createOneShot(durationMs.toLong(), android.os.VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(durationMs.toLong())
        }
    }
    }
    
    @JavascriptInterface
    fun openPlayer(streamUrl: String, metaId: String, videoId: String) {
        Log.d(TAG, "openPlayer: stream=$streamUrl, meta=$metaId, video=$videoId")
        (context as? Activity)?.runOnUiThread {
            try {
                val intent = Intent(context, PlayerActivity::class.java).apply {
                    putExtra(PlayerActivity.EXTRA_STREAM_URL, streamUrl)
                    putExtra(PlayerActivity.EXTRA_META_ID, metaId)
                    putExtra(PlayerActivity.EXTRA_VIDEO_ID, videoId)
                    putExtra(PlayerActivity.EXTRA_TITLE, "Playing...")
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to open player", e)
                showToast("Failed to open player: ${e.message}")
            }
        }
    }
    
    @JavascriptInterface
    fun openExternalPlayer(streamUrl: String) {
        Log.d(TAG, "openExternalPlayer: $streamUrl")
        (context as? Activity)?.runOnUiThread {
            try {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(Uri.parse(streamUrl), "video/*")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(Intent.createChooser(intent, "Play with"))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to open external player", e)
                showToast("No video player found")
            }
        }
    }
    
    @JavascriptInterface
    fun shareUrl(url: String) {
        Log.d(TAG, "shareUrl: $url")
        (context as? Activity)?.runOnUiThread {
            try {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, url)
                }
                context.startActivity(Intent.createChooser(intent, "Share via"))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to share URL", e)
                showToast("Failed to share")
            }
        }
    }
    
    @JavascriptInterface
    fun copyToClipboard(text: String) {
        Log.d(TAG, "copyToClipboard: $text")
        (context as? Activity)?.runOnUiThread {
            try {
                val clipboard = ContextCompat.getSystemService(context, ClipboardManager::class.java)
                val clip = ClipData.newPlainText("Stremio", text)
                clipboard?.setPrimaryClip(clip)
                showToast("Copied to clipboard")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to copy to clipboard", e)
                showToast("Failed to copy")
            }
        }
    }
    
    @JavascriptInterface
    fun openUrl(url: String) {
        Log.d(TAG, "openUrl: $url")
        (context as? Activity)?.runOnUiThread {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to open URL", e)
                showToast("Failed to open link")
            }
        }
    }
    
    @JavascriptInterface
    fun getDeviceInfo(): String {
        return try {
            JSONObject().apply {
                put("platform", "android")
                put("model", Build.MODEL)
                put("manufacturer", Build.MANUFACTURER)
                put("androidVersion", Build.VERSION.RELEASE)
                put("sdkVersion", Build.VERSION.SDK_INT)
                put("device", Build.DEVICE)
            }.toString()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get device info", e)
            "{\"error\":\"${e.message}\"}"
        }
    }
    
    @JavascriptInterface
    fun setFullscreen(enabled: Boolean) {
        Log.d(TAG, "setFullscreen: $enabled")
        (context as? Activity)?.runOnUiThread {
            try {
                val activity = context as? Activity
                activity?.window?.decorView?.systemUiVisibility = if (enabled) {
                    (View.SYSTEM_UI_FLAG_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
                } else {
                    View.SYSTEM_UI_FLAG_VISIBLE
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set fullscreen", e)
            }
        }
    }
    
    @JavascriptInterface
    fun vibrate(durationMs: Int) {
        Log.d(TAG, "vibrate: ${durationMs}ms")
        (context as? Activity)?.runOnUiThread {
            try {
                val vibrator = ContextCompat.getSystemService(context, Vibrator::class.java)
                if (vibrator?.hasVibrator() == true) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createOneShot(durationMs.toLong(), VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(durationMs.toLong())
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to vibrate", e)
            }
        }
    }
    
    @JavascriptInterface
    fun getPlatform(): String = "android"
    
    @JavascriptInterface
    fun getVersion(): String = "1.0.0"
}
