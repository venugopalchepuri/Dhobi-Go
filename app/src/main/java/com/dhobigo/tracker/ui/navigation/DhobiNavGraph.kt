package com.dhobigo.tracker.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.dhobigo.tracker.DhobiApplication
import com.dhobigo.tracker.data.ocr.ExtractedChitData
import com.dhobigo.tracker.ui.components.DhobiBottomBar
import com.dhobigo.tracker.ui.screens.add.AddLaundryScreen
import com.dhobigo.tracker.ui.screens.add.AddLaundryViewModel
import com.dhobigo.tracker.ui.screens.auth.AuthViewModel
import com.dhobigo.tracker.ui.screens.auth.ForgotPasswordScreen
import com.dhobigo.tracker.ui.screens.auth.LoginScreen
import com.dhobigo.tracker.ui.screens.auth.RegisterScreen
import com.dhobigo.tracker.ui.screens.auth.WelcomeScreen
import com.dhobigo.tracker.ui.screens.details.RecordDetailsScreen
import com.dhobigo.tracker.ui.screens.details.RecordDetailsViewModel
import com.dhobigo.tracker.ui.screens.history.HistoryScreen
import com.dhobigo.tracker.ui.screens.history.HistoryViewModel
import com.dhobigo.tracker.ui.screens.home.HomeScreen
import com.dhobigo.tracker.ui.screens.home.HomeViewModel
import com.dhobigo.tracker.ui.screens.ocr.OcrConfirmationScreen
import com.dhobigo.tracker.ui.screens.settings.SettingsScreen
import com.dhobigo.tracker.ui.screens.settings.SettingsViewModel
import java.io.File

@Composable
fun DhobiNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Show bottom bar only on primary top-level tabs
    val showBottomBar = currentRoute in listOf(
        Screen.Home.route,
        Screen.History.route,
        Screen.Settings.route
    )

    val app = DhobiApplication.instance
    val isLoggedIn = app.authRepository.isLoggedIn
    val startDestination = if (isLoggedIn) Screen.Home.route else Screen.Welcome.route

    // Temporary container for passing OCR data to confirmation screen
    var pendingOcrData by remember { mutableStateOf<ExtractedChitData?>(null) }
    var pendingOcrFile by remember { mutableStateOf<File?>(null) }
    var addLaundryViewModelRef by remember { mutableStateOf<AddLaundryViewModel?>(null) }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (showBottomBar) {
                DhobiBottomBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(Screen.Home.route) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            // Screen 0: Welcome & Onboarding
            composable(Screen.Welcome.route) {
                WelcomeScreen(
                    onNavigateToLogin = {
                        navController.navigate(Screen.Login.route)
                    },
                    onNavigateToRegister = {
                        navController.navigate(Screen.Register.route)
                    }
                )
            }

            // Screen 0.1: Login
            composable(Screen.Login.route) {
                val authViewModel: AuthViewModel = viewModel(
                    factory = AuthViewModel.Factory(app.authRepository)
                )
                LoginScreen(
                    viewModel = authViewModel,
                    onNavigateToRegister = {
                        navController.navigate(Screen.Register.route)
                    },
                    onNavigateToForgotPassword = {
                        navController.navigate(Screen.ForgotPassword.route)
                    },
                    onLoginSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Screen 0.2: Register
            composable(Screen.Register.route) {
                val authViewModel: AuthViewModel = viewModel(
                    factory = AuthViewModel.Factory(app.authRepository)
                )
                RegisterScreen(
                    viewModel = authViewModel,
                    onNavigateToLogin = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Welcome.route)
                        }
                    },
                    onRegisterSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Screen 0.3: Forgot Password
            composable(Screen.ForgotPassword.route) {
                val authViewModel: AuthViewModel = viewModel(
                    factory = AuthViewModel.Factory(app.authRepository)
                )
                ForgotPasswordScreen(
                    viewModel = authViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Screen A: Home Dashboard
            composable(Screen.Home.route) {
                val homeViewModel: HomeViewModel = viewModel(
                    factory = HomeViewModel.Factory(app.repository, app.authRepository)
                )
                HomeScreen(
                    viewModel = homeViewModel,
                    onNavigateToAddLaundry = {
                        navController.navigate(Screen.AddLaundry.createRoute())
                    },
                    onNavigateToRecordDetails = { recordId ->
                        navController.navigate(Screen.RecordDetails.createRoute(recordId))
                    },
                    onNavigateToHistory = {
                        navController.navigate(Screen.History.route) {
                            launchSingleTop = true
                        }
                    }
                )
            }

            // Screen F: History
            composable(Screen.History.route) {
                val historyViewModel: HistoryViewModel = viewModel(
                    factory = HistoryViewModel.Factory(app.repository)
                )
                HistoryScreen(
                    viewModel = historyViewModel,
                    onNavigateToRecordDetails = { recordId ->
                        navController.navigate(Screen.RecordDetails.createRoute(recordId))
                    }
                )
            }

            // Screen I: Settings & Account
            composable(Screen.Settings.route) {
                val settingsViewModel: SettingsViewModel = viewModel(
                    factory = SettingsViewModel.Factory(app.repository, app.authRepository)
                )
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onSignOut = {
                        navController.navigate(Screen.Welcome.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            // Screen B: Add / Edit Laundry
            composable(
                route = Screen.AddLaundry.route,
                arguments = listOf(
                    navArgument("recordId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val recordId = backStackEntry.arguments?.getString("recordId")
                val addLaundryViewModel: AddLaundryViewModel = viewModel(
                    factory = AddLaundryViewModel.Factory(
                        app.repository,
                        app.imageStorageManager,
                        app.textRecognitionManager,
                        recordId
                    )
                )
                addLaundryViewModelRef = addLaundryViewModel

                AddLaundryScreen(
                    viewModel = addLaundryViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToOcrConfirm = { data, file ->
                        pendingOcrData = data
                        pendingOcrFile = file
                        navController.navigate(Screen.OcrConfirm.route)
                    }
                )
            }

            // Screen D: OCR Confirmation Screen
            composable(Screen.OcrConfirm.route) {
                val ocrData = pendingOcrData
                val ocrFile = pendingOcrFile

                if (ocrData != null && ocrFile != null) {
                    OcrConfirmationScreen(
                        extractedData = ocrData,
                        imageFile = ocrFile,
                        onApplyResults = { slipNumber, dateMillis, confirmedQuantities ->
                            addLaundryViewModelRef?.applyOcrResults(
                                slipNumber = slipNumber,
                                dateMillis = dateMillis,
                                confirmedQuantities = confirmedQuantities
                            )
                            navController.popBackStack()
                        },
                        onCancel = {
                            navController.popBackStack()
                        }
                    )
                } else {
                    navController.popBackStack()
                }
            }

            // Screen E: Record Details
            composable(
                route = Screen.RecordDetails.route,
                arguments = listOf(
                    navArgument("recordId") {
                        type = NavType.StringType
                    }
                )
            ) { backStackEntry ->
                val recordId = backStackEntry.arguments?.getString("recordId") ?: ""
                val detailsViewModel: RecordDetailsViewModel = viewModel(
                    factory = RecordDetailsViewModel.Factory(app.repository, recordId)
                )

                RecordDetailsScreen(
                    viewModel = detailsViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToEdit = { id ->
                        navController.navigate(Screen.AddLaundry.createRoute(id))
                    }
                )
            }
        }
    }
}
