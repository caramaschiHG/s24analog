package com.roll24

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.roll24.camera.CameraPermissionScreen
import com.roll24.camera.CameraScreen
import com.roll24.ui.theme.Roll24Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Roll24Theme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Roll24App()
                }
            }
        }
    }
}

@Composable
fun Roll24App(viewModel: Roll24ViewModel = viewModel()) {
    CameraPermissionScreen {
        CameraScreen(viewModel = viewModel)
    }
}
