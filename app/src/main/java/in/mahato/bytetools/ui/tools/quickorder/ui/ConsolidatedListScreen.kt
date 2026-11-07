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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import `in`.mahato.bytetools.ui.tools.quickorder.domain.engine.SmartSplitEngine
import `in`.mahato.bytetools.ui.tools.quickorder.domain.model.ConsolidatedItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsolidatedListScreen(
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
    val participants by viewModel.participants.collectAsState()
    val consolidatedItems by viewModel.consolidatedItems.collectAsState()
    val orderItems by viewModel.orderItems.collectAsState()
    val billItems by viewModel.billItems.collectAsState()
    val summary by viewModel.billSummary.collectAsState()
    val settlements by viewModel.settlements.collectAsState()
    val payments by viewModel.payments.collectAsState()

    var showLockDialog by remember { mutableStateOf(false) }
    var showPdfExportSheet by remember { mutableStateOf(false) }

    fun shareList() {
        val ord = order ?: return
        val text = SmartSplitEngine.generateShoppingListShareText(ord, consolidatedItems)
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(sendIntent, "Share Consolidated Shopping List"))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(order?.orderName ?: "Shopping List", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Step 3: Consolidated Shopping List",
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
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF Report", tint = PurplePrimary)
                    }
                    IconButton(onClick = { shareList() }) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                    IconButton(onClick = {
                        val ord = order ?: return@IconButton
                        val text = SmartSplitEngine.generateShoppingListShareText(ord, consolidatedItems)
                        clipboardManager.setText(AnnotatedString(text))
                        android.widget.Toast.makeText(context, "Shopping list copied!", android.widget.Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { navController.navigate("collect_orders/$orderId") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Edit Orders")
                    }

                    Button(
                        onClick = { navController.navigate("enter_bill/$orderId") },
                        colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1.3f)
                    ) {
                        Text("Enter Shop Bill", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(16.dp))
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
                currentStep = 2,
                onStepClick = { step ->
                    when (step) {
                        1 -> navController.navigate("collect_orders/$orderId")
                        2 -> { /* current screen */ }
                        3 -> navController.navigate("enter_bill/$orderId")
                        4 -> if (order?.status == `in`.mahato.bytetools.ui.tools.quickorder.domain.model.OrderStatus.RECONCILED || order?.status == `in`.mahato.bytetools.ui.tools.quickorder.domain.model.OrderStatus.SETTLED) navController.navigate("reconciliation/$orderId")
                        5 -> if (order?.status == `in`.mahato.bytetools.ui.tools.quickorder.domain.model.OrderStatus.RECONCILED || order?.status == `in`.mahato.bytetools.ui.tools.quickorder.domain.model.OrderStatus.SETTLED) navController.navigate("settlement_dashboard/$orderId")
                        6 -> if (order?.status == `in`.mahato.bytetools.ui.tools.quickorder.domain.model.OrderStatus.SETTLED) navController.navigate("payment_tracking/$orderId")
                    }
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Metrics Summary
            val totalUnits = consolidatedItems.sumOf { it.totalOrderedQuantity }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricStatCard(
                    title = "Unique Items",
                    value = "${consolidatedItems.size}",
                    icon = Icons.Default.Category,
                    containerColor = PurpleBackground,
                    contentColor = PurplePrimary,
                    modifier = Modifier.weight(1f)
                )
                MetricStatCard(
                    title = "Total Qty",
                    value = if (totalUnits % 1.0 == 0.0) totalUnits.toInt().toString() else String.format("%.1f", totalUnits),
                    icon = Icons.Default.Inventory2,
                    containerColor = Color(0xFFE0F2FE),
                    contentColor = Color(0xFF0369A1),
                    modifier = Modifier.weight(1f)
                )
                MetricStatCard(
                    title = "Buyers",
                    value = "${participants.size}",
                    icon = Icons.Default.Groups,
                    containerColor = GreenBackground,
                    contentColor = GreenSuccess,
                    modifier = Modifier.weight(1f)
                )
            }

            // Lock Banner
            val isLocked = order?.isLocked == true
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isLocked) AmberBackground else MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = null,
                            tint = if (isLocked) AmberWarning else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (isLocked) "Orders Locked: ready for shopping" else "Orders Open: participants can add items",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    TextButton(onClick = { showLockDialog = true }) {
                        Text(
                            if (isLocked) "Unlock" else "Lock Orders",
                            fontWeight = FontWeight.Bold,
                            color = PurplePrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Consolidated Items List
            if (consolidatedItems.isEmpty()) {
                EmptyQuickOrderView(
                    icon = Icons.Default.ShoppingBag,
                    title = "No Products in List",
                    subtitle = "Go back to Step 2 and add products for participants.",
                    actionButtonText = "Collect Orders",
                    onActionClick = { navController.navigate("collect_orders/$orderId") },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(consolidatedItems) { item ->
                        ConsolidatedItemCard(item = item)
                    }

                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }
        }
    }

    if (showLockDialog) {
        val currentlyLocked = order?.isLocked == true
        AlertDialog(
            onDismissRequest = { showLockDialog = false },
            title = { Text(if (currentlyLocked) "Unlock Orders?" else "Lock Orders?") },
            text = {
                Text(
                    if (currentlyLocked) {
                        "Unlocking will allow further modifications to participant orders."
                    } else {
                        "Locking orders finalizes the list so nobody alters quantities while you are at the shop."
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.lockOrders(orderId, !currentlyLocked)
                        showLockDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary)
                ) {
                    Text(if (currentlyLocked) "Unlock" else "Lock Orders")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLockDialog = false }) {
                    Text("Cancel")
                }
            }
        )
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
fun ConsolidatedItemCard(
    item: ConsolidatedItem
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
                            .background(PurpleBackground),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Checklist,
                            contentDescription = null,
                            tint = PurplePrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            item.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        val buyersCount = item.participantOrders.size
                        Text(
                            "Ordered by $buyersCount participant${if (buyersCount > 1) "s" else ""}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                val totalQtyStr = if (item.totalOrderedQuantity % 1.0 == 0.0) item.totalOrderedQuantity.toInt().toString() else String.format("%.1f", item.totalOrderedQuantity)
                Column(horizontalAlignment = Alignment.End) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = PurpleBackground
                    ) {
                        Text(
                            text = "$totalQtyStr ${item.unit}",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = PurplePrimary
                        )
                    }
                }
            }

            // Expandable breakdown of participant ownership
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 14.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "Individual Breakdown:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    item.participantOrders.forEach { orderPart ->
                        val partQtyStr = if (orderPart.quantity % 1.0 == 0.0) orderPart.quantity.toInt().toString() else String.format("%.1f", orderPart.quantity)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(PurplePrimary)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(orderPart.participantName, style = MaterialTheme.typography.bodyMedium)
                            }
                            Text(
                                "$partQtyStr ${orderPart.unit}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}
