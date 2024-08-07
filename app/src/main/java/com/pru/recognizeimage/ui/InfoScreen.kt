@file:OptIn(ExperimentalMaterial3Api::class)

package com.pru.recognizeimage.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pru.recognizeimage.utils.Global

@Composable
fun InfoScreen(backListener: () -> Unit) {
    Scaffold(containerColor = Color.White, topBar = {
        TopAppBar(title = {
            Text(text = "Occurrences", modifier = Modifier.padding(start = 10.dp))
        }, navigationIcon = {
            IconButton(
                onClick = { backListener.invoke() },
                modifier = Modifier.padding(start = 10.dp)
            ) {
                Icon(imageVector = Icons.Default.ArrowBackIosNew, contentDescription = null)
            }
        })
    }) { paddingValue ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValue)
                .fillMaxSize()
                .padding(10.dp)
        ) {
            for (item in Global.similarChars) {
                item {
                    ListItem(headlineContent = {
                        Text(text = item.key.toString(), fontWeight = FontWeight.SemiBold, modifier = Modifier.fillMaxWidth())
                    }, supportingContent = {
                        Text(text = item.value.joinToString(", "), modifier = Modifier.fillMaxWidth())
                    })
                }
            }
        }
    }
}