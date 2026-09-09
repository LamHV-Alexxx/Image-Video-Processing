package com.alex.image_video_processing.ui.opengl.camera

import android.opengl.GLSurfaceView
import android.view.Surface
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun OpenGLCameraScreen(
    modifier: Modifier = Modifier,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var selectedFilter by remember { mutableIntStateOf(0) }
    var rendererRef by remember { mutableStateOf<CameraGLRenderer?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        // 1. GLSurfaceView hiển thị GPU output
        AndroidView(
            factory = { ctx ->
                GLSurfaceView(ctx).apply {
                    setEGLContextClientVersion(3) // Dùng OpenGL ES 3.0

                    val renderer = CameraGLRenderer { surfaceTexture ->
                        // Đã c SurfaceTexture từ GPU -> Bind vào CameraX Preview
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build()

                            // Gán SurfaceTexture làm nơi nhận luồng video CameraX
                            preview.setSurfaceProvider { request ->
                                val surface = Surface(surfaceTexture)
                                request.provideSurface(surface, ContextCompat.getMainExecutor(ctx)) {
                                    surface.release()
                                }
                            }

                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview
                                )
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                    }

                    rendererRef = renderer
                    setRenderer(renderer)
                    renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY // Vẽ liên tục
                }
            }, modifier = Modifier.fillMaxSize()
        )

        // 2. Control Panel chọn Filters
        ControlPanel(
            onFilterSelected = { filter ->
                selectedFilter = filter
                rendererRef?.filterType = filter
            },
            Modifier
                .align(Alignment.BottomCenter)
                .padding(24.dp)
                .fillMaxWidth(),
        )
    }
}

@Composable
private fun ControlPanel(
    onFilterSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Button(onClick = {
            onFilterSelected(0)
        }) { Text("Gốc") }

        Button(onClick = {
            onFilterSelected(1)
        }) { Text("Grayscale") }

        Button(onClick = {
            onFilterSelected(2)
        }) { Text("Tương Phản") }

        Button(onClick = {
            onFilterSelected(3)
        }) { Text("Blur") }
    }
}