package com.zen.airai.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.zen.airai.ui.navigation.AirAINavigation
import com.zen.airai.ui.theme.AirAITheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AirAITheme {
                val navController = rememberNavController()
                AirAINavigation(navController = navController)
            }
        }
    }
}
