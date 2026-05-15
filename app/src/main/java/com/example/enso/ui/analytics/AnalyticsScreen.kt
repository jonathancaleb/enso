package com.example.enso.ui.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.enso.data.local.TypeTotal
import com.example.enso.data.local.entity.TransactionType

private val chartColors = listOf(
    Color(0xFFD97757), Color(0xFF6B9F78), Color(0xFF5B8CB8), Color(0xFFD4A853),
    Color(0xFF9B6BB0), Color(0xFFD96B8C), Color(0xFF5BBCB8), Color(0xFFB87850),
    Color(0xFF7B8BA0), Color(0xFFC4704E), Color(0xFF6BAF8F), Color(0xFF8B7BC4),
    Color(0xFFCCA040), Color(0xFFA0607A),
)

@Composable
fun AnalyticsScreen(viewModel: AnalyticsViewModel) {
    val period by viewModel.period.collectAsState()
    val providerFilter by viewModel.providerFilter.collectAsState()
    val breakdown by viewModel.breakdown.collectAsState()
    val totalSpent by viewModel.totalSpent.collectAsState()
    val totalReceived by viewModel.totalReceived.collectAsState()

    val incomingTypes = TransactionType.incomingTypes.toSet()
    val spendingBreakdown = breakdown.filter { it.type !in incomingTypes }
    val totalOfBreakdown = spendingBreakdown.sumOf { it.total }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Analytics", style = MaterialTheme.typography.headlineLarge)

        Spacer(modifier = Modifier.height(16.dp))

        // Provider filter
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AnalyticsProviderFilter.entries.forEach { f ->
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

        Spacer(modifier = Modifier.height(12.dp))

        // Time period chips — scrollable
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TimePeriod.entries.forEach { p ->
                FilterChip(
                    selected = period == p,
                    onClick = { viewModel.setPeriod(p) },
                    label = { Text(p.label, style = MaterialTheme.typography.labelMedium) },
                    shape = RoundedCornerShape(20.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.onBackground,
                        selectedLabelColor = MaterialTheme.colorScheme.background,
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SummaryMiniCard(
                label = "Spent", amount = totalSpent,
                color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f)
            )
            SummaryMiniCard(
                label = "Received", amount = totalReceived,
                color = Color(0xFF6B9F78), modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        if (spendingBreakdown.isNotEmpty()) {
            Text(text = "Spending Breakdown", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    DonutChart(
                        items = spendingBreakdown,
                        total = totalOfBreakdown,
                        modifier = Modifier.size(200.dp)
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    spendingBreakdown.forEachIndexed { index, item ->
                        val pct = if (totalOfBreakdown > 0) (item.total / totalOfBreakdown * 100) else 0.0
                        LegendRow(
                            color = chartColors[index % chartColors.size],
                            label = item.type.displayName,
                            amount = item.total, percentage = pct
                        )
                        if (index < spendingBreakdown.lastIndex) {
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No spending data for this period", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SummaryMiniCard(label: String, amount: Double, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
                Spacer(modifier = Modifier.width(8.dp))
                Text(label, style = MaterialTheme.typography.labelMedium)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "UGX ${String.format("%,.0f", amount)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun DonutChart(items: List<TypeTotal>, total: Double, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val strokeWidth = 36.dp.toPx()
        val radius = (size.minDimension - strokeWidth) / 2
        val topLeft = Offset((size.width - radius * 2) / 2, (size.height - radius * 2) / 2)
        val arcSize = Size(radius * 2, radius * 2)
        var startAngle = -90f
        if (total <= 0) return@Canvas
        items.forEachIndexed { index, item ->
            val sweep = (item.total / total * 360f).toFloat()
            drawArc(
                color = chartColors[index % chartColors.size],
                startAngle = startAngle, sweepAngle = sweep.coerceAtLeast(1f),
                useCenter = false, topLeft = topLeft, size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
            )
            startAngle += sweep
        }
    }
}

@Composable
private fun LegendRow(color: Color, label: String, amount: Double, percentage: Double) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
            Spacer(modifier = Modifier.width(10.dp))
            Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground)
        }
        Text(
            text = "${String.format("%,.0f", amount)} (${String.format("%.0f", percentage)}%)",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
