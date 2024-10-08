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
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.pru.recognizeimage.theme.RecognizeImageTheme
import com.pru.recognizeimage.ui.CameraScreen
import com.pru.recognizeimage.ui.CameraViewModel
import com.pru.recognizeimage.ui.InfoScreen
import com.pru.recognizeimage.ui.ScanScreen
import com.pru.recognizeimage.utils.ScreenRoutes

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
                        ScanScreen(
                            viewModel = viewModel,
                            scanListener = {
                                navController.navigate(ScreenRoutes.CameraScreen)
                            }
                        )
                    }
                    composable<ScreenRoutes.CameraScreen> {
                        CameraScreen(viewModel) {
                            viewModel.readImageFromUri()
                            navController.navigateUp()
                        }
                    }
                    composable<ScreenRoutes.InfoScreen> {
                        InfoScreen {
                            navController.navigateUp()
                        }
                    }
                }
            }
        }
    }
}