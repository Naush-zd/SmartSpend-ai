package dev.nausheen.smartspend.expenses

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.nausheen.smartspend.data.model.Expense

@Composable
fun ExpensesScreen(
    accessToken: String?,
    modifier: Modifier = Modifier,
    vm: ExpensesViewModel = viewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }

    LaunchedEffect(accessToken) { vm.load(accessToken) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAdd = true },
                text = { Text("Add") },
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
                "Expenses",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            when {
                state.loading -> CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                state.error != null -> Text("Error: ${state.error}")
                state.expenses.isEmpty() -> Text(
                    "No expenses yet. Tap Add to create one, or scan a receipt.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(state.expenses, key = { it.id }) { e ->
                        SwipeableExpenseRow(e, onDelete = { vm.delete(accessToken, e.id) })
                    }
                }
            }
        }
    }

    if (showAdd) {
        AddExpenseDialog(
            onDismiss = { showAdd = false },
            onConfirm = { title, amount, category, note ->
                vm.add(accessToken, title, amount, category, note)
                showAdd = false
            },
        )
    }
}

@Composable
private fun SwipeableExpenseRow(e: Expense, onDelete: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) { onDelete(); true } else false
        },
    )
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.error)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Text("Delete", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        },
    ) {
        ExpenseCard(e)
    }
}

@Composable
private fun ExpenseCard(e: Expense) {
    val (chipBg, chipFg) = dev.nausheen.smartspend.ui.theme.categoryColors(e.category)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(e.title, fontWeight = FontWeight.SemiBold)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(percent = 50))
                        .background(chipBg)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        "${e.category}${e.expenseDate?.let { "  ·  $it" } ?: ""}",
                        color = chipFg,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
            Text(
                "₹${"%.0f".format(e.amount)}",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

private val CATEGORIES = listOf(
    "Food & Dining", "Groceries", "Transport", "Shopping",
    "Utilities", "Health", "Entertainment", "Travel", "Other",
)

@Composable
private fun AddExpenseDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, amount: Double, category: String, note: String?) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(CATEGORIES.first()) }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add expense") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(title, { title = it }, label = { Text("Title") }, singleLine = true)
                OutlinedTextField(amount, { amount = it }, label = { Text("Amount") }, singleLine = true)
                CategoryPicker(category) { category = it }
                OutlinedTextField(note, { note = it }, label = { Text("Note (optional)") }, singleLine = true)
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amt = amount.toDoubleOrNull() ?: return@TextButton
                    if (title.isBlank()) return@TextButton
                    onConfirm(title.trim(), amt, category, note.ifBlank { null })
                }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun CategoryPicker(selected: String, onSelect: (String) -> Unit) {
    Column {
        Text("Category: $selected", fontWeight = FontWeight.Medium)
        androidx.compose.foundation.layout.FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            CATEGORIES.forEach { c ->
                androidx.compose.material3.FilterChip(
                    selected = c == selected,
                    onClick = { onSelect(c) },
                    label = { Text(c) },
                )
            }
        }
    }
}
