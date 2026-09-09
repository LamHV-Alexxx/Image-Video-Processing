package com.alex.image_video_processing.ui.opengl.image

import android.content.Context
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class StaticImageRenderer(
    private val context: Context,
    private val imageResId: Int // ID ảnh trong drawable
): GLSurfaceView.Renderer {
    private var textureId: Int = -1
    private var program: Int = 0
    var isGrayscale: Boolean = false

    // Tọa độ vẽ khung hình chữ nhật
    private val vertices = floatArrayOf(
        -1.0f, -1.0f,  // Góc dưới trái
        1.0f, -1.0f,  // Góc dưới phải
        -1.0f,  1.0f,  // Góc trên trái
        1.0f,  1.0f   // Góc trên phải
    )

    // Tọa độ dán ảnh (Texture coordinates: từ 0.0 đến 1.0)
    private val texCoords = floatArrayOf(
        0.0f, 1.0f,  // Trái dưới
        1.0f, 1.0f,  // Phải dưới
        0.0f, 0.0f,  // Trái trên
        1.0f, 0.0f   // Phải trên
    )

    private val vertexBuffer: FloatBuffer = createFloatBuffer(vertices)
    private val texBuffer: FloatBuffer = createFloatBuffer(texCoords)

    override fun onSurfaceCreated(
        p0: GL10?,
        p1: EGLConfig?
    ) {
        GLES30.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)

        // 1. Đọc file ảnh từ Drawable thành Bitmap
    }

    override fun onSurfaceChanged(
        p0: GL10?,
        p1: Int,
        p2: Int
    ) {
        TODO("Not yet implemented")
    }

    override fun onDrawFrame(p0: GL10?) {
        TODO("Not yet implemented")
    }

    private fun createFloatBuffer(coords: FloatArray): FloatBuffer {
        val bb = ByteBuffer.allocateDirect(coords.size * 4)
        bb.order(ByteOrder.nativeOrder())
        return bb.asFloatBuffer().apply {
            put(coords)
            position(0)
        }
    }
}