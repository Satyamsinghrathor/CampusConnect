package com.example.campusconnect.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed class NavRoutes {

    @Serializable
    object Home : NavRoutes()

    @Serializable
    object Profile : NavRoutes()

    @Serializable
    object Event : NavRoutes()

    @Serializable
    object Notice : NavRoutes()

    @Serializable
    object Dashboard : NavRoutes()
}