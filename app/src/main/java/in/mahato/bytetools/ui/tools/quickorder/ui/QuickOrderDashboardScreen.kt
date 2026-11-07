package `in`.mahato.bytetools.ui.tools.quickorder.ui

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import `in`.mahato.bytetools.ui.navigation.Screen
import `in`.mahato.bytetools.ui.tools.quickorder.domain.engine.SmartSplitEngine
import `in`.mahato.bytetools.ui.tools.quickorder.domain.model.GroupOrder
import `in`.mahato.bytetools.ui.tools.quickorder.domain.model.OrderStatus
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickOrderDashboardScreen(
    navController: NavController,
    viewModel: QuickOrderViewModel
) {
    val orders by viewModel.allOrders.collectAsState()
    var orderToDelete by remember { mutableStateOf<GroupOrder?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Group Shopping Split",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "Group shopping, bills & fair split",
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
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = false }
                            launchSingleTop = true
                        }
                    }) {
                        Icon(Icons.Default.Home, contentDescription = "Home", tint = PurplePrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { navController.navigate(Screen.CreateGroupOrder.route) },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New Group Order", fontWeight = FontWeight.Bold) },
                containerColor = PurplePrimary,
                contentColor = Color.White
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (orders.isEmpty()) {
                EmptyQuickOrderView(
                    icon = Icons.Default.ShoppingCart,
                    title = "No Group Orders Yet",
                    subtitle = "Start a group purchase session for Diwali crackers, groceries, party food or office supplies.",
                    actionButtonText = "Create First Group Order",
                    onActionClick = { navController.navigate(Screen.CreateGroupOrder.route) },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Summary Stats Row
                val activeCount = orders.count { it.status != OrderStatus.SETTLED }
                val settledCount = orders.count { it.status == OrderStatus.SETTLED }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricStatCard(
                        title = "Active Orders",
                        value = "$activeCount",
                        icon = Icons.Default.PendingActions,
                        containerColor = PurpleBackground,
                        contentColor = PurplePrimary,
                        modifier = Modifier.weight(1f)
                    )
                    MetricStatCard(
                        title = "Settled Orders",
                        value = "$settledCount",
                        icon = Icons.Default.TaskAlt,
                        containerColor = GreenBackground,
                        contentColor = GreenSuccess,
                        modifier = Modifier.weight(1f)
                    )
                }

                Text(
                    text = "Recent Sessions (${orders.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )

                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(orders, key = { it.id }) { order ->
                        GroupOrderCard(
                            order = order,
                            onClick = {
                                viewModel.selectOrder(order.id)
                                // Navigate to the appropriate screen according to status
                                when (order.status) {
                                    OrderStatus.DRAFTING -> navController.navigate("collect_orders/${order.id}")
                                    OrderStatus.LOCKED -> navController.navigate("consolidated_list/${order.id}")
                                    OrderStatus.SHOPPING -> navController.navigate("enter_bill/${order.id}")
                                    OrderStatus.RECONCILED,
                                    OrderStatus.SETTLED -> navController.navigate("settlement_dashboard/${order.id}")
                                }
                            },
                            onDelete = { orderToDelete = order }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }

    if (orderToDelete != null) {
        AlertDialog(
            onDismissRequest = { orderToDelete = null },
            title = { Text("Delete Group Order?") },
            text = { Text("Are you sure you want to delete '${orderToDelete?.orderName}'? All participant orders, bills and payments will be removed.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        orderToDelete?.id?.let { viewModel.deleteGroupOrder(it) }
                        orderToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = RedDanger)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { orderToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun GroupOrderCard(
    order: GroupOrder,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dateStr = remember(order.createdAt) {
        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(order.createdAt))
    }

    Card(
        onClick = onClick,
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
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(PurpleBackground),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = PurplePrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = order.orderName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Organizer: ${order.organizerName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                OrderStatusChip(order.status)
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (order.finalBillAmountPaise > 0L) {
                    Text(
                        text = "Bill: ${SmartSplitEngine.formatPaise(order.finalBillAmountPaise, order.currency)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = PurplePrimary
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun OrderStatusChip(status: OrderStatus) {
    val (bgColor, textColor, label) = when (status) {
        OrderStatus.DRAFTING -> Triple(PurpleBackground, PurplePrimary, "Collecting")
        OrderStatus.LOCKED -> Triple(Color(0xFFE0F2FE), Color(0xFF0369A1), "Locked")
        OrderStatus.SHOPPING -> Triple(AmberBackground, AmberWarning, "Bill Entry")
        OrderStatus.RECONCILED -> Triple(Color(0xFFEDE9FE), Color(0xFF6D28D9), "Reconciled")
        OrderStatus.SETTLED -> Triple(GreenBackground, GreenSuccess, "Settled")
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bgColor
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
