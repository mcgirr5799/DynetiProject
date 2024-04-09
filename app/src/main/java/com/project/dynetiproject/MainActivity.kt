package com.project.dynetiproject
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import android.Manifest
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Surface
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LifecycleOwner
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt

class MainActivity : ComponentActivity() {

    private lateinit var imageCapture: ImageCapture
    private var imageBitmap by mutableStateOf<ImageBitmap?>(null)

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

        // Set the CatDogImageClassifier as the analyzer for the ImageAnalysis

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

    }

    private fun requestCameraPermission() {
        //camera and external storage permission
        requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }

    private fun startCamera() {
        val previewView = PreviewView(this@MainActivity)

        setContent {
            MaterialTheme {
                CameraScreen(previewView, ::takePicture)
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

            imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), { imageProxy ->
                val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                // Convert the image to Bitmap
                val bitmap = imageProxy.toBitmap()
                // Convert the bitmap to a ByteBuffer
                val byteBuffer = convertBitmapToByteBuffer(bitmap)
                // Run the model on the ByteBuffer
                val output = catDogImageClassifier.classify(byteBuffer)
                // Handle the output
                handleOutput(output)
                imageProxy.close()
            })

            // When setting up the camera, include the ImageCapture, Preview and ImageAnalysis in the camera's use cases
            cameraProvider.bindToLifecycle(this as LifecycleOwner, cameraSelector, imageCapture, preview, imageAnalysis)
        }, ContextCompat.getMainExecutor(this))
    }

    // This is a placeholder function. You should replace this with your own implementation.
    fun handleOutput(output: Array<FloatArray>) {
        val confidence = sqrt(output[0][0] * output[0][0] + output[0][1] * output[0][1])
        if (confidence > 0.5) {
            if (output[0][0] > output[0][1]) {
                Log.d("DynetiProject", "The image is classified as a cat with confidence $confidence")
            } else {
                Log.d("DynetiProject", "The image is classified as a dog with confidence $confidence")
            }
        } else {
            Log.d("DynetiProject", "The image is not classified as a cat or a dog")
        }
    }

    fun takePicture() {
        val imageCapture = imageCapture ?: return

        val photoFile = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US).format(System.currentTimeMillis()) + ".jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val savedUri = outputFileResults.savedUri ?: photoFile.toUri()
                    Toast.makeText(this@MainActivity, "Photo capture succeeded: $savedUri", Toast.LENGTH_SHORT).show()
                    Log.d("DynetiProject", "Photo capture succeeded: $savedUri")
                    // Update the imageBitmap with the new photo
                    val bitmap = BitmapFactory.decodeFile(savedUri.path)
                    imageBitmap = bitmap.asImageBitmap()

                    // Convert the bitmap to a ByteBuffer
                    val byteBuffer = convertBitmapToByteBuffer(bitmap)

                    // Run the model on the ByteBuffer
                    val output = catDogImageClassifier.classify(byteBuffer)

                    // Save the output and the image
                    saveOutputAndImage(output, savedUri)
                }

                override fun onError(exception: ImageCaptureException) {
                    Toast.makeText(this@MainActivity, "Photo capture failed: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val modelInputSize = 224 // Assuming the model takes input of size 224x224 pixels
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, modelInputSize, modelInputSize, true)

        val byteBuffer = ByteBuffer.allocateDirect(4 * modelInputSize * modelInputSize * 3) // 4 bytes for each pixel, 3 for RGB
        byteBuffer.order(ByteOrder.nativeOrder())
        val intValues = IntArray(resizedBitmap.width * resizedBitmap.height)
        resizedBitmap.getPixels(intValues, 0, resizedBitmap.width, 0, 0, resizedBitmap.width, resizedBitmap.height)
        var pixel = 0
        for (i in 0 until resizedBitmap.width) {
            for (j in 0 until resizedBitmap.height) {
                val value = intValues[pixel++]
                byteBuffer.putFloat(((value shr 16 and 0xFF) - 127.5f) / 127.5f)
                byteBuffer.putFloat(((value shr 8 and 0xFF) - 127.5f) / 127.5f)
                byteBuffer.putFloat(((value and 0xFF) - 127.5f) / 127.5f)
            }
        }
        return byteBuffer
    }

    private fun saveOutputAndImage(output: Array<FloatArray>, imageUri: Uri) {
        // Convert the output to a string
        val outputString = output.joinToString(separator = ", ") { it.joinToString() }

        // Create a new file to save the output and the image Uri
        val outputFile = File(outputDirectory, "output_${System.currentTimeMillis()}.txt")
        outputFile.writeText("Image Uri: $imageUri\nOutput: $outputString")
    }

    private val outputDirectory: File
        get() {
            val imagesDir = File(applicationContext.filesDir, "images")
            if (!imagesDir.exists()) {
                imagesDir.mkdirs()
            }
            return imagesDir
        }
}

@Composable
fun CameraScreen(previewView: PreviewView, onTakePicture: () -> Unit) {
    var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    MaterialTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AndroidView(
                        factory = { previewView },
                        modifier = Modifier.clickable { onTakePicture() } // Capture image when the PreviewView is clicked
                    ) { view ->
                        // No need to do anything here, the camera preview is handled by the AndroidView
                    }

                    imageBitmap?.let { bitmap ->
                        Image(
                            bitmap = bitmap,
                            contentDescription = "Captured Image",
                            modifier = Modifier
                                .padding(16.dp)
                                .align(Alignment.BottomCenter)
                        )
                    }
                }
            }
        }
    }
}