package com.pru.recognizeimage.ui

import android.Manifest
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
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
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
    val plateNumber = remember {
        mutableStateOf<List<PlateNumb>>(listOf())
    }
    var showMore by remember {
        mutableStateOf(true)
    }

    var showLoader by remember {
        mutableStateOf(false)
    }

    val scope = rememberCoroutineScope()
    val gridState = rememberLazyGridState()

    LaunchedEffect(viewModel.capturedUri) {
        if (viewModel.capturedUri != null) {
            val uri = Uri.fromFile(viewModel.capturedUri!!)
            showLoader = true
            viewModel.handleScanCameraImage(uri = uri, bitmapListener = {
                bitmap = it
            }) { pn ->
                if (pn.isBlank()) {
                    showLoader = false
                    plateNumber.value = emptyList()
                    results.value = emptyList()
                    return@handleScanCameraImage
                }
                scope.launch(Dispatchers.IO) {
                    val processedList = mutableListOf<Result>()
                    var resultText = pn.replace("[^a-zA-Z0-9]".toRegex(), "").uppercase()
                    for (pos in Global.ignoreStrings) {
                        resultText = when (pos.at) {
                            Global.Position.End -> resultText.replaceAfterLast(pos.with, "")
                            Global.Position.Middle -> resultText.replace(pos.with, "")
                            Global.Position.Start -> resultText.replaceFirst(pos.with, "")
                        }
                    }
                    plateNumber.value = resultText.map {
                        PlateNumb(
                            actual = it, char = it, tapped = 0
                        )
                    }
                    val cases = mutableListOf<Pair<Char, List<Char>>>()
                    for (i in resultText.indices) {
                        val ls = similarChars[resultText[i]] ?: emptyList()
                        val returnList = ls.toMutableList()
                        returnList.add(resultText[i])
                        cases.add(Pair(resultText[i], returnList.map { it.uppercaseChar() }))
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
            visible = !gridState.isScrollingUp(), enter = fadeIn(), exit = fadeOut()
        ) {
            FloatingActionButton(
                modifier = Modifier, onClick = {
                    scope.launch {
                        gridState.scrollToItem(0)
                    }
                }, containerColor = Color.White, contentColor = Color.Black
            ) {
                Icon(
                    Icons.Default.ArrowUpward, contentDescription = "go to top"
                )
            }
        }
    }) { paddingValue ->
        Column(
            modifier = Modifier
                .padding(paddingValue)
                .fillMaxSize()
        ) {
            LazyColumn(
                horizontalAlignment = Alignment.CenterHorizontally,
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
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.CAMERA
                            )
                        )
                    }, modifier = Modifier.padding(top = 10.dp, bottom = 10.dp)) {
                        Text(text = "Scan Number Plate")
                    }
                    if (plateNumber.value.isNotEmpty()) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            for(index in plateNumber.value.indices) {
                                val pn = plateNumber.value[index]
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 2.dp)
                                        .border(
                                            width = 1.dp,
                                            color = if (pn.active) Color.Black else Color.LightGray,
                                            shape = RoundedCornerShape(5.dp)
                                        )
                                        .padding(5.dp)
                                        .combinedClickable(
                                            onClick = {
                                                if (!pn.active) return@combinedClickable
                                                val pm = plateNumber.value.toMutableList()
                                                val tapped = pn.tapped
                                                val chars = similarChars[pn.actual]
                                                chars
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

                                                val sm = results.value.filter { plateNumber.value.map { it.char }.joinToString("") == it.resultValue }.getOrNull(0) ?: return@combinedClickable
                                                sm.isSelected = true
                                                val rm = results.value.toMutableList()
                                                rm.forEach { it.isSelected = false }
                                                rm[rm.indexOf(sm)] = sm.copy(isSelected = true)
                                                results.value = emptyList()
                                                results.value = rm
                                            },
                                            onLongClick = {
                                                val pm = plateNumber.value.toMutableList()
                                                pm[index] = pn.copy(active = !pn.active)
                                                plateNumber.value = emptyList()
                                                plateNumber.value = pm
                                            },
                                        )
                                ) {
                                    Text(
                                        text = pn.char.toString(),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .padding(4.dp)
                                            .alpha(if (pn.active) 1f else 0.5f)
                                    )
                                }
                            }
                        }
                        Box(
                            modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopEnd
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
                LazyVerticalGrid(columns = GridCells.Fixed(2), state = gridState) {
                    val singleOccurrences = results.value.filter { !it.multipleOccurrences }
                    items(singleOccurrences.size) { index ->
                        val res = singleOccurrences[index]
                        PlateSimilarItem(res) { out ->
                            val rm = results.value.toMutableList()
                            rm.forEach { it.isSelected = false }
                            res.isSelected = true
                            rm[index] = res
                            results.value = emptyList()
                            results.value = rm

                            plateNumber.value = out.map {
                                PlateNumb(
                                    actual = it, char = it, tapped = 0
                                )
                            }
                        }
                    }
                    if (viewModel.allowMultipleOccurrences.value) {
                        val multipleOccurrences = results.value.filter { it.multipleOccurrences }
                            .sortedBy { it.resultValue }
                        items(multipleOccurrences.size) { index ->
                            val res = multipleOccurrences[index]
                            PlateSimilarItem(res) { out ->
                                val rm = results.value.toMutableList()
                                rm.forEach { it.isSelected = false }
                                res.isSelected = true
                                rm[index] = res
                                results.value = emptyList()
                                results.value = rm

                                plateNumber.value = out.map {
                                    PlateNumb(
                                        actual = it, char = it, tapped = 0
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }


    }
}

@Composable
private fun PlateSimilarItem(
    res: Result, onClick: (String) -> Unit
) {
    ListItem(headlineContent = {
        Text(
            text = res.resultValue,
            modifier = Modifier.alpha(if (res.isSelected) 1f else 0.5f),
            color = if (res.multipleOccurrences) Color.Red else Color.Black,
            fontSize = 16.sp
        )
    }, trailingContent = {
        if (res.isSelected) {
            Icon(
                Icons.Filled.Check, contentDescription = null, tint = Color.Green
            )
        }
    }, modifier = Modifier.clickable {
        onClick.invoke(res.resultValue)
    })
}

data class Result(
    var resultValue: String, var isSelected: Boolean, var multipleOccurrences: Boolean
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
fun LazyGridState.isScrollingUp(): Boolean {
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

data class PlateNumb(var actual: Char, var char: Char, var tapped: Int, var active: Boolean = true)


@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    RecognizeImageTheme {

    }
}