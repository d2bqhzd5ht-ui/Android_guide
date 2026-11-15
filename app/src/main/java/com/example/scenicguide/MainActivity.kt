package com.example.scenicguide

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.scenicguide.ui.AppRoot
import com.example.scenicguide.ui.theme.ScenicGuideTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ScenicGuideTheme {
                AppRoot()
            }
        }
    }
}
