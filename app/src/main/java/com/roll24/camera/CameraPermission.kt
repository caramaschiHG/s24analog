package com.roll24.camera

import android.Manifest
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.roll24.R
import com.roll24.ui.theme.AccentGold
import com.roll24.ui.theme.Black
import com.roll24.ui.theme.White

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraPermissionScreen(
    onPermissionGranted: @Composable () -> Unit
) {
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    if (cameraPermissionState.status.isGranted) {
        onPermissionGranted()
    } else {
        PermissionRequestScreen(
            onRequestPermission = { cameraPermissionState.launchPermissionRequest() }
        )
    }
}

@Composable
private fun PermissionRequestScreen(
    onRequestPermission: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Black)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Roll24",
            style = androidx.compose.material3.MaterialTheme.typography.displaySmall,
            color = AccentGold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.camera_permission_required),
            style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
            color = White,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onRequestPermission,
            colors = ButtonDefaults.buttonColors(
                containerColor = AccentGold,
                contentColor = Black
            )
        ) {
            Text(
                text = stringResource(R.string.grant_permission),
                style = androidx.compose.material3.MaterialTheme.typography.labelLarge
            )
        }
    }
}
