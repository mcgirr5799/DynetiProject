package com.project.dynetiproject.model
data class ImageResult(
    val key: String,
    val filename: String,
    val className: String,
    val confidence: Float,
    val boundingBox: Map<String, Float>
)