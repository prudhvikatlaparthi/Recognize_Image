package com.pru.recognizeimage.ui

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
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
import com.pru.recognizeimage.utils.Global
import com.pru.recognizeimage.utils.Global.generateDynamicCombinations
import com.pru.recognizeimage.utils.Global.similarChars
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@ExperimentalFoundationApi
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
    val listState = rememberLazyListState()

    LaunchedEffect(viewModel.capturedUri) {
        if (viewModel.capturedUri != null) {
            val uri = Uri.fromFile(viewModel.capturedUri!!)
            showLoader = true
            viewModel.handleScanCameraImage(uri = uri, bitmapListener = {
                bitmap = it
            }) { pn ->
                scope.launch(Dispatchers.IO) {
                    val processedList = mutableListOf<Result>()
                    var resultText = pn.replace("[^a-zA-Z0-9]".toRegex(), "")
                        .uppercase()
                    for (pos in Global.ignoreStrings){
                        resultText = when(pos.at){
                            Global.Position.End -> resultText.replaceAfterLast(pos.with, "")
                            Global.Position.Middle -> resultText.replace(pos.with, "")
                            Global.Position.Start -> resultText.replaceFirst(pos.with, "")
                        }
                    }
                    val processedText =
                        StringBuilder(
                            resultText
                        )
                    plateNumber = processedText.toString()
                    val cases = mutableListOf<Pair<Char, List<Char>>>()
                    for (i in processedText.indices) {
                        val ls = similarChars[processedText[i]] ?: emptyList()
                        val returnList = ls.toMutableList()
                        returnList.add(processedText[i])
                        cases.add(Pair(processedText[i], returnList.map { it.uppercaseChar() }))
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

    Scaffold(containerColor = Color.White, floatingActionButton = {
        AnimatedVisibility(
            visible = !listState.isScrollingUp(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            FloatingActionButton(
                modifier = Modifier,
                onClick = {
                    scope.launch {
                        listState.scrollToItem(0)
                    }
                },
                containerColor = Color.White, contentColor = Color.Black
            ) {
                Icon(
                    Icons.Default.ArrowUpward,
                    contentDescription = "go to top"
                )
            }
        }
    }) {
        Column(
            modifier = Modifier
                .padding(it)
                .fillMaxSize()
        ) {
            LazyColumn(
                horizontalAlignment = Alignment.CenterHorizontally,
                state = listState
            ) {
                stickyHeader {
                    if (bitmap != null) {
                        Surface(
                            Modifier
                                .fillParentMaxWidth()
                                .padding(top = 5.dp, start = 5.dp, end = 5.dp)
                        ) {
                            Image(
                                bitmap!!.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier
                                    .border(
                                        width = 1.dp,
                                        color = Color.Black,
                                        shape = RoundedCornerShape(5.dp)
                                    )
                                    .width(320.dp)
                                    .height(150.dp),
                            )
                        }
                    }
                }
                item {
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = viewModel.requiredCrop.value, onCheckedChange = {
                                viewModel.requiredCrop.value = it
                            }, modifier = Modifier)
                            Text(text = "CROP", modifier = Modifier, fontSize = 12.sp)
                        }
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = viewModel.allowMultipleOccurrences.value,
                                onCheckedChange = {
                                    viewModel.allowMultipleOccurrences.value = it
                                },
                                modifier = Modifier
                            )
                            Text(
                                text = "Multiple\nOccurrences".uppercase(),
                                modifier = Modifier,
                                fontSize = 12.sp
                            )
                        }
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
                    if (plateNumber.isNotEmpty()) {
                        Text(
                            text = plateNumber,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(10.dp)
                        )
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.TopEnd
                        ) {
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
                    }
                    if (showLoader) {
                        CircularProgressIndicator()
                    }
                }
            }
            if (!showMore) {
                LazyVerticalGrid(columns = GridCells.Fixed(2)) {
                    items(results.value.filter { !it.multipleOccurrences }) { res ->
                        ListItem(res, results) {
                            plateNumber = it
                        }
                    }
                    if (viewModel.allowMultipleOccurrences.value) {
                        items(results.value.filter { it.multipleOccurrences }
                            .sortedBy { it.resultValue }) { res ->
                            ListItem(res, results) {
                                plateNumber = it
                            }
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
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        return resultValue == (other as Result).resultValue
    }

    override fun hashCode(): Int {
        return resultValue.hashCode()
    }
}

@Composable
fun LazyListState.isScrollingUp(): Boolean {
    var previousIndex by remember(this) { mutableStateOf(firstVisibleItemIndex) }
    var previousScrollOffset by remember(this) { mutableStateOf(firstVisibleItemScrollOffset) }
    return remember(this) {
        derivedStateOf {
            if (previousIndex != firstVisibleItemIndex) {
                previousIndex > firstVisibleItemIndex
            } else {
                previousScrollOffset >= firstVisibleItemScrollOffset
            }.also {
                previousIndex = firstVisibleItemIndex
                previousScrollOffset = firstVisibleItemScrollOffset
            }
        }
    }.value
}


@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    RecognizeImageTheme {

    }
}