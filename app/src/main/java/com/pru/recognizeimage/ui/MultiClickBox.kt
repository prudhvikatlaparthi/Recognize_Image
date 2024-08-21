package com.pru.recognizeimage.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay

@Composable
private fun MultiClickBox(
    modifier: Modifier = Modifier,
    singleClick: () -> Unit,
    doubleClick: () -> Unit,
    longClick: () -> Unit,
    content: @Composable () -> Unit
) {
    var lastClickTimestamp by remember { mutableStateOf(0L) }
    var clickCount by remember { mutableStateOf(0) }
    val delayMillis = 300L

    LaunchedEffect(key1 = clickCount) {
        if (clickCount == 1) {
            delay(delayMillis)
            if (clickCount == 1) {
                println("Single Click!")
            }
            clickCount = 0
        } else if (clickCount == 2) {
            println("Double Click!")
            clickCount = 0
        }
    }

    Box(
        modifier = modifier
            .clickable {
                val currentTimestamp = System.currentTimeMillis()
                clickCount = if (currentTimestamp - lastClickTimestamp < delayMillis) {
                    2
                } else {
                    1
                }
                lastClickTimestamp = currentTimestamp
            }
    ) {
        content()
    }
}