package com.pru.recognizeimage.utils

import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.provider.MediaStore
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.pru.recognizeimage.R
import com.pru.recognizeimage.appContext
import com.pru.recognizeimage.ui.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File


object Global {

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
        'A' to listOf('4', '9'),
        'B' to listOf('6', '8'),
        'C' to listOf('0', 'O'),
        'D' to listOf('B', '0', 'O'),
        'E' to listOf('3','F'),
        'F' to listOf('7', 'E'),
        'G' to listOf('9', '6', '0', 'O','B'),
        'H' to listOf('4', 'I', '1'),
        'I' to listOf('1', 'L'),
        'J' to listOf('7'),
        'K' to listOf('B'),
        'L' to listOf('1', '4'),
        'M' to listOf('N', 'W'),
        'N' to listOf('M'),
        'O' to listOf('0', 'Q', '0'),
        'P' to listOf('9', 'Q'),
        'Q' to listOf('9', '0', 'O'),
        'R' to listOf('7', 'P'),
        'S' to listOf('5', 'C', '6'),
        'T' to listOf('7'),
        'U' to listOf('V'),
        'V' to listOf('U', 'Y'),
        'W' to listOf('V', 'M'),
        'X' to listOf('8'),
        'Y' to listOf('V'),
        'Z' to listOf('2'),
        '0' to listOf('O', 'G', 'Q'),
        '1' to listOf('i', 'L'),
        '2' to listOf('Z'),
        '3' to listOf('E'),
        '4' to listOf('A', 'H', 'L'),
        '5' to listOf('S', '6', 'C'),
        '6' to listOf('B', '8'),
        '7' to listOf('F', 'T', 'Z', '2'),
        '8' to listOf('B', '6', '0', 'O'),
        '9' to listOf('6', 'G', 'P', 'Q', '0', '3')
    )

    suspend fun generateDynamicCombinations(
        cases: List<Pair<Char, List<Char>>>,
        allowMultipleOccurrences: Boolean
    ): List<Result> = withContext(Dispatchers.IO) {
        val combinations = mutableListOf<Result>()
        val placeholders = cases.map { it.first }.joinToString("")

        // Add the placeholder combination
        combinations.add(
            Result(
                resultValue = placeholders,
                isSelected = true,
                multipleOccurrences = false
            )
        )

        // Add combinations of single placeholders with their corresponding values
        for ((char, values) in cases) {
            for (value in values) {
                val combinationWithPlaceholder =
                    placeholders.replace(char.toString(), value.toString())
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

        if (allowMultipleOccurrences && placeholders.length <12) {
            // Start generating full combinations
            generateFullCombinations("", 0)
        }


        // Remove duplicates and sort
        combinations.distinct()
    }

}