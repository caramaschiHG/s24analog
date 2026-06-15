package com.roll24

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.roll24.camera.CameraPermissionScreen
import com.roll24.camera.CameraScreen
import com.roll24.image.ImageSaver
import com.roll24.review.ReviewScreen
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
    val context = LocalContext.current
    val showReview by viewModel.showReview.collectAsState()
    val processedBitmap by viewModel.processedBitmap.collectAsState()
    val selectedProfile by viewModel.selectedProfile.collectAsState()

    CameraPermissionScreen {
        if (showReview && processedBitmap != null) {
            ReviewScreen(
                bitmap = processedBitmap!!,
                profile = selectedProfile,
                onSave = {
                    val uri = ImageSaver.saveToGallery(context, processedBitmap!!, selectedProfile)
                    if (uri != null) {
                        Toast.makeText(context, R.string.photo_saved, Toast.LENGTH_SHORT).show()
                    }
                    viewModel.dismissReview()
                },
                onDiscard = {
                    Toast.makeText(context, R.string.photo_discarded, Toast.LENGTH_SHORT).show()
                    viewModel.dismissReview()
                }
            )
        } else {
            CameraScreen(viewModel = viewModel)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun Roll24Preview() {
    Roll24Theme {
        Roll24App()
    }
}
