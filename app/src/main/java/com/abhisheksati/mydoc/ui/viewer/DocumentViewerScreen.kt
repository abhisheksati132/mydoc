package com.abhisheksati.mydoc.ui.viewer

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.abhisheksati.mydoc.MyDocApp
import com.abhisheksati.mydoc.converter.OfficeToPdfConverter
import com.abhisheksati.mydoc.designsystem.tokens.DocumentFormatColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppColors
import com.abhisheksati.mydoc.designsystem.tokens.LocalAppTypography
import com.abhisheksati.mydoc.domain.model.DocumentFile
import com.abhisheksati.mydoc.tools.model.PdfTool
import com.abhisheksati.mydoc.ui.viewer.pdf.PdfViewerScreen
import com.abhisheksati.mydoc.viewmodel.ViewerViewModel
import java.io.File

/**
 * Universal in-app document viewer.
 *
 * Architecture:
 * - Native PDF files: Rendered directly via PdfViewerScreen.
 * - Office documents (Word, Excel, PowerPoint, Text, CSV):
 *   Converted seamlessly in the background via OfficeToPdfConverter and displayed
 *   directly inside the native in-app PDF viewer.
 * - 100% in-app: No external companion APKs, no app switching, zero friction.
 */
@Composable
fun DocumentViewerScreen(
    document: DocumentFile,
    onBackClick: () -> Unit,
    onShareClick: () -> Unit,
    onToolsClick: (() -> Unit)? = null,
    onSelectTool: ((PdfTool) -> Unit)? = null,
    onDocumentUpdated: ((DocumentFile) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as MyDocApp
    val ext = document.extension.lowercase()

    // 1. Direct PDF files: Render directly with native in-app PDF viewer
    if (ext == "pdf") {
        val viewerViewModel: ViewerViewModel = viewModel(
            key = document.uri,
            factory = ViewerViewModel.Factory(context, app.documentRepository, document)
        )
        PdfViewerScreen(
            document = document,
            viewModel = viewerViewModel,
            onBackClick = onBackClick,
            onShareClick = onShareClick,
            onToolsClick = onToolsClick,
            onSelectTool = onSelectTool,
            onDocumentUpdated = onDocumentUpdated,
            modifier = modifier.fillMaxSize()
        )
        return
    }

    // 2. Office & Non-PDF Documents: Convert in-app to PDF and view natively
    var conversionResult by remember(document.uri) { mutableStateOf<Result<File>?>(null) }
    var isConverting by remember(document.uri) { mutableStateOf(true) }

    LaunchedEffect(document.uri) {
        isConverting = true
        val result = OfficeToPdfConverter.convertToPdf(
            context = context,
            sourceUri = Uri.parse(document.uri),
            fileName = document.displayName
        )
        conversionResult = result
        isConverting = false
    }

    if (isConverting) {
        OfficeConvertingScreen(
            document = document,
            onBackClick = onBackClick,
            modifier = modifier
        )
        return
    }

    val convertedFile = conversionResult?.getOrNull()
    if (convertedFile != null && convertedFile.exists()) {
        val convertedDoc = remember(convertedFile.absolutePath) {
            DocumentFile(
                uri = Uri.fromFile(convertedFile).toString(),
                displayName = document.displayName,
                sizeBytes = convertedFile.length(),
                mimeType = "application/pdf",
                lastOpenedTimestamp = System.currentTimeMillis(),
                pageCount = 0
            )
        }
        val viewerViewModel: ViewerViewModel = viewModel(
            key = convertedDoc.uri,
            factory = ViewerViewModel.Factory(context, app.documentRepository, convertedDoc)
        )
        PdfViewerScreen(
            document = convertedDoc,
            viewModel = viewerViewModel,
            onBackClick = onBackClick,
            onShareClick = onShareClick,
            onToolsClick = onToolsClick,
            onSelectTool = onSelectTool,
            onDocumentUpdated = onDocumentUpdated,
            modifier = modifier.fillMaxSize()
        )
        return
    }

    // 3. Fallback if document cannot be converted
    OfficeConversionErrorScreen(
        document = document,
        error = conversionResult?.exceptionOrNull(),
        onBackClick = onBackClick,
        onShareClick = onShareClick,
        onOpenWithAnotherApp = {
            try {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(Uri.parse(document.uri), document.mimeType.ifEmpty { "*/*" })
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Open with"))
            } catch (_: Exception) {
                Toast.makeText(context, "No external app installed for this file type", Toast.LENGTH_SHORT).show()
            }
        },
        modifier = modifier
    )
}

/**
 * Modern Material 3 loading screen while converting Office documents into PDF.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OfficeConvertingScreen(
    document: DocumentFile,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current
    val ext = document.extension.lowercase()

    val accentColor = when (ext) {
        "doc", "docx", "odt", "rtf" -> DocumentFormatColors.wordTint
        "xls", "xlsx", "csv", "ods" -> DocumentFormatColors.excelTint
        "ppt", "pptx", "odp" -> DocumentFormatColors.pptTint
        else -> colors.accent
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colors.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = document.displayName,
                        style = typography.title.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back",
                            tint = colors.textPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = colors.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = accentColor.copy(alpha = 0.12f),
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Description,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            CircularProgressIndicator(
                color = accentColor,
                strokeWidth = 3.dp,
                modifier = Modifier.size(36.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Preparing document",
                style = typography.title.copy(fontWeight = FontWeight.SemiBold, fontSize = 17.sp),
                color = colors.textPrimary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Rendering ${ext.uppercase()} directly inside mydoc…",
                style = typography.body.copy(fontSize = 13.5.sp),
                color = colors.textSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Fallback screen if an unsupported or corrupted file cannot be parsed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OfficeConversionErrorScreen(
    document: DocumentFile,
    error: Throwable?,
    onBackClick: () -> Unit,
    onShareClick: () -> Unit,
    onOpenWithAnotherApp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val typography = LocalAppTypography.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colors.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = document.displayName,
                        style = typography.title.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back",
                            tint = colors.textPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onShareClick) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = "Share",
                            tint = colors.textPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = colors.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFFEF4444).copy(alpha = 0.12f),
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.ErrorOutline,
                        contentDescription = null,
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Could not preview document",
                style = typography.title.copy(fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
                color = colors.textPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = error?.localizedMessage ?: "This document format is encrypted or unsupported.",
                style = typography.caption.copy(fontSize = 13.sp),
                color = colors.textSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onOpenWithAnotherApp,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accent,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Open with App", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                OutlinedButton(
                    onClick = onShareClick,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(0.8f)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share", fontSize = 13.sp)
                }
            }
        }
    }
}
