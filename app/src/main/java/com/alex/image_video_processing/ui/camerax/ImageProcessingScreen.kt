package com.alex.image_video_processing.ui.camerax

import android.graphics.Bitmap
import android.util.Log
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.alex.image_video_processing.image_processor.FilterType
import com.alex.image_video_processing.image_processor.ImageProcessor
import com.alex.image_video_processing.ui.camerax.components.InfoOverlay
import com.alex.image_video_processing.ui.camerax.components.rotateBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ImageProcessingScreen() {
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    // State chọn bộ lọc hiện tại
    var selectedFilter by remember { mutableStateOf(FilterType.NONE) }

    // State lưu giữ bitmap nhận được từ ImageAnalysis để hiển thị
    var processedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // State thời gian xử lý (ms)
    var processingTimeMs by remember { mutableLongStateOf(0L) }

    Box(Modifier.fillMaxSize()) {
        // 1. Luồng CameraX Background
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder()
                        .build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                        .setTargetResolution(Size(480, 640))
                        .build()

                    // Chuyển Dispatcher.Default thành Executor cho CameraX
                    // CameraX sẽ đẩy công việc phân tích vào Dispatcher.Default (Thread pool CPU)
                    val backgroundExecutor = Dispatchers.Default.asExecutor()

                    imageAnalysis.setAnalyzer(backgroundExecutor) { imageProxy ->
                        coroutineScope.launch(Dispatchers.Default) {
                            val startTime = System.currentTimeMillis()
                            try {
                                // Xử lý biến đổi ImageProxy -> Bitmap
                                val bitmap = imageProxy.toBitmap()
                                val rotatedBitmap = rotateBitmap(bitmap, imageProxy.imageInfo.rotationDegrees)

                                // Áp dụng Filter dựa trên State người dùng chọn
                                val result = when (selectedFilter) {
                                    FilterType.NONE -> rotatedBitmap
                                    FilterType.GRAYSCALE -> ImageProcessor.toGrayscale(rotatedBitmap)
                                    FilterType.SOBEL_EDGE -> ImageProcessor.toSobelEdge(rotatedBitmap)
                                }

                                val timeTaken = System.currentTimeMillis() - startTime

                                // Đẩy bitmap đã xử lý lên Main Thread để cập nhật Compose state
                                withContext(Dispatchers.Main) {
                                    processedBitmap = result
                                    processingTimeMs = timeTaken
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
            infoText = "Processing Time: $processingTimeMs ms",
            bitmap = processedBitmap,
            modifier = Modifier.align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(16.dp)
        )

        // 3. UI Control Panel bên dưới góc màn hình
        ControlPanel(
            selectedFilter = selectedFilter,
            onFilterSelected = { selectedFilter = it },
            modifier = Modifier.align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )
    }
}

@Composable
private fun ControlPanel(
    selectedFilter: FilterType,
    onFilterSelected: (FilterType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = modifier.fillMaxWidth()
    ) {
        FilterButton("Gốc", selectedFilter == FilterType.NONE) {
            onFilterSelected(FilterType.NONE)
        }
        FilterButton("Grayscale", selectedFilter == FilterType.GRAYSCALE) {
            onFilterSelected(FilterType.GRAYSCALE)
        }
        FilterButton("Sobel Edge", selectedFilter == FilterType.SOBEL_EDGE) {
            onFilterSelected(FilterType.SOBEL_EDGE)
        }
    }
}

@Composable
private fun FilterButton(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray
        )
    ) {
        Text(text = label)
    }
}