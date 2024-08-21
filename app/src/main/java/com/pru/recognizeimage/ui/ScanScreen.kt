package com.pru.recognizeimage.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Add
import androidx.compose.material.icons.sharp.ArrowBack
import androidx.compose.material.icons.sharp.ArrowLeft
import androidx.compose.material.icons.sharp.ArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.pru.recognizeimage.R
import com.pru.recognizeimage.appContext
import com.pru.recognizeimage.theme.RecognizeImageTheme
import com.pru.recognizeimage.utils.Global
import com.pru.recognizeimage.utils.Global.dashedBorder
import com.pru.recognizeimage.utils.Global.similarChars

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@ExperimentalFoundationApi
@Composable
fun ScanScreen(
    viewModel: CameraViewModel,
    scanListener: (Boolean) -> Unit
) {

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

    Scaffold(containerColor = Color.White,
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(R.string.app_name))
                },
                navigationIcon = {
                    IconButton(onClick = {

                    }) {
                        Icon(Icons.Sharp.ArrowBack, contentDescription = null)
                    }
                },
            )
        }) { paddingValue ->
        Column(
            modifier = Modifier
                .padding(paddingValue)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (viewModel.bitmap.value != null) {
                Surface(
                    Modifier
                        .fillMaxWidth()
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
                    Text(text = "CROP IMAGE", modifier = Modifier, fontSize = 12.sp)
                }
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = viewModel.imageProcess.value, onCheckedChange = {
                        viewModel.imageProcess.value = it
                    }, modifier = Modifier)
                    Text(
                        text = "Image Process".uppercase(),
                        modifier = Modifier,
                        fontSize = 12.sp
                    )
                }
            }
            if (viewModel.plateNumber.value.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp, end = 10.dp, start = 10.dp)
                ) {
                    if (viewModel.showAddChar) {
                        AddButton(
                            modifier = Modifier
                                .padding(end = 8.dp)
                        ) {
                            viewModel.addPosition = Global.Position.Start
                            viewModel.showSelectionDialog.value = true
                        }
                    }
                    FlowRow(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.Center,
                        verticalArrangement = Arrangement.Center
                    ) {
                        for (index in viewModel.plateNumber.value.indices) {
                            val pn = viewModel.plateNumber.value[index]
                            Column(
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                androidx.compose.animation.AnimatedVisibility(visible = pn.enableToChangePosition && index != (viewModel.plateNumber.value.size - 1)) {
                                    IconButton(
                                        modifier = Modifier
                                            .padding(bottom = 6.dp)
                                            .size(24.dp),
                                        onClick = {
                                            viewModel.changeCharPosition(
                                                index,
                                                Global.Position.Start
                                            )
                                        },
                                        colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        Icon(
                                            Icons.Sharp.ArrowRight,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                }
                                CharItem(
                                    modifier = Modifier.padding(bottom = 6.dp),
                                    data = pn.char.toString(),
                                    active = pn.active,
                                    onClick = {
                                        viewModel.changeChar(index)
                                    },
                                    onDoubleClick = {
                                        if (viewModel.showAddChar) {
                                            viewModel.enableToChangePosition(index)
                                        }
                                    },
                                    onLongClick = {
                                        viewModel.disableChar(index)
                                    }
                                )
                                androidx.compose.animation.AnimatedVisibility(visible = pn.enableToChangePosition && index != 0) {
                                    IconButton(
                                        modifier = Modifier
                                            .padding(bottom = 6.dp)
                                            .size(24.dp),
                                        onClick = {
                                            viewModel.changeCharPosition(index, Global.Position.End)
                                        },
                                        colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        Icon(
                                            Icons.Sharp.ArrowLeft,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (viewModel.showAddChar) {
                        AddButton(modifier = Modifier.padding(start = 2.dp, bottom = 8.dp)) {
                            viewModel.addPosition = Global.Position.End
                            viewModel.showSelectionDialog.value = true
                        }
                    }
                }
            }
            if (viewModel.showLoader.value) {
                CircularProgressIndicator()
            }
            Spacer(modifier = Modifier.weight(1f))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
            ) {
                Button(onClick = {
                    multiplePermissionsLauncher.launch(
                        arrayOf(
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.CAMERA
                        )
                    )
                }, modifier = Modifier.weight(1f)) {
                    Text(text = if (viewModel.bitmap.value == null) "SCAN" else "RESCAN")
                }

                if (viewModel.bitmap.value != null) {
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(onClick = {
                        Toast.makeText(
                            appContext,
                            viewModel.plateNumber.value.filter { it.active }.map { it.char }
                                .joinToString(""),
                            Toast.LENGTH_SHORT
                        ).show()
                    }, modifier = Modifier.weight(1f)) {
                        Text(text = "DONE")
                    }
                }
            }

            if (viewModel.showSelectionDialog.value) {
                Dialog(onDismissRequest = {
                    viewModel.showSelectionDialog.value = false
                }) {

                    Card {
                        FlowRow(modifier = Modifier.padding(10.dp)) {
                            for (item in similarChars.keys) {
                                CharItem(
                                    modifier = Modifier.padding(bottom = 6.dp),
                                    data = item.toString(),
                                    active = true,
                                    onClick = {
                                        viewModel.showSelectionDialog.value = false
                                        viewModel.addChar(item)
                                    },
                                    onDoubleClick = null,
                                    onLongClick = null
                                )
                            }
                        }
                    }
                }
            }
        }


    }
}

@ExperimentalFoundationApi
@Composable
private fun CharItem(
    modifier: Modifier = Modifier,
    data: String,
    active: Boolean,
    onClick: (() -> Unit)? = null,
    onDoubleClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .padding(end = 6.dp)
            .border(
                width = 1.dp,
                color = if (active) Color.Black else Color.LightGray,
                shape = RoundedCornerShape(5.dp)
            )
            .padding(5.dp)
            .combinedClickable(
                onClick = {
                    onClick?.invoke()
                },
                onLongClick = {
                    onLongClick?.invoke()
                },
                onDoubleClick = {
                    onDoubleClick?.invoke()
                }
            ), contentAlignment = Alignment.Center
    ) {
        Text(
            text = data,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding(4.dp)
                .size(16.dp)
                .alpha(if (active) 1f else 0.5f),
            textAlign = TextAlign.Center
        )
    }
}

@ExperimentalFoundationApi
@Composable
private fun AddButton(modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .dashedBorder(
                strokeWidth = 1.dp,
                color = Color.Gray,
                cornerRadiusDp = 5.dp
            )
            .padding(5.dp)
            .combinedClickable(
                onClick = {
                    onClick.invoke()
                }
            ), contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Sharp.Add, contentDescription = null, modifier = Modifier
                .padding(4.dp)
                .size(16.dp)
        )
    }
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

data class PlateNumb(
    var actual: Char,
    var char: Char,
    var tapped: Int,
    var active: Boolean = true,
    var enableToChangePosition: Boolean = false
)


@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    RecognizeImageTheme {

    }
}