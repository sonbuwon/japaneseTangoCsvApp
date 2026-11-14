package com.example.csvtango

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.csvtango.ui.TangoApp
import com.example.csvtango.ui.theme.CsvTangoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 배색이 어두운 쪽 하나뿐이라 시스템 바 아이콘은 밝은 색으로 고정한다.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent {
            CsvTangoTheme {
                TangoApp()
            }
        }
    }
}
