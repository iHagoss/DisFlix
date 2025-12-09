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
 * WebAppInterface - JavaScript ↔ Android Bridge
 *
 * This interface is exposed to the WebView's JavaScript context as "window.Android".
 * It allows the Stremio Web UI to invoke native Android functionality.
 *
 * Usage in JavaScript:
 *   window.Android.log("Hello from JS");
 *   window.Android.openPlayer(streamUrl, title);
 *   window.Android.getDeviceInfo();
 *
 * All methods marked with @JavascriptInterface are callable from JavaScript.
 * These methods run on the WebView's JavaScript thread, NOT the UI thread.
 *
 * IMPORTANT: This bridge enables addon synchronization after user login.
 * When the user authenticates in the web UI, it calls syncAddons() which
 * fetches the user's addon configuration from Stremio's servers.
 */
class WebAppInterface(private val context: Context) {

    companion object {
        private const val TAG = "WebAppInterface"
    }

    /**
     * Displays a short toast message on the Android UI.
     * JavaScript usage: window.Android.showToast("Message text");
     */
    @JavascriptInterface
    fun showToast(message: String) {
        (context as? Activity)?.runOnUiThread {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Logs messages from JavaScript to Android's logcat.
     * JavaScript usage: window.Android.log("Debug message");
     */
    @JavascriptInterface
    fun log(message: String) {
        Log.d(TAG, "[JS] $message")
    }

    /**
     * Called when the Stremio Bridge is ready in the WebView.
     * JavaScript usage: (called internally by the web UI)
     */
    @JavascriptInterface
    fun onBridgeReady() {
        Log.d(TAG, "Stremio Bridge is ready")
        (context as? MainActivity)?.runOnUiThread {
            (context as? MainActivity)?.onBridgeReady()
        }
    }

    /**
     * Dispatches an action to the Stremio Core.
     * JavaScript usage: const result = await window.Android.dispatchAction('{"action":"someAction"}');
     */
    @JavascriptInterface
    fun dispatchAction(actionJson: String): String {
        Log.d(TAG, "Dispatching action: $actionJson")
        return try {
            if (StremioCore.isLibraryLoaded()) {
                StremioCore.dispatchAction(actionJson, "{}")
            } else {
                "{\"status\":\"received\",\"fallback\":true}"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Action dispatch error", e)
            "{\"error\":\"${e.message}\"}"
        }
    }

    /**
     * Navigates the web UI to a specific path.
     * JavaScript usage: window.Android.navigate("/some/path");
     */
    @JavascriptInterface
    fun navigate(path: String) {
        Log.d(TAG, "Navigate to: $path")
        (context as? MainActivity)?.runOnUiThread {
            (context as? MainActivity)?.navigateToPath(path)
        }
    }

    /**
     * Opens the Stremio in-app player for a given stream URL and title.
     * JavaScript usage: window.Android.openPlayer("streamUrl", "Video Title");
     * 
     * Note: This 2-argument version is for simple playback with just URL and title.
     */
    @JavascriptInterface
    fun openPlayer(streamUrl: String, title: String) {
        Log.d(TAG, "Opening player: $title - $streamUrl")
        (context as? Activity)?.runOnUiThread {
            try {
                val intent = Intent(context, PlayerActivity::class.java).apply {
                    putExtra(PlayerActivity.EXTRA_STREAM_URL, streamUrl)
                    putExtra(PlayerActivity.EXTRA_TITLE, title)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to open player", e)
                showToast("Failed to open player: ${e.message}")
            }
        }
    }

    /**
     * Opens the Stremio in-app player with metadata IDs.
     * JavaScript usage: window.Android.openPlayer("streamUrl", "metaId", "videoId");
     * 
     * Called by bridge.js when launching playback from the web UI.
     * The metaId and videoId are used for tracking and skip intro functionality.
     */
    @JavascriptInterface
    fun openPlayer(streamUrl: String, metaId: String, videoId: String) {
        Log.d(TAG, "openPlayer (3-arg): stream=$streamUrl, meta=$metaId, video=$videoId")
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

    /**
     * Opens an external video player on the device for a given stream URL.
     * JavaScript usage: window.Android.openExternalPlayer("streamUrl");
     */
    @JavascriptInterface
    fun openExternalPlayer(streamUrl: String) {
        Log.d(TAG, "Opening external player: $streamUrl")
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

    /**
     * Shares a given URL using the device's standard sharing mechanism.
     * JavaScript usage: window.Android.shareUrl("http://example.com");
     */
    @JavascriptInterface
    fun shareUrl(url: String) {
        Log.d(TAG, "Sharing URL: $url")
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

    /**
     * Copies the provided text to the device's clipboard.
     * JavaScript usage: window.Android.copyToClipboard("Text to copy");
     */
    @JavascriptInterface
    fun copyToClipboard(text: String) {
        Log.d(TAG, "Copying to clipboard")
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

    /**
     * Opens a given URL in the device's default web browser.
     * JavaScript usage: window.Android.openUrl("http://example.com");
     */
    @JavascriptInterface
    fun openUrl(url: String) {
        Log.d(TAG, "Opening URL: $url")
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

    /**
     * Retrieves device information such as model, manufacturer, and Android version.
     * JavaScript usage: const deviceInfo = JSON.parse(window.Android.getDeviceInfo());
     */
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

    /**
     * Sets the fullscreen mode for the activity window.
     * JavaScript usage: window.Android.setFullscreen(true);
     */
    @JavascriptInterface
    fun setFullscreen(enabled: Boolean) {
        Log.d(TAG, "Setting fullscreen: $enabled")
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

    /**
     * Vibrates the device for a specified duration.
     * JavaScript usage: window.Android.vibrate(500);
     */
    @JavascriptInterface
    fun vibrate(durationMs: Int) {
        Log.d(TAG, "Vibrating for ${durationMs}ms")
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

    /**
     * Returns the platform identifier.
     * JavaScript usage: const platform = window.Android.getPlatform();
     */
    @JavascriptInterface
    fun getPlatform(): String = "android"

    /**
     * Returns the current version of the application.
     * JavaScript usage: const version = window.Android.getVersion();
     */
    @JavascriptInterface
    fun getVersion(): String = "1.0.0"
}
