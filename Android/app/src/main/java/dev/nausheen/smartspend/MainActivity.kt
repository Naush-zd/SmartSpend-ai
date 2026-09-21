package dev.nausheen.smartspend

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.nausheen.smartspend.auth.AuthScreen
import dev.nausheen.smartspend.auth.AuthViewModel
import dev.nausheen.smartspend.auth.SessionGate
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
    val authViewModel: AuthViewModel = viewModel()
    // Session is restored from storage asynchronously on launch; gate on it so
    // a returning user lands straight on Home instead of the login screen.
    val gate by authViewModel.gate.collectAsStateWithLifecycle()

    when (gate) {
        SessionGate.Loading -> SplashScreen()
        SessionGate.SignedIn -> HomeScreen(
            token = authViewModel.accessToken(),
            onSignOut = { authViewModel.signOut() },
        )
        SessionGate.SignedOut -> Scaffold(modifier = Modifier.fillMaxSize()) { p ->
            AuthScreen(
                vm = authViewModel,
                modifier = Modifier.padding(p),
                onAuthenticated = { /* gate flips to SignedIn and swaps the screen */ },
            )
        }
    }
}

@Composable
private fun SplashScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

private enum class Tab(val label: String) { Scan("Scan"), Expenses("Expenses"), Splits("Splits") }

@Composable
private fun HomeScreen(token: String?, onSignOut: () -> Unit) {
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
            Tab.Scan -> ScanScreen(accessToken = token, modifier = m, onSignOut = onSignOut)
            Tab.Expenses -> ExpensesScreen(accessToken = token, modifier = m)
            Tab.Splits -> SplitsScreen(accessToken = token, modifier = m)
        }
    }
}
