package com.example.bordermove26

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.bordermove26.ui.screens.EditScreen
import com.example.bordermove26.ui.screens.ProcessingScreen
import com.example.bordermove26.ui.screens.ResultScreen
import com.example.bordermove26.ui.screens.VideoSelectionScreen
import com.example.bordermove26.ui.theme.Bordermove26Theme
import com.example.bordermove26.viewmodels.VideoEditViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Bordermove26Theme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AppNavigation(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun AppNavigation(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val viewModel: VideoEditViewModel = viewModel()
    val state by viewModel.state.collectAsState()

    NavHost(
        navController = navController,
        startDestination = "selection",
        modifier = modifier
    ) {
        composable("selection") {
            VideoSelectionScreen(
                onVideoSelected = { uri ->
                    viewModel.selectVideo(uri)
                    navController.navigate("processing")
                }
            )
        }

        composable("processing") {
            ProcessingScreen(
                state = state,
                onProcessingComplete = {
                    navController.navigate("edit") {
                        popUpTo("processing") { inclusive = true }
                    }
                },
                onStartProcessing = {
                    viewModel.processVideo()
                }
            )
        }

        composable("edit") {
            EditScreen(
                state = state,
                onCropVideo = {
                    viewModel.cropVideo()
                }
            )

            if (state.croppedVideoUri != null && !state.isCropping) {
                navController.navigate("result") {
                    popUpTo("edit") { inclusive = true }
                }
            }
        }

        composable("result") {
            ResultScreen(
                croppedVideoUri = state.croppedVideoUri,
                onSaveToGallery = {
                    viewModel.saveVideoToGallery()
                },
                onBack = {
                    navController.navigate("selection") {
                        popUpTo("selection") { inclusive = true }
                    }
                }
            )
        }
    }
}