package com.pru.recognizeimage.ui

import kotlinx.serialization.Serializable


@Serializable
sealed interface ScreenRoutes {
    @Serializable
    data object ScanScreen : ScreenRoutes

    @Serializable
    data class CameraScreen(var crop: Boolean) : ScreenRoutes
}