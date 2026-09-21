package dev.nausheen.smartspend.scan

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Scan a receipt", fontWeight = FontWeight.Bold)
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
    onAddToExpenses: () -> Unit,
    onSplit: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(result.merchantName ?: "Unknown merchant", fontWeight = FontWeight.Bold)
            result.receiptDate?.let { Text("Date: $it") }
            result.totalAmount?.let { Text("Total: ${result.currency} ${"%.2f".format(it)}") }

            if (result.isAnomaly && result.anomalyReason != null) {
                Text("⚠ ${result.anomalyReason}", fontWeight = FontWeight.Medium)
            }

            result.items.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text(item.name)
                        item.category?.let { AssistChip(onClick = {}, label = { Text(it) }) }
                    }
                    Text("${result.currency} ${"%.2f".format(item.amount)}")
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onAddToExpenses) { Text("Add to expenses") }
                OutlinedButton(onClick = onSplit) { Text("Split this") }
            }
        }
    }
}
