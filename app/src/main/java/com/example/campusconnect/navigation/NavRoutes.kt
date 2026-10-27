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

    @Serializable
    object Login : NavRoutes()

    @Serializable
    object Register: NavRoutes()

    /** Checks users/{uid} after authentication and routes to Dashboard or Profile Setup. */
    @Serializable
    object ProfileGate : NavRoutes()

    @Serializable
    object ProfileSetup : NavRoutes()

    /** Shown to teachers, staff, HODs and admins until they are approved. */
    @Serializable
    object AwaitingApproval : NavRoutes()

    /** Pending sign-ups for college admins and the super admin to review. */
    @Serializable
    object Approvals : NavRoutes()

    /** Super admin only: add and remove colleges and departments. */
    @Serializable
    object ManageColleges : NavRoutes()
}