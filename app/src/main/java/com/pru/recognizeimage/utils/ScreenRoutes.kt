package com.pru.recognizeimage.utils

import kotlinx.serialization.Serializable


@Serializable
sealed interface ScreenRoutes {
    @Serializable
    data object ScanScreen : ScreenRoutes

    @Serializable
    data object CameraScreen : ScreenRoutes

    @Serializable
    data object InfoScreen : ScreenRoutes
}