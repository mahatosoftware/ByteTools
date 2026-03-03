package `in`.mahato.bytetools.ui.tools.gps.camera

import android.Manifest
import android.content.Context
import android.content.ContentValues
import android.graphics.*
import android.location.Geocoder
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import `in`.mahato.bytetools.ui.navigation.Screen
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.location.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun GPSCameraScreen(navController: NavController) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val permissionsState = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var flashMode by remember { mutableIntStateOf(ImageCapture.FLASH_MODE_OFF) }
    
    var latitude by remember { mutableDoubleStateOf(0.0) }
    var longitude by remember { mutableDoubleStateOf(0.0) }
    var address by remember { mutableStateOf("Fetching address...") }
    var isCapturing by remember { mutableStateOf(false) }

    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // Dynamically update flash mode when it changes
    LaunchedEffect(flashMode) {
        Log.d("GPSCamera", "Updating flash mode to: $flashMode")
        imageCapture?.flashMode = flashMode
    }

    // Location Updates
    LaunchedEffect(permissionsState.allPermissionsGranted) {
        if (permissionsState.allPermissionsGranted) {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setMinUpdateIntervalMillis(2000)
                .build()

            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    result.lastLocation?.let { location ->
                        latitude = location.latitude
                        longitude = location.longitude
                        scope.launch {
                            address = getAddress(context, location.latitude, location.longitude)
                        }
                    }
                }
            }

            try {
                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    context.mainLooper
                )
            } catch (e: SecurityException) {
                Log.e("GPSCamera", "Location permission error", e)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GPS Camera", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        navController.navigate(Screen.ImageGallery.route)
                    }) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = "Gallery")
                    }
                    IconButton(onClick = {
                        flashMode = when (flashMode) {
                            ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_ON
                            ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_AUTO
                            else -> ImageCapture.FLASH_MODE_OFF
                        }
                    }) {
                        Icon(
                            imageVector = when (flashMode) {
                                ImageCapture.FLASH_MODE_ON -> Icons.Default.FlashOn
                                ImageCapture.FLASH_MODE_AUTO -> Icons.Default.FlashAuto
                                else -> Icons.Default.FlashOff
                            },
                            contentDescription = "Flash Mode"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (permissionsState.allPermissionsGranted) {
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).apply {
                            previewView = this
                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                            cameraProviderFuture.addListener({
                                val cameraProvider = cameraProviderFuture.get()
                                val preview = Preview.Builder().build().also {
                                    it.setSurfaceProvider(surfaceProvider)
                                }
                                imageCapture = ImageCapture.Builder()
                                    .setFlashMode(flashMode)
                                    .build()

                                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                                try {
                                    cameraProvider.unbindAll()
                                    cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        cameraSelector,
                                        preview,
                                        imageCapture
                                    )
                                } catch (e: Exception) {
                                    Log.e("GPSCamera", "Use case binding failed", e)
                                }
                            }, ContextCompat.getMainExecutor(ctx))
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Overlay UI
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Lat: $latitude, Lng: $longitude",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = address,
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                // Capture Button
                FloatingActionButton(
                    onClick = {
                        val currentImageCapture = imageCapture
                        if (!isCapturing && currentImageCapture != null) {
                            isCapturing = true
                            captureImage(
                                context,
                                currentImageCapture,
                                cameraExecutor,
                                latitude,
                                longitude,
                                address
                            ) { result ->
                                isCapturing = false
                                scope.launch {
                                    if (result != null) {
                                        val snackbarResult = snackbarHostState.showSnackbar(
                                            message = "Photo saved with GPS data",
                                            actionLabel = "View",
                                            duration = SnackbarDuration.Long
                                        )
                                        if (snackbarResult == SnackbarResult.ActionPerformed) {
                                            navController.navigate(Screen.ImageGallery.route)
                                        }
                                    } else {
                                        snackbarHostState.showSnackbar("Failed to capture image")
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 80.dp),
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    if (isCapturing) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                    } else {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Capture")
                    }
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Permissions required")
                    Button(onClick = { permissionsState.launchMultiplePermissionRequest() }) {
                        Text("Grant Permissions")
                    }
                }
            }
        }
    }
}

private fun captureImage(
    context: Context,
    imageCapture: ImageCapture,
    executor: ExecutorService,
    lat: Double,
    lng: Double,
    address: String,
    onResult: (Uri?) -> Unit
) {
    val name = "GPS_IMG_${System.currentTimeMillis()}"
    val photoFile = File.createTempFile(name, ".jpg", context.cacheDir)

    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

    imageCapture.takePicture(
        outputOptions,
        executor,
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                try {
                    // Add overlay to the saved bitmap
                    val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath) ?: throw Exception("Failed to decode bitmap")
                    val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
                    val canvas = Canvas(mutableBitmap)
                    
                    val paint = Paint().apply {
                        color = android.graphics.Color.WHITE
                        textSize = mutableBitmap.width / 35f
                        isAntiAlias = true
                        setShadowLayer(5f, 0f, 0f, android.graphics.Color.BLACK)
                    }

                    val rectPaint = Paint().apply {
                        color = android.graphics.Color.argb(150, 0, 0, 0)
                    }

                    val textLat = "Coordinates: $lat, $lng"
                    val textTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                    val textApp = "Shot on Byte Tools"
                    
                    val margin = mutableBitmap.width / 40f
                    val spacing = 10f
                    val lineHeight = paint.textSize + spacing
                    
                    // Setup text wrapping for address
                    val textPaint = android.text.TextPaint(paint)
                    val staticLayout = android.text.StaticLayout.Builder.obtain(
                        address, 0, address.length, textPaint, (mutableBitmap.width - (margin * 2)).toInt()
                    )
                    .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL)
                    .setLineSpacing(0f, 1f)
                    .build()

                    val addressHeight = staticLayout.height
                    val rectHeight = (lineHeight * 3) + addressHeight + (margin * 2) + spacing

                    // Draw semi-transparent background for text at the bottom
                    canvas.drawRect(
                        0f, 
                        mutableBitmap.height - rectHeight, 
                        mutableBitmap.width.toFloat(), 
                        mutableBitmap.height.toFloat(), 
                        rectPaint
                    )
                    
                    // Draw text lines
                    var currentY = mutableBitmap.height - rectHeight + margin + paint.textSize
                    canvas.drawText(textLat, margin, currentY, paint)
                    
                    // Moving currentY to the start of the address block
                    currentY += spacing 
                    canvas.save()
                    canvas.translate(margin, currentY)
                    staticLayout.draw(canvas)
                    canvas.restore()
                    
                    // Move currentY past the multi-line address + its line height
                    currentY += addressHeight + lineHeight 
                    canvas.drawText(textTime, margin, currentY, paint)
                    
                    currentY += lineHeight
                    paint.isFakeBoldText = true
                    canvas.drawText(textApp, margin, currentY, paint)

                    // Save the processed bitmap to MediaStore
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, "$name.jpg")
                        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/ByteTools")
                        }
                    }

                    val uri = context.contentResolver.insert(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        contentValues
                    ) ?: throw Exception("Failed to insert into MediaStore")

                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        mutableBitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                    }

                    // Delete temp file
                    photoFile.delete()
                    
                    onResult(uri)
                } catch (e: Exception) {
                    Log.e("GPSCamera", "Overlay/Save failed: ${e.message}", e)
                    onResult(null)
                }
            }

            override fun onError(exc: ImageCaptureException) {
                Log.e("GPSCamera", "Photo capture failed: ${exc.message}", exc)
                onResult(null)
            }
        }
    )
}

suspend fun getAddress(context: Context, lat: Double, lng: Double): String {
    return withContext(Dispatchers.IO) {
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocation(lat, lng, 1)
            if (!addresses.isNullOrEmpty()) {
                addresses[0].getAddressLine(0) ?: "Unknown Address"
            } else "Unknown Location"
        } catch (e: Exception) {
            "Unknown Address"
        }
    }
}
