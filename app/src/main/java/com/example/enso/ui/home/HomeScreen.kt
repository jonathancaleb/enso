package com.example.enso.ui.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.enso.data.local.entity.TransactionEntity
import com.example.enso.data.local.entity.TransactionType
import com.example.enso.ui.theme.EnsoGreen
import com.example.enso.util.DateUtils

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onSyncClick: () -> Unit,
    onSeeAllClick: () -> Unit
) {
    val spentToday by viewModel.spentToday.collectAsState()
    val spentWeek by viewModel.spentThisWeek.collectAsState()
    val spentMonth by viewModel.spentThisMonth.collectAsState()
    val receivedMonth by viewModel.receivedThisMonth.collectAsState()
    val recentTxns by viewModel.recentTransactions.collectAsState()
    val txnCount by viewModel.transactionCount.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val syncResult by viewModel.syncResult.collectAsState()
    val providerFilter by viewModel.providerFilter.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(syncResult) {
        syncResult?.let {
            val msg = if (it.count > 0) "${it.count} new transactions synced"
                else "Already up to date"
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissSyncResult()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "enso", style = MaterialTheme.typography.headlineLarge)
                    Text(
                        text = "$txnCount transactions synced",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                SyncButton(isSyncing = isSyncing, onClick = onSyncClick)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Provider filter
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ProviderFilter.entries.forEach { f ->
                    FilterChip(
                        selected = providerFilter == f,
                        onClick = { viewModel.setProviderFilter(f) },
                        label = { Text(f.label, style = MaterialTheme.typography.labelMedium) },
                        shape = RoundedCornerShape(20.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            containerColor = MaterialTheme.colorScheme.surface,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "This Month",
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "UGX ${formatAmount(spentMonth)}",
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.displayLarge,
                        fontSize = 32.sp
                    )
                    Text(
                        text = "spent",
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Received",
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                text = "UGX ${formatAmount(receivedMonth)}",
                                color = MaterialTheme.colorScheme.onPrimary,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Net",
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                text = "UGX ${formatAmount(receivedMonth - spentMonth)}",
                                color = MaterialTheme.colorScheme.onPrimary,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(label = "Today", amount = spentToday, modifier = Modifier.weight(1f))
                StatCard(label = "This Week", amount = spentWeek, modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(28.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Recent", style = MaterialTheme.typography.titleLarge)
                TextButton(onClick = onSeeAllClick) {
                    Text(
                        "See all",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (recentTxns.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "No transactions yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Tap sync to import from SMS", style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column {
                        recentTxns.forEachIndexed { index, txn ->
                            RecentTransactionRow(txn)
                            if (index < recentTxns.lastIndex) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                                        .height(1.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
        )
    }
}

@Composable
private fun SyncButton(isSyncing: Boolean, onClick: () -> Unit) {
    if (isSyncing) {
        val infiniteTransition = rememberInfiniteTransition(label = "sync")
        val rotation by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "syncRotation"
        )
        IconButton(onClick = {}, enabled = false) {
            Icon(
                Icons.Default.Sync,
                contentDescription = "Syncing",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.rotate(rotation)
            )
        }
    } else {
        IconButton(onClick = onClick) {
            Icon(
                Icons.Default.Sync,
                contentDescription = "Sync SMS",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun StatCard(label: String, amount: Double, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = label, style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "UGX ${formatAmount(amount)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun RecentTransactionRow(txn: TransactionEntity) {
    val isIncoming = txn.type in TransactionType.incomingTypes
    val iconBg = if (isIncoming) EnsoGreen.copy(alpha = 0.1f)
        else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
    val iconTint = if (isIncoming) EnsoGreen else MaterialTheme.colorScheme.primary
    val sign = if (isIncoming) "+" else "-"
    val amountColor = if (isIncoming) EnsoGreen else MaterialTheme.colorScheme.onBackground

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape).background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (isIncoming) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = txn.description, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
            Text(text = DateUtils.formatDateShort(txn.date), style = MaterialTheme.typography.bodySmall)
        }
        Text(
            text = "$sign UGX ${formatAmount(txn.amount)}",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold, color = amountColor
        )
    }
}

private fun formatAmount(amount: Double): String {
    val abs = kotlin.math.abs(amount)
    return String.format("%,.0f", abs)
}
