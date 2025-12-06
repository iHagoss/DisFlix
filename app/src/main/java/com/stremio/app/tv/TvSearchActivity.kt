package com.stremio.app.tv

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import com.stremio.app.R
import com.stremio.app.StremioCore

class TvSearchActivity : FragmentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tv_search)
        
        try {
            StremioCore.initCore(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        StremioCore.shutdown()
    }
}
