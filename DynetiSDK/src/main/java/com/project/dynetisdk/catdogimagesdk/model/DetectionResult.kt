package com.project.dynetisdk.catdogimagesdk.model

import android.graphics.RectF

class DetectionResult(val className: String, val confidence: Float, val boundingBox: RectF) {
    override fun toString(): String {
        return "DetectionResult(className='$className', confidence=$confidence, boundingBox=$boundingBox)"
    }
}
