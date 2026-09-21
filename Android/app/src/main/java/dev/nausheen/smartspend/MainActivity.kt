package dev.nausheen.smartspend

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.nausheen.smartspend.auth.AuthScreen
import dev.nausheen.smartspend.auth.AuthViewModel
import dev.nausheen.smartspend.auth.SessionGate
import dev.nausheen.smartspend.expenses.ExpensesScreen
import dev.nausheen.smartspend.scan.ScanScreen
import dev.nausheen.smartspend.splits.SplitsScreen
import dev.nausheen.smartspend.ui.theme.Charcoal
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
    val gate by authViewModel.gate.collectAsStateWithLifecycle()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        when (gate) {
            SessionGate.Loading -> SplashScreen()
            SessionGate.SignedIn -> HomeScreen(
                token = authViewModel.accessToken(),
                onSignOut = { authViewModel.signOut() },
            )
            SessionGate.SignedOut -> AuthScreen(
                vm = authViewModel,
                onAuthenticated = { /* gate flips to SignedIn */ },
            )
        }
    }
}

@Composable
private fun SplashScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

private enum class Tab(val label: String) { Scan("Scan"), Expenses("Expenses"), Splits("Splits") }

@Composable
private fun HomeScreen(token: String?, onSignOut: () -> Unit) {
    var tab by remember { mutableStateOf(Tab.Scan) }
    // Content clears the floating pill (nav bar inset + pill height + margin).
    val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(modifier = Modifier.fillMaxSize()) {
        val content = Modifier.padding(bottom = navInset + 96.dp)
        when (tab) {
            Tab.Scan -> ScanScreen(accessToken = token, modifier = content, onSignOut = onSignOut)
            Tab.Expenses -> ExpensesScreen(accessToken = token, modifier = content)
            Tab.Splits -> SplitsScreen(accessToken = token, modifier = content)
        }

        GlassPillNav(
            selected = tab,
            onSelect = { tab = it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = navInset + 16.dp),
        )
    }
}

@Composable
private fun GlassPillNav(selected: Tab, onSelect: (Tab) -> Unit, modifier: Modifier = Modifier) {
    val pill = RoundedCornerShape(percent = 50)
    Row(
        modifier = modifier
            .shadow(elevation = 16.dp, shape = pill, ambientColor = Charcoal, spotColor = Charcoal)
            .clip(pill)
            .background(Color.White.copy(alpha = 0.86f))
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Tab.entries.forEach { t ->
            PillSegment(tab = t, selected = t == selected, onClick = { onSelect(t) })
        }
    }
}

@Composable
private fun PillSegment(tab: Tab, selected: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        label = "pillBg",
    )
    val fg by animateColorAsState(
        if (selected) Color.White else Charcoal.copy(alpha = 0.6f),
        label = "pillFg",
    )
    val hPad by animateDpAsState(if (selected) 22.dp else 16.dp, label = "pillPad")
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(bg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = hPad, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = tab.label,
            color = fg,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        )
    }
}
