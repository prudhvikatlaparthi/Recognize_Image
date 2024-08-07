package com.pru.recognizeimage.utils

import kotlinx.serialization.Serializable


@Serializable
sealed interface ScreenRoutes {
    @Serializable
    data object ScanScreen : ScreenRoutes

    @Serializable
    data class CameraScreen(var crop: Boolean) : ScreenRoutes

    @Serializable
    data object InfoScreen : ScreenRoutes
}