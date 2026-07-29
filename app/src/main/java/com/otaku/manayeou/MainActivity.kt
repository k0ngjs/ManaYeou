package com.otaku.manayeou

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.otaku.manayeou.data.local.SettingsRepository
import com.otaku.manayeou.ui.navigation.AppNavigation
import com.otaku.manayeou.ui.theme.ManayeouTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        SettingsRepository(this).getThemeMode() // ThemeState 초기값 로드
        setContent {
            ManayeouTheme {
                AppNavigation()
            }
        }
    }
}
