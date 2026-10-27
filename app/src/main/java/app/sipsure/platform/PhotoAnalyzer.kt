package app.sipsure.platform

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import app.sipsure.domain.PhotoFeatures
import java.io.File
import kotlin.math.sqrt

/** Decodes a captured photo into cheap [PhotoFeatures]. Never logs or retains pixels. */
object PhotoAnalyzer {
    private const val STATS_SIZE = 64
    private const val HASH_SIZE = 8

    /** Returns null if the file is not a decodable image. */
    fun analyze(file: File): PhotoFeatures? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sample = 1
        while (bounds.outWidth / (sample * 2) >= 512 && bounds.outHeight / (sample * 2) >= 512) sample *= 2
        val decoded = BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })
            ?: return null

        val stats = Bitmap.createScaledBitmap(decoded, STATS_SIZE, STATS_SIZE, true)
        val luma = lumas(stats)
        val mean = luma.average()
        val std = sqrt(luma.sumOf { (it - mean) * (it - mean) } / luma.size)

        val thumb = Bitmap.createScaledBitmap(decoded, HASH_SIZE, HASH_SIZE, true)
        val thumbLuma = lumas(thumb)
        val thumbMean = thumbLuma.average()
        var hash = 0L
        thumbLuma.forEachIndexed { i, v -> if (v >= thumbMean) hash = hash or (1L shl i) }

        val result = PhotoFeatures(
            widthPx = bounds.outWidth,
            heightPx = bounds.outHeight,
            meanLuma = mean,
            lumaStdDev = std,
            hash = hash,
            capturedAtMs = file.lastModified().takeIf { it > 0 },
        )
        if (stats !== decoded) stats.recycle()
        if (thumb !== decoded) thumb.recycle()
        decoded.recycle()
        return result
    }

    private fun lumas(bitmap: Bitmap): DoubleArray {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return DoubleArray(pixels.size) {
            0.299 * Color.red(pixels[it]) + 0.587 * Color.green(pixels[it]) + 0.114 * Color.blue(pixels[it])
        }
    }
}
