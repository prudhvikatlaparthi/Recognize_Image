package com.pru.recognizeimage.ui

import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.pru.recognizeimage.appContext
import com.pru.recognizeimage.utils.Global
import com.pru.recognizeimage.utils.Global.changeBitmapContrastBrightness
import com.pru.recognizeimage.utils.Global.dpToPx
import com.pru.recognizeimage.utils.Global.generateDynamicCombinations
import com.pru.recognizeimage.utils.Global.rotateBitmapIfNeeded
import com.pru.recognizeimage.utils.Global.similarChars
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

class CameraViewModel : ViewModel() {

    private val imageCapture = ImageCapture.Builder()
        .setTargetResolution(Size(dpToPx(320), dpToPx(150)))
        .build()
    private val imageAnalyzer = ImageAnalysis.Builder()
        .build()
    private val preview = Preview.Builder()
        .setTargetResolution(Size(dpToPx(320), dpToPx(150)))
        .build()

    var capturedUri: File? = null

    private var cameraControl: CameraControl? = null

    val requiredCrop = mutableStateOf(false)
    private val allowMultipleOccurrences = mutableStateOf(false)
    val imageProcess = mutableStateOf(false)


    val bitmap = mutableStateOf<Bitmap?>(null)
    private val results =
        mutableStateOf<List<Result>>(listOf())
    val plateNumber =
        mutableStateOf<List<PlateNumb>>(listOf())

    var showLoader =
        mutableStateOf(false)

    var showSelectionDialog =
        mutableStateOf(false)

    var addPosition: Global.Position = Global.Position.None

    val showAddChar = false

    fun startCamera(surfaceProvider: Preview.SurfaceProvider, lifecycleOwner: LifecycleOwner) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(appContext)
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            preview.setSurfaceProvider(surfaceProvider)


            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                val camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture,
                    imageAnalyzer
                )
                cameraControl = camera.cameraControl
            } catch (exc: Exception) {
                exc.printStackTrace()
            }

        }, ContextCompat.getMainExecutor(appContext))
    }

    fun takePhoto(result: (Boolean, String) -> Unit) {
        val photoFile = File(
            Global.getOutputDirectory(), Global.getFileName() + ".png"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(outputOptions,
            ContextCompat.getMainExecutor(appContext),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    result.invoke(false, exc.message ?: "")
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    capturedUri = photoFile
                    result.invoke(true, "")
                }
            })
    }

    fun focusOnPoint(view: PreviewView, x: Float, y: Float) {
        // Convert view coordinates to camera coordinates
        val factory = view.meteringPointFactory
        val point = factory.createPoint(x, y)

        // Create FocusMeteringAction
        val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF)
            .setAutoCancelDuration(5, TimeUnit.SECONDS)
            .build()

        // Start focus and metering
        val result = cameraControl?.startFocusAndMetering(action) ?: return
        result.addListener({
            try {
                val success = result.get().isFocusSuccessful
                if (success) {
                    Log.d("TAG", "Focus successful")
                } else {
                    Log.d("TAG", "Focus failed")
                }
            } catch (e: Exception) {
                Log.e("TAG", "Focus failed with exception: $e")
            }
        }, ContextCompat.getMainExecutor(appContext))
    }


    private suspend fun handleScanCameraImage(
        uri: Uri?,
        bitmapListener: (Bitmap) -> Unit,
        visionTextListener: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        var bitmap = MediaStore.Images.Media.getBitmap(appContext.contentResolver, uri)
        if (!requiredCrop.value) {
            bitmap = rotateBitmapIfNeeded(bitmap)
        }
        if (imageProcess.value) {
            bitmap = changeBitmapContrastBrightness(bitmap, 1.5f, -0.2f)
        }
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        bitmapListener.invoke(bitmap)
        val imageInput = InputImage.fromBitmap(bitmap, 0)
        recognizer.process(imageInput).addOnSuccessListener { visionText ->
            Log.i("Prudhvi Log", "handleScanCameraImage: All text ${visionText.text}")
            val allElements = visionText.textBlocks
                .flatMap { it.lines }
                .flatMap { it.elements }

            val maxHeight = allElements.maxOfOrNull { it.boundingBox?.height() ?: 0 } ?: 0
            val tolerance = 30

            val largestTextElements = allElements.filter { element ->
                val height = element.boundingBox?.height() ?: 0
                height in (maxHeight - tolerance)..maxHeight
            }

            allElements.forEach {
                Log.i(
                    "Prudhvi Log",
                    "handleScanCameraImage: Text & Height ${it.text} ${it.boundingBox?.height()}"
                )
            }

            if (largestTextElements.isNotEmpty()) {
                var largeText = ""
                largestTextElements.forEach { largestTextElement ->
                    largeText += largestTextElement.text
                }
                if (requiredCrop.value) {
                    visionTextListener.invoke(visionText.text)
                } else {
                    visionTextListener.invoke(largeText)
                }
            } else {
                Log.i("Prudhvi Log", "handleScanCameraImage: No text elements found.")
                println("No text elements found.")
                visionTextListener.invoke("CLARK")
            }
        }.addOnFailureListener {
            it.printStackTrace()
        }
    }

    fun readImageFromUri() {
        viewModelScope.launch(Dispatchers.IO) {
            if (capturedUri != null) {
                val uri = Uri.fromFile(capturedUri!!)
                showLoader.value = true
                handleScanCameraImage(uri = uri, bitmapListener = {
                    bitmap.value = it
                }) { pn ->
                    if (pn.isBlank()) {
                        showLoader.value = false
                        plateNumber.value = emptyList()
                        results.value = emptyList()
                        return@handleScanCameraImage
                    }
                    viewModelScope.launch(Dispatchers.IO) {
                        val processedList = mutableListOf<Result>()
                        var resultText = pn.replace("[^a-zA-Z0-9]".toRegex(), "").uppercase()
                        for (pos in Global.ignoreStrings) {
                            resultText = when (pos.at) {
                                Global.Position.End -> resultText.replaceAfterLast(pos.with, "")
                                Global.Position.Middle -> resultText.replace(pos.with, "")
                                Global.Position.Start -> resultText.replaceFirst(pos.with, "")
                                Global.Position.None -> resultText
                            }
                        }
                        plateNumber.value = resultText.map {
                            PlateNumb(
                                actual = it, char = it, tapped = 0
                            )
                        }
                        val cases = mutableListOf<Pair<Char, List<Char>>>()
                        for (i in resultText.indices) {
                            val ls = similarChars[resultText[i]] ?: emptySet()
                            val returnList = ls.toMutableList()
                            returnList.sorted()
                            returnList.add(resultText[i])
                            cases.add(Pair(resultText[i], returnList.map { it.uppercaseChar() }))
                        }
                        val combinations =
                            generateDynamicCombinations(cases, allowMultipleOccurrences.value)
                        for (cmb in combinations) {
                            processedList.add(cmb)
                        }
                        results.value = processedList
                        showLoader.value = false
                    }
                }
            }
        }
    }

    fun changeChar(
        index: Int
    ) {
        viewModelScope.launch {
            val pn = plateNumber.value[index]
            if (!pn.active) return@launch
            showLoader.value = true
            val pm =
                plateNumber.value.toMutableList()
            val tapped = pn.tapped
            val chars = similarChars[pn.actual]
            chars
                ?.toList()
                ?.getOrNull(tapped)
                ?.let {
                    pn.char = it
                    pn.tapped = tapped + 1
                } ?: run {
                pn.char = pn.actual
                pn.tapped = 0
            }
            pm[index] = pn
            plateNumber.value = emptyList()
            plateNumber.value = pm

            showLoader.value = false
        }
    }

    fun disableChar(index: Int) {
        val pn = plateNumber.value[index]
        val pm = plateNumber.value.toMutableList()
        pm[index] = pn.copy(active = !pn.active)
        plateNumber.value = emptyList()
        plateNumber.value = pm
    }

    fun addChar(item: Char) {
        if (addPosition == Global.Position.None) return
        val old = plateNumber.value.toMutableList()
        old.add(
            if (addPosition == Global.Position.Start) 0 else (plateNumber.value.size),
            PlateNumb(
                actual = item,
                char = item,
                tapped = 0,
                active = true,
                enableToChangePosition = false
            )
        )
        plateNumber.value = emptyList()
        plateNumber.value = old
    }

    fun enableToChangePosition(index: Int) {
        val old = plateNumber.value.toMutableList()
        val status = old[index].enableToChangePosition
        old.forEach { it.enableToChangePosition = false }
        old[index] = old[index].copy(enableToChangePosition = !status)
        plateNumber.value = emptyList()
        plateNumber.value = old
    }

    fun changeCharPosition(index: Int, position: Global.Position) {
        val destinationPos = index + when (position) {
            Global.Position.Start -> 1
            Global.Position.End -> -1
            else -> 0
        }
        val old = plateNumber.value.toMutableList()
        val source = old[index].copy(enableToChangePosition = false)
        val destination = old[destinationPos].copy(enableToChangePosition = false)
        old[index] = destination
        old[destinationPos] = source
        plateNumber.value = emptyList()
        plateNumber.value = old
    }
}