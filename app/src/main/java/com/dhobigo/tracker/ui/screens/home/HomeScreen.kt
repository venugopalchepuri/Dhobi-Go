package com.dhobigo.tracker.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.LocalLaundryService
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dhobigo.tracker.ui.components.EmptyStateView
import com.dhobigo.tracker.ui.components.LaundryRecordCard
import com.dhobigo.tracker.ui.components.StatCard
import com.dhobigo.tracker.ui.theme.MintAccent
import com.dhobigo.tracker.ui.theme.MintSoftBg
import com.dhobigo.tracker.ui.theme.StatusCollectedBg
import com.dhobigo.tracker.ui.theme.StatusCollectedText
import com.dhobigo.tracker.ui.theme.StatusPartialBg
import com.dhobigo.tracker.ui.theme.StatusPartialText
import com.dhobigo.tracker.ui.theme.StatusPendingBg
import com.dhobigo.tracker.ui.theme.StatusPendingText
import com.dhobigo.tracker.util.DateUtils

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToAddLaundry: () -> Unit,
    onNavigateToRecordDetails: (String) -> Unit,
    onNavigateToHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val stats by viewModel.dashboardStats.collectAsStateWithLifecycle()
    val recentRecords by viewModel.recentRecords.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        // 1. Personalized Header
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        val firstName = currentUser?.fullName?.split(" ")?.firstOrNull() ?: "there"
                        Text(
                            text = "Hello, $firstName 👋",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = DateUtils.formatFullDate(System.currentTimeMillis()),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }

                    // Avatar or User Emblem
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = currentUser?.fullName?.take(1)?.uppercase() ?: "D",
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Optional College / University badge if present
                if (!currentUser?.college.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = currentUser?.college ?: "",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Prominent Add Laundry CTA
                Button(
                    onClick = onNavigateToAddLaundry,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Record New Laundry Bag",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // 2. Summary Stat Cards (Clean 2x2 grid)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = "At Dhobi",
                        value = "${stats.bagsAtDhobi}",
                        subtitle = "Active bags",
                        icon = Icons.Default.LocalLaundryService,
                        iconTint = MaterialTheme.colorScheme.primary,
                        iconBg = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.weight(1f)
                    )

                    StatCard(
                        title = "Awaiting",
                        value = "${stats.itemsAwaitingReturn}",
                        subtitle = "Clothes pending",
                        icon = Icons.Default.HourglassEmpty,
                        iconTint = StatusPendingText,
                        iconBg = StatusPendingBg,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = "Partially Returned",
                        value = "${stats.partiallyReturnedBags}",
                        subtitle = "Bags with items due",
                        icon = Icons.AutoMirrored.Filled.ReceiptLong,
                        iconTint = StatusPartialText,
                        iconBg = StatusPartialBg,
                        modifier = Modifier.weight(1f)
                    )

                    StatCard(
                        title = "Collected",
                        value = "${stats.totalCollectedBags}",
                        subtitle = "All-time cleared",
                        icon = Icons.Default.CheckCircleOutline,
                        iconTint = StatusCollectedText,
                        iconBg = StatusCollectedBg,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 3. Recent Records Section Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Laundry Slips",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                if (recentRecords.isNotEmpty()) {
                    Text(
                        text = "See All",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable(onClick = onNavigateToHistory)
                    )
                }
            }
        }

        // 4. Recent Records List or Helpful Empty State
        if (recentRecords.isEmpty()) {
            item {
                EmptyStateView(
                    icon = Icons.AutoMirrored.Filled.ReceiptLong,
                    title = "No Laundry Bags Yet",
                    description = "When you give clothes to your dhobi, tap 'Record New Laundry Bag' to photograph your paper chit and save quantities.",
                    actionButtonText = "Record First Bag",
                    onActionClick = onNavigateToAddLaundry
                )
            }
        } else {
            items(recentRecords, key = { it.record.id }) { recordWithItems ->
                Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                    LaundryRecordCard(
                        recordWithItems = recordWithItems,
                        onClick = { onNavigateToRecordDetails(recordWithItems.record.id) }
                    )
                }
            }
        }
    }
}
