package dev.nausheen.smartspend

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.nausheen.smartspend.auth.AuthScreen
import dev.nausheen.smartspend.auth.AuthViewModel
import dev.nausheen.smartspend.expenses.ExpensesScreen
import dev.nausheen.smartspend.scan.ScanScreen
import dev.nausheen.smartspend.splits.SplitsScreen
import dev.nausheen.smartspend.ui.theme.SmartSpendTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SmartSpendTheme {
                AppRoot()
            }
        }
    }
}

@Composable
private fun AppRoot() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()

    NavHost(navController = navController, startDestination = "auth") {
        composable("auth") {
            Scaffold(modifier = Modifier.fillMaxSize()) { p ->
                AuthScreen(
                    vm = authViewModel,
                    modifier = Modifier.padding(p),
                    onAuthenticated = {
                        navController.navigate("home") { popUpTo("auth") { inclusive = true } }
                    },
                )
            }
        }
        composable("home") {
            HomeScreen(token = authViewModel.accessToken())
        }
    }
}

private enum class Tab(val label: String) { Scan("Scan"), Expenses("Expenses"), Splits("Splits") }

@Composable
private fun HomeScreen(token: String?) {
    var tab by remember { mutableStateOf(Tab.Scan) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = tab == t,
                        onClick = { tab = t },
                        icon = {},
                        label = { Text(t.label) },
                    )
                }
            }
        },
    ) { padding ->
        val m = Modifier.padding(padding)
        when (tab) {
            Tab.Scan -> ScanScreen(accessToken = token, modifier = m)
            Tab.Expenses -> ExpensesScreen(accessToken = token, modifier = m)
            Tab.Splits -> SplitsScreen(accessToken = token, modifier = m)
        }
    }
}
