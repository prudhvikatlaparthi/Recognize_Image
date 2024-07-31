package com.pru.recognizeimage.ui

import android.annotation.SuppressLint
import android.net.Uri
import android.view.MotionEvent
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import com.pru.recognizeimage.appContext
import com.pru.recognizeimage.theme.RecognizeImageTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File


@SuppressLint("ClickableViewAccessibility")
@Composable
fun CameraScreen(crop: Boolean, viewModel: CameraViewModel, resultListener: () -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val activityListener = rememberLauncherForActivityResult(contract = CropImageContract()) { result ->
        result.originalUri?.path?.let {
            viewModel.capturedUri = File(it)
            scope.launch {
                delay(200)
                resultListener.invoke()
            }
        }
    }
    Scaffold(containerColor = Color.Black) {
        Column(
            modifier = Modifier
                .padding(it)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AndroidView(
                factory = { context ->
                    PreviewView(context).apply {
                        layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                        this.clipToOutline = true
                        post {
                            viewModel.startCamera(this.surfaceProvider, lifecycleOwner)
                        }
                        this.setOnTouchListener { _, event ->
                            if (event.action == MotionEvent.ACTION_DOWN) {
                                viewModel.focusOnPoint(x = event.x, y = event.y, view = this)
                                true
                            } else {
                                false
                            }
                        }
                    }
                }, modifier = Modifier
                    .width(320.dp)
                    .height(
                        200.dp
                    )

            )
            Button(onClick = {
                viewModel.takePhoto { res, msg ->
                    if (!res) {
                        Toast.makeText(appContext, msg, Toast.LENGTH_SHORT).show()
                        return@takePhoto
                    }
                    if (crop) {
                        activityListener.launch(
                            CropImageContractOptions(
                                uri = Uri.fromFile(viewModel.capturedUri!!),
                                cropImageOptions = CropImageOptions(
                                    guidelines = CropImageView.Guidelines.ON,
                                    cropShape = CropImageView.CropShape.RECTANGLE,
                                    aspectRatioX = 1,
                                    aspectRatioY = 1,
                                    outputCompressQuality = 100
                                )
                            )
                        )
                    } else {
                        resultListener.invoke()
                    }
                }
            }, modifier = Modifier.padding(top = 50.dp)) {
                Text(text = "Capture")
            }
        }

    }
}


@Preview(showBackground = true)
@Composable
fun CameraViewPreview() {
    RecognizeImageTheme {

    }
}