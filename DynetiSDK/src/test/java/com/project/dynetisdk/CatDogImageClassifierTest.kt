package com.project.dynetisdk

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.net.Uri
import com.project.dynetiproject.CatDogImageClassifier
import com.project.dynetisdk.catdogimagesdk.TFLiteModel
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.io.File
import java.nio.ByteBuffer

@RunWith(RobolectricTestRunner::class)
@Config(manifest=Config.NONE, qualifiers="port")
class CatDogImageClassifierTest {

    private lateinit var context: Context
    private lateinit var bitmap: Bitmap
    private lateinit var uri: Uri
    private lateinit var file: File
    private lateinit var classifier: CatDogImageClassifier

    @Before
    fun setUp() {
        context = RuntimeEnvironment.application
        bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        uri = Uri.parse("android.resource://com.project.dynetisdk/drawable/test_image")
        file = File(RuntimeEnvironment.application.filesDir, "test_file.txt")
        classifier = CatDogImageClassifier(context)
    }

    @Test
    fun testClassify() {
        val input = ByteBuffer.allocate(1)
        val output = arrayOf(floatArrayOf(0.5f, 0.5f))

        val result = classifier.classify(input)

        Assert.assertArrayEquals(output, result)
    }

    @Test
    fun testConvertBitmapToByteBuffer() {
        val byteBuffer = classifier.convertBitmapToByteBuffer(bitmap)

        Assert.assertNotNull(byteBuffer)
    }

    @Test
    fun testHandleOutput() {
        val output = arrayOf(floatArrayOf(0.5f, 0.5f))
        val result = classifier.handleOutput(output)

        Assert.assertEquals("Dog", result.className)
        Assert.assertEquals(0.5f, result.confidence, 0.01f)
        Assert.assertEquals(RectF(), result.boundingBox)
    }

    @Test
    fun testSaveOutputAndImage() {
        val output = arrayOf(floatArrayOf(0.5f, 0.5f))
        val outputDirectory = File(RuntimeEnvironment.application.filesDir, "test_output")

        // Proceed with the test
        classifier.saveOutputAndImage(output, uri, outputDirectory)

        // Validate the behavior
        Assert.assertTrue(outputDirectory.exists())
        Assert.assertTrue(outputDirectory.isDirectory)
    }
}