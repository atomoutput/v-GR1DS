package com.volcagrids.ui.components

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlin.random.Random

// A generative "Data Noise" visualizer akin to Alva Noto visuals
class GlitchShader {
    
    // Generates a 1-bit style noise pattern representing density
    fun generateNoiseMap(width: Int, height: Int, densityX: Float, densityY: Float, color: Color): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width * height)
        val colorInt = color.toArgb()
        val bgInt = Color(0xFF0A0A0A).toArgb() // Almost black
        
        val threshold = (densityX + densityY) / 2f
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                // Procedural grid lines
                val isGrid = (x % 16 == 0) || (y % 16 == 0)
                
                // Noise cluster based on proximity to "cursor" logic or just raw static
                val noise = Random.nextFloat()
                
                // Create a "Data Band" look
                val isBand = (y % 8) < 2
                
                var pixelColor = bgInt
                
                if (isGrid) {
                    pixelColor = Color(0xFF222222).toArgb()
                } else if (isBand && noise < threshold * 0.3f) {
                    pixelColor = colorInt
                }
                
                pixels[y * width + x] = pixelColor
            }
        }
        
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }
}
