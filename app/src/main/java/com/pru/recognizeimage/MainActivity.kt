@file:OptIn(ExperimentalFoundationApi::class)

package com.pru.recognizeimage

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.pru.recognizeimage.utils.ScreenRoutes
import com.pru.recognizeimage.theme.RecognizeImageTheme
import com.pru.recognizeimage.ui.CameraScreen
import com.pru.recognizeimage.ui.CameraViewModel
import com.pru.recognizeimage.ui.ScanScreen

class MainActivity : ComponentActivity() {

    private val viewModel by viewModels<CameraViewModel>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val systemUiController = rememberSystemUiController()
            systemUiController.setSystemBarsColor(color = Color.White)
            systemUiController.setNavigationBarColor(color = Color.Black)
            RecognizeImageTheme {
                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = ScreenRoutes.ScanScreen
                ) {
                    composable<ScreenRoutes.ScanScreen> {
                        ScanScreen(viewModel) {
                            navController.navigate(ScreenRoutes.CameraScreen(crop = it))
                        }
                    }
                    composable<ScreenRoutes.CameraScreen> {
                        val data = it.toRoute<ScreenRoutes.CameraScreen>().crop
                        CameraScreen(data, viewModel) {
                            navController.popBackStack()
                        }
                    }
                }
            }
        }
    }
}