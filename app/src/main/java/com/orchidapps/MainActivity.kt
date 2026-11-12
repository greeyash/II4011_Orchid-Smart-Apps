package com.orchidapps

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.orchidapps.exploration.ExplorationSession
import com.orchidapps.ui.OrchidApp
import com.orchidapps.ui.theme.OrchidTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) handleIntent(intent)
        setContent {
            OrchidTheme {
                OrchidApp()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    /** Notifikasi kedatangan membawa ID POI agar halaman detailnya langsung terbuka. */
    private fun handleIntent(intent: Intent?) {
        intent?.getStringExtra(EXTRA_POI_ID)?.let(ExplorationSession::requestOpenPoi)
    }

    companion object {
        const val EXTRA_POI_ID = "com.orchidapps.extra.POI_ID"
    }
}
