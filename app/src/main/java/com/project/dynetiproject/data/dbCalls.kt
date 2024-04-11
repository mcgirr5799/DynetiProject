package com.project.dynetiproject.data

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.project.dynetiproject.CatDogImageClassifier
import com.project.dynetiproject.model.ImageResult
import com.project.dynetisdk.catdogimagesdk.model.DetectionResult
import java.sql.Timestamp
import java.text.SimpleDateFormat

/**
 * Creates an ImageResult object from the given filename and detection result.
 *
 * @param filename The name of the file where the image is stored.
 * @param detectionResult The result of the image classification.
 * @return An ImageResult object.
 */
fun createImageResult(filename: String, detectionResult: DetectionResult): ImageResult {
    val key = Firebase.database.reference.push().key ?: throw IllegalArgumentException("Failed to generate a key")

    return ImageResult(
        key = key,
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
}

/**
 * Saves an ImageResult object to the Firebase database.
 *
 * @param imageResult The ImageResult object to save.
 */
fun saveToFirebaseDatabase(imageResult: ImageResult) {
    val database = Firebase.database
    database.getReference("images/${imageResult.key}").setValue(imageResult)
        .addOnSuccessListener {
            Log.d("Firebase", "Data saved successfully.")
        }
        .addOnFailureListener { e ->
            Log.e("Firebase", "Failed to save data.", e)
            FirebaseCrashlytics.getInstance().recordException(e)
        }
}

/**
 * Gathers all the image classifications from the Firebase database and passes them to the given callback function.
 *
 * @param onResult The callback function to pass the list of ImageResult objects to.
 */
fun gatherClassifications(onResult: (List<ImageResult>) -> Unit) {
    val resultList = mutableListOf<ImageResult>()

    FirebaseDatabase.getInstance().reference.child("images").get().addOnSuccessListener {
        for (entry in it.children){
            val classification = entry.child("className").value.toString()
            val confidence = entry.child("confidence").value.toString().toFloat()
            val filename = entry.child("filename").value.toString()
            val boundingBox = mapOf(
                "left" to entry.child("boundingBox").child("left").value.toString().toFloat(),
                "top" to entry.child("boundingBox").child("top").value.toString().toFloat(),
                "right" to entry.child("boundingBox").child("right").value.toString().toFloat(),
                "bottom" to entry.child("boundingBox").child("bottom").value.toString().toFloat()
            )

            val timestampMap = entry.child("timestamp").value as? Map<String, Any>
            val timeLong = timestampMap?.get("time") as? Long
            val timestamp = if (timeLong != null) {
                Timestamp(timeLong)
            } else {
                // Assign a default value or skip this entry
                Timestamp(System.currentTimeMillis())
            }

            val imageResult = ImageResult(entry.key ?: "", filename, classification, confidence, boundingBox, timestamp)
            resultList.add(imageResult)

            // Log the details of the ImageResult
            Log.d("Firebase DB", "Retrieved ImageResult: $imageResult")
        }
        onResult(resultList)

        // Log the entire resultList
        Log.d("Firebase DB", "Retrieved resultList: $resultList")
    }
}