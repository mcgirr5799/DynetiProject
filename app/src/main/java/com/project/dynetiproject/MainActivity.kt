package com.project.dynetiproject

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.RectF
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.core.net.toUri
import androidx.lifecycle.LifecycleOwner
import com.google.firebase.Firebase
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.database.database
import com.project.dynetiproject.data.saveToFirebaseDatabase
import com.project.dynetiproject.model.ImageResult
import com.project.dynetiproject.ui.theme.DynetiProjectTheme
import java.io.File
import java.sql.Timestamp

class MainActivity : ComponentActivity() {

    private lateinit var imageCapture: ImageCapture
    var imageBitmap by mutableStateOf<ImageBitmap?>(null)
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                startCamera()
            } else {
                Toast.makeText(
                    this@MainActivity,
                    "Camera permission is required to take photos",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }

    private lateinit var imageAnalysis: ImageAnalysis
    private lateinit var catDogImageClassifier: CatDogImageClassifier

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize the ImageAnalysis
        imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            requestCameraPermission()
        }

        catDogImageClassifier = CatDogImageClassifier(this)

        //MAKE FIREBASE CRASHLYTICS WORK
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
        FirebaseCrashlytics.getInstance().log("App started")
    }

    private fun requestCameraPermission() {
        //camera permission
        requestPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    private fun startCamera() {
        val previewView = PreviewView(this@MainActivity)

        // Initialize a MutableState for the DetectionResult
        var detectionResult by mutableStateOf(
            CatDogImageClassifier.DetectionResult(
                "Unknown",
                0f,
                RectF()
            )
        )

        setContent {
            MaterialTheme {
                CameraScreen(previewView, ::takePicture, detectionResult, isTakingPicture)
            }
        }

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            // Initialize the ImageCapture instance
            imageCapture = ImageCapture.Builder().build()

            // Initialize the Preview instance
            val preview = androidx.camera.core.Preview.Builder().build()

            // Connect the Preview instance to the PreviewView
            preview.setSurfaceProvider(previewView.surfaceProvider)

            // Initialize the ImageAnalysis instance
            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this)) { imageProxy ->
                val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                // Convert the image to Bitmap
                val bitmap = imageProxy.toBitmap()
                // Convert the bitmap to a ByteBuffer
                val byteBuffer = catDogImageClassifier.convertBitmapToByteBuffer(bitmap)
                // Run the model on the ByteBuffer
                val output = catDogImageClassifier.classify(byteBuffer)
                // Handle the output and update the detectionResult
                detectionResult = catDogImageClassifier.handleOutput(output)
                imageProxy.close()
            }

            // When setting up the camera, include the ImageCapture, Preview and ImageAnalysis in the camera's use cases
            cameraProvider.bindToLifecycle(this as LifecycleOwner, cameraSelector, imageCapture, preview, imageAnalysis)
        }, ContextCompat.getMainExecutor(this))
    }

    var isTakingPicture by mutableStateOf(false)

    fun takePicture() {
        if (isTakingPicture) return

        isTakingPicture = true

        val imageCapture = imageCapture

        val filename = "image_${System.currentTimeMillis()}.jpg"
        val photoFile = File(filesDir, filename)

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    isTakingPicture = false
                    val savedUri = outputFileResults.savedUri ?: photoFile.toUri()
                    val bitmap = BitmapFactory.decodeFile(savedUri.path)
                    imageBitmap = bitmap.asImageBitmap()

                    val byteBuffer = catDogImageClassifier.convertBitmapToByteBuffer(bitmap)
                    val output = catDogImageClassifier.classify(byteBuffer)
                    val detectionResult = catDogImageClassifier.handleOutput(output)

                    val imageResult = ImageResult(
                        key = Firebase.database.reference.push().key ?: throw IllegalArgumentException("Failed to generate a key"),
                        filename = filename,
                        className = detectionResult.className,
                        confidence = detectionResult.confidence,
                        boundingBox = mapOf(
                            "left" to detectionResult.boundingBox.left,
                            "top" to detectionResult.boundingBox.top,
                            "right" to detectionResult.boundingBox.right,
                            "bottom" to detectionResult.boundingBox.bottom
                        ),
                        timestamp = Timestamp(System.currentTimeMillis())
                    )

                    saveToFirebaseDatabase(imageResult)

                    Log.d("Take Picture", "Result: " +  output.toString() + " " + detectionResult.className + " " + detectionResult.confidence + " " + detectionResult.boundingBox.toString() + " " + savedUri.toString())
                    Log.d("MainActivity", "Saved image to: ${photoFile.absolutePath}")
                    Toast.makeText(this@MainActivity, "Image captured successfully!", Toast.LENGTH_SHORT).show()
                }

                override fun onError(exception: ImageCaptureException) {
                    isTakingPicture = false
                    Toast.makeText(this@MainActivity, "Photo capture failed: ${exception.message}", Toast.LENGTH_SHORT).show()
                    FirebaseCrashlytics.getInstance().recordException(exception)
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(previewView: PreviewView, onTakePicture: () -> Unit, detectionResult: CatDogImageClassifier.DetectionResult, isTakingPicture: Boolean) {
    DynetiProjectTheme {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = {
                    Text(text = "Cat/Dog Recognition",
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                actions = {
                    val context = LocalContext.current
                    IconButton(onClick = {
                        //launch ListActivity
                        val intent = Intent(context, ListActivity::class.java)
                        context.startActivity(intent)
                    }) {
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = "List")
                    }
                }
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
            ) {
                AndroidView(
                    factory = { previewView },
                    modifier = Modifier.fillMaxSize()
                ) { view ->
                    // No need to do anything here, the camera preview is handled by the AndroidView
                }

                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawRect(
                        color = Color.Red,
                        topLeft = Offset(detectionResult.boundingBox.left, detectionResult.boundingBox.top),
                        size = Size(detectionResult.boundingBox.width(), detectionResult.boundingBox.height()),
                        style = Stroke(width = 2f)
                    )
                }

                Text(
                    text = "${detectionResult.className} (${"%.2f".format(detectionResult.confidence)})",
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                )
            }
            Button(
                onClick = { onTakePicture() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.CenterHorizontally),
                enabled = !isTakingPicture
            ) {
                Text(text = if (isTakingPicture) "Taking Picture..." else "Take Picture")
            }
        }
    }
}
