package com.alex.image_video_processing.ui.opengl

import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class CameraGLRenderer(
    private val onSurfaceTextureAvailable: (SurfaceTexture) -> Unit,
): GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {
    private var textureId: Int = -1
    private lateinit var surfaceTexture: SurfaceTexture

    // Toạ độ 2 hình tam giác ghép thành hình chữ nhật phủ toàn màn hình (-1 -> 1)
    private val squareVertices = floatArrayOf(
        -1.0f, -1.0f,
        1.0f, -1.0f,
        -1.0f,  1.0f,
        1.0f,  1.0f
    )

    // Toạ độ ma trận Texture (0 -> 1)
    private val textureVertices = floatArrayOf(
        0.0f, 0.0f,
        1.0f, 0.0f,
        0.0f, 1.0f,
        1.0f, 1.0f
    )

    private val vertexBuffer: FloatBuffer = createFloatBuffer(squareVertices)
    private val textureBuffer: FloatBuffer = createFloatBuffer(textureVertices)

    private var program: Int = 0
    private var updateSurface = false

    // Chế độ Filter đang chọn (0: Gốc, 1: Grayscale, 2: Brightness/Contrast, 3: Blur)
    var filterType: Int = 0

    override fun onSurfaceCreated(
        p0: GL10?,
        p1: EGLConfig?
    ) {
        GLES30.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)

        // 1. Tạo OES Texture ID cho Camera
        val textures = IntArray(1)
        GLES30.glGenTextures(1, textures, 0)
        textureId = textures[0]

        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)

        // 2. Tạo SurfaceTexture kết nối với CameraX
        surfaceTexture = SurfaceTexture(textureId)
        surfaceTexture.setOnFrameAvailableListener(this)

        // Báo cho Compose / CameraX biết surface đã sẵn sàng
        onSurfaceTextureAvailable(surfaceTexture)

        // 3. Khởi tạo và Compile Shaders
        initShaders()
    }

    override fun onSurfaceChanged(
        p0: GL10?,
        p1: Int,
        p2: Int
    ) {
        GLES30.glViewport(0, 0, p1, p2)
    }

    override fun onDrawFrame(p0: GL10?) {
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)

        synchronized(this) {
            if (updateSurface) {
                surfaceTexture.updateTexImage() // Nạp frame mới nhất từ Camera vào GPU Texture
                updateSurface = false
            }
        }

        GLES30.glUseProgram(program)

        // Truyền trạng thái Filter sang GPU
        val uFilterTypeLoc = GLES30.glGetUniformLocation(program, "uFilterType")
        GLES30.glUniform1i(uFilterTypeLoc, filterType)

        // Bind Buffers & Draw
        val aPositionLoc = GLES30.glGetAttribLocation(program, "aPosition")
        val aTexCoordLoc = GLES30.glGetAttribLocation(program, "aTexCoord")

        GLES30.glEnableVertexAttribArray(aPositionLoc)
        GLES30.glVertexAttribPointer(aPositionLoc, 2, GLES30.GL_FLOAT, false, 0, vertexBuffer)

        GLES30.glEnableVertexAttribArray(aTexCoordLoc)
        GLES30.glVertexAttribPointer(aTexCoordLoc, 2, GLES30.GL_FLOAT, false, 0, textureBuffer)

        // Vẽ 4 đỉnh thành 2 tam giác (TRIANGLE_STRIP)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)

        GLES30.glDisableVertexAttribArray(aPositionLoc)
        GLES30.glDisableVertexAttribArray(aTexCoordLoc)
    }

    override fun onFrameAvailable(p0: SurfaceTexture?) {
        synchronized(this) {
            updateSurface = true
        }
    }

    private fun createFloatBuffer(coords: FloatArray): FloatBuffer {
        val bb = ByteBuffer.allocateDirect(coords.size * 4)
        bb.order(ByteOrder.nativeOrder())
        return bb.asFloatBuffer().apply {
            put(coords)
            position(0)
        }
    }

    private fun initShaders() {
        val vertexCode = """
            #version 300 es
            in vec4 aPosition;
            in vec2 aTexCoord;
            out vec2 vTexCoord;
            
            void main() {
                gl_Position = aPosition;
                
                // Xoay/Lật tọa độ Texture nếu khung hình camera bị quay ngang
                vTexCoord = vec2(aTexCoord.x, 1.0 - aTexCoord.y);
            }
        """.trimIndent()

        // Fragment Shader tích hợp nhều bài toán Filter
        val fragmentCode = """
            #version 300 es
            #extension GL_OES_EGL_image_external_essl3 : require
            precision mediump float;
            
            in vec2 vTexCoord;
            uniform samplerExternalOES uTexture;
            uniform int uFilterType;
            
            out vec4 fragColor;
            
            void main() {
                vec4 color = texture(uTexture, vTexCoord);
                
                if (uFilterType == 1) {
                    // 1. Grayscale
                    float gray = dot(color.rgb, vec3(0.299, 0.587, 0.114));
                    fragColor = vec4(vec3(gray), color.a);
                }
                else if (uFilterType == 2) {
                    // 2. Brightness & Contrast
                    float brightness = 0.15; // Tăng độ sáng
                    float contrast = 1.4; // Tăng tương phản
                    vec3 result = (color.rgb - 0.5) * contrast + 0.5 + brightness;
                    fragColor = vec4(result, color.a);
                }
                else if (uFilterType == 3) {
                    // 3. Simple Box Blur (Lấy trung bình của 9 điểm xung quanh)
                    vec2 step = vec2(1.0 / 640.0, 1.0 / 480.0); // Kích thước 1 pixel
                    vec3 sum = vec3(0.0);
                    for(int x = -1; x <= 1; x++) {
                        for(int y = -1; y <= 1; y++) {
                            sum += texture(uTexture, vTexCoord + vec2(x, y) * step).rgb;
                        }
                    }
                    fragColor = vec4(sum / 9.0, color.a);
                }
                else {
                    // 0. Ảnh gốc
                    fragColor = color;
                }
            }
            
        """.trimIndent()

        val vShader = loadShader(GLES30.GL_VERTEX_SHADER, vertexCode)
        val fShader = loadShader(GLES30.GL_FRAGMENT_SHADER, fragmentCode)

        program = GLES30.glCreateProgram().also {
            GLES30.glAttachShader(it, vShader)
            GLES30.glAttachShader(it, fShader)
            GLES30.glLinkProgram(it)
        }
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        return GLES30.glCreateShader(type).also {
            GLES30.glShaderSource(it, shaderCode)
            GLES30.glCompileShader(it)
        }
    }
}