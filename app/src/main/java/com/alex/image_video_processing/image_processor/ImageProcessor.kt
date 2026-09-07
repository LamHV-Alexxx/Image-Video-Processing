package com.alex.image_video_processing.image_processor

import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.sqrt

enum class FilterType {
    NONE,
    GRAYSCALE,
    SOBEL_EDGE
}

object ImageProcessor {
    /**
     * 1. Thuật toán Grayscale tối ưu bằng IntArray
     */
    suspend fun toGrayscale(input: Bitmap): Bitmap = withContext(Dispatchers.Default) {
        val width = input.width
        val height = input.height
        val pixels = IntArray(width * height)

        // Đọc toàn bộ pixel vào mảng 1 chiều (Nhanh hơn gọi getPixel từng điểm)
        input.getPixels(pixels, 0, width, 0, 0, width, height)

        for (i in pixels.indices) {
            val pixel = pixels[i]
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF

            // Công thức BT.601
            val gray = (r * 0.299 + g * 0.587 + b * 0.114).toInt().coerceIn(0, 255)

            // Tái tạo lại màu ARGB (alpha giữ nguyên 0xFF)
            pixels[i] = (0xFF shl 24) or (gray shl 16) or (gray shl 8) or gray
        }

        // Tạo bitmap mới từ mảng pixels đã sửa
        return@withContext Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
    }

    /**
     * 2. Thuật toán Sobel Edge Detection
     */
    suspend fun toSobelEdge(input: Bitmap): Bitmap = withContext(Dispatchers.Default) {
        val width = input.width
        val height = input.height
        val pixels = IntArray(width * height)
        input.getPixels(pixels, 0, width, 0, 0, width, height)

        // BƯỚC 1: Chuyển mảng pixel sang GrayScale trước để tính toán nhanh hơn
        val grayPixels = IntArray(width * height)
        for (i in pixels.indices) {
            val p = pixels[i]
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            grayPixels[i] = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
        }

        val outputPixels = IntArray(width * height)

        // Kernel sobel
        val gx = arrayOf(
            intArrayOf(-1, 0, 1),
            intArrayOf(-2, 0, 2),
            intArrayOf(-1, 0, 1)
        )
        val gy = arrayOf(
            intArrayOf(-1, -2, -1),
            intArrayOf(0, 0, 0),
            intArrayOf(1, 2, 1)
        )

        // BƯỚC 2: Quét Kernel 3x3 qua từng pixel (bỏ qua các pixel mép ngoài cùng)
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                var sumX = 0
                var sumY = 0

                // Tích chập ma trận 3x3
                for (ky in -1..1) {
                    for (kx in -1..1) {
                        val pixelGray = grayPixels[(y + ky) * width + (x + kx)]
                        sumX += pixelGray * gx[ky + 1][kx + 1]
                        sumY += pixelGray * gy[ky + 1][kx + 1]
                    }
                }

                // Tính độ lớn Gradient G = sqrt(Gx^2 + Gy^2)
                val magnitude = sqrt((sumX * sumX + sumY * sumY).toDouble()).toInt()

                // Lưu kết quả dưới dạng ảnh trắng đen (Nền đen, đường viền trắng)
                outputPixels[y * width + x] = (0xFF shl 24) or (magnitude shl 16) or (magnitude shl 8) or magnitude
            }
        }

        return@withContext Bitmap.createBitmap(outputPixels, width, height, Bitmap.Config.ARGB_8888)
    }
}