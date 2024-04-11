package com.project.dynetiproject.model

import java.sql.Timestamp

/**
 * Data class representing the result of an image classification.
 *
 * @param key The unique key for this result in the Firebase database.
 * @param filename The name of the file where the image is stored.
 * @param className The class name of the detected object (e.g., "Cat" or "Dog").
 * @param confidence The confidence score of the classification.
 * @param boundingBox A map representing the bounding box of the detected object. The keys are "left", "top", "right", and "bottom".
 * @param timestamp The timestamp when the image was captured and classified.
 */
data class ImageResult(
    val key: String,
    val filename: String,
    val className: String,
    val confidence: Float,
    val boundingBox: Map<String, Float>,
    val timestamp: Timestamp
)