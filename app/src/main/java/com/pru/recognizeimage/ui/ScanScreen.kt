package com.pru.recognizeimage.ui

import android.Manifest
import android.content.Intent
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.sharp.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.pru.recognizeimage.utils.Global.similarChars
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@ExperimentalFoundationApi
@Composable
fun ScanScreen(viewModel: CameraViewModel, scanListener: (Boolean) -> Unit, infoListener : () -> Unit) {

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



    val scope = rememberCoroutineScope()
    val gridState = rememberLazyGridState()

    Scaffold(containerColor = Color.White, floatingActionButton = {
        GoTop(gridState)
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
                    if (viewModel.bitmap.value != null) {
                        Surface(
                            Modifier
                                .fillParentMaxWidth()
                                .padding(top = 5.dp, start = 5.dp, end = 5.dp)
                        ) {
                            Image(
                                viewModel.bitmap.value!!.asImageBitmap(),
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
                            /*Checkbox(
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
                            )*/
                            Checkbox(checked = viewModel.imageProcess.value, onCheckedChange = {
                                viewModel.imageProcess.value = it
                            }, modifier = Modifier)
                            Text(text = "Image Process".uppercase(), modifier = Modifier, fontSize = 12.sp)
                        }
                    }
                    /*Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = viewModel.imageProcess.value, onCheckedChange = {
                            viewModel.imageProcess.value = it
                        }, modifier = Modifier)
                        Text(text = "Image Process".uppercase(), modifier = Modifier, fontSize = 12.sp)
                    }*/
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
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
                        IconButton(onClick = { infoListener.invoke() }) {
                            Icon(imageVector = Icons.Sharp.Info, contentDescription = null, tint = Color.Gray)
                        }
                    }
                    if (viewModel.plateNumber.value.isNotEmpty()) {
                        FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            for(index in viewModel.plateNumber.value.indices) {
                                val pn = viewModel.plateNumber.value[index]
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 2.dp, vertical = 4.dp)
                                        .border(
                                            width = 1.dp,
                                            color = if (pn.active) Color.Black else Color.LightGray,
                                            shape = RoundedCornerShape(5.dp)
                                        )
                                        .padding(5.dp)
                                        .combinedClickable(
                                            onClick = {
                                                scope.launch {
                                                    if (!pn.active) return@launch
                                                    viewModel.showLoader.value = true
                                                    val pm = viewModel.plateNumber.value.toMutableList()
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
                                                    viewModel.plateNumber.value = emptyList()
                                                    viewModel.plateNumber.value = pm

                                                    /*val rm = viewModel.results.value.toMutableList()
                                                    rm.forEach { it.isSelected = false }
                                                    val sm = viewModel.results.value
                                                        .filter {
                                                            viewModel.plateNumber.value
                                                                .map { it.char }
                                                                .joinToString("") == it.resultValue
                                                        }
                                                        .getOrNull(0) ?: run {
                                                        viewModel.results.value = emptyList()
                                                        viewModel.results.value = rm
                                                        viewModel.showLoader.value = false
                                                        return@launch
                                                    }
                                                    sm.isSelected = true

                                                    rm[rm.indexOf(sm)] = sm.copy(isSelected = true)
                                                    viewModel.results.value = emptyList()
                                                    viewModel.results.value = rm*/
                                                    viewModel.showLoader.value = false
                                                }
                                            },
                                            onLongClick = {
                                                val pm = viewModel.plateNumber.value.toMutableList()
                                                pm[index] = pn.copy(active = !pn.active)
                                                viewModel.plateNumber.value = emptyList()
                                                viewModel.plateNumber.value = pm
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
                        /*Box(
                            modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopEnd
                        ) {
                            TextButton(
                                onClick = {
                                    viewModel.showMore.value = !viewModel.showMore.value
                                }, modifier = Modifier.padding(10.dp)
                            ) {
                                Text(
                                    text = if (viewModel.showMore.value) "Show More" else "Show Less",
                                )
                            }
                        }*/
                    }
                    if (viewModel.showLoader.value) {
                        CircularProgressIndicator()
                    }
                }
            }
            /*if (!viewModel.showMore.value) {
                LazyVerticalGrid(columns = GridCells.Fixed(2), state = gridState) {
                    val singleOccurrences = viewModel.results.value.filter { !it.multipleOccurrences }
                    items(singleOccurrences.size) { index ->
                        val res = singleOccurrences[index]
                        PlateSimilarItem(res) { out ->
                            val rm = viewModel.results.value.toMutableList()
                            rm.forEach { it.isSelected = false }
                            res.isSelected = true
                            rm[index] = res
                            viewModel.results.value = emptyList()
                            viewModel.results.value = rm

                            viewModel.plateNumber.value = out.map {
                                PlateNumb(
                                    actual = it, char = it, tapped = 0
                                )
                            }
                        }
                    }
                    if (viewModel.allowMultipleOccurrences.value) {
                        val multipleOccurrences = viewModel.results.value.filter { it.multipleOccurrences }
                            .sortedBy { it.resultValue }
                        items(multipleOccurrences.size) { index ->
                            val res = multipleOccurrences[index]
                            PlateSimilarItem(res) { out ->
                                val rm = viewModel.results.value.toMutableList()
                                rm.forEach { it.isSelected = false }
                                res.isSelected = true
                                rm[index] = res
                                viewModel.results.value = emptyList()
                                viewModel.results.value = rm

                                viewModel.plateNumber.value = out.map {
                                    PlateNumb(
                                        actual = it, char = it, tapped = 0
                                    )
                                }
                            }
                        }
                    }
                }
            }*/
        }


    }
}

@Composable
private fun GoTop(
    gridState: LazyGridState,
) {
    val scope = rememberCoroutineScope()
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