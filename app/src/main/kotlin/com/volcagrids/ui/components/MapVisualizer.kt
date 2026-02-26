package com.volcagrids.ui.components

import android.graphics.Bitmap
import android.graphics.Color
import com.volcagrids.engine.Resources

class MapVisualizer {
    private val nodeDensities = FloatArray(25)

    init {
        // Pre-calculate the average density of each of the 25 nodes
        for (i in 0 until 25) {
            val node = Resources.nodeTable[i]
            var sum = 0
            for (val8 in node) {
                sum += val8
            }
            nodeDensities[i] = sum.toFloat() / (96 * 255f)
        }
    }

    private val drumMap = arrayOf(
        intArrayOf(10, 8, 0, 9, 11),
        intArrayOf(15, 7, 13, 12, 6),
        intArrayOf(18, 14, 4, 5, 3),
        intArrayOf(23, 16, 21, 1, 2),
        intArrayOf(24, 19, 17, 20, 22)
    )

    fun generateHeatmap(width: Int, height: Int, colorTint: Int): Bitmap {
        // Optimization: Use a 64x64 buffer for performance and scale it up in the UI
        val renderW = 64
        val renderH = 64
        val bitmap = Bitmap.createBitmap(renderW, renderH, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(renderW * renderH)

        for (py in 0 until renderH) {
            for (px in 0 until renderW) {
                val x = (px.toFloat() / renderW * 255).toInt()
                val y = (py.toFloat() / renderH * 255).toInt()

                val i = (x shr 6).coerceAtMost(3)
                val j = (y shr 6).coerceAtMost(3)
                val xi = ((x shl 2) and 0xff) / 255f
                val yi = ((y shl 2) and 0xff) / 255f

                val d00 = nodeDensities[drumMap[i][j]]
                val d10 = nodeDensities[drumMap[i + 1][j]]
                val d01 = nodeDensities[drumMap[i][j + 1]]
                val d11 = nodeDensities[drumMap[i + 1][j + 1]]

                // Bi-linear interpolation of density
                val d0 = d00 * (1 - xi) + d10 * xi
                val d1 = d01 * (1 - xi) + d11 * xi
                val density = d0 * (1 - yi) + d1 * yi

                val alpha = (density * 150).toInt() // Max opacity 150/255
                pixels[py * renderW + px] = (alpha shl 24) or (colorTint and 0x00FFFFFF)
            }
        }
        bitmap.setPixels(pixels, 0, renderW, 0, 0, renderW, renderH)
        return bitmap
    }
}
