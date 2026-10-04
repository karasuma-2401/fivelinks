package com.karasuma.fivelinks.fivelinks_cmp

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            App(onNightThemeChange = ::applySystemBarStyle)
        }
    }

    /** Bar icons follow the in-app day/night theme instead of the system one. */
    private fun applySystemBarStyle(isNight: Boolean) {
        val style = if (isNight) {
            SystemBarStyle.dark(Color.TRANSPARENT)
        } else {
            SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        }
        enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
