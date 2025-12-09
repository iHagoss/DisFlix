
package com.stremio.app

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject

class StreamingServerActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "StreamingServerActivity"
    }

    private lateinit var serverModeSpinner: Spinner
    private lateinit var serverUrlInput: EditText
    private lateinit var addUrlButton: Button
    private lateinit var urlsListView: ListView
    private lateinit var saveButton: Button
    private lateinit var testConnectionButton: Button
    private lateinit var statusText: TextView
    private lateinit var progressBar: ProgressBar

    private val serverUrls = mutableListOf<String>()
    private lateinit var urlsAdapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_streaming_server)

        initializeViews()
        setupServerModeSpinner()
        loadCurrentSettings()
        setupListeners()
    }

    private fun initializeViews() {
        serverModeSpinner = findViewById(R.id.server_mode_spinner)
        serverUrlInput = findViewById(R.id.server_url_input)
        addUrlButton = findViewById(R.id.add_url_button)
        urlsListView = findViewById(R.id.urls_list_view)
        saveButton = findViewById(R.id.save_button)
        testConnectionButton = findViewById(R.id.test_connection_button)
        statusText = findViewById(R.id.status_text)
        progressBar = findViewById(R.id.progress_bar)

        // Setup URLs list adapter
        urlsAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, serverUrls)
        urlsListView.adapter = urlsAdapter
    }

    private fun setupServerModeSpinner() {
        val modes = arrayOf("Embedded", "Remote")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, modes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        serverModeSpinner.adapter = adapter

        serverModeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val isRemote = position == 1
                serverUrlInput.isEnabled = isRemote
                addUrlButton.isEnabled = isRemote
                testConnectionButton.isEnabled = isRemote
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun loadCurrentSettings() {
        try {
            // Load settings from StremioCore
            val settingsJson = StremioCore.getStreamingServerSettings()
            val settings = JSONObject(settingsJson)
            
            val mode = settings.optString("mode", "embedded")
            serverModeSpinner.setSelection(if (mode == "remote") 1 else 0)

            val urls = settings.optJSONArray("urls")
            if (urls != null) {
                serverUrls.clear()
                for (i in 0 until urls.length()) {
                    serverUrls.add(urls.getString(i))
                }
                urlsAdapter.notifyDataSetChanged()
            }

            statusText.text = "Settings loaded successfully"
            statusText.setTextColor(getColor(android.R.color.holo_green_dark))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load settings", e)
            statusText.text = "Failed to load settings: ${e.message}"
            statusText.setTextColor(getColor(android.R.color.holo_red_dark))
        }
    }

    private fun setupListeners() {
        addUrlButton.setOnClickListener {
            val url = serverUrlInput.text.toString().trim()
            if (url.isNotEmpty()) {
                if (isValidUrl(url)) {
                    serverUrls.add(url)
                    urlsAdapter.notifyDataSetChanged()
                    serverUrlInput.text.clear()
                    statusText.text = "URL added"
                    statusText.setTextColor(getColor(android.R.color.holo_green_dark))
                } else {
                    statusText.text = "Invalid URL format"
                    statusText.setTextColor(getColor(android.R.color.holo_orange_dark))
                }
            }
        }

        urlsListView.setOnItemLongClickListener { _, _, position, _ ->
            serverUrls.removeAt(position)
            urlsAdapter.notifyDataSetChanged()
            statusText.text = "URL removed"
            statusText.setTextColor(getColor(android.R.color.holo_green_dark))
            true
        }

        saveButton.setOnClickListener {
            saveSettings()
        }

        testConnectionButton.setOnClickListener {
            testConnection()
        }

        findViewById<ImageButton>(R.id.back_button).setOnClickListener {
            finish()
        }
    }

    private fun isValidUrl(url: String): Boolean {
        return try {
            val pattern = "^https?://.*".toRegex()
            pattern.matches(url)
        } catch (e: Exception) {
            false
        }
    }

    private fun saveSettings() {
        try {
            progressBar.visibility = View.VISIBLE
            
            val mode = if (serverModeSpinner.selectedItemPosition == 1) "remote" else "embedded"
            val settings = JSONObject().apply {
                put("mode", mode)
                put("urls", serverUrls)
            }

            StremioCore.updateStreamingServerSettings(settings.toString())
            
            progressBar.visibility = View.GONE
            statusText.text = "Settings saved successfully"
            statusText.setTextColor(getColor(android.R.color.holo_green_dark))
            
            Toast.makeText(this, "Streaming server settings saved", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save settings", e)
            progressBar.visibility = View.GONE
            statusText.text = "Failed to save: ${e.message}"
            statusText.setTextColor(getColor(android.R.color.holo_red_dark))
        }
    }

    private fun testConnection() {
        if (serverUrls.isEmpty()) {
            statusText.text = "No URLs to test"
            statusText.setTextColor(getColor(android.R.color.holo_orange_dark))
            return
        }

        progressBar.visibility = View.VISIBLE
        statusText.text = "Testing connection..."
        statusText.setTextColor(getColor(android.R.color.darker_gray))

        // Test the first URL in the list
        Thread {
            try {
                val testUrl = serverUrls[0]
                val result = StremioCore.testStreamingServerConnection(testUrl)
                val resultJson = JSONObject(result)
                val success = resultJson.optBoolean("success", false)
                
                runOnUiThread {
                    progressBar.visibility = View.GONE
                    if (success) {
                        statusText.text = "Connection successful"
                        statusText.setTextColor(getColor(android.R.color.holo_green_dark))
                    } else {
                        val error = resultJson.optString("error", "Unknown error")
                        statusText.text = "Connection failed: $error"
                        statusText.setTextColor(getColor(android.R.color.holo_red_dark))
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Connection test failed", e)
                runOnUiThread {
                    progressBar.visibility = View.GONE
                    statusText.text = "Test failed: ${e.message}"
                    statusText.setTextColor(getColor(android.R.color.holo_red_dark))
                }
            }
        }.start()
    }
}
