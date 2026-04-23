package com.example.smartpark

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.smartpark.auth.AuthViewModel
import com.example.smartpark.navigation.SmartParkNavGraph
import com.example.smartpark.ui.theme.SmartParkTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SmartParkTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Single shared ViewModel so login state flows across screens
                    val authViewModel: AuthViewModel = viewModel()
                    SmartParkNavGraph(authViewModel = authViewModel)
                }
            }
        }
    }
}