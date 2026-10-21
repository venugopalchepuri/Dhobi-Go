package com.dhobigo.tracker.ui.navigation

sealed class Screen(val route: String) {
    data object Welcome : Screen("welcome")
    data object Login : Screen("login")
    data object Register : Screen("register")
    data object ForgotPassword : Screen("forgot_password")

    data object Home : Screen("home")
    data object History : Screen("history")
    data object Settings : Screen("settings")

    data object AddLaundry : Screen("add_laundry?recordId={recordId}") {
        fun createRoute(recordId: String? = null): String {
            return if (recordId.isNullOrBlank()) "add_laundry" else "add_laundry?recordId=$recordId"
        }
    }

    data object RecordDetails : Screen("record_details/{recordId}") {
        fun createRoute(recordId: String): String = "record_details/$recordId"
    }

    data object OcrConfirm : Screen("ocr_confirm")
}
