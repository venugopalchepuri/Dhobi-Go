package com.dhobigo.tracker.ui.screens.collection

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dhobigo.tracker.data.local.model.LaundryRecordWithItems
import com.dhobigo.tracker.ui.components.ConfirmationDialog
import com.dhobigo.tracker.ui.components.QuantityStepper
import com.dhobigo.tracker.ui.theme.DangerRed
import com.dhobigo.tracker.ui.theme.StatusCollectedText
import com.dhobigo.tracker.ui.theme.StatusPendingBg
import com.dhobigo.tracker.ui.theme.StatusPendingText

@Composable
fun CollectionTrackingDialog(
    recordWithItems: LaundryRecordWithItems,
    onSaveProgress: (returnedMap: Map<Long, Int>, forceCollected: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val returnedMap = remember {
        mutableStateMapOf<Long, Int>().apply {
            recordWithItems.items.forEach { put(it.id, it.quantityReturned) }
        }
    }

    var showForceCollectWarning by remember { mutableStateOf(false) }

    val totalGiven = recordWithItems.items.sumOf { it.quantityGiven }
    val totalReturned = recordWithItems.items.sumOf { returnedMap[it.id] ?: 0 }
    val totalOutstanding = totalGiven - totalReturned

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 24.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Track Return",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Record returned clothes from the dhobi",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }

                    // Outstanding Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (totalOutstanding == 0) MaterialTheme.colorScheme.secondaryContainer else StatusPendingBg)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (totalOutstanding == 0) "All Collected" else "$totalOutstanding Missing",
                            color = if (totalOutstanding == 0) StatusCollectedText else StatusPendingText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Quick Action: Mark All Returned Button
                OutlinedButton(
                    onClick = {
                        recordWithItems.items.forEach { returnedMap[it.id] = it.quantityGiven }
                    },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.DoneAll,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Mark All Items As Returned",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Items list
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(recordWithItems.items, key = { it.id }) { item ->
                        val currentReturned = returnedMap[item.id] ?: 0
                        val outstanding = item.quantityGiven - currentReturned

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.categoryName,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Given: ${item.quantityGiven}  •  Due: $outstanding",
                                        color = if (outstanding > 0) DangerRed else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.sp
                                    )
                                }

                                QuantityStepper(
                                    quantity = currentReturned,
                                    onQuantityChange = { newReturned ->
                                        returnedMap[item.id] = newReturned.coerceIn(0, item.quantityGiven)
                                    },
                                    maxQuantity = item.quantityGiven,
                                    minQuantity = 0
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Discrepancy Option if items outstanding
                if (totalOutstanding > 0) {
                    TextButton(
                        onClick = { showForceCollectWarning = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = DangerRed,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Mark Bag as Collected despite $totalOutstanding missing items",
                            color = DangerRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // Dialog Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Button(
                        onClick = {
                            onSaveProgress(returnedMap.toMap(), false)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.weight(1.3f)
                    ) {
                        Text("Save Progress", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showForceCollectWarning) {
        ConfirmationDialog(
            title = "Confirm Collection with Discrepancy",
            message = "There are $totalOutstanding items still marked as missing. If you mark this bag as Collected, the discrepancy will be recorded in the bag's history. Do you want to proceed?",
            confirmButtonText = "Mark Collected",
            dismissButtonText = "Review Items",
            isDestructive = true,
            onConfirm = {
                showForceCollectWarning = false
                onSaveProgress(returnedMap.toMap(), true)
                onDismiss()
            },
            onDismiss = { showForceCollectWarning = false }
        )
    }
}
