package com.pru.recognizeimage.ui

import android.net.Uri
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import com.pru.recognizeimage.R
import com.pru.recognizeimage.appContext
import com.pru.recognizeimage.theme.RecognizeImageTheme
import java.io.File


@Composable
fun CameraScreen(crop: Boolean, viewModel: CameraViewModel, resultListener: () -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val activityListener = rememberLauncherForActivityResult(contract = CropImageContract()) {
        it.uriContent?.path?.let {
            viewModel.capturedFile = File(it)
        }
        resultListener.invoke()
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
                        implementationMode =
                            PreviewView.ImplementationMode.COMPATIBLE
                        post {
                            viewModel.startCamera(this.surfaceProvider, lifecycleOwner)
                        }
                    }
                }, modifier = Modifier
                    .width(dimensionResource(id = R.dimen.sc_width))
                    .height(
                        dimensionResource(id = R.dimen.sc_height)
                    )
                    .paint(
                        painterResource(id = R.drawable.bg),
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
                                uri = Uri.fromFile(viewModel.capturedFile!!),
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