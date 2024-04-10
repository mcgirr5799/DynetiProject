
package com.project.dynetiproject

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.database.FirebaseDatabase
import com.project.dynetiproject.data.gatherClassifications
import com.project.dynetiproject.model.ImageResult
import com.project.dynetiproject.ui.theme.DynetiProjectTheme
import java.io.File

class ListActivity : ComponentActivity() {

    private val entryList = mutableStateOf(listOf<ImageResult>())
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DynetiProjectTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LoadAndDisplayData()
                }
            }
        }
    }

    @Composable
    fun LoadAndDisplayData() {
        val context = LocalContext.current
        var isLoading by remember { mutableStateOf(true) }

        LaunchedEffect(key1 = Unit) {
            gatherClassifications { resultList ->
                // This code will be executed once the data is retrieved from Firebase
                // resultList is the list of ImageResult objects
                val mutableList = resultList.toMutableList()

                val iterator = mutableList.iterator()
                while (iterator.hasNext()) {
                    val imageResult = iterator.next()
                    val file = File(context.filesDir, imageResult.filename)
                    if (!file.exists()) {
                        iterator.remove()
                        Log.d("ListActivity", "Removed ImageResult: $imageResult because file does not exist")
                    }
                }
                entryList.value = mutableList
                isLoading = false
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (entryList.value.isNotEmpty()) {
                ListComposable(list = entryList.value)
            }
        }
    }
}
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListComposable(list: List<ImageResult>) {
    DynetiProjectTheme {
        Column {
            TopAppBar(
                title = {
                    Text(text = "Cat/Dog Recognition")
                }
            )
            LazyColumn {
                //get the file and show as thumbnail
                items(list) { imageResult ->
                    Row {
                        val context = LocalContext.current
                        val file = File(context.filesDir, imageResult.filename)
                        if (file.exists()) {
                            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                            val imageBitmap = bitmap.asImageBitmap()

                            Image(
                                bitmap = imageBitmap,
                                contentDescription = null, // decorative
                                modifier = Modifier.size(100.dp),
                                contentScale = ContentScale.Crop
                            )

                            Column {
                                // Display the classification and confidence
                                Text(
                                    text = "${imageResult.className} (${(imageResult.confidence * 100).toInt()}%)",
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                                // Display the timestamp
                                Text(
                                    text = imageResult.timestamp.toString(),
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}




