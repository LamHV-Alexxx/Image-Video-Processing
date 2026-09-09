package com.alex.image_video_processing.ui.opengl.image

const val grayScaleShader = """
    #version 300 es
    precision mediump float;
    
    in vec2 vTexCoord;
    uniform sampler2D uTexture; // Với ảnh tĩnh Bitmap, dùng sampler2D chuẩn!
    uniform int uIsGrayscale; // Biến bật/tắt màu xám (0 là tắt, 1 là bật)
    
    out vec4 fragColor;
    
    void main() {
        // 1. Đọc màu RGBA của pixel tại tọa độ vTexCoord
        vec4 color = texture(uTexture, vTexCoord);
        
        if (uIsGrayscale == 1) {
            // 2. Tính độ sáng Y = 0.299R + 0.587G + 0.114B
            float gray = dot(color.rgb, vec3(0.299, 0.587, 0.114));
            fragColor = vec4(gray, gray, gray, color.a);
        } else {
            fragColor = color;
        }
    }
    
"""