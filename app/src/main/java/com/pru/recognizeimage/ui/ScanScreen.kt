package com.pru.recognizeimage.ui

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pru.recognizeimage.appContext
import com.pru.recognizeimage.theme.RecognizeImageTheme
import com.pru.recognizeimage.utils.Global.generateDynamicCombinations
import com.pru.recognizeimage.utils.Global.similarChars
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.sin

@Composable
fun ScanScreen(viewModel: CameraViewModel, scanListener: (Boolean) -> Unit) {
    val onActivityPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                scanListener.invoke(viewModel.requiredCrop.value)
            } else Unit
        }
    }

    val multiplePermissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                scanListener.invoke(viewModel.requiredCrop.value)
            } else {

                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:" + appContext.packageName)
                onActivityPermissionLauncher.launch(intent)
            }
        } else {
            scanListener.invoke(viewModel.requiredCrop.value)
        }
    }

    var bitmap by remember {
        mutableStateOf<Bitmap?>(null)
    }
    val results = remember {
        mutableStateOf<List<Result>>(listOf())
    }
    var plateNumber by remember {
        mutableStateOf("")
    }
    var showMore by remember {
        mutableStateOf(true)
    }

    var showLoader by remember {
        mutableStateOf(false)
    }

    val scope = rememberCoroutineScope()

    LaunchedEffect(viewModel.capturedUri) {
        if (viewModel.capturedUri != null) {
            val uri = Uri.fromFile(viewModel.capturedUri!!)
            showLoader = true
            viewModel.handleScanCameraImage(uri = uri, bitmapListener = {
                bitmap = it
            }) { pn ->
                scope.launch(Dispatchers.IO) {
                    val processedList = mutableListOf<Result>()
                    val singleLineText =
                        StringBuilder(
                            pn.replace("[^a-zA-Z0-9]".toRegex(), "")
                                .uppercase()
                                .replace("IND", "")
                        )
                    plateNumber = singleLineText.toString()
                    val cases = mutableListOf<Pair<Char, List<Char>>>()
                    for (i in singleLineText.indices) {
                        val ls = similarChars[singleLineText[i]] ?: emptyList()
                        val returnList = ls.toMutableList()
                        returnList.add(singleLineText[i])
                        cases.add(Pair(singleLineText[i], returnList))
                    }
                    val combinations =
                        generateDynamicCombinations(cases, viewModel.allowMultipleOccurrences.value)
                    for (cmb in combinations) {
                        processedList.add(cmb)
                    }
                    results.value = processedList
                    showLoader = false
                }
            }
        }
    }

    Scaffold(containerColor = Color.White) {
        LazyColumn(
            modifier = Modifier
                .padding(it)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item {
                Row(
                    modifier = Modifier,
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.5f), contentAlignment = Alignment.TopEnd
                    ) {
                        Checkbox(checked = viewModel.requiredCrop.value, onCheckedChange = {
                            viewModel.requiredCrop.value = it
                        }, modifier = Modifier)
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1.5f), contentAlignment = Alignment.TopStart
                    ) {
                        Text(text = "CROP", modifier = Modifier, fontSize = 12.sp)
                    }
                }
                Row(
                    modifier = Modifier,
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.5f), contentAlignment = Alignment.TopEnd
                    ) {
                        Checkbox(
                            checked = viewModel.allowMultipleOccurrences.value,
                            onCheckedChange = {
                                viewModel.allowMultipleOccurrences.value = it
                            },
                            modifier = Modifier
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1.5f), contentAlignment = Alignment.TopStart
                    ) {
                        Text(
                            text = "Allow Multiple Occurrences".uppercase(),
                            modifier = Modifier,
                            fontSize = 12.sp
                        )
                    }
                }
                if (bitmap != null) {
                    Image(
                        bitmap!!.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .width(320.dp)
                            .height(150.dp),
                    )
                }
                Button(onClick = {
                    multiplePermissionsLauncher.launch(
                        arrayOf(
                            android.Manifest.permission.READ_EXTERNAL_STORAGE,
                            android.Manifest.permission.CAMERA
                        )
                    )
                }, modifier = Modifier.padding(top = 10.dp)) {
                    Text(text = "Scan Number Plate")
                }
                Spacer(modifier = Modifier.height(10.dp))
                if (plateNumber.isNotEmpty()) {
                    Text(
                        text = plateNumber,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(10.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopEnd) {
                        TextButton(
                            onClick = {
                                showMore = !showMore
                            }, modifier = Modifier.padding(10.dp)
                        ) {
                            Text(
                                text = if (showMore) "Show More" else "Show Less",
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
                if (showLoader) {
                    CircularProgressIndicator()
                }
            }

            if (!showMore) {
                items(results.value.filter { !it.multipleOccurrences }) { res ->
                    ListItem(res, results) {
                        plateNumber = it
                    }
                }
                if (viewModel.allowMultipleOccurrences.value) {
                    items(results.value.filter { it.multipleOccurrences }) { res ->
                        ListItem(res, results) {
                            plateNumber = it
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ListItem(
    res: Result,
    results: MutableState<List<Result>>,
    onClick: (String) -> Unit
) {
    ListItem(
        headlineContent = {
            Text(
                text = res.resultValue,
                modifier = Modifier.alpha(if (res.isSelected) 1f else 0.5f),
                color = if (res.multipleOccurrences) Color.Red else Color.Black,
                fontSize = 16.sp
            )
        },
        trailingContent = {
            if (res.isSelected) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = Color.Green
                )
            }
        }, modifier = Modifier.clickable {
            val prev = results.value
            val s = prev.indexOf(res)
            val rm = results.value.toMutableList()
            rm.forEach { it.isSelected = false }
            res.isSelected = true
            rm[s] = res
            results.value = emptyList()
            results.value = rm
            onClick.invoke(res.resultValue)
        })
}

data class Result(
    var resultValue: String,
    var isSelected: Boolean,
    var multipleOccurrences: Boolean
)


@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    RecognizeImageTheme {
        ScanScreen(viewModel = CameraViewModel()) {

        }
    }
}