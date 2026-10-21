package com.dhobigo.tracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.dhobigo.tracker.ui.navigation.DhobiNavGraph
import com.dhobigo.tracker.ui.theme.DhobiTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DhobiTrackerTheme {
                val navController = rememberNavController()
                DhobiNavGraph(navController = navController)
            }
        }
    }
}
