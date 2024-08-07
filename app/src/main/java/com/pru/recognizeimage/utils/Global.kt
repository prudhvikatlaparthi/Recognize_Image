package com.pru.recognizeimage.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import com.pru.recognizeimage.R
import com.pru.recognizeimage.appContext
import com.pru.recognizeimage.ui.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File


object Global {

    sealed interface Position {
        data object Start : Position
        data object End : Position
        data object Middle : Position
    }

    data class PositionReplace(var at: Position, var with: String)

    val ignoreStrings =
//        setOf(PositionReplace(Position.Start, "IND"), PositionReplace(Position.Middle, "GAUTENG"))
        setOf<PositionReplace>()

    fun dpToPx(dp: Int): Int {
        return (dp * appContext.resources.displayMetrics.density).toInt()
    }

    fun getOutputDirectory(): File {
        val mediaDir = appContext.externalMediaDirs?.firstOrNull()?.let {
            File(it, appContext.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists()) mediaDir else appContext.filesDir
    }

    fun getFileName(): String {
        return System.currentTimeMillis().toString()
    }

    fun rotateBitmapIfNeeded(bitmap: Bitmap): Bitmap {
        return if (bitmap.height > bitmap.width) {
            // Rotate the bitmap 90 degrees to the right
            val matrix = Matrix()
            matrix.postRotate(90f)
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } else {
            // No rotation needed, return the original bitmap
            bitmap
        }
    }

    val similarChars = mapOf(
        'A' to setOf('4', '9'),
        'B' to setOf('6', '8', 'R'),
        'C' to setOf('0', 'O'),
        'D' to setOf('B', '0', 'O'),
        'E' to setOf('3', 'F'),
        'F' to setOf('7', 'E'),
        'G' to setOf('9', '6', '0', 'O', 'B'),
        'H' to setOf('M', '4', 'I', '1'),
        'I' to setOf('1', 'T', 'L'),
        'J' to setOf('7'),
        'K' to setOf('B'),
        'L' to setOf('1', '4'),
        'M' to setOf('N', 'W', 'H'),
        'N' to setOf('M', 'W'),
        'O' to setOf('0', 'D', 'Q', '6'),
        'P' to setOf('9', 'Q'),
        'Q' to setOf('9', '0', 'O'),
        'R' to setOf('7', 'P', 'B', '8'),
        'S' to setOf('5', 'C', '6'),
        'T' to setOf('I', '1', '7'),
        'U' to setOf('V'),
        'V' to setOf('U', 'Y'),
        'W' to setOf('V', 'M'),
        'X' to setOf('8'),
        'Y' to setOf('V'),
        'Z' to setOf('2', '4'),
        '0' to setOf('O', 'G', 'Q'),
        '1' to setOf('I', 'L'),
        '2' to setOf('Z'),
        '3' to setOf('E'),
        '4' to setOf('A', 'Z', 'H', 'L'),
        '5' to setOf('S', '6', 'C'),
        '6' to setOf('B', '8', '9', 'R'),
        '7' to setOf('F', 'T', 'Z', '2'),
        '8' to setOf('B', '6', '0', 'O', 'X', 'R'),
        '9' to setOf('6', 'G', 'P', 'Q', '0', '3', 'A')
    )

    suspend fun generateDynamicCombinations(
        cases: List<Pair<Char, List<Char>>>,
        allowMultipleOccurrences: Boolean
    ): List<Result> = withContext(Dispatchers.IO) {
        val combinations = mutableListOf<Result>()
        val placeholders = StringBuilder(cases.map { it.first }.joinToString(""))

        // Add the placeholder combination
        combinations.add(
            Result(
                resultValue = placeholders.toString(),
                isSelected = true,
                multipleOccurrences = false
            )
        )

        // Add combinations of single placeholders with their corresponding values
        for (i in cases.indices) {
            val (_, values) = cases[i]
            for (value in values) {
                placeholders.setCharAt(i, value)
                val combinationWithPlaceholder = placeholders.toString()
                if (combinationWithPlaceholder != value.toString()) {
                    val result = Result(
                        resultValue = combinationWithPlaceholder,
                        isSelected = false,
                        multipleOccurrences = false
                    )
                    combinations.add(result)
                }
            }
        }

        // Generate full combinations
        fun generateFullCombinations(current: String, index: Int) {
            if (index == placeholders.length) {
                val result = Result(
                    resultValue = current,
                    isSelected = false,
                    multipleOccurrences = true
                )
                combinations.add(result)
                return
            }
            val char = placeholders[index]
            cases.find { it.first == char }?.second?.forEach { value ->
                generateFullCombinations(current + value, index + 1)
            }
        }

        if (allowMultipleOccurrences && placeholders.length < 12) {
            // Start generating full combinations
            generateFullCombinations("", 0)
        }


        // Remove duplicates and sort
        combinations.distinct()
    }

    fun applyContrastAndSharpen(bitmap: Bitmap, contrastValue: Float): Bitmap {
        val resultBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config)
        val canvas = Canvas(resultBitmap)

        // Contrast
        val contrastMatrix = ColorMatrix().apply {
            set(floatArrayOf(
                contrastValue, 0f, 0f, 0f, 0f,
                0f, contrastValue, 0f, 0f, 0f,
                0f, 0f, contrastValue, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            ))
        }

        // Sharpening
        val sharpenMatrix = ColorMatrix().apply {
            set(floatArrayOf(
                0f, -0.5f, 0f, 0f, 0f,
                -0.5f, 3f, -0.5f, 0f, 0f,
                0f, -0.5f, 0f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            ))
        }

        val combinedMatrix = ColorMatrix()
        combinedMatrix.postConcat(contrastMatrix) // Apply contrast first
        combinedMatrix.postConcat(sharpenMatrix) // Then apply sharpening

        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(combinedMatrix)
        }

        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return resultBitmap
    }

    fun sharpenBitmap(bitmap: Bitmap): Bitmap {
        // Create a new bitmap with the same size and config
        val sharpenedBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config)


        // Create a paint object with sharpening color matrix
        val paint = Paint()
        val matrix = ColorMatrix()
        matrix.set(
            floatArrayOf(
                1.0f, -0.5f, 0.0f, 0.0f, 0.0f,
                -0.5f, 2.0f, -0.5f, 0.0f, 0.0f,
                0.0f, -0.5f, 1.0f, 0.0f, 0.0f,
                0.0f, 0.0f, 0.0f, 1.0f, 0.0f
            )
        )
        paint.setColorFilter(ColorMatrixColorFilter(matrix))


        // Apply the paint to the new bitmap
        val canvas = Canvas(sharpenedBitmap)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)

        return sharpenedBitmap
    }

    fun changeBitmapContrastBrightness(bmp: Bitmap, contrast: Float, brightness: Float): Bitmap {
        val cm = ColorMatrix(floatArrayOf(
            contrast, 0f, 0f, 0f, brightness,
            0f, contrast, 0f, 0f, brightness,
            0f, 0f, contrast, 0f, brightness,
            0f, 0f, 0f, 1f, 0f
        ))
        val ret = Bitmap.createBitmap(bmp.width, bmp.height, bmp.config)
        val canvas = Canvas(ret)
        val paint = Paint()
        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(bmp, 0f, 0f, paint)
        return ret
    }

}