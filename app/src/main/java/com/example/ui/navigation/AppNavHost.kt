package com.example.ui.navigation

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.preferences.AppPreferences
import com.example.ui.components.BottomNavBar
import com.example.ui.documents.DocumentsScreen
import com.example.ui.documents.DocumentsViewModel
import com.example.ui.home.HomeScreen
import com.example.ui.home.HomeViewModel
import com.example.ui.onboarding.OnboardingScreen
import com.example.ui.scanner.CameraScanScreen
import com.example.ui.scanner.CropFilterScreen
import com.example.ui.settings.PrivacyScreen
import com.example.ui.settings.ProUpgradeScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.tools.CompressPdfScreen
import com.example.ui.tools.ImageToPdfScreen
import com.example.ui.tools.MergePdfScreen
import com.example.ui.tools.OcrScreen
import com.example.ui.tools.PasswordProtectScreen
import com.example.ui.tools.PdfToImageScreen
import com.example.ui.tools.SignPdfScreen
import com.example.ui.tools.SplitPdfScreen
import com.example.ui.tools.ToolsScreen
import com.example.ui.viewer.DocumentViewerScreen

@Composable
fun AppNavHost(preferences: AppPreferences) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Home.route

    val isOnboardingDone = preferences.isOnboardingCompleted.value
    val startDestination = if (isOnboardingDone) Screen.Home.route else Screen.Onboarding.route

    val primaryTabs = listOf(
        Screen.Home.route,
        Screen.Tools.route,
        Screen.Documents.route,
        Screen.Settings.route
    )

    val showBottomBar = currentRoute in primaryTabs

    val homeViewModel: HomeViewModel = viewModel()
    val documentsViewModel: DocumentsViewModel = viewModel()

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (showBottomBar) innerPadding else androidx.compose.foundation.layout.PaddingValues())
        ) {
            NavHost(
                navController = navController,
                startDestination = startDestination
            ) {
                // Onboarding
                composable(Screen.Onboarding.route) {
                    OnboardingScreen(
                        onFinish = {
                            preferences.setOnboardingCompleted(true)
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Onboarding.route) { inclusive = true }
                            }
                        }
                    )
                }

                // Home
                composable(Screen.Home.route) {
                    HomeScreen(
                        viewModel = homeViewModel,
                        onNavigateToScan = { navController.navigate(Screen.CameraScan.route) },
                        onNavigateToImageToPdf = { navController.navigate(Screen.ImageToPdf.route) },
                        onNavigateToCompress = { navController.navigate(Screen.CompressPdf.route) },
                        onNavigateToMerge = { navController.navigate(Screen.MergePdf.route) },
                        onNavigateToSplit = { navController.navigate(Screen.SplitPdf.route) },
                        onNavigateToPdfToImage = { navController.navigate(Screen.PdfToImage.route) },
                        onNavigateToOcr = { navController.navigate(Screen.Ocr.route) },
                        onNavigateToSign = { navController.navigate(Screen.SignPdf.route) },
                        onNavigateToPro = { navController.navigate(Screen.ProUpgrade.route) },
                        onNavigateToDocuments = { navController.navigate(Screen.Documents.route) },
                        onOpenDocument = { doc ->
                            navController.navigate(Screen.DocumentViewer.createRoute(doc.id))
                        }
                    )
                }

                // Tools
                composable(Screen.Tools.route) {
                    ToolsScreen(
                        onNavigateToScan = { navController.navigate(Screen.CameraScan.route) },
                        onNavigateToImageToPdf = { navController.navigate(Screen.ImageToPdf.route) },
                        onNavigateToCompress = { navController.navigate(Screen.CompressPdf.route) },
                        onNavigateToMerge = { navController.navigate(Screen.MergePdf.route) },
                        onNavigateToSplit = { navController.navigate(Screen.SplitPdf.route) },
                        onNavigateToPdfToImage = { navController.navigate(Screen.PdfToImage.route) },
                        onNavigateToOcr = { navController.navigate(Screen.Ocr.route) },
                        onNavigateToSign = { navController.navigate(Screen.SignPdf.route) },
                        onNavigateToPasswordProtect = { navController.navigate(Screen.PasswordProtect.route) },
                        isProUser = preferences.isProUser.value
                    )
                }

                // Documents
                composable(Screen.Documents.route) {
                    DocumentsScreen(
                        viewModel = documentsViewModel,
                        onOpenDocument = { doc ->
                            navController.navigate(Screen.DocumentViewer.createRoute(doc.id))
                        },
                        onNavigateToScan = { navController.navigate(Screen.CameraScan.route) }
                    )
                }

                // Settings
                composable(Screen.Settings.route) {
                    SettingsScreen(
                        preferences = preferences,
                        onNavigateToPro = { navController.navigate(Screen.ProUpgrade.route) },
                        onNavigateToPrivacy = { navController.navigate(Screen.Privacy.route) }
                    )
                }

                // Camera Scanner
                composable(Screen.CameraScan.route) {
                    CameraScanScreen(
                        onNavigateToCropFilter = { navController.navigate(Screen.CropFilter.route) },
                        onClose = { navController.popBackStack() }
                    )
                }

                // Crop & Filter Screen
                composable(Screen.CropFilter.route) {
                    CropFilterScreen(
                        onSaved = { docId ->
                            navController.navigate(Screen.DocumentViewer.createRoute(docId)) {
                                popUpTo(Screen.Home.route)
                            }
                        },
                        onBack = { navController.popBackStack() }
                    )
                }

                // Image to PDF
                composable(Screen.ImageToPdf.route) {
                    ImageToPdfScreen(
                        onSaved = { docId ->
                            navController.navigate(Screen.DocumentViewer.createRoute(docId)) {
                                popUpTo(Screen.Home.route)
                            }
                        },
                        onBack = { navController.popBackStack() }
                    )
                }

                // Compress PDF
                composable(Screen.CompressPdf.route) {
                    CompressPdfScreen(
                        onSaved = { docId ->
                            navController.navigate(Screen.DocumentViewer.createRoute(docId)) {
                                popUpTo(Screen.Home.route)
                            }
                        },
                        onBack = { navController.popBackStack() }
                    )
                }

                // Merge PDF
                composable(Screen.MergePdf.route) {
                    MergePdfScreen(
                        onSaved = { docId ->
                            navController.navigate(Screen.DocumentViewer.createRoute(docId)) {
                                popUpTo(Screen.Home.route)
                            }
                        },
                        onBack = { navController.popBackStack() }
                    )
                }

                // Split PDF
                composable(Screen.SplitPdf.route) {
                    SplitPdfScreen(
                        onSaved = { docId ->
                            navController.navigate(Screen.DocumentViewer.createRoute(docId)) {
                                popUpTo(Screen.Home.route)
                            }
                        },
                        onBack = { navController.popBackStack() }
                    )
                }

                // PDF to Image
                composable(Screen.PdfToImage.route) {
                    PdfToImageScreen(
                        onBack = { navController.popBackStack() }
                    )
                }

                // OCR Screen
                composable(Screen.Ocr.route) {
                    OcrScreen(
                        onSaved = { docId ->
                            navController.navigate(Screen.DocumentViewer.createRoute(docId)) {
                                popUpTo(Screen.Home.route)
                            }
                        },
                        onBack = { navController.popBackStack() }
                    )
                }

                // Sign PDF Screen
                composable(Screen.SignPdf.route) {
                    SignPdfScreen(
                        onSaved = { docId ->
                            navController.navigate(Screen.DocumentViewer.createRoute(docId)) {
                                popUpTo(Screen.Home.route)
                            }
                        },
                        onBack = { navController.popBackStack() }
                    )
                }

                // Password Protect Screen
                composable(Screen.PasswordProtect.route) {
                    PasswordProtectScreen(
                        onSaved = { docId ->
                            navController.navigate(Screen.DocumentViewer.createRoute(docId)) {
                                popUpTo(Screen.Home.route)
                            }
                        },
                        onBack = { navController.popBackStack() }
                    )
                }

                // Document Viewer
                composable(
                    route = Screen.DocumentViewer.route,
                    arguments = listOf(navArgument("docId") { type = NavType.LongType })
                ) { backStackEntry ->
                    val docId = backStackEntry.arguments?.getLong("docId") ?: 0L
                    DocumentViewerScreen(
                        docId = docId,
                        onBack = { navController.popBackStack() },
                        onNavigateToPasswordProtect = { navController.navigate(Screen.PasswordProtect.route) }
                    )
                }

                // Pro Upgrade Screen
                composable(Screen.ProUpgrade.route) {
                    ProUpgradeScreen(
                        preferences = preferences,
                        onBack = { navController.popBackStack() }
                    )
                }

                // Privacy Screen
                composable(Screen.Privacy.route) {
                    PrivacyScreen(
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
