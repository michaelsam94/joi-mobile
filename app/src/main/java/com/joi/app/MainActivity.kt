package com.joi.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.joi.app.navigation.JoiNavHost
import com.joi.designsystem.theme.JoiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as JoiApp).container

        setContent {
            JoiTheme {
                JoiNavHost(container = container)
            }
        }
    }
}
