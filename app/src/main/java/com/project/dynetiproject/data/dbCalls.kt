package com.project.dynetiproject.data

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.project.dynetiproject.CatDogImageClassifier
import com.project.dynetiproject.model.ImageResult

fun createImageResult(filename: String, detectionResult: CatDogImageClassifier.DetectionResult): ImageResult {
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
        )
    )
}
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