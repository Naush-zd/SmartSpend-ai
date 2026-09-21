package dev.nausheen.smartspend.splits

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.nausheen.smartspend.data.model.Split
import dev.nausheen.smartspend.data.model.SplitMemberIn

@Composable
fun SplitsScreen(
    accessToken: String?,
    modifier: Modifier = Modifier,
    vm: SplitsViewModel = viewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var showCreate by remember { mutableStateOf(false) }

    LaunchedEffect(accessToken) { vm.load(accessToken) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreate = true },
                text = { Text("New split") },
                icon = {},
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .padding(top = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Bill splits",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            when {
                state.loading -> CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                state.error != null -> Text("Error: ${state.error}")
                state.splits.isEmpty() -> Text(
                    "No splits yet. Tap New split, or split a scanned receipt.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(state.splits, key = { it.id }) { s ->
                        SplitCard(s, onMarkPaid = { vm.markPaid(accessToken, it) })
                    }
                }
            }
        }
    }

    if (showCreate) {
        CreateSplitDialog(
            onDismiss = { showCreate = false },
            onConfirm = { title, total, members ->
                vm.create(accessToken, title, total, members)
                showCreate = false
            },
        )
    }
}

@Composable
private fun SplitCard(s: Split, onMarkPaid: (memberId: String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(s.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text("₹${"%.0f".format(s.totalAmount)}", fontWeight = FontWeight.Bold)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            s.members.forEach { m ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("${m.name} · ₹${"%.0f".format(m.amountOwed)}")
                    if (m.isPaid) {
                        Text("Paid", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    } else {
                        TextButton(onClick = { onMarkPaid(m.id) }) { Text("Mark paid") }
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateSplitDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, total: Double, members: List<SplitMemberIn>) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var total by remember { mutableStateOf("") }
    val names = remember { mutableStateListOf("", "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New split") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(title, { title = it }, label = { Text("Title") }, singleLine = true)
                OutlinedTextField(total, { total = it }, label = { Text("Total amount") }, singleLine = true)
                Text("People", fontWeight = FontWeight.Medium)
                names.forEachIndexed { i, n ->
                    OutlinedTextField(
                        value = n,
                        onValueChange = { names[i] = it },
                        label = { Text("Person ${i + 1}") },
                        singleLine = true,
                    )
                }
                OutlinedButton(onClick = { names.add("") }) { Text("Add person") }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amt = total.toDoubleOrNull() ?: return@TextButton
                    val people = names.map { it.trim() }.filter { it.isNotEmpty() }
                    if (title.isBlank() || people.isEmpty()) return@TextButton
                    // even split
                    val share = amt / people.size
                    val members = people.map { SplitMemberIn(name = it, amountOwed = share) }
                    onConfirm(title.trim(), amt, members)
                }
            ) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
