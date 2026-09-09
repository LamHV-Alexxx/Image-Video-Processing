package com.alex.image_video_processing.ui.menu

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.alex.image_video_processing.ui.camerax.CameraScreen
import com.alex.image_video_processing.ui.camerax.ImageProcessingScreen
import com.alex.image_video_processing.ui.camerax.components.CameraPreviewScreen
import com.alex.image_video_processing.ui.opengl.camera.OpenGLCameraScreen

enum class Feature(val label: String) {
    Camera("CameraX — preview"),
    ImageProcessing("ImageProcessing"),
    OpenGlCamera("OpenGL Camera"),
}

@Composable
fun MenuScreen(modifier: Modifier = Modifier) {
    var current by remember { mutableStateOf<Feature?>(null) }
    val back: () -> Unit = { current = null }

    if (current != null) BackHandler(onBack = back)

    val content = Modifier
        .fillMaxSize()
        .statusBarsPadding()

    when (current) {
        null -> FeatureMenu(modifier = modifier.then(content), onSelect = { current = it })
        Feature.Camera -> CameraScreen(
            content = { CameraPreviewScreen() }
        )
        Feature.ImageProcessing -> CameraScreen(
            content = { ImageProcessingScreen() }
        )
        Feature.OpenGlCamera -> CameraScreen(
            content = { OpenGLCameraScreen() }
        )
    }
}

@Composable
private fun FeatureMenu(modifier: Modifier, onSelect: (Feature) -> Unit) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Image & Video Processing",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(vertical = 16.dp),
        )
        Feature.entries.forEach { feature ->
            Button(onClick = { onSelect(feature) }, modifier = Modifier.fillMaxWidth()) {
                Text(feature.label)
            }
        }
    }
}
