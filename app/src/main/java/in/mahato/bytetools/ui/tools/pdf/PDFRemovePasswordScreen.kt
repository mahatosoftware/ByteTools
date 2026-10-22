package `in`.mahato.bytetools.ui.tools.pdf

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException
import `in`.mahato.bytetools.ui.navigation.Screen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PDFRemovePasswordScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var pdfUriString by rememberSaveable { mutableStateOf<String?>(null) }
    var fileName by rememberSaveable { mutableStateOf("") }
    var fileSize by rememberSaveable { mutableLongStateOf(0L) }
    var isEncrypted by rememberSaveable { mutableStateOf(false) }
    var isCheckingEncryption by remember { mutableStateOf(false) }
    var pageCount by rememberSaveable { mutableIntStateOf(0) }

    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    var unlockedFilePath by rememberSaveable { mutableStateOf<String?>(null) }
    var savedToHistory by rememberSaveable { mutableStateOf(false) }

    val pdfUri = pdfUriString?.let(Uri::parse)
    val unlockedFile = unlockedFilePath?.let(::File)

    // Save As (Export to device storage / Downloads) launcher
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf"),
        onResult = { destinationUri ->
            if (destinationUri != null && unlockedFile != null) {
                scope.launch(Dispatchers.IO) {
                    try {
                        context.contentResolver.openOutputStream(destinationUri)?.use { out ->
                            unlockedFile.inputStream().use { input ->
                                input.copyTo(out)
                            }
                        }
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Saved successfully", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Error saving: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    )

    // File picker launcher
    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri != null) {
                pdfUriString = uri.toString()
                fileName = queryFileName(context, uri)
                fileSize = queryFileSize(context, uri)
                password = ""
                errorMessage = null
                unlockedFilePath = null
                savedToHistory = false
                isEncrypted = false
                pageCount = 0

                scope.launch {
                    isCheckingEncryption = true
                    checkFileEncryption(
                        context = context,
                        uri = uri,
                        onResult = { encrypted, pages ->
                            isEncrypted = encrypted
                            pageCount = pages
                            isCheckingEncryption = false
                        },
                        onError = { error ->
                            isCheckingEncryption = false
                            errorMessage = error
                        }
                    )
                }
            }
        }
    )

    fun startUnlock() {
        if (pdfUri == null) return
        if (isEncrypted && password.isEmpty()) {
            errorMessage = "Please enter the PDF password"
            return
        }

        focusManager.clearFocus()
        isProcessing = true
        errorMessage = null

        scope.launch {
            unlockPdf(
                context = context,
                uri = pdfUri,
                password = password,
                originalName = fileName,
                onSuccess = { file, pages ->
                    unlockedFilePath = file.absolutePath
                    pageCount = pages
                    isProcessing = false
                    savedToHistory = false
                    Toast.makeText(context, "Password removed successfully!", Toast.LENGTH_SHORT).show()
                },
                onError = { err ->
                    isProcessing = false
                    errorMessage = err
                }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Remove Password", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Step 1: Pick / Selected File Card
                if (pdfUri == null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        onClick = { pickerLauncher.launch(arrayOf("application/pdf")) },
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                        )
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize().padding(16.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.LockOpen,
                                        contentDescription = null,
                                        modifier = Modifier.size(32.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(Modifier.height(12.dp))
                                Text("Select Password-Protected PDF", fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Tap to choose a protected document",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isEncrypted) MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isEncrypted) Icons.Default.Lock else Icons.Default.PictureAsPdf,
                                        contentDescription = null,
                                        tint = if (isEncrypted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = fileName,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = formatFileSize(fileSize),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                TextButton(onClick = { pickerLauncher.launch(arrayOf("application/pdf")) }) {
                                    Text("Change")
                                }
                            }

                            Spacer(Modifier.height(12.dp))

                            // Encryption status badge
                            if (isCheckingEncryption) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Checking document encryption...", style = MaterialTheme.typography.bodySmall)
                                }
                            } else if (isEncrypted) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Lock,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            "Password Protected (Encrypted)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFE8F5E9)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = Color(0xFF2E7D32)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            "Document is Not Encrypted",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF1B5E20),
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Step 2: Password Input Form (when file is encrypted or has user password)
                AnimatedVisibility(
                    visible = pdfUri != null && isEncrypted && unlockedFile == null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                "Enter PDF Password",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Provide the password used to open this document. Protection will be permanently removed from the unlocked copy.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedTextField(
                                value = password,
                                onValueChange = {
                                    password = it
                                    errorMessage = null
                                },
                                label = { Text("Document Password") },
                                placeholder = { Text("Enter password") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                leadingIcon = {
                                    Icon(Icons.Default.Key, contentDescription = null)
                                },
                                trailingIcon = {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(
                                            imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = if (passwordVisible) "Hide password" else "Show password"
                                        )
                                    }
                                },
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(onDone = { startUnlock() }),
                                isError = errorMessage != null,
                                supportingText = {
                                    if (errorMessage != null) {
                                        Text(
                                            text = errorMessage!!,
                                            color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            )

                            Button(
                                onClick = { startUnlock() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                enabled = password.isNotEmpty() && !isProcessing,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.LockOpen, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Remove Password", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                // Notice when PDF is NOT password protected
                AnimatedVisibility(
                    visible = pdfUri != null && !isEncrypted && !isCheckingEncryption && unlockedFile == null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("No Password Required", fontWeight = FontWeight.Bold)
                            Text(
                                "This PDF is not password protected. You can already view and share it without unlocking.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        pdfUri?.let { uri ->
                                            navController.navigate(Screen.PDFViewer.route + "?uri=${Uri.encode(uri.toString())}")
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("View PDF")
                                }
                                OutlinedButton(
                                    onClick = { pickerLauncher.launch(arrayOf("application/pdf")) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Choose Another")
                                }
                            }
                        }
                    }
                }

                // Step 3: Success Card (Unlocked PDF Ready)
                AnimatedVisibility(
                    visible = unlockedFile != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    unlockedFile?.let { file ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                    Spacer(Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "Password Removed!",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            "Your document is now fully unlocked.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            file.name,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            "${formatFileSize(file.length())} • $pageCount pages",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                // Action Buttons Row
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            val uriString = Uri.fromFile(file).toString()
                                            navController.navigate(Screen.PDFViewer.route + "?uri=${Uri.encode(uriString)}")
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("View")
                                    }

                                    OutlinedButton(
                                        onClick = { sharePdf(context, file) },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Share")
                                    }

                                    Button(
                                        onClick = {
                                            val result = savePdfToHistory(context, file)
                                            if (result != null) {
                                                savedToHistory = true
                                                Toast.makeText(context, "Saved to PDF History", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "Could not save PDF", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        enabled = !savedToHistory
                                    ) {
                                        Text(if (savedToHistory) "Saved" else "Save")
                                    }
                                }

                                // Export to Downloads / Save As button
                                OutlinedButton(
                                    onClick = { exportLauncher.launch(file.name) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Export / Save to Device")
                                }

                                // Unlock another
                                TextButton(
                                    onClick = {
                                        pdfUriString = null
                                        fileName = ""
                                        fileSize = 0L
                                        password = ""
                                        unlockedFilePath = null
                                        errorMessage = null
                                        savedToHistory = false
                                        isEncrypted = false
                                    },
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                ) {
                                    Text("Unlock Another PDF")
                                }
                            }
                        }
                    }
                }
            }

            // Loading overlay
            if (isProcessing) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black.copy(alpha = 0.45f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                CircularProgressIndicator()
                                Text(
                                    "Removing password & decrypting...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private suspend fun checkFileEncryption(
    context: Context,
    uri: Uri,
    onResult: (Boolean, Int) -> Unit,
    onError: (String) -> Unit
) = withContext(Dispatchers.IO) {
    try {
        val stream = context.contentResolver.openInputStream(uri)
            ?: return@withContext withContext(Dispatchers.Main) { onError("Cannot open PDF file") }

        try {
            val doc = PDDocument.load(stream)
            val encrypted = doc.isEncrypted
            val pages = doc.numberOfPages
            doc.close()
            withContext(Dispatchers.Main) { onResult(encrypted, pages) }
        } catch (e: InvalidPasswordException) {
            withContext(Dispatchers.Main) { onResult(true, 0) }
        } catch (e: Exception) {
            if (e.message?.contains("password", ignoreCase = true) == true ||
                e.message?.contains("encrypted", ignoreCase = true) == true
            ) {
                withContext(Dispatchers.Main) { onResult(true, 0) }
            } else {
                withContext(Dispatchers.Main) { onError(e.localizedMessage ?: "Failed to read PDF") }
            }
        } finally {
            stream.close()
        }
    } catch (e: Exception) {
        withContext(Dispatchers.Main) { onError(e.localizedMessage ?: "Cannot access file") }
    }
}

private suspend fun unlockPdf(
    context: Context,
    uri: Uri,
    password: String,
    originalName: String,
    onSuccess: (File, Int) -> Unit,
    onError: (String) -> Unit
) = withContext(Dispatchers.IO) {
    try {
        val stream = context.contentResolver.openInputStream(uri)
            ?: return@withContext withContext(Dispatchers.Main) { onError("Cannot open file input stream") }

        val doc = try {
            PDDocument.load(stream, password)
        } catch (e: InvalidPasswordException) {
            stream.close()
            return@withContext withContext(Dispatchers.Main) {
                onError("Incorrect password. Please verify and try again.")
            }
        } catch (e: Exception) {
            stream.close()
            val msg = e.message.orEmpty()
            return@withContext withContext(Dispatchers.Main) {
                if (msg.contains("password", ignoreCase = true) || msg.contains("decrypt", ignoreCase = true)) {
                    onError("Incorrect password. Please verify and try again.")
                } else {
                    onError("Decryption failed: ${e.localizedMessage}")
                }
            }
        }

        // Remove all encryption/security
        doc.setAllSecurityToBeRemoved(true)
        val pages = doc.numberOfPages

        val baseName = if (originalName.endsWith(".pdf", ignoreCase = true)) {
            originalName.dropLast(4)
        } else {
            originalName.ifEmpty { "document" }
        }
        val outFileName = "Unlocked_${baseName}.pdf"
        val outFile = File(context.cacheDir, outFileName)

        FileOutputStream(outFile).use { fos ->
            doc.save(fos)
        }
        doc.close()
        stream.close()

        withContext(Dispatchers.Main) {
            onSuccess(outFile, pages)
        }
    } catch (e: Exception) {
        withContext(Dispatchers.Main) {
            onError(e.localizedMessage ?: "Failed to remove password")
        }
    }
}

private fun queryFileName(context: Context, uri: Uri): String {
    var name: String? = null
    if (uri.scheme == "content") {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1 && cursor.moveToFirst()) {
                name = cursor.getString(nameIndex)
            }
        }
    }
    return name ?: uri.lastPathSegment ?: "document.pdf"
}

private fun queryFileSize(context: Context, uri: Uri): Long {
    return try {
        context.contentResolver.openFileDescriptor(uri, "r")?.use {
            it.statSize
        } ?: 0L
    } catch (e: Exception) {
        0L
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, units.size - 1)
    return String.format(Locale.US, "%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}

private fun sharePdf(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share Unlocked PDF"))
}

private fun savePdfToHistory(context: Context, sourceFile: File): File? {
    return runCatching {
        val outDir = File(context.getExternalFilesDir(null), "ByteToolsPDF").apply { mkdirs() }
        val outFile = File(outDir, sourceFile.name)
        sourceFile.copyTo(outFile, overwrite = true)
        outFile
    }.getOrNull()
}
