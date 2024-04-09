package com.project.dynetiproject

import android.content.Context
import com.project.dynetiproject.catdogimagesdk.TFLiteModel
import java.nio.ByteBuffer

class CatDogImageClassifier(context: Context) {
    private val model: TFLiteModel = TFLiteModel(context, "catDogModel.tflite") // Replace "model.tflite" with your model's name
    fun classify(input: ByteBuffer): Array<FloatArray> {
        return model.classify(input)
    }
}