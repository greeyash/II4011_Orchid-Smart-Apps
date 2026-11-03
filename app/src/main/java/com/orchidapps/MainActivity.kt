package com.orchidapps

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.orchidapps.ui.OrchidApp
import com.orchidapps.ui.theme.OrchidTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OrchidTheme {
                OrchidApp()
            }
        }
    }
}
