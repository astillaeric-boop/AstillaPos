package com.example.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.CategorySalesCard
import com.example.ui.viewmodel.AnalyticsState
import com.example.ui.viewmodel.AnalyticsViewModel
import com.example.ui.viewmodel.ChartDataPoint
import com.example.ui.viewmodel.TimePeriod
import java.util.Locale
import androidx.compose.material3.ExperimentalMaterial3Api

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val state by viewModel.analyticsState.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val totalUtang by viewModel.totalOutstandingUtang.collectAsStateWithLifecycle()
    val currency = settings.currencySymbol

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
            .padding(bottom = 80.dp)
    ) {
        // Top Header with Title and Print Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Store Analytics",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Financial reporting & sales trends",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = { viewModel.printFullStoreReport(context) },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.testTag("print_report_button")
            ) {
                Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Print Report", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Time Period Filter Tabs
        PrimaryTabRow(
            selectedTabIndex = state.period.ordinal,
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.clip(RoundedCornerShape(12.dp))
        ) {
            TimePeriod.entries.forEach { period ->
                Tab(
                    selected = state.period == period,
                    onClick = { viewModel.setPeriod(period) },
                    text = { Text(period.displayName, fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                    modifier = Modifier.testTag("period_tab_${period.name.lowercase()}")
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Year-Over-Year (YoY) Comparison Widget
        YoYComparisonWidget(
            state = state,
            currency = currency
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Sales Trends Chart Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ShowChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${state.period.displayName} Sales Trend",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "${state.salesCount} Orders",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                SalesTrendChart(
                    points = state.chartPoints,
                    primaryColor = MaterialTheme.colorScheme.primary,
                    currency = currency
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Accounting Full Store Breakdown Card
        Text(
            text = "Accounting Summary (${state.period.displayName})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AccountingKpiCard(
                title = "Total Revenue / Gross Sales",
                value = "$currency${String.format(Locale.US, "%.2f", state.totalRevenue)}",
                subtitle = "Cash: $currency${String.format(Locale.US, "%.0f", state.cashSales)} • Utang: $currency${String.format(Locale.US, "%.0f", state.creditSales)}",
                accentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .weight(1f)
                    .testTag("kpi_gross_sales")
            )

            AccountingKpiCard(
                title = "Cash Flow / Cash in Hand",
                value = "$currency${String.format(Locale.US, "%.2f", state.cashCollected)}",
                subtitle = "Cash Sales + Utang Bayad",
                accentColor = Color(0xFF0284C7),
                modifier = Modifier
                    .weight(1f)
                    .testTag("kpi_cash_collected")
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AccountingKpiCard(
                title = "Cost of Goods (COGS)",
                value = "$currency${String.format(Locale.US, "%.2f", state.totalCogs)}",
                subtitle = "Total Product Cost",
                accentColor = Color(0xFFE11D48),
                modifier = Modifier
                    .weight(1f)
                    .testTag("kpi_cogs")
            )

            AccountingKpiCard(
                title = "Net Profit",
                value = "$currency${String.format(Locale.US, "%.2f", state.netProfit)}",
                subtitle = "Revenue - COGS",
                accentColor = Color(0xFF10B981),
                modifier = Modifier
                    .weight(1f)
                    .testTag("kpi_net_profit")
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AccountingKpiCard(
                title = "Total Outstanding Utang",
                value = "$currency${String.format(Locale.US, "%.2f", state.totalOutstandingUtang)}",
                subtitle = "Unpaid Customer Receivables",
                accentColor = Color(0xFF8B5CF6),
                modifier = Modifier
                    .weight(1f)
                    .testTag("kpi_outstanding_utang")
            )

            AccountingKpiCard(
                title = "Sales Tax Collected",
                value = "$currency${String.format(Locale.US, "%.2f", state.totalTax)}",
                subtitle = "${state.salesCount} Sales Orders",
                accentColor = Color(0xFFF59E0B),
                modifier = Modifier
                    .weight(1f)
                    .testTag("kpi_tax_collected")
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AccountingKpiCard(
                title = "Current Inventory Value",
                value = "$currency${String.format(Locale.US, "%.2f", state.totalInventoryValue)}",
                subtitle = "Total Stock Asset Valuation (Cost)",
                accentColor = Color(0xFF0D9488),
                modifier = Modifier
                    .weight(1f)
                    .testTag("kpi_inventory_value")
            )

            AccountingKpiCard(
                title = "Total Supplier Restock",
                value = "$currency${String.format(Locale.US, "%.2f", state.totalDeliveryCost)}",
                subtitle = "${state.deliveryCount} Deliveries Logged",
                accentColor = Color(0xFF6366F1),
                modifier = Modifier
                    .weight(1f)
                    .testTag("kpi_supplier_restock")
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Deliveries & Restock Report Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocalShipping,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Deliveries & Restock Expenditure",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total Restock Deliveries:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${state.deliveryCount} Shipments", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total Supplier Purchase Cost:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "$currency${String.format(Locale.US, "%.2f", state.totalDeliveryCost)}",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFDC2626)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Sales by Category Breakdown Card
        CategorySalesCard(
            categorySales = state.categorySales,
            currency = currency,
            periodLabel = state.period.displayName
        )

        // Top Selling Leaderboard
        if (state.topSellingItems.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Top Selling Products (${state.period.displayName})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    state.topSellingItems.forEachIndexed { idx, item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("${idx + 1}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(item.first, fontWeight = FontWeight.Medium)
                            }
                            Text("${item.second} sold", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun YoYComparisonWidget(
    state: AnalyticsState,
    currency: String
) {
    val isPositive = state.yoyDelta >= 0

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPositive) Color(0xFFF0FDF4) else Color(0xFFFEF2F2)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = if (isPositive) Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isPositive) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                        contentDescription = null,
                        tint = if (isPositive) Color(0xFF15803D) else Color(0xFFDC2626)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Year-Over-Year Comparison",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF334155)
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${if (isPositive) "+" else ""}${String.format(Locale.US, "%.1f", state.yoyPercentage)}%",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isPositive) Color(0xFF15803D) else Color(0xFFDC2626)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "(${if (isPositive) "+" else ""}$currency${String.format(Locale.US, "%.2f", state.yoyDelta)})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF64748B)
                    )
                }

                Text(
                    text = "vs. $currency${String.format(Locale.US, "%.2f", state.lastYearRevenue)} same period last year",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B)
                )
            }
        }
    }
}

@Composable
fun AccountingKpiCard(
    title: String,
    value: String,
    subtitle: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = accentColor)
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), fontSize = 10.sp)
        }
    }
}

@Composable
fun SalesTrendChart(
    points: List<ChartDataPoint>,
    primaryColor: Color,
    currency: String
) {
    val maxValue = points.maxOfOrNull { it.value } ?: 1f
    val effectiveMax = if (maxValue <= 0f) 100f else maxValue * 1.15f

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val count = points.size

                if (count == 0) return@Canvas

                val barWidth = (w / count) * 0.55f
                val spacing = w / count

                // Draw subtle grid lines
                drawLine(
                    color = Color.LightGray.copy(alpha = 0.3f),
                    start = Offset(0f, h * 0.25f),
                    end = Offset(w, h * 0.25f),
                    strokeWidth = 1.dp.toPx()
                )
                drawLine(
                    color = Color.LightGray.copy(alpha = 0.3f),
                    start = Offset(0f, h * 0.75f),
                    end = Offset(w, h * 0.75f),
                    strokeWidth = 1.dp.toPx()
                )

                // Draw rounded bars
                for (i in 0 until count) {
                    val p = points[i]
                    val fraction = (p.value / effectiveMax).coerceIn(0f, 1f)
                    val barHeight = (h * fraction).coerceAtLeast(4.dp.toPx())
                    val x = (i * spacing) + (spacing - barWidth) / 2f
                    val y = h - barHeight

                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(primaryColor, primaryColor.copy(alpha = 0.5f)),
                            startY = y,
                            endY = h
                        ),
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )
                }
            }
        }

        // X-Axis Labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            points.forEach { point ->
                Text(
                    text = point.label,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
