package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.ui.theme.MyApplicationTheme
import com.example.nerdvault.ui.NerdVaultDashboard
import com.example.nerdvault.viewmodel.NerdVaultViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: NerdVaultViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Full Edge-to-edge system drawing configuration
        enableEdgeToEdge()
        
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = com.example.nerdvault.ui.TechBlack
                ) {
                    NerdVaultDashboard(viewModel = viewModel)
                }
            }
        }
    }
}
