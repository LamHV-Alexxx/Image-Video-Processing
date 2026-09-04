package com.alex.image_video_processing.ui.camerax.components

import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun CameraPreviewScreen() {
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    // State lưu giữ bitmap nhận được từ ImageAnalysis để hiển thị
    var processedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // State lưu chỉ số FPS thực tế xử lý được
    var fpsText by remember { mutableStateOf("0 FPS") }

    Box(Modifier.fillMaxSize()) {
        // 1. Luồng Camera Preview hiển thị toàn màn hình
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()

                    // Usecase 1: Preview
                    val preview = Preview.Builder()
                        .build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }

                    // Biến tính FPS
                    var lastFrameTimestamp = System.currentTimeMillis()

                    // Usecase 2: Image Analysis
                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                        .build()

                    // Chuyển Dispatcher.Default thành Executor cho CameraX
                    // CameraX sẽ đẩy công việc phân tích vào Dispatcher.Default (Thread pool CPU)
                    val backgroundExecutor = Dispatchers.Default.asExecutor()

                    imageAnalysis.setAnalyzer(backgroundExecutor) { imageProxy ->
                        // Tính FPS
                        val currentTimestamp = System.currentTimeMillis()
                        val deltaTime = currentTimestamp - lastFrameTimestamp
                        if (deltaTime > 0) {
                            val currentFps = 1000 / deltaTime
                            fpsText = "$currentFps FPS ($deltaTime ms)"
                        }
                        lastFrameTimestamp = currentTimestamp

                        coroutineScope.launch(Dispatchers.Default) {
                            try {
                                // Xử lý biến đổi ImageProxy -> Bitmap
                                val bitmap = imageProxy.toBitmap()
                                val rotatedBitmap = rotateBitmap(bitmap, imageProxy.imageInfo.rotationDegrees)

                                // Thực hiện logic xử lý ảnh
                                val finalProcessedBitmap = processMyImage(rotatedBitmap)

                                // Đẩy bitmap đã xử lý lên Main Thread để cập nhật Compose state
                                withContext(Dispatchers.Main) {
                                    processedBitmap = finalProcessedBitmap
                                }
                            } finally {
                                // Giải phóng (BẮT BUỘC)
                                imageProxy.close()
                            }
                        }
                    }

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageAnalysis,
                        )
                    } catch (e: Exception) {
                        Log.e("CameraCompose", "Binding failed", e)
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            }, modifier = Modifier.fillMaxSize()
        )

        // 2. UI Overlay hiển thị thông tin FPS và Ảnh xem trước đã xử lý
        InfoOverlay(
            fpsText = fpsText,
            bitmap = processedBitmap,
            modifier = Modifier.align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(16.dp)
        )
    }
}

@Composable
fun InfoOverlay(
    fpsText: String,
    bitmap: Bitmap?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = "Tốc độ xử lý: $fpsText",
            color = Color.Green,
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Hiển thị khung xem trước Bitmap thu nhỏ đã qua xử lý
        bitmap?.let { bitmap ->
            Text(text = "Khung hình từ ImageAnalysis:", color = Color.White)
            Spacer(modifier = Modifier.height(4.dp))
            Image(
                bitmap = bitmap.asImageBitmap(), // Chuyển android.graphics.Bitmap -> Compose ImageBitmap
                contentDescription = "Processed Frame",
                modifier = Modifier
                    .size(width = 160.dp, height = 210.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(2.dp, Color.White, RoundedCornerShape(12.dp))
            )
        }
    }
}

// Func hỗ trợ xoay bitmap cho đúng góc nghiêng của cảm biến
private fun rotateBitmap(bitmap: Bitmap, rotationDegrees: Int): Bitmap {
    val matrix = Matrix().apply {
        postRotate(rotationDegrees.toFloat())
    }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

// Hàm mô phỏng xử lý ảnh đơn giản
private fun processMyImage(inputBitmap: Bitmap): Bitmap {
    return inputBitmap
}