package com.project.dynetiproject.model

import java.sql.Timestamp

data class ImageResult(
    val key: String,
    val filename: String,
    val className: String,
    val confidence: Float,
    val boundingBox: Map<String, Float>,
    val timestamp: Timestamp
)