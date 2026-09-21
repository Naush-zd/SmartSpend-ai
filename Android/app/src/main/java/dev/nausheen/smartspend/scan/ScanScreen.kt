package dev.nausheen.smartspend.scan

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import dev.nausheen.smartspend.data.model.ScanResult

@Composable
fun ScanScreen(
    accessToken: String?,
    modifier: Modifier = Modifier,
    onSignOut: () -> Unit = {},
    vm: ScanViewModel = viewModel(),
) {
    val context = LocalContext.current
    val state by vm.state.collectAsStateWithLifecycle()
    val actionMessage by vm.actionMessage.collectAsStateWithLifecycle()
    val actionInProgress by vm.actionInProgress.collectAsStateWithLifecycle()
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) { imageUri = uri; vm.reset() } }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success -> if (success) { imageUri = pendingCameraUri; vm.reset() } }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val uri = createCameraImageUri(context)
            pendingCameraUri = uri
            cameraLauncher.launch(uri)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            Text(
                "Scan a receipt",
                style = androidx.compose.material3.MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            androidx.compose.material3.TextButton(onClick = onSignOut) { Text("Sign out") }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = {
                galleryLauncher.launch(
                    androidx.activity.result.PickVisualMediaRequest(
                        ActivityResultContracts.PickVisualMedia.ImageOnly
                    )
                )
            }) { Text("Gallery") }

            OutlinedButton(onClick = {
                permissionLauncher.launch(android.Manifest.permission.CAMERA)
            }) { Text("Camera") }
        }

        imageUri?.let { uri ->
            AsyncImage(
                model = uri,
                contentDescription = "Selected receipt",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
            )
            Button(
                onClick = { vm.scan(context, uri, accessToken) },
                enabled = state !is ScanUiState.Loading,
            ) { Text("Scan receipt") }
        }

        when (val s = state) {
            is ScanUiState.Loading -> CircularProgressIndicator()
            is ScanUiState.Error -> Text("Error: ${s.message}")
            is ScanUiState.Success -> ResultCard(
                result = s.result,
                actionEnabled = !actionInProgress,
                onAddToExpenses = { vm.addToExpenses(s.result, accessToken) },
                onSplit = { vm.splitReceipt(s.result, accessToken) },
            )
            ScanUiState.Idle -> Unit
        }

        actionMessage?.let { Text(it, fontWeight = FontWeight.Medium) }
    }
}

@Composable
private fun ResultCard(
    result: ScanResult,
    actionEnabled: Boolean,
    onAddToExpenses: () -> Unit,
    onSplit: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
        ),
        elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                result.merchantName ?: "Unknown merchant",
                fontWeight = FontWeight.Bold,
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
            )
            result.receiptDate?.let {
                Text("Date: $it", color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
            }
            result.totalAmount?.let {
                Text("Total: ₹${"%.0f".format(it)}", fontWeight = FontWeight.SemiBold)
            }

            if (result.isAnomaly && result.anomalyReason != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                        .background(androidx.compose.material3.MaterialTheme.colorScheme.error.copy(alpha = 0.1f))
                        .padding(10.dp),
                ) {
                    Text(
                        "⚠ ${result.anomalyReason}",
                        color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            result.items.forEach { item ->
                val (chipBg, chipFg) = dev.nausheen.smartspend.ui.theme.categoryColors(item.category)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(item.name, fontWeight = FontWeight.Medium)
                        item.category?.let {
                            Box(
                                modifier = Modifier
                                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(percent = 50))
                                    .background(chipBg)
                                    .padding(horizontal = 10.dp, vertical = 3.dp),
                            ) {
                                Text(it, color = chipFg, style = androidx.compose.material3.MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                    Text("₹${"%.0f".format(item.amount)}", fontWeight = FontWeight.SemiBold)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onAddToExpenses,
                    enabled = actionEnabled,
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                        contentColor = androidx.compose.ui.graphics.Color.White,
                    ),
                ) { Text("Add to expenses") }
                OutlinedButton(onClick = onSplit, enabled = actionEnabled) { Text("Split this") }
            }
        }
    }
}
