
package com.project.dynetiproject

import androidx.compose.ui.res.painterResource

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons

import androidx.compose.material3.Icon

import androidx.compose.material3.IconButton

import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton

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
import androidx.compose.ui.unit.sp
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
    var sortOption by remember { mutableStateOf("timestamp") }
    var isSortAscending by remember { mutableStateOf(true) }
    var isMenuExpanded by remember { mutableStateOf(false) }
    val sortedList = when (sortOption) {
        "timestamp" -> if (isSortAscending) list.sortedBy { it.timestamp } else list.sortedByDescending { it.timestamp }
        "classification" -> if (isSortAscending) list.sortedBy { it.className } else list.sortedByDescending { it.className }
        "confidence" -> if (isSortAscending) list.sortedBy { it.confidence } else list.sortedByDescending { it.confidence }
        else -> list
    }
    DynetiProjectTheme {
        Column {
            TopAppBar(
                title = {
                    Text(text = "Cat/Dog Recognition")
                },
                actions = {
                    IconButton(onClick = { isMenuExpanded = true }) {
                        Icon(
                            painter = painterResource(id = R.drawable.baseline_filter_alt_24),
                            contentDescription = "Sort options"
                        )
                    }
                    DropdownMenu(
                        expanded = isMenuExpanded,
                        onDismissRequest = { isMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            { Text("Sort by timestamp")},
                            onClick = {
                                sortOption = "timestamp"
                                isMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            { Text("Sort by classification")},
                            onClick = {
                                sortOption = "classification"
                                isMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            { Text("Sort by confidence")},
                            onClick = {
                                sortOption = "confidence"
                                isMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            { Text(if (isSortAscending) "Sort descending" else "Sort ascending") },
                            onClick = {
                                isSortAscending = !isSortAscending
                                isMenuExpanded = false
                            }
                        )
                    }
                }
            )
            LazyColumn {
                //get the file and show as thumbnail
                items(sortedList) { imageResult ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Row(modifier = Modifier.padding(8.dp)) {
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

                                Spacer(modifier = Modifier.width(8.dp))

                                Column(modifier = Modifier.padding(start = 8.dp), verticalArrangement = Arrangement.Center) {
                                    Text(
                                        text = "${imageResult.className} (${(imageResult.confidence * 100).toInt()}%)",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontSize = 20.sp, // Adjust this value as needed
                                        modifier = Modifier.fillMaxWidth()
                                    )


                                    // Display the timestamp
                                    Text(
                                        text = imageResult.timestamp.toString(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontSize = 18.sp, // Adjust this value as needed
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}




