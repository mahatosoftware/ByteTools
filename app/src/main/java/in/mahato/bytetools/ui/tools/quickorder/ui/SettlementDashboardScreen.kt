package `in`.mahato.bytetools.ui.tools.quickorder.ui

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import `in`.mahato.bytetools.ui.tools.quickorder.domain.engine.SmartSplitEngine
import `in`.mahato.bytetools.ui.tools.quickorder.domain.model.*
import kotlin.math.abs
import kotlin.math.roundToLong

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    val purchasePayments by viewModel.purchasePayments.collectAsState()
    val settlementTransfers by viewModel.settlementTransfers.collectAsState()

    val curr = order?.currency ?: "₹"
    val totalBill = summary?.finalBillAmountPaise ?: 0L
    val totalAllocated = settlements.sumOf { it.finalPayablePaise }
    val unallocated = totalBill - totalAllocated

    var showPdfExportSheet by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showEditPayersDialog by remember { mutableStateOf(false) }
    var showWriteOffDialog by remember { mutableStateOf(false) }

    fun shareParticipantBreakdown(settlement: ParticipantSettlement) {
        val ord = order ?: return
        val text = SmartSplitEngine.generateParticipantShareText(ord, settlement, settlementTransfers)
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
        val text = SmartSplitEngine.generateAllSettlementShareText(ord, sum, settlements, purchasePayments, settlementTransfers)
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
        val csv = SmartSplitEngine.generateCsvExport(ord, sum, settlements, settlementTransfers)
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
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, tint = PurplePrimary) },
                                onClick = {
                                    showMenu = false
                                    navController.navigate("enter_bill/$orderId")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("View Reconciliation") },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.FactCheck, contentDescription = null, tint = PurplePrimary) },
                                onClick = {
                                    showMenu = false
                                    navController.navigate("reconciliation/$orderId")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("View Consolidated List") },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.ListAlt, contentDescription = null, tint = PurplePrimary) },
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
                onStepClick = { step -> navigateToWizardStep(navController, orderId, step, isFinalized = true) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

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

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Allocated to Members:", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    SmartSplitEngine.formatPaise(totalAllocated, curr),
                                    fontWeight = FontWeight.Bold,
                                    color = if (unallocated == 0L) GreenSuccess else MaterialTheme.colorScheme.onSurface
                                )
                            }

                            if (unallocated != 0L) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = AmberBackground,
                                    border = BorderStroke(1.dp, AmberWarning.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Default.Warning,
                                                    contentDescription = null,
                                                    tint = AmberWarning,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    "Unallocated Difference",
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = AmberWarning
                                                )
                                            }
                                            Text(
                                                (if (unallocated > 0L) "+" else "") + SmartSplitEngine.formatPaise(unallocated, curr),
                                                fontWeight = FontWeight.ExtraBold,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = AmberWarning
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            if (unallocated > 0L) {
                                                "Bill total (${SmartSplitEngine.formatPaise(totalBill, curr)}) is higher than member allocations (${SmartSplitEngine.formatPaise(totalAllocated, curr)})."
                                            } else {
                                                "Member allocations (${SmartSplitEngine.formatPaise(totalAllocated, curr)}) exceed bill total (${SmartSplitEngine.formatPaise(totalBill, curr)})."
                                            },
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        Spacer(modifier = Modifier.height(10.dp))
                                        Button(
                                            onClick = { showWriteOffDialog = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = AmberWarning),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(36.dp)
                                        ) {
                                            Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                "Write Off on Final Amount",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Purchase Paid By:",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    val payersStr = if (purchasePayments.isNotEmpty()) {
                                        purchasePayments.joinToString(", ") { pp ->
                                            val name = settlements.firstOrNull { it.participantId == pp.participantId }?.participantName ?: "Member"
                                            "$name (${SmartSplitEngine.formatPaise(pp.amountPaise, curr, includeDecimals = pp.amountPaise % 100 != 0L)})"
                                        }
                                    } else {
                                        "${order?.organizerName ?: "Organizer"} (${SmartSplitEngine.formatPaise(totalBill, curr)})"
                                    }
                                    Text(
                                        payersStr,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = PurplePrimary
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))
                                OutlinedButton(
                                    onClick = { showEditPayersDialog = true },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Edit Payers", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Settlement Plan (Who Pays Whom)
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
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Payments, contentDescription = null, tint = PurplePrimary, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Settlement Plan (Who Pays Whom)",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = PurpleBackground
                                ) {
                                    Text(
                                        "${settlementTransfers.size} ${if (settlementTransfers.size == 1) "Transfer" else "Transfers"}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PurplePrimary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Simplified transfers to settle all purchase debts with minimum transactions.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            if (settlementTransfers.isEmpty()) {
                                Surface(
                                    color = GreenBackground,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = GreenSuccess, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("All members are fully settled! No payments required.", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GreenSuccess)
                                    }
                                }
                            } else {
                                settlementTransfers.forEach { transfer ->
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(transfer.fromParticipantName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "pays", tint = PurplePrimary, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(transfer.toParticipantName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                                }
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    "Amount: ${SmartSplitEngine.formatPaise(transfer.amountPaise, curr)}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (transfer.isFullySettled) {
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = GreenBackground
                                                    ) {
                                                        Row(modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                                                            Icon(Icons.Default.Check, contentDescription = null, tint = GreenSuccess, modifier = Modifier.size(12.dp))
                                                            Spacer(modifier = Modifier.width(3.dp))
                                                            Text("Settled", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GreenSuccess)
                                                        }
                                                    }
                                                } else {
                                                    Text(
                                                        SmartSplitEngine.formatPaise(transfer.remainingPaise, curr),
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = PurplePrimary,
                                                        fontSize = 14.sp
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    IconButton(
                                                        onClick = {
                                                            val text = "Hi ${transfer.fromParticipantName},\nPlease settle ${SmartSplitEngine.formatPaise(transfer.remainingPaise, curr)} with ${transfer.toParticipantName} for ${order?.orderName ?: "group purchase"}."
                                                            val sendIntent = Intent().apply {
                                                                action = Intent.ACTION_SEND
                                                                putExtra(Intent.EXTRA_TEXT, text)
                                                                type = "text/plain"
                                                            }
                                                            context.startActivity(Intent.createChooser(sendIntent, "Send Transfer Reminder"))
                                                        },
                                                        modifier = Modifier.size(28.dp)
                                                    ) {
                                                        Icon(Icons.Default.Share, contentDescription = "Share", tint = GreenSuccess, modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
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
                                            Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, tint = PurplePrimary, modifier = Modifier.size(20.dp))
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
                                            Icon(Icons.AutoMirrored.Filled.FactCheck, contentDescription = null, tint = Color(0xFF00796B), modifier = Modifier.size(20.dp))
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
                        "Member Settlement Balances (${settlements.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Participant Cards
                items(settlements) { s ->
                    ParticipantSettlementCard(
                        settlement = s,
                        currency = curr,
                        transfers = settlementTransfers,
                        onShare = { shareParticipantBreakdown(s) },
                        onCopy = {
                            val ord = order ?: return@ParticipantSettlementCard
                            val text = SmartSplitEngine.generateParticipantShareText(ord, s, settlementTransfers)
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
            purchasePayments = purchasePayments,
            settlementTransfers = settlementTransfers,
            navController = navController,
            onWriteOffDifference = { newFinal ->
                viewModel.writeOffUnallocatedDifference(orderId, newFinal)
            },
            onDismiss = { showPdfExportSheet = false }
        )
    }

    if (showEditPayersDialog && order != null) {
        EditPurchasePayersDialog(
            order = order!!,
            totalBillPaise = totalBill,
            participants = participants,
            currentPurchasePayments = purchasePayments,
            onDismiss = { showEditPayersDialog = false },
            onSave = { updatedPayments ->
                viewModel.savePurchasePayments(orderId, updatedPayments)
                showEditPayersDialog = false
            }
        )
    }

    if (showWriteOffDialog) {
        AlertDialog(
            onDismissRequest = { showWriteOffDialog = false },
            icon = {
                Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = PurplePrimary, modifier = Modifier.size(28.dp))
            },
            title = {
                Text("Write Off Unallocated Difference?", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Writing off will adjust the grand total payable from ${SmartSplitEngine.formatPaise(totalBill, curr)} to ${SmartSplitEngine.formatPaise(totalAllocated, curr)} to match the exact member allocations.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Current Bill Total:", fontSize = 12.sp)
                                Text(SmartSplitEngine.formatPaise(totalBill, curr), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Member Allocations:", fontSize = 12.sp)
                                Text(SmartSplitEngine.formatPaise(totalAllocated, curr), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = GreenSuccess)
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Difference to Write Off:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = AmberWarning)
                                Text(
                                    (if (unallocated > 0L) "-" else "+") + SmartSplitEngine.formatPaise(abs(unallocated), curr),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp,
                                    color = AmberWarning
                                )
                            }
                        }
                    }

                    Text(
                        "After write-off, the settlement will be 100% balanced and ready for payment tracking or PDF export.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.writeOffUnallocatedDifference(orderId, totalAllocated)
                        showWriteOffDialog = false
                        android.widget.Toast.makeText(
                            context,
                            "Unallocated difference written off! Final amount is now ${SmartSplitEngine.formatPaise(totalAllocated, curr)}.",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary)
                ) {
                    Text("Write Off", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showWriteOffDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ParticipantSettlementCard(
    settlement: ParticipantSettlement,
    currency: String,
    transfers: List<SettlementTransfer> = emptyList(),
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
                    if (settlement.netBalancePaise > 0L) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = GreenBackground
                        ) {
                            Text(
                                "Gets back +${SmartSplitEngine.formatPaise(settlement.netBalancePaise, currency)}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = GreenSuccess,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "Share: ${SmartSplitEngine.formatPaise(settlement.finalPayablePaise, currency)} • Paid: ${SmartSplitEngine.formatPaise(settlement.paidForPurchasePaise, currency)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else if (settlement.netBalancePaise < 0L) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = RedBackground
                        ) {
                            Text(
                                "Owes ${SmartSplitEngine.formatPaise(kotlin.math.abs(settlement.netBalancePaise), currency)}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = RedDanger,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "Share: ${SmartSplitEngine.formatPaise(settlement.finalPayablePaise, currency)} • Paid: ${SmartSplitEngine.formatPaise(settlement.paidForPurchasePaise, currency)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = GreenBackground
                        ) {
                            Text(
                                "Settled Up ✅",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = GreenSuccess,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "Share: ${SmartSplitEngine.formatPaise(settlement.finalPayablePaise, currency)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Peer Transfers Summary for this participant
            val myPayTransfers = transfers.filter { it.fromParticipantId == settlement.participantId }
            val myReceiveTransfers = transfers.filter { it.toParticipantId == settlement.participantId }

            if (myPayTransfers.isNotEmpty() || myReceiveTransfers.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                        myPayTransfers.forEach { t ->
                            val settledSuffix = if (t.isFullySettled) " (Settled ✅)" else ""
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("👉 Pay ", fontSize = 11.sp, color = RedDanger, fontWeight = FontWeight.Bold)
                                Text("${SmartSplitEngine.formatPaise(t.amountPaise, currency)} to ${t.toParticipantName}$settledSuffix", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        myReceiveTransfers.forEach { t ->
                            val settledSuffix = if (t.isFullySettled) " (Received ✅)" else ""
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("👉 Receive ", fontSize = 11.sp, color = GreenSuccess, fontWeight = FontWeight.Bold)
                                Text("${SmartSplitEngine.formatPaise(t.amountPaise, currency)} from ${t.fromParticipantName}$settledSuffix", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
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
                        val qtyStr = if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else String.format(java.util.Locale.US, "%.1f", item.quantity)
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

                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Your Cost Share:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        Text(SmartSplitEngine.formatPaise(settlement.finalPayablePaise, currency), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = PurplePrimary)
                    }

                    if (settlement.paidForPurchasePaise > 0L) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Paid Upfront for Purchase:", style = MaterialTheme.typography.bodySmall)
                            Text(SmartSplitEngine.formatPaise(settlement.paidForPurchasePaise, currency), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        val netLabel = if (settlement.netBalancePaise > 0L) "Net to Receive:" else if (settlement.netBalancePaise < 0L) "Net Due to Pay:" else "Net Settlement:"
                        val netColor = if (settlement.netBalancePaise > 0L) GreenSuccess else if (settlement.netBalancePaise < 0L) RedDanger else PurplePrimary
                        Text(netLabel, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.ExtraBold)
                        Text(
                            SmartSplitEngine.formatPaise(kotlin.math.abs(settlement.netBalancePaise), currency),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = netColor
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditPurchasePayersDialog(
    order: GroupOrder,
    totalBillPaise: Long,
    participants: List<Participant>,
    currentPurchasePayments: List<PurchasePayment>,
    onDismiss: () -> Unit,
    onSave: (List<PurchasePayment>) -> Unit
) {
    val curr = order.currency
    var isMultiple by remember {
        mutableStateOf(currentPurchasePayments.size > 1)
    }
    var singlePayerId by remember {
        val initialId = if (currentPurchasePayments.isNotEmpty()) {
            currentPurchasePayments.first().participantId
        } else {
            participants.firstOrNull { it.isOrganizer }?.id ?: participants.firstOrNull()?.id ?: 0L
        }
        mutableStateOf(initialId)
    }

    val amounts = remember {
        val map = mutableStateMapOf<Long, String>()
        if (currentPurchasePayments.isNotEmpty()) {
            currentPurchasePayments.forEach {
                map[it.participantId] = if (it.amountPaise % 100L == 0L) (it.amountPaise / 100L).toString() else (it.amountPaise / 100.0).toString()
            }
        } else {
            map[singlePayerId] = if (totalBillPaise % 100L == 0L) (totalBillPaise / 100L).toString() else (totalBillPaise / 100.0).toString()
        }
        map
    }

    val methods = remember {
        val map = mutableStateMapOf<Long, String>()
        currentPurchasePayments.forEach { map[it.participantId] = it.paymentMethod }
        map
    }

    val totalEnteredPaise = if (!isMultiple) {
        totalBillPaise
    } else {
        participants.sumOf { p ->
            val text = amounts[p.id] ?: ""
            ((text.replace(',', '.').toDoubleOrNull() ?: 0.0) * 100).roundToLong()
        }
    }
    val diffPaise = totalBillPaise - totalEnteredPaise

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Payments, contentDescription = null, tint = PurplePrimary, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Edit Purchase Payers", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Capture how much different people paid towards the purchase bill of ${SmartSplitEngine.formatPaise(totalBillPaise, curr)}.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !isMultiple,
                        onClick = { isMultiple = false },
                        label = { Text("Single Person", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PurplePrimary,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = isMultiple,
                        onClick = {
                            isMultiple = true
                            if (amounts.isEmpty() && singlePayerId != 0L) {
                                val amtStr = if (totalBillPaise % 100L == 0L) (totalBillPaise / 100L).toString() else (totalBillPaise / 100.0).toString()
                                amounts[singlePayerId] = amtStr
                            }
                        },
                        label = { Text("Multiple People", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PurplePrimary,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                if (!isMultiple) {
                    Text("Select Payer:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        participants.forEach { p ->
                            FilterChip(
                                selected = singlePayerId == p.id,
                                onClick = { singlePayerId = p.id },
                                label = { Text(if (p.isOrganizer) "${p.name} (Org)" else p.name, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PurpleBackground,
                                    selectedLabelColor = PurplePrimary
                                )
                            )
                        }
                    }
                    val payerName = participants.firstOrNull { it.id == singlePayerId }?.name ?: "Organizer"
                    Text(
                        "$payerName paid ${SmartSplitEngine.formatPaise(totalBillPaise, curr)} (100%)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = PurplePrimary
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Amounts paid by members:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        TextButton(
                            onClick = {
                                if (participants.isNotEmpty()) {
                                    val split = SmartSplitEngine.distributePaiseEqually(totalBillPaise, participants.map { it.id })
                                    split.forEach { (id, paise) ->
                                        amounts[id] = if (paise % 100L == 0L) (paise / 100L).toString() else (paise / 100.0).toString()
                                    }
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Split Equally", fontSize = 11.sp, color = PurplePrimary)
                        }
                    }

                    participants.forEach { p ->
                        val amt = amounts[p.id] ?: ""

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(p.name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                    TextButton(
                                        onClick = {
                                            val otherSum = participants.filter { it.id != p.id }.sumOf { other ->
                                                val t = amounts[other.id] ?: ""
                                                ((t.replace(',', '.').toDoubleOrNull() ?: 0.0) * 100).roundToLong()
                                            }
                                            val rem = kotlin.math.max(0L, totalBillPaise - otherSum)
                                            amounts[p.id] = if (rem % 100L == 0L) (rem / 100L).toString() else (rem / 100.0).toString()
                                        },
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text("Fill Rem.", fontSize = 10.sp, color = PurplePrimary)
                                    }
                                }
                                OutlinedTextField(
                                    value = amt,
                                    onValueChange = { amounts[p.id] = it },
                                    label = { Text("Amount ($curr)") },
                                    placeholder = { Text("0") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (diffPaise == 0L) GreenBackground else AmberBackground,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Total: ${SmartSplitEngine.formatPaise(totalEnteredPaise, curr)}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (diffPaise == 0L) GreenSuccess else AmberWarning
                            )
                            Text(
                                if (diffPaise == 0L) "Balanced ✅" else "Diff: ${SmartSplitEngine.formatPaise(diffPaise, curr)}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (diffPaise == 0L) GreenSuccess else AmberWarning
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val result = if (!isMultiple) {
                        val pName = participants.firstOrNull { it.id == singlePayerId }?.name ?: "Organizer"
                        listOf(
                            PurchasePayment(
                                groupOrderId = order.id,
                                participantId = singlePayerId,
                                participantName = pName,
                                amountPaise = totalBillPaise,
                                paymentMethod = "Cash"
                            )
                        )
                    } else {
                        participants.mapNotNull { p ->
                            val text = amounts[p.id] ?: ""
                            val paise = ((text.replace(',', '.').toDoubleOrNull() ?: 0.0) * 100).roundToLong()
                            if (paise > 0L) {
                                PurchasePayment(
                                    groupOrderId = order.id,
                                    participantId = p.id,
                                    participantName = p.name,
                                    amountPaise = paise,
                                    paymentMethod = methods[p.id] ?: "Cash"
                                )
                            } else null
                        }
                    }
                    onSave(result)
                },
                colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary)
            ) {
                Text("Save Payers", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
