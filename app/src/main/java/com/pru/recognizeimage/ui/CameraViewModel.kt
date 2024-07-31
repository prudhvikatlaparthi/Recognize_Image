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
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.pru.recognizeimage.utils.Global
import com.pru.recognizeimage.utils.Global.dpToPx
import com.pru.recognizeimage.appContext
import com.pru.recognizeimage.utils.Global.rotateBitmapIfNeeded
import kotlinx.coroutines.Dispatchers
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

    private var cameraControl : CameraControl? = null

    val requiredCrop = mutableStateOf(false)
    val allowMultipleOccurrences = mutableStateOf(false)

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


    suspend fun handleScanCameraImage(
        uri: Uri?,
        bitmapListener: (Bitmap) -> Unit,
        visionTextListener: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        var bitmap = MediaStore.Images.Media.getBitmap(appContext.contentResolver, uri)
        bitmap = rotateBitmapIfNeeded(bitmap)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        bitmapListener.invoke(bitmap)
        val imageInput = InputImage.fromBitmap(bitmap, 0)
        recognizer.process(imageInput).addOnSuccessListener { visionText ->
            Log.i("Prudhvi Log", "handleScanCameraImage: $visionText")
            visionTextListener.invoke(visionText.text)
        }.addOnFailureListener {
            it.printStackTrace()
        }
    }
}