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

    private fun rotateBitmapIfNeeded(bitmap: Bitmap): Bitmap {
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

    private val similarChars = mapOf(
        'a' to listOf('4', '9'),
        'b' to listOf('6', '8'),
        'c' to listOf('0', 'o'),
        'd' to listOf('b', '0', 'o'),
        'e' to listOf('3'),
        'f' to listOf('7', 'e'),
        'g' to listOf('9', '6', '0', 'o','b'),
        'h' to listOf('4', 'i', '1'),
        'i' to listOf('1', 'l'),
        'j' to listOf('7'),
        'k' to listOf('b'),
        'l' to listOf('1', '4'),
        'm' to listOf('n', 'w'),
        'n' to listOf('m'),
        'o' to listOf('0', 'q', '0'),
        'p' to listOf('9', 'q'),
        'q' to listOf('9', '0', 'o'),
        'r' to listOf('7', 'p'),
        's' to listOf('5', 'c', '6'),
        't' to listOf('7'),
        'u' to listOf('v'),
        'v' to listOf('u', 'y'),
        'w' to listOf('v', 'm'),
        'x' to listOf('8'),
        'y' to listOf('v'),
        'z' to listOf('2'),
        '0' to listOf('o', 'g', 'q'),
        '1' to listOf('i', 'l'),
        '2' to listOf('z'),
        '3' to listOf('e'),
        '4' to listOf('a', 'h', 'l'),
        '5' to listOf('s', '6', 'c'),
        '6' to listOf('b', '8'),
        '7' to listOf('f', 't', 'z', '2'),
        '8' to listOf('b', '6', '0', 'o'),
        '9' to listOf('6', 'g', 'p', 'q', '0', '3')
    )

    fun handleScanCameraImage(
        uri: Uri,
        bitmapListener: (Bitmap) -> Unit,
        resultListener: (String, List<Result>) -> Unit
    ) {
        var bitmap = MediaStore.Images.Media.getBitmap(appContext.contentResolver, uri)
        bitmap = rotateBitmapIfNeeded(bitmap)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        bitmapListener.invoke(bitmap)
        val imageInput = InputImage.fromBitmap(bitmap, 0)
        recognizer.process(imageInput).addOnSuccessListener { visionText ->
            val results = mutableListOf<Result>()
            val text = visionText.text
//            val text = "ab"
            val singleLineText =
                StringBuilder(
                    text.replace("[^a-zA-Z0-9]".toRegex(), "")
                        .lowercase()
                        .replace("ind", "")
                )
            val resultValue = singleLineText.toString().uppercase()
            /*results.add(Result(resultValue = resultValue, isSelected = true, multipleOccurrences = false))
            for (i in singleLineText.indices) {
                val ls = similarChars[singleLineText[i]] ?: emptyList()
                for (c in ls) {
                    val temp = StringBuilder(singleLineText)
                    temp.setCharAt(i, c)
                    results.add(
                        Result(
                            resultValue = temp.toString().uppercase(),
                            isSelected = false,
                            multipleOccurrences = false
                        )
                    )
                }
            }*/

            val cases = mutableMapOf<Char,List<Char>>()
            for (i in singleLineText.indices) {
                val ls = similarChars[singleLineText[i]] ?: emptyList()
                cases[singleLineText[i]] = ls
            }
            val combinations = generateDynamicCombinations(cases)
            for (cmb in combinations){
                results.add(cmb)
            }


            resultListener.invoke(resultValue, results)
        }.addOnFailureListener {
            it.printStackTrace()
        }
    }


    private fun generateDynamicCombinations(cases: Map<Char, List<Char>>): List<Result> {
        val combinations = mutableListOf<Result>()
        val placeholders = cases.keys.joinToString("")

        // Add the placeholder combination
        combinations.add(Result(resultValue = placeholders.uppercase(), isSelected = true, multipleOccurrences = false))

        // Add combinations of single placeholders with their corresponding values
        for ((char, values) in cases) {
            for (value in values) {
                val combinationWithPlaceholder = placeholders.replace(char.toString(), value.toString())
                if (combinationWithPlaceholder != value.toString()) {
                    val result = Result(
                        resultValue = combinationWithPlaceholder.uppercase(),
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
                    resultValue = current.uppercase(),
                    isSelected = false,
                    multipleOccurrences = true
                )
                combinations.add(result)
                return
            }
            val char = placeholders[index]
            cases[char]?.forEach { value ->
                generateFullCombinations(current + value, index + 1)
            }
        }

        // Start generating full combinations
        generateFullCombinations("", 0)

        // Remove duplicates and sort
        return combinations.distinct()
    }

}