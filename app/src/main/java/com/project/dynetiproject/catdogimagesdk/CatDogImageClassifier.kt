package com.project.dynetiproject

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.net.Uri
import com.project.dynetiproject.catdogimagesdk.TFLiteModel
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

class CatDogImageClassifier(context: Context) {
    private val model: TFLiteModel = TFLiteModel(context, "catDogModel.tflite") // Replace "model.tflite" with your model's name
    fun classify(input: ByteBuffer): Array<FloatArray> {
        return model.classify(input)
    }

    fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
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

    data class DetectionResult(val className: String, val confidence: Float, val boundingBox: RectF)

    fun handleOutput(output: Array<FloatArray>): DetectionResult {
        val maxIndex = output[0].indices.maxByOrNull { output[0][it] } ?: -1
        val confidence = output[0][maxIndex]
        val className = if (maxIndex == 0) "Cat" else "Dog"
        val boundingBox = RectF() // Initialize an empty RectF
        return DetectionResult(className, confidence, boundingBox)
    }

    fun saveOutputAndImage(output: Array<FloatArray>, imageUri: Uri, outputDirectory: File) {
        // Convert the output to a string
        val outputString = output.joinToString(separator = ", ") { it.joinToString() }

        // Create a new file to save the output and the image Uri
        val outputFile = File(outputDirectory, "output_${System.currentTimeMillis()}.txt")
        outputFile.writeText("Image Uri: $imageUri\nOutput: $outputString")
    }
}