package com.example.ui.navigation

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Home : Screen("home")
    object Tools : Screen("tools")
    object Documents : Screen("documents")
    object Settings : Screen("settings")

    object CameraScan : Screen("camera_scan")
    object CropFilter : Screen("crop_filter")
    object ImageToPdf : Screen("image_to_pdf")
    object CompressPdf : Screen("compress_pdf")
    object MergePdf : Screen("merge_pdf")
    object SplitPdf : Screen("split_pdf")
    object PdfToImage : Screen("pdf_to_image")
    object Ocr : Screen("ocr")
    object SignPdf : Screen("sign_pdf")
    object PasswordProtect : Screen("password_protect")
    object DocumentViewer : Screen("document_viewer/{docId}") {
        fun createRoute(docId: Long) = "document_viewer/$docId"
    }
    object ProUpgrade : Screen("pro_upgrade")
    object Privacy : Screen("privacy")
}
