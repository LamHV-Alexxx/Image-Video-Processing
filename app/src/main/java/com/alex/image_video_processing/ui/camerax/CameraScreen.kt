package com.alex.image_video_processing.ui.camerax

import android.Manifest
import android.graphics.Bitmap
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.alex.image_video_processing.ui.camerax.components.CameraPreviewScreen
import com.alex.image_video_processing.ui.menu.Feature
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(
    feature: Feature,
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
            cameraPermissionState.status.isGranted ->
                when(feature) {
                    Feature.ImageProcessing -> ImageProcessingScreen()
                    else -> CameraPreviewScreen()
                }

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