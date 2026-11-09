package `in`.mahato.bytetools.ui.tools.quickorder.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import `in`.mahato.bytetools.ui.tools.quickorder.domain.engine.SmartSplitEngine
import `in`.mahato.bytetools.ui.tools.quickorder.domain.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReconciliationScreen(
    orderId: Long,
    navController: NavController,
    viewModel: QuickOrderViewModel
) {
    LaunchedEffect(orderId) {
        viewModel.selectOrder(orderId)
    }

    val order by viewModel.activeOrder.collectAsState()
    val participants by viewModel.participants.collectAsState()
    val billItems by viewModel.billItems.collectAsState()
    val initialReconItems by viewModel.reconciliationItems.collectAsState()
    val billSummary by viewModel.billSummary.collectAsState()
    val allOrderItems by viewModel.orderItems.collectAsState()
    val consolidatedItems by viewModel.consolidatedItems.collectAsState()
    val settlements by viewModel.settlements.collectAsState()
    val payments by viewModel.payments.collectAsState()
    val purchasePayments by viewModel.purchasePayments.collectAsState()
    val settlementTransfers by viewModel.settlementTransfers.collectAsState()

    var showPdfExportSheet by remember { mutableStateOf(false) }

    // Local mutable state for participant allocations per reconciliation item
    // Key: BillItem ID -> Map<ParticipantId, AllocatedQuantity>
    val allocationsState = remember { mutableStateMapOf<Long, MutableMap<Long, Double>>() }

    LaunchedEffect(initialReconItems) {
        if (allocationsState.isEmpty() && initialReconItems.isNotEmpty()) {
            for (recon in initialReconItems) {
                val map = mutableMapOf<Long, Double>()
                if (recon.participantAllocations.isNotEmpty()) {
                    map.putAll(recon.participantAllocations)
                } else if (recon.status == ReconciliationStatus.NEW_ITEM && participants.isNotEmpty()) {
                    // Default new items to split equally or assigned to organizer
                    val organizer = participants.firstOrNull { it.isOrganizer } ?: participants.first()
                    map[organizer.id] = recon.purchasedQuantity
                }
                allocationsState[recon.billItem.id] = map
            }
        }
    }

    // Validation: Check if there are any mismatch errors where allocated != purchased
    var validationError by remember { mutableStateOf<String?>(null) }
    val isFinalized = order?.status == OrderStatus.RECONCILED || order?.status == OrderStatus.SETTLED

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(order?.orderName ?: "Reconciliation", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (isFinalized) "Smart Bill Reconciliation (View Only)" else "Step 5: Smart Bill Reconciliation",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isFinalized) GreenSuccess else MaterialTheme.colorScheme.onSurfaceVariant
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
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (validationError != null) {
                        Text(
                            validationError ?: "",
                            color = RedDanger,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    if (isFinalized) {
                        Button(
                            onClick = { navController.navigate("settlement_dashboard/$orderId") },
                            colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(50.dp)
                        ) {
                            Text("View Settlement Dashboard", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                        }
                    } else {
                        Button(
                            onClick = {
                                // Check that allocations equal purchased quantities for all bill items
                                for (recon in initialReconItems) {
                                    if (recon.purchasedQuantity <= 0.0) continue
                                    val allocMap = allocationsState[recon.billItem.id] ?: emptyMap()
                                    val totalAllocated = allocMap.values.sum()
                                    if (kotlin.math.abs(totalAllocated - recon.purchasedQuantity) > 0.01) {
                                        validationError = "Allocated quantity (${totalAllocated}) for '${recon.billItem.productName}' must equal purchased quantity (${recon.purchasedQuantity})."
                                        return@Button
                                    }
                                }

                                // Convert to ItemAllocation entities
                                val allocationsToSave = mutableListOf<ItemAllocation>()
                                for (recon in initialReconItems) {
                                    val allocMap = allocationsState[recon.billItem.id] ?: emptyMap()
                                    for ((pId, qty) in allocMap) {
                                        if (qty > 0.0) {
                                            allocationsToSave.add(
                                                ItemAllocation(
                                                    groupOrderId = orderId,
                                                    billItemId = recon.billItem.id,
                                                    participantId = pId,
                                                    allocatedQuantity = qty
                                                )
                                            )
                                        }
                                    }
                                }

                                val summary = billSummary ?: BillSummary(
                                    groupOrderId = orderId,
                                    subtotalPaise = billItems.sumOf { it.totalPricePaise },
                                    finalBillAmountPaise = billItems.sumOf { it.totalPricePaise }
                                )

                                viewModel.confirmReconciliation(
                                    orderId = orderId,
                                    allocations = allocationsToSave,
                                    updatedBillItems = billItems,
                                    updatedSummary = summary
                                )
                                navController.navigate("settlement_dashboard/$orderId")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(50.dp)
                        ) {
                            Text("Confirm Allocations & Split", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.Default.Check, contentDescription = null)
                        }
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
                currentStep = 4,
                onStepClick = { step -> navigateToWizardStep(navController, orderId, step, isFinalized) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (isFinalized) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFE8F5E9)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = GreenSuccess, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("View-Only Mode (Settlement Finalized)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = GreenSuccess)
                            Text("Allocations are locked to preserve the finalized financial settlement.", fontSize = 11.sp, color = Color(0xFF1B5E20))
                        }
                    }
                }
            }

            // Guidance Banner
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = PurpleBackground),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = PurplePrimary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "Verify and adjust quantities for each member if there was a shortfall or extra items purchased.",
                        style = MaterialTheme.typography.bodySmall,
                        color = PurpleDark
                    )
                }
            }

            if (initialReconItems.isEmpty()) {
                EmptyQuickOrderView(
                    icon = Icons.AutoMirrored.Filled.ReceiptLong,
                    title = "No Bill Items to Reconcile",
                    subtitle = "Please enter the shopkeeper bill or scan a receipt first before allocating items.",
                    actionButtonText = "Enter Shop Bill",
                    onActionClick = { navController.navigate("enter_bill/$orderId") },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    itemsIndexed(initialReconItems) { _, recon ->
                    ReconciliationItemCard(
                        recon = recon,
                        participants = participants,
                        allocations = allocationsState[recon.billItem.id] ?: mutableMapOf(),
                        currency = order?.currency ?: "₹",
                        isReadOnly = isFinalized,
                        onAllocationChange = { pId, newQty ->
                            if (!isFinalized) {
                                val currentMap = allocationsState.getOrPut(recon.billItem.id) { mutableMapOf() }
                                val updated = HashMap(currentMap)
                                if (newQty <= 0.0) {
                                    updated.remove(pId)
                                } else {
                                    updated[pId] = newQty
                                }
                                allocationsState[recon.billItem.id] = updated
                                validationError = null
                            }
                        },
                        onShareExtraEqually = {
                            if (!isFinalized) {
                                val perPerson = recon.purchasedQuantity / participants.size
                                val updated = mutableMapOf<Long, Double>()
                                for (p in participants) {
                                    updated[p.id] = perPerson
                                }
                                allocationsState[recon.billItem.id] = updated
                                validationError = null
                            }
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
            }
        }
    }

    if (showPdfExportSheet && order != null) {
        QuickOrderPdfExportBottomSheet(
            order = order!!,
            summary = billSummary,
            settlements = settlements,
            participants = participants,
            orderItems = allOrderItems,
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
}

@Composable
fun ReconciliationItemCard(
    recon: ReconciliationItem,
    participants: List<Participant>,
    allocations: Map<Long, Double>,
    currency: String,
    isReadOnly: Boolean = false,
    onAllocationChange: (Long, Double) -> Unit,
    onShareExtraEqually: () -> Unit
) {
    val totalAllocated = allocations.values.sum()
    val isBalanced = kotlin.math.abs(totalAllocated - recon.purchasedQuantity) < 0.01

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        recon.billItem.productName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Ordered: ${recon.orderedQuantity} ${recon.billItem.unit} • Purchased: ${recon.purchasedQuantity} ${recon.billItem.unit} (${SmartSplitEngine.formatPaise(recon.billItem.totalPricePaise, currency)})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                ReconciliationStatusBadge(recon.status)
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(10.dp))

            // Allocation header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Participant Allocations",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Allocated: $totalAllocated / ${recon.purchasedQuantity}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isBalanced) GreenSuccess else AmberWarning
                )
            }

            if (!isReadOnly && (recon.status == ReconciliationStatus.SURPLUS || recon.status == ReconciliationStatus.NEW_ITEM)) {
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedButton(
                    onClick = onShareExtraEqually,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Split extra equally among all members", fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Participant Steppers
            participants.forEach { p ->
                val qty = allocations[p.id] ?: 0.0
                val originallyOrdered = recon.matchedConsolidatedItem?.participantOrders?.firstOrNull { it.participantId == p.id }?.quantity ?: 0.0

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(p.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        if (originallyOrdered > 0.0) {
                            Text(
                                "Ordered: $originallyOrdered",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    QuantityStepper(
                        quantity = qty,
                        unit = recon.billItem.unit,
                        onQuantityChange = { newQ -> onAllocationChange(p.id, newQ) },
                        minQuantity = 0.0,
                        step = 1.0,
                        enabled = !isReadOnly
                    )
                }
            }
        }
    }
}
