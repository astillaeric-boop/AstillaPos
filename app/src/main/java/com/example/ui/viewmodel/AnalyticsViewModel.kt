package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.dao.CategorySalesSummary
import com.example.data.local.entity.AppSettingsEntity
import com.example.data.local.entity.CreditTransactionEntity
import com.example.data.local.entity.DeliveryEntity
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.SaleWithItems
import com.example.data.repository.PosRepository
import com.example.util.PrinterManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

enum class TimePeriod(val displayName: String) {
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    YEARLY("Yearly")
}

data class ChartDataPoint(
    val label: String,
    val value: Float
)

data class AnalyticsState(
    val period: TimePeriod = TimePeriod.DAILY,
    val totalRevenue: Double = 0.0,            // Gross Sales (Cash Sales + Credit Sales)
    val cashSales: Double = 0.0,               // Cash, GCash, Maya, Bank sales
    val creditSales: Double = 0.0,             // Credit (Utang) sales
    val cashCollected: Double = 0.0,           // Actual Cash Flow: Cash Sales + Utang Payments Received
    val utangPaymentsCollected: Double = 0.0,   // "Bayad Utang" collected in this period
    val totalCogs: Double = 0.0,               // Cost of Goods Sold (purchase cost of all items sold/borrowed)
    val netProfit: Double = 0.0,               // Total Revenue - COGS
    val grossProfit: Double = 0.0,             // Total Revenue - totalTax - totalCogs (retained for backward compatibility)
    val totalTax: Double = 0.0,
    val totalOutstandingUtang: Double = 0.0,   // Active customer unpaid balances
    val salesCount: Int = 0,
    val lastYearRevenue: Double = 0.0,
    val yoyDelta: Double = 0.0,
    val yoyPercentage: Double = 0.0,
    val totalDeliveryCost: Double = 0.0,
    val deliveryCount: Int = 0,
    val chartPoints: List<ChartDataPoint> = emptyList(),
    val topSellingItems: List<Pair<String, Int>> = emptyList(),
    val categorySales: List<CategorySalesSummary> = emptyList(),
    val totalInventoryValue: Double = 0.0
)

class AnalyticsViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    val repository = PosRepository(database)

    val settings: StateFlow<AppSettingsEntity> = repository.appSettings
        .map { it ?: AppSettingsEntity() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppSettingsEntity()
        )

    val totalOutstandingUtang: StateFlow<Double> = repository.totalOutstandingCredit
        .map { it ?: 0.0 }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.0
        )

    private val _selectedPeriod = MutableStateFlow(TimePeriod.DAILY)
    val selectedPeriod: StateFlow<TimePeriod> = _selectedPeriod.asStateFlow()

    val analyticsState: StateFlow<AnalyticsState> = combine(
        combine(repository.allSales, repository.allDeliveries, repository.allCreditTransactions) { sales, del, credit ->
            Triple(sales, del, credit)
        },
        repository.totalOutstandingCredit,
        _selectedPeriod,
        repository.allProducts
    ) { (allSales, allDeliveries, allCreditTxs), totalUtang, period, allProducts ->
        computeAnalytics(allSales, allDeliveries, allCreditTxs, totalUtang ?: 0.0, period, allProducts)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AnalyticsState()
    )

    val deliveries: StateFlow<List<DeliveryEntity>> = repository.allDeliveries
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        viewModelScope.launch {
            // Automatically delete receipts older than 6 months for compliance & storage optimization
            repository.cleanupOldReceipts()
        }
    }

    fun setPeriod(period: TimePeriod) {
        _selectedPeriod.value = period
    }

    private fun computeAnalytics(
        allSales: List<SaleWithItems>,
        allDeliveries: List<DeliveryEntity>,
        allCreditTransactions: List<CreditTransactionEntity>,
        totalOutstandingUtang: Double,
        period: TimePeriod,
        allProducts: List<ProductEntity> = emptyList()
    ): AnalyticsState {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()

        val (startTime, endTime, lastYearStart, lastYearEnd) = when (period) {
            TimePeriod.DAILY -> {
                calendar.timeInMillis = now
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val start = calendar.timeInMillis
                val end = now

                calendar.add(Calendar.YEAR, -1)
                val lyStart = calendar.timeInMillis
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                val lyEnd = calendar.timeInMillis
                listOf(start, end, lyStart, lyEnd)
            }
            TimePeriod.WEEKLY -> {
                val start = now - 7L * 24 * 60 * 60 * 1000
                val end = now
                val lyStart = start - 365L * 24 * 60 * 60 * 1000
                val lyEnd = end - 365L * 24 * 60 * 60 * 1000
                listOf(start, end, lyStart, lyEnd)
            }
            TimePeriod.MONTHLY -> {
                val start = now - 30L * 24 * 60 * 60 * 1000
                val end = now
                val lyStart = start - 365L * 24 * 60 * 60 * 1000
                val lyEnd = end - 365L * 24 * 60 * 60 * 1000
                listOf(start, end, lyStart, lyEnd)
            }
            TimePeriod.YEARLY -> {
                val start = now - 365L * 24 * 60 * 60 * 1000
                val end = now
                val lyStart = start - 365L * 24 * 60 * 60 * 1000
                val lyEnd = end - 365L * 24 * 60 * 60 * 1000
                listOf(start, end, lyStart, lyEnd)
            }
        }

        val periodSales = allSales.filter { it.sale.timestamp in startTime..endTime }
        val lastYearSales = allSales.filter { it.sale.timestamp in lastYearStart..lastYearEnd }
        val periodCreditTxs = allCreditTransactions.filter { it.timestamp in startTime..endTime }

        // Cash Sales: Tenders paid immediately (Cash, GCash, Maya, Bank)
        val cashSales = periodSales.filter {
            !it.sale.paymentType.contains("Utang", ignoreCase = true) &&
                !it.sale.paymentType.contains("Credit", ignoreCase = true)
        }.sumOf { it.sale.totalAmount }

        // Credit Sales: Goods lent to customers on Utang (Accrual basis: recorded on date items leave inventory)
        val creditSales = periodSales.filter {
            it.sale.paymentType.contains("Utang", ignoreCase = true) ||
                it.sale.paymentType.contains("Credit", ignoreCase = true)
        }.sumOf { it.sale.totalAmount }

        // Gross Sales (Total Revenue): Cash Sales + Credit (Utang) Sales
        val totalRevenue = cashSales + creditSales

        // Utang Payments Received in this period ("Bayad Utang" logged as CUSTOMER_PAYMENT / PAYMENT)
        val utangPayments = periodCreditTxs.filter {
            it.transactionType.contains("PAYMENT", ignoreCase = true)
        }.sumOf { it.amount }

        // Cash Collected (Actual Cash Flow): Cash Sales + Utang Payments Received
        val cashCollected = cashSales + utangPayments

        val totalTax = periodSales.sumOf { it.sale.taxAmount }

        var totalCogs = 0.0
        val itemCounts = mutableMapOf<String, Int>()
        for (sale in periodSales) {
            for (item in sale.items) {
                totalCogs += (item.unitCost * item.quantity)
                itemCounts[item.productName] = (itemCounts[item.productName] ?: 0) + item.quantity
            }
        }

        // Net Profit = Total Revenue - COGS
        val netProfit = totalRevenue - totalCogs
        val grossProfit = totalRevenue - totalTax - totalCogs
        val lastYearRevenue = lastYearSales.sumOf { it.sale.totalAmount }
        val yoyDelta = totalRevenue - lastYearRevenue
        val yoyPercentage = if (lastYearRevenue > 0) {
            ((totalRevenue - lastYearRevenue) / lastYearRevenue) * 100.0
        } else if (totalRevenue > 0) {
            100.0
        } else {
            0.0
        }

        val periodDeliveries = allDeliveries.filter { it.timestamp in startTime..endTime }
        val totalDeliveryCost = periodDeliveries.sumOf { it.totalCost }

        // Generate chart points based on period
        val chartPoints = mutableListOf<ChartDataPoint>()
        when (period) {
            TimePeriod.DAILY -> {
                val intervals = 6
                val step = (endTime - startTime) / intervals
                for (i in 0 until intervals) {
                    val s = startTime + (i * step)
                    val e = s + step
                    val bucketRevenue = periodSales.filter { it.sale.timestamp in s..e }
                        .sumOf { it.sale.totalAmount }.toFloat()
                    val hr = (i * 4)
                    chartPoints.add(ChartDataPoint(label = "${hr}h", value = bucketRevenue))
                }
            }
            TimePeriod.WEEKLY -> {
                val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                for (i in 6 downTo 0) {
                    val dayStart = now - ((i + 1) * 24L * 60 * 60 * 1000)
                    val dayEnd = now - (i * 24L * 60 * 60 * 1000)
                    val bucketRevenue = periodSales.filter { it.sale.timestamp in dayStart..dayEnd }
                        .sumOf { it.sale.totalAmount }.toFloat()
                    val dayName = days[(6 - i) % 7]
                    chartPoints.add(ChartDataPoint(label = dayName, value = bucketRevenue))
                }
            }
            TimePeriod.MONTHLY -> {
                for (week in 1..4) {
                    val wStart = startTime + ((week - 1) * 7L * 24 * 60 * 60 * 1000)
                    val wEnd = wStart + (7L * 24 * 60 * 60 * 1000)
                    val bucketRevenue = periodSales.filter { it.sale.timestamp in wStart..wEnd }
                        .sumOf { it.sale.totalAmount }.toFloat()
                    chartPoints.add(ChartDataPoint(label = "W$week", value = bucketRevenue))
                }
            }
            TimePeriod.YEARLY -> {
                val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                for (i in 0..11) {
                    val mStart = startTime + (i * 30L * 24 * 60 * 60 * 1000)
                    val mEnd = mStart + (30L * 24 * 60 * 60 * 1000)
                    val bucketRevenue = periodSales.filter { it.sale.timestamp in mStart..mEnd }
                        .sumOf { it.sale.totalAmount }.toFloat()
                    chartPoints.add(ChartDataPoint(label = months[i], value = bucketRevenue))
                }
            }
        }

        val topSellingItems = itemCounts.toList()
            .sortedByDescending { it.second }
            .take(5)

        // Compute Category-based Sales Breakdown
        val productCategoryMap = allProducts.associate { it.id to it.category.ifBlank { "General" } }
        val categoryStats = mutableMapOf<String, Pair<Int, Double>>() // category -> (unitsSold, totalRevenue)
        for (sale in periodSales) {
            for (item in sale.items) {
                val cat = productCategoryMap[item.productId] ?: "General"
                val existing = categoryStats[cat] ?: Pair(0, 0.0)
                categoryStats[cat] = Pair(
                    existing.first + item.quantity,
                    existing.second + (item.unitPrice * item.quantity)
                )
            }
        }
        val categorySales = categoryStats.map { (catName, stats) ->
            CategorySalesSummary(
                category = catName,
                unitsSold = stats.first,
                totalRevenue = stats.second
            )
        }.sortedByDescending { it.totalRevenue }

        // Total Current Inventory Valuation (Asset Value = SUM(stockQuantity * costPrice) for all stockQuantity > 0)
        val totalInventoryValue = allProducts.filter { it.stockQuantity > 0 }
            .sumOf { it.stockQuantity * it.costPrice }

        return AnalyticsState(
            period = period,
            totalRevenue = totalRevenue,
            cashSales = cashSales,
            creditSales = creditSales,
            cashCollected = cashCollected,
            utangPaymentsCollected = utangPayments,
            totalCogs = totalCogs,
            netProfit = netProfit,
            grossProfit = grossProfit,
            totalTax = totalTax,
            totalOutstandingUtang = totalOutstandingUtang,
            salesCount = periodSales.size,
            lastYearRevenue = lastYearRevenue,
            yoyDelta = yoyDelta,
            yoyPercentage = yoyPercentage,
            totalDeliveryCost = totalDeliveryCost,
            deliveryCount = periodDeliveries.size,
            chartPoints = chartPoints,
            topSellingItems = topSellingItems,
            categorySales = categorySales,
            totalInventoryValue = totalInventoryValue
        )
    }

    fun printFullStoreReport(context: Context) {
        val state = analyticsState.value
        val currentSettings = settings.value

        val html = PrinterManager.generateStoreReportHtml(
            settings = currentSettings,
            periodName = state.period.displayName,
            totalRevenue = state.totalRevenue,
            totalCogs = state.totalCogs,
            grossProfit = state.grossProfit,
            totalTax = state.totalTax,
            totalSalesCount = state.salesCount,
            totalRestockExpenditure = state.totalDeliveryCost,
            topSellingItems = state.topSellingItems,
            cashCollected = state.cashCollected,
            cashSales = state.cashSales,
            creditSales = state.creditSales,
            utangPaymentsCollected = state.utangPaymentsCollected,
            netProfit = state.netProfit,
            totalOutstandingUtang = state.totalOutstandingUtang
        )

        PrinterManager.printHtmlDocument(
            context = context,
            jobName = "AstillaPOS_Accounting_Report_${state.period.displayName}",
            htmlContent = html
        )
    }
}
