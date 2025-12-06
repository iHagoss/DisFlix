package com.stremio.app

import android.os.Bundle
import androidx.fragment.app.FragmentActivity

class TvSearchActivity : FragmentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tv_search)
        
        // StremioCore initialization is handled by the MainActivity Router, not here.
        
        // You would typically initialize your Leanback Search Fragment here
        // if (savedInstanceState == null) {
        //     supportFragmentManager.beginTransaction()
        //         .replace(R.id.search_fragment_container, SearchFragment())
        //         .commitNow()
        // }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // StremioCore shutdown is handled by the MainActivity Router to prevent conflicts.
    }
}
