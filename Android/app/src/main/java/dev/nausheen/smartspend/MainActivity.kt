package dev.nausheen.smartspend

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.nausheen.smartspend.auth.AuthScreen
import dev.nausheen.smartspend.auth.AuthViewModel
import dev.nausheen.smartspend.scan.ScanScreen
import dev.nausheen.smartspend.ui.theme.SmartSpendTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SmartSpendTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AppNav(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
private fun AppNav(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    // Shared across screens so the scan screen can read the access token.
    val authViewModel: AuthViewModel = viewModel()

    NavHost(navController = navController, startDestination = "auth", modifier = modifier) {
        composable("auth") {
            AuthScreen(
                vm = authViewModel,
                onAuthenticated = {
                    navController.navigate("scan") {
                        popUpTo("auth") { inclusive = true }
                    }
                },
            )
        }
        composable("scan") {
            ScanScreen(accessToken = authViewModel.accessToken())
        }
    }
}
