package com.pru.recognizeimage

import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.provider.MediaStore
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
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

    val repeatChars = mutableMapOf(
        Pair('C', 2), Pair('D', 2), Pair('E', 2), Pair('G', 4),
        Pair('H', 2), Pair('I', 3), Pair('L', 2), Pair('M', 2),
        Pair('O', 3), Pair('P', 2), Pair('Q', 2),
        Pair('S', 3), Pair('V', 2), Pair('W', 2), Pair('0', 3),
        Pair('1', 2), Pair('4', 2),
        Pair('5', 3), Pair('6', 5), Pair('7', 2), Pair('8', 2),
        Pair('9', 5)
    )

    fun giveMeSimilar(c: Char, j: Int): Char {
        when (c) {
            'A' -> {
                return '4'
            }

            'B' -> {
                return '8'
            }

            'C' -> {
                return if (j == 0) 'O' else '0'
            }

            'D' -> {
                return if (j == 0) 'O' else '0'
            }

            'E' -> {
                return if (j == 0) 'E' else '3'
            }

            'F' -> {
                return 'E'
            }

            'G' -> {
                return if (j == 0) '6' else if (j == 1) '0' else if (j == 2) 'O' else '9'
            }

            'H' -> {
                return if (j == 0) 'I' else '1'
            }

            'I' -> {
                return if (j == 0) '1' else if (j == 1) 'i' else 'L'
            }

            'J' -> {
                return '7'
            }

            'K' -> {
                return 'B'
            }

            'L' -> {
                return if (j == 0) '4' else '1'
            }

            'M' -> {
                return if (j == 0) 'N' else 'W'
            }

            'N' -> {
                return 'M'
            }

            'O' -> {
                return if (j == 0) 'G' else if (j == 1) 'Q' else '0'
            }

            'P' -> {
                return if (j == 0) '9' else 'Q'
            }

            'Q' -> {
                return if (j == 0) '0' else 'O'
            }

            'R' -> {
                return 'P'
            }

            'S' -> {
                return if (j == 0) '5' else if (j == 1) 'C' else '6'
            }

            'T' -> {
                return '7'
            }

            'U' -> {
                return 'V'
            }

            'V' -> {
                return if (j == 0) 'U' else 'Y'
            }

            'W' -> {
                return if (j == 0) 'M' else 'V'
            }

            'X' -> {
                return '8'
            }

            'Y' -> {
                return 'V'
            }

            'Z' -> {
                return '2'
            }

            '0' -> {
                return if (j == 0) 'G' else if (j == 2) 'Q' else 'O'
            }

            '1' -> {
                return if (j == 0) 'I' else 'L'
            }

            '2' -> {
                return 'Z'
            }

            '3' -> {
                return 'E'
            }

            '4' -> {
                return if (j == 0) 'A' else 'L'
            }

            '5' -> {
                return if (j == 0) 'S' else if (j == 1) '6' else 'C'
            }

            '6' -> {
                return if (j == 0) 'S' else if (j == 1) 'B' else if (j == 2) 'G' else if (j == 3) '9' else 'C'
            }

            '7' -> {
                return if (j == 0) 'Z' else 'T'
            }

            '8' -> {
                return if (j == 0) '8' else '0'
            }

            '9' -> {
                return if (j == 0) 'G' else if (j == 1) '6' else if (j == 2) '0' else if (j == 3) '3' else 'Q'
            }
        }
        return '$'
    }

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
            val singleLineText =
                StringBuilder(
                    text.replace("[^a-zA-Z0-9]".toRegex(), "")
                        .uppercase()
                        .replace("IND", "")
                )
            results.add(Result(resultValue = singleLineText.toString(), isSelected = true))
            for (i in singleLineText.indices) {
                var rc = repeatChars[singleLineText[i]] ?: 0
                rc = if (rc > 0) rc - 1 else rc
                for (j in 0..rc) {
                    val temp = StringBuilder(singleLineText)
                    val st = giveMeSimilar(singleLineText[i], j)
                    temp.setCharAt(i, st)
                    results.add(Result(resultValue = temp.toString(), isSelected = false))
                }
            }
            resultListener.invoke(singleLineText.toString(), results)
        }.addOnFailureListener {
            it.printStackTrace()
        }
    }

}