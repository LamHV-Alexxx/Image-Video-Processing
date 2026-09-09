package com.alex.image_video_processing.ui.camerax

import android.Manifest
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    Surface(modifier.fillMaxSize()) {
        when {
            cameraPermissionState.status.isGranted -> content()

            cameraPermissionState.status.shouldShowRationale ->
                PermissionRequestContent {
                    cameraPermissionState.launchPermissionRequest()
                }

            else -> NoPermissionContent()
        }
    }
}

@Composable
fun PermissionRequestContent(
    onGranted: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Ứng dụng cần quyền Camera để xử lý khung hình.")
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onGranted) {
            Text("Cấp quyền")
        }
    }
}

@Composable
fun NoPermissionContent() {
    Box(contentAlignment = Alignment.Center) {
        Text("Chưa được cấp quyền Camera. Hãy mở Settings để bật.")
    }
}