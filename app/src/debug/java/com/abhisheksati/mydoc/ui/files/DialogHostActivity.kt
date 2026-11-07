package com.abhisheksati.mydoc.ui.files

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import com.abhisheksati.mydoc.designsystem.tokens.AppDesignSystemTheme

class DialogHostActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AppDesignSystemTheme { content() } }
    }

    companion object {
        var content: @Composable () -> Unit = {}
    }
}