package com.project.dynetisdk.catdogimagesdk

import android.content.Context
import android.content.res.AssetFileDescriptor
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class TFLiteModel(context: Context, modelName: String) {
    private val interpreter: Interpreter

    init {
        val assetManager = context.assets
        val model = loadModelFile(assetManager.openFd(modelName))
        val interpreterOptions = Interpreter.Options()
        interpreter = Interpreter(model, interpreterOptions)
    }

    private fun loadModelFile(fileDescriptor: AssetFileDescriptor): MappedByteBuffer {
        FileInputStream(fileDescriptor.fileDescriptor).use { inputStream ->
            val fileChannel = inputStream.channel
            val startOffset = fileDescriptor.startOffset
            val declaredLength = fileDescriptor.declaredLength
            return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
        }
    }

    fun classify(input: ByteBuffer): Array<FloatArray> {
        val output = Array(1) { FloatArray(2) } // Adjust this to match the output shape of your model
        interpreter.run(input, output)
        return output
    }
}