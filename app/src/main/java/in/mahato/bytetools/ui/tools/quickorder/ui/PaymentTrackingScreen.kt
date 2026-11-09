package `in`.mahato.bytetools.ui.tools.quickorder.ui

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import `in`.mahato.bytetools.ui.tools.quickorder.domain.engine.SmartSplitEngine
import `in`.mahato.bytetools.ui.tools.quickorder.domain.model.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.roundToLong

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentTrackingScreen(
    orderId: Long,
    navController: NavController,
    viewModel: QuickOrderViewModel
) {
    LaunchedEffect(orderId) {
        viewModel.selectOrder(orderId)
    }

    val context = LocalContext.current
    val order by viewModel.activeOrder.collectAsState()
    val participants by viewModel.participants.collectAsState()
    val settlements by viewModel.settlements.collectAsState()
    val allPayments by viewModel.payments.collectAsState()
    val purchasePayments by viewModel.purchasePayments.collectAsState()
    val settlementTransfers by viewModel.settlementTransfers.collectAsState()

    var recordingPaymentParticipant by remember { mutableStateOf<ParticipantSettlement?>(null) }
    var targetRecipientId by remember { mutableStateOf<Long?>(null) }
    var paymentAmountText by remember { mutableStateOf("") }
    var paymentNotes by remember { mutableStateOf("") }

    val curr = order?.currency ?: "₹"
    val totalBill = settlements.sumOf { it.finalPayablePaise }
    val totalCollected = settlements.sumOf { it.amountPaidPaise }
    val totalRemaining = settlements.sumOf { it.outstandingBalancePaise }
    val progress = if (totalBill > 0L) (totalCollected.toFloat() / totalBill.toFloat()).coerceIn(0f, 1f) else 0f

    fun sendReminder(settlement: ParticipantSettlement) {
        val ord = order ?: return
        val text = SmartSplitEngine.generateParticipantShareText(ord, settlement, settlementTransfers)
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(sendIntent, "Send Payment Reminder to ${settlement.participantName}"))
    }

    fun sendTransferReminder(transfer: SettlementTransfer) {
        val ord = order ?: return
        val text = "Hi ${transfer.fromParticipantName},\nPlease settle ${SmartSplitEngine.formatPaise(transfer.remainingPaise, curr)} with ${transfer.toParticipantName} for ${ord.orderName}."
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(sendIntent, "Send Payment Reminder to ${transfer.fromParticipantName}"))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(order?.orderName ?: "Payment Tracking", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Step 7: Payment Tracking & Receipts",
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
                    IconButton(onClick = {
                        navController.navigate("home") {
                            popUpTo("home") { inclusive = false }
                            launchSingleTop = true
                        }
                    }) {
                        Icon(Icons.Default.Home, contentDescription = "Home", tint = PurplePrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            OrderStepIndicator(
                currentStep = 6,
                onStepClick = { step ->
                    when (step) {
                        1 -> navController.navigate("collect_orders/$orderId")
                        2 -> navController.navigate("consolidated_list/$orderId")
                        3 -> navController.navigate("enter_bill/$orderId")
                        4 -> navController.navigate("reconciliation/$orderId")
                        5 -> navController.navigate("settlement_dashboard/$orderId")
                        6 -> { /* current screen */ }
                    }
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Progress Banner
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Collection Progress", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(
                                    "${(progress * 100).toInt()}% Collected",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = PurplePrimary
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp)),
                                color = GreenSuccess,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )

                            Spacer(modifier = Modifier.height(14.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Total Collected", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(SmartSplitEngine.formatPaise(totalCollected, curr), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = GreenSuccess)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Remaining Due", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(SmartSplitEngine.formatPaise(totalRemaining, curr), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (totalRemaining > 0L) RedDanger else GreenSuccess)
                                }
                            }
                        }
                    }
                }

                // Purchase Contributions (Paid Upfront)
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, tint = PurplePrimary, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Purchase Contributions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = PurpleBackground
                                ) {
                                    Text(
                                        "${purchasePayments.size.coerceAtLeast(1)} ${if (purchasePayments.size <= 1) "Contributor" else "Contributors"}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PurplePrimary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Amounts paid directly to the store/merchant when purchasing the bill items.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            if (purchasePayments.isEmpty()) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(order?.organizerName ?: "Organizer", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                            Text("Paid 100% of bill upfront", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Text(SmartSplitEngine.formatPaise(totalBill, curr), fontWeight = FontWeight.Bold, color = PurplePrimary)
                                    }
                                }
                            } else {
                                purchasePayments.forEach { pp ->
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 3.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(pp.participantName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                                if (pp.paymentMethod.isNotBlank() && pp.paymentMethod != "Cash" && pp.paymentMethod != "Paid") {
                                                    Text("via ${pp.paymentMethod}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }
                                            Text(SmartSplitEngine.formatPaise(pp.amountPaise, curr), fontWeight = FontWeight.Bold, color = PurplePrimary)
                                        }
                                    }
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
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
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
                                    Text("Settlement Plan (Who Pays Whom)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
                                "Direct payments between members to balance all debts with fewest transactions.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(10.dp))

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
                                                    "Transfer: ${SmartSplitEngine.formatPaise(transfer.amountPaise, curr)}" +
                                                            if (transfer.settledPaise > 0L) " • Settled: ${SmartSplitEngine.formatPaise(transfer.settledPaise, curr)}" else "",
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
                                                        onClick = { sendTransferReminder(transfer) },
                                                        modifier = Modifier.size(32.dp)
                                                    ) {
                                                        Icon(Icons.Default.Share, contentDescription = "Remind", tint = GreenSuccess, modifier = Modifier.size(16.dp))
                                                    }
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Button(
                                                        onClick = {
                                                            val debtor = settlements.firstOrNull { it.participantId == transfer.fromParticipantId }
                                                            recordingPaymentParticipant = debtor
                                                            targetRecipientId = transfer.toParticipantId
                                                            paymentAmountText = if (transfer.remainingPaise % 100L == 0L) (transfer.remainingPaise / 100L).toString() else (transfer.remainingPaise / 100.0).toString()
                                                            paymentNotes = "Settlement to ${transfer.toParticipantName}"
                                                        },
                                                        shape = RoundedCornerShape(8.dp),
                                                        colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                        modifier = Modifier.height(30.dp)
                                                    ) {
                                                        Text("Pay", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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

                // Section Header: Participant Ledgers
                item {
                    Text(
                        "Participant Ledgers (${settlements.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }

                items(settlements) { s ->
                    val participantPayments = allPayments.filter { it.participantId == s.participantId }

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Member Header & Status
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(if (s.isOrganizer) PurpleBackground else Color(0xFFF1F5F9)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (s.isOrganizer) {
                                            Icon(Icons.Default.Star, contentDescription = null, tint = PurplePrimary, modifier = Modifier.size(18.dp))
                                        } else {
                                            Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(s.participantName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Text(
                                            if (s.isOrganizer) "Organizer" else "Member",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                PaymentStatusBadge(s.paymentStatus)
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(10.dp))

                            // 3-Column Financial Breakdown: Cost Share, Paid Upfront, Net Settlement Balance
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Cost Share", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(SmartSplitEngine.formatPaise(s.finalPayablePaise, curr), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                }
                                Column {
                                    Text("Paid Upfront", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        SmartSplitEngine.formatPaise(s.paidForPurchasePaise, curr),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (s.paidForPurchasePaise > 0L) PurplePrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Net Balance", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    val netText = when {
                                        s.netBalancePaise > 0L -> "+${SmartSplitEngine.formatPaise(s.netBalancePaise, curr)}"
                                        s.netBalancePaise < 0L -> "-${SmartSplitEngine.formatPaise(abs(s.netBalancePaise), curr)}"
                                        else -> "₹0"
                                    }
                                    val netColor = when {
                                        s.netBalancePaise > 0L -> GreenSuccess
                                        s.netBalancePaise < 0L -> RedDanger
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                    Text(netText, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = netColor)
                                    Text(
                                        when {
                                            s.netBalancePaise > 0L -> "Gets back"
                                            s.netBalancePaise < 0L -> "Owes"
                                            else -> "Settled Up"
                                        },
                                        fontSize = 10.sp,
                                        color = netColor
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Payment Tracking Row (Paid, Due, Record, Remind)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    if (s.amountPaidPaise > 0L) {
                                        Text(
                                            "Settled: ${SmartSplitEngine.formatPaise(s.amountPaidPaise, curr)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = GreenSuccess
                                        )
                                    }
                                    if (s.amountReceivedPaise > 0L) {
                                        Text(
                                            "Received: ${SmartSplitEngine.formatPaise(s.amountReceivedPaise, curr)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = PurplePrimary
                                        )
                                    }
                                    if (s.outstandingBalancePaise > 0L) {
                                        Text(
                                            "Remaining Due: ${SmartSplitEngine.formatPaise(s.outstandingBalancePaise, curr)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = RedDanger
                                        )
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (s.outstandingBalancePaise > 0L) {
                                        IconButton(
                                            onClick = { sendReminder(s) },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(Icons.Default.Share, contentDescription = "Reminder", tint = GreenSuccess)
                                        }

                                        Button(
                                            onClick = {
                                                recordingPaymentParticipant = s
                                                val pendingTransfer = settlementTransfers.firstOrNull { it.fromParticipantId == s.participantId && !it.isFullySettled }
                                                targetRecipientId = pendingTransfer?.toParticipantId
                                                val amtPaise = pendingTransfer?.remainingPaise ?: s.outstandingBalancePaise
                                                paymentAmountText = if (amtPaise % 100L == 0L) (amtPaise / 100L).toString() else (amtPaise / 100.0).toString()
                                                paymentNotes = if (pendingTransfer != null) "Settlement to ${pendingTransfer.toParticipantName}" else ""
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                            modifier = Modifier.height(36.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Record", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            // Payment History List
                            if (participantPayments.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("Payment History:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                participantPayments.forEach { pmt ->
                                    val dateStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(pmt.paymentDate))
                                    val toName = if (pmt.toParticipantId != null) {
                                        settlements.firstOrNull { it.participantId == pmt.toParticipantId }?.participantName ?: "Member"
                                    } else null

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 2.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            val methodSuffix = if (pmt.paymentMethod.isNotBlank() && pmt.paymentMethod != "Paid" && pmt.paymentMethod != "Cash") " via ${pmt.paymentMethod}" else ""
                                            Text(
                                                "${SmartSplitEngine.formatPaise(pmt.amountPaise, curr)}" +
                                                        (if (toName != null) " to $toName" else "") +
                                                        methodSuffix,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                listOfNotNull(dateStr, pmt.transactionReference?.ifBlank { null }, pmt.notes?.ifBlank { null }).joinToString(" • "),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        IconButton(
                                            onClick = { viewModel.deletePayment(pmt.id) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }

    // Record Payment Dialog
    recordingPaymentParticipant?.let { participant ->
        val otherParticipants = participants.filter { it.id != participant.participantId }

        AlertDialog(
            onDismissRequest = { recordingPaymentParticipant = null },
            title = { Text("Record Settlement Payment") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Payer: ${participant.participantName}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)

                    if (otherParticipants.isNotEmpty()) {
                        Text("Paying To", style = MaterialTheme.typography.labelSmall)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            otherParticipants.forEach { p ->
                                val isSelected = (targetRecipientId == p.id) || (targetRecipientId == null && p.isOrganizer)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { targetRecipientId = p.id },
                                    label = {
                                        Text(
                                            if (p.isOrganizer) "${p.name} (Organizer)" else p.name,
                                            fontSize = 11.sp
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = PurplePrimary,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = paymentAmountText,
                        onValueChange = { paymentAmountText = it },
                        label = { Text("Amount Paid ($curr)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = paymentNotes,
                        onValueChange = { paymentNotes = it },
                        label = { Text("Notes (Optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amountPaise = (paymentAmountText.toDoubleOrNull()?.times(100))?.roundToLong() ?: 0L
                        if (amountPaise > 0L) {
                            viewModel.recordPayment(
                                orderId = orderId,
                                participantId = participant.participantId,
                                toParticipantId = targetRecipientId,
                                amountPaise = amountPaise,
                                method = "Paid",
                                reference = null,
                                notes = paymentNotes.ifBlank { null }
                            )
                            recordingPaymentParticipant = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary)
                ) {
                    Text("Save Payment", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { recordingPaymentParticipant = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
