package `in`.mahato.bytetools.ui.tools.quickorder.ui

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import `in`.mahato.bytetools.ui.tools.quickorder.domain.engine.SmartSplitEngine
import `in`.mahato.bytetools.ui.tools.quickorder.domain.model.ParticipantSettlement
import `in`.mahato.bytetools.ui.tools.quickorder.domain.model.SplitMethod

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettlementDashboardScreen(
    orderId: Long,
    navController: NavController,
    viewModel: QuickOrderViewModel
) {
    LaunchedEffect(orderId) {
        viewModel.selectOrder(orderId)
    }

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val order by viewModel.activeOrder.collectAsState()
    val summary by viewModel.billSummary.collectAsState()
    val settlements by viewModel.settlements.collectAsState()
    val participants by viewModel.participants.collectAsState()
    val orderItems by viewModel.orderItems.collectAsState()
    val consolidatedItems by viewModel.consolidatedItems.collectAsState()
    val billItems by viewModel.billItems.collectAsState()
    val payments by viewModel.payments.collectAsState()

    var showPdfExportSheet by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    fun shareParticipantBreakdown(settlement: ParticipantSettlement) {
        val ord = order ?: return
        val text = SmartSplitEngine.generateParticipantShareText(ord, settlement)
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(sendIntent, "Share with ${settlement.participantName}"))
    }

    fun shareAll() {
        val ord = order ?: return
        val sum = summary ?: return
        val text = SmartSplitEngine.generateAllSettlementShareText(ord, sum, settlements)
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(sendIntent, "Share All Settlement Summaries"))
    }

    fun exportCsv() {
        val ord = order ?: return
        val sum = summary ?: return
        val csv = SmartSplitEngine.generateCsvExport(ord, sum, settlements)
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, csv)
            type = "text/comma-separated-values"
        }
        context.startActivity(Intent.createChooser(sendIntent, "Export CSV Settlement"))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(order?.orderName ?: "Settlement", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Step 6: Final Cost Allocation",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showPdfExportSheet = true }) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF Reports", tint = PurplePrimary)
                    }
                    IconButton(onClick = { shareAll() }) {
                        Icon(Icons.Default.Share, contentDescription = "Share All")
                    }
                    IconButton(onClick = { exportCsv() }) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Export CSV")
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More Options")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("View Shopkeeper Bill") },
                                leadingIcon = { Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = PurplePrimary) },
                                onClick = {
                                    showMenu = false
                                    navController.navigate("enter_bill/$orderId")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("View Reconciliation") },
                                leadingIcon = { Icon(Icons.Default.FactCheck, contentDescription = null, tint = PurplePrimary) },
                                onClick = {
                                    showMenu = false
                                    navController.navigate("reconciliation/$orderId")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("View Consolidated List") },
                                leadingIcon = { Icon(Icons.Default.ListAlt, contentDescription = null, tint = PurplePrimary) },
                                onClick = {
                                    showMenu = false
                                    navController.navigate("consolidated_list/$orderId")
                                }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Exit to Home Screen") },
                                leadingIcon = { Icon(Icons.Default.Home, contentDescription = null, tint = PurplePrimary) },
                                onClick = {
                                    showMenu = false
                                    navController.navigate("home") {
                                        popUpTo("home") { inclusive = false }
                                        launchSingleTop = true
                                    }
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showPdfExportSheet = true },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = PurplePrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("PDF Reports", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { shareAll() },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.weight(0.9f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share", fontSize = 12.sp)
                    }

                    Button(
                        onClick = { navController.navigate("payment_tracking/$orderId") },
                        colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1.2f)
                    ) {
                        Text("Payments", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            OrderStepIndicator(
                currentStep = 5,
                onStepClick = { step ->
                    when (step) {
                        1 -> navController.navigate("collect_orders/$orderId")
                        2 -> navController.navigate("consolidated_list/$orderId")
                        3 -> navController.navigate("enter_bill/$orderId")
                        4 -> navController.navigate("reconciliation/$orderId")
                        5 -> { /* current screen */ }
                        6 -> navController.navigate("payment_tracking/$orderId")
                    }
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            val curr = order?.currency ?: "₹"
            val totalBill = summary?.finalBillAmountPaise ?: 0L
            val totalAllocated = settlements.sumOf { it.finalPayablePaise }
            val unallocated = totalBill - totalAllocated

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Grand Summary Card
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Final Settlement Overview",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (unallocated == 0L) GreenBackground else AmberBackground
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            if (unallocated == 0L) Icons.Default.CheckCircle else Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = if (unallocated == 0L) GreenSuccess else AmberWarning,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            if (unallocated == 0L) "100% Balanced" else "Diff: ${SmartSplitEngine.formatPaise(unallocated, curr)}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (unallocated == 0L) GreenSuccess else AmberWarning
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Original Items Subtotal:", style = MaterialTheme.typography.bodyMedium)
                                Text(SmartSplitEngine.formatPaise(summary?.subtotalPaise ?: 0L, curr), fontWeight = FontWeight.Bold)
                            }

                            val overallDisc = summary?.overallDiscountPaise ?: 0L
                            if (overallDisc > 0L) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Total Discount Savings:", style = MaterialTheme.typography.bodyMedium, color = GreenSuccess)
                                    Text("-${SmartSplitEngine.formatPaise(overallDisc, curr)}", fontWeight = FontWeight.Bold, color = GreenSuccess)
                                }
                            }

                            val taxes = summary?.taxPaise ?: 0L
                            if (taxes > 0L) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Taxes & GST:", style = MaterialTheme.typography.bodyMedium)
                                    Text("+${SmartSplitEngine.formatPaise(taxes, curr)}", fontWeight = FontWeight.Bold)
                                }
                            }

                            val charges = summary?.additionalChargesPaise ?: 0L
                            if (charges > 0L) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Extra Charges (${summary?.additionalChargesNote.orEmpty().ifBlank { "Packaging" }}):", style = MaterialTheme.typography.bodyMedium)
                                    Text("+${SmartSplitEngine.formatPaise(charges, curr)}", fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Grand Total Payable:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    SmartSplitEngine.formatPaise(totalBill, curr),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = PurplePrimary
                                )
                            }
                        }
                    }
                }

                // Audit & Records (View Only)
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Audit & Records",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = PurpleBackground
                                ) {
                                    Text(
                                        "VIEW ONLY",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PurplePrimary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    onClick = { navController.navigate("enter_bill/$orderId") },
                                    shape = RoundedCornerShape(12.dp),
                                    color = PurpleBackground,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = PurplePrimary, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Shop Bill", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = PurpleDark)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Inspect purchased items, prices & discounts", fontSize = 11.sp, color = Color(0xFF4A148C), lineHeight = 14.sp)
                                    }
                                }

                                Surface(
                                    onClick = { navController.navigate("reconciliation/$orderId") },
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFE0F2F1),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.FactCheck, contentDescription = null, tint = Color(0xFF00796B), modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Reconciliation", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF004D40))
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Review member quantity allocations", fontSize = 11.sp, color = Color(0xFF004D40), lineHeight = 14.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                // Discount Allocation Strategy Selector
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                "Discount Split Method",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = summary?.discountSplitMethod == SplitMethod.PROPORTIONAL,
                                    onClick = { viewModel.updateSplitMethod(orderId, SplitMethod.PROPORTIONAL) },
                                    label = { Text("Proportional (Fair)") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = PurplePrimary,
                                        selectedLabelColor = Color.White
                                    )
                                )
                                FilterChip(
                                    selected = summary?.discountSplitMethod == SplitMethod.EQUAL,
                                    onClick = { viewModel.updateSplitMethod(orderId, SplitMethod.EQUAL) },
                                    label = { Text("Equal Discount") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = PurplePrimary,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                }

                // Participant Breakdown Header
                item {
                    Text(
                        "Individual Payables (${settlements.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Participant Cards
                items(settlements) { s ->
                    ParticipantSettlementCard(
                        settlement = s,
                        currency = curr,
                        onShare = { shareParticipantBreakdown(s) },
                        onCopy = {
                            val ord = order ?: return@ParticipantSettlementCard
                            val text = SmartSplitEngine.generateParticipantShareText(ord, s)
                            clipboardManager.setText(AnnotatedString(text))
                            android.widget.Toast.makeText(context, "Copied breakdown for ${s.participantName}!", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }

    if (showPdfExportSheet && order != null) {
        QuickOrderPdfExportBottomSheet(
            order = order!!,
            summary = summary,
            settlements = settlements,
            participants = participants,
            orderItems = orderItems,
            consolidatedItems = consolidatedItems,
            billItems = billItems,
            payments = payments,
            navController = navController,
            onDismiss = { showPdfExportSheet = false }
        )
    }
}

@Composable
fun ParticipantSettlementCard(
    settlement: ParticipantSettlement,
    currency: String,
    onShare: () -> Unit,
    onCopy: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (settlement.isOrganizer) PurpleBackground else Color(0xFFF1F5F9)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (settlement.isOrganizer) {
                            Icon(Icons.Default.Star, contentDescription = "Organizer", tint = PurplePrimary, modifier = Modifier.size(20.dp))
                        } else {
                            Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            settlement.participantName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${settlement.items.size} products ordered",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        SmartSplitEngine.formatPaise(settlement.finalPayablePaise, currency),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = PurplePrimary
                    )
                    val totalSavings = settlement.itemDiscountPaise + settlement.overallDiscountSharePaise
                    if (totalSavings > 0L) {
                        Text(
                            "Save ${SmartSplitEngine.formatPaise(totalSavings, currency)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = GreenSuccess,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons Row (Share on WhatsApp, Copy)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PaymentStatusBadge(settlement.paymentStatus)

                Row {
                    IconButton(onClick = onCopy, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onShare, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Share, contentDescription = "Share WhatsApp", tint = GreenSuccess, modifier = Modifier.size(16.dp))
                    }
                }
            }

            // Expandable breakdown
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    Text(
                        "Itemized Calculation:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    settlement.items.forEach { item ->
                        val qtyStr = if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else String.format("%.1f", item.quantity)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "• ${item.productName} ($qtyStr ${item.unit} @ ${SmartSplitEngine.formatPaise(item.unitPricePaise, currency)})",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                SmartSplitEngine.formatPaise(item.netAmountPaise, currency),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Subtotal:", style = MaterialTheme.typography.labelSmall)
                        Text(SmartSplitEngine.formatPaise(settlement.originalSubtotalPaise, currency), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }

                    val totalDisc = settlement.itemDiscountPaise + settlement.overallDiscountSharePaise
                    if (totalDisc > 0L) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Discount Share:", style = MaterialTheme.typography.labelSmall, color = GreenSuccess)
                            Text("-${SmartSplitEngine.formatPaise(totalDisc, currency)}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = GreenSuccess)
                        }
                    }

                    val extra = settlement.taxSharePaise + settlement.additionalChargesSharePaise
                    if (extra > 0L) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Taxes & Charges:", style = MaterialTheme.typography.labelSmall)
                            Text("+${SmartSplitEngine.formatPaise(extra, currency)}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
