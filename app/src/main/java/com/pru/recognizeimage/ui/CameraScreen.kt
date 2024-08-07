package com.pru.recognizeimage.ui

import android.annotation.SuppressLint
import android.net.Uri
import android.view.MotionEvent
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.camera.view.PreviewView
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
    var showLoader by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val activityListener =
        rememberLauncherForActivityResult(contract = CropImageContract()) { result ->
            result.getUriFilePath(appContext, true)?.let {
                viewModel.capturedUri = File(it)
                scope.launch {
                    delay(500)
                    resultListener.invoke()
                }
            }
        }
    Scaffold(containerColor = if(showLoader) Color.White else Color.Black) {
        Box(
            modifier = Modifier
                .padding(it)
                .fillMaxSize(), contentAlignment = Alignment.Center
        ) {

            if (showLoader) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                Column(
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
                                        viewModel.focusOnPoint(
                                            x = event.x,
                                            y = event.y,
                                            view = this
                                        )
                                        true
                                    } else {
                                        false
                                    }
                                }
                            }
                        }, modifier = Modifier
                            .border(
                                width = 1.dp,
                                color = Color.White,
                                shape = RoundedCornerShape(5.dp)
                            )
                            .width(320.dp)
                            .height(
                                150.dp
                            )

                    )
                    Button(onClick = {
                        viewModel.takePhoto { res, msg ->
                            if (!res) {
                                Toast.makeText(appContext, msg, Toast.LENGTH_SHORT).show()
                                return@takePhoto
                            }
                            if (crop) {
                                showLoader = true
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

    }
}


@Preview(showBackground = true)
@Composable
fun CameraViewPreview() {
    RecognizeImageTheme {

    }
}