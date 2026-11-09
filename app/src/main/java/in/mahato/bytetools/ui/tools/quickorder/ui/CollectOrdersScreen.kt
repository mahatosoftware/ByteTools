package `in`.mahato.bytetools.ui.tools.quickorder.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import `in`.mahato.bytetools.ui.tools.quickorder.domain.engine.SmartSplitEngine
import `in`.mahato.bytetools.ui.tools.quickorder.domain.model.Participant
import `in`.mahato.bytetools.ui.tools.quickorder.domain.model.ParticipantOrderItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectOrdersScreen(
    orderId: Long,
    navController: NavController,
    viewModel: QuickOrderViewModel
) {
    LaunchedEffect(orderId) {
        viewModel.selectOrder(orderId)
    }

    val order by viewModel.activeOrder.collectAsState()
    val participants by viewModel.participants.collectAsState()
    val allOrderItems by viewModel.orderItems.collectAsState()
    val distinctProducts by viewModel.distinctProducts.collectAsState()

    var selectedParticipantId by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(participants) {
        if (selectedParticipantId == null && participants.isNotEmpty()) {
            selectedParticipantId = participants.first().id
        }
    }

    var showAddParticipantDialog by remember { mutableStateOf(false) }
    var newParticipantName by remember { mutableStateOf("") }

    // Add item form state
    var productName by remember { mutableStateOf("") }
    var quantityText by remember { mutableStateOf("1") }
    var selectedUnit by remember { mutableStateOf("pcs") }
    var estimatedPriceText by remember { mutableStateOf("") }
    var notesText by remember { mutableStateOf("") }
    var showForm by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current

    val currentParticipant = participants.firstOrNull { it.id == selectedParticipantId }
    val currentParticipantItems = allOrderItems.filter { it.participantId == selectedParticipantId }

    // Autocomplete dropdown
    val matchingSuggestions = remember(productName, distinctProducts) {
        if (productName.isBlank()) emptyList()
        else distinctProducts.filter { it.contains(productName, ignoreCase = true) && !it.equals(productName, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(order?.orderName ?: "Collect Orders", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Step 2: Collect Individual Orders",
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
                    IconButton(onClick = { showAddParticipantDialog = true }) {
                        Icon(Icons.Default.PersonAdd, contentDescription = "Add Participant")
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
            // Consolidated Footer
            val totalQty = allOrderItems.sumOf { it.quantity }
            val estimatedTotalPaise = allOrderItems.sumOf {
                ((it.estimatedUnitPricePaise ?: 0L) * it.quantity).toLong()
            }
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        val totalQtyStr = if (totalQty % 1.0 == 0.0) totalQty.toInt().toString() else String.format("%.1f", totalQty)
                        Text(
                            "$totalQtyStr total items ordered",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        if (estimatedTotalPaise > 0L) {
                            Text(
                                "Est: ${SmartSplitEngine.formatPaise(estimatedTotalPaise, order?.currency ?: "₹")}",
                                style = MaterialTheme.typography.labelSmall,
                                color = PurplePrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Button(
                        onClick = { navController.navigate("consolidated_list/$orderId") },
                        colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Consolidated List", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
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
                currentStep = 1,
                onStepClick = { step -> navigateToWizardStep(navController, orderId, step) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Participant Tabs / Selector
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(participants) { p ->
                    val isSelected = p.id == selectedParticipantId
                    val itemCount = allOrderItems.count { it.participantId == p.id }

                    Surface(
                        onClick = { selectedParticipantId = p.id },
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) PurplePrimary else MaterialTheme.colorScheme.surface,
                        border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        shadowElevation = if (isSelected) 2.dp else 0.dp,
                        modifier = Modifier.height(38.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (p.isOrganizer) {
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = "Organizer",
                                    tint = if (isSelected) Color.White else PurplePrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                p.name,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                fontSize = 13.sp
                            )
                            if (itemCount > 0) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) Color.White.copy(alpha = 0.3f) else PurpleBackground),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "$itemCount",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else PurplePrimary
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    OutlinedButton(
                        onClick = { showAddParticipantDialog = true },
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(38.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add", fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Main Items Area for the Selected Participant
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Header for Participant
                item {
                    currentParticipant?.let { p ->
                        val pItems = currentParticipantItems
                        val pSubtotal = pItems.sumOf { ((it.estimatedUnitPricePaise ?: 0L) * it.quantity).toLong() }
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        "${p.name}'s Order",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "${pItems.size} items added",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (pSubtotal > 0L) {
                                    Text(
                                        "Est: ${SmartSplitEngine.formatPaise(pSubtotal, order?.currency ?: "₹")}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = PurplePrimary
                                    )
                                }
                            }
                        }
                    }
                }

                // Add Item Section / Toggle
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showForm = !showForm },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AddCircle, contentDescription = null, tint = PurplePrimary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Add Product for ${currentParticipant?.name ?: ""}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Icon(
                                    if (showForm) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null
                                )
                            }

                            AnimatedVisibility(visible = showForm) {
                                Column(modifier = Modifier.padding(top = 16.dp)) {
                                    OutlinedTextField(
                                        value = productName,
                                        onValueChange = { productName = it },
                                        label = { Text("Product Name *") },
                                        placeholder = { Text("e.g. Flower Pots, Chakkar, Milk") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    )

                                    // Autocomplete suggestions
                                    if (matchingSuggestions.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            items(matchingSuggestions) { sug ->
                                                SuggestionChip(
                                                    onClick = { productName = sug },
                                                    label = { Text(sug, fontSize = 11.sp) }
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = quantityText,
                                            onValueChange = { quantityText = it },
                                            label = { Text("Quantity *") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            singleLine = true,
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp)
                                        )

                                        OutlinedTextField(
                                            value = estimatedPriceText,
                                            onValueChange = { estimatedPriceText = it },
                                            label = { Text("Est. Price (${order?.currency ?: "₹"})") },
                                            placeholder = { Text("Opt.") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            singleLine = true,
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Unit selector chips
                                    Text("Unit", style = MaterialTheme.typography.labelSmall)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        items(listOf("pcs", "packets", "boxes", "kg", "litres", "bottles")) { u ->
                                            val isSel = selectedUnit == u
                                            FilterChip(
                                                selected = isSel,
                                                onClick = { selectedUnit = u },
                                                label = { Text(u, fontSize = 11.sp) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = PurplePrimary,
                                                    selectedLabelColor = Color.White
                                                )
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    OutlinedTextField(
                                        value = notesText,
                                        onValueChange = { notesText = it },
                                        label = { Text("Notes (Optional)") },
                                        placeholder = { Text("e.g. Medium size, specific brand") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Button(
                                        onClick = {
                                            val pId = selectedParticipantId ?: return@Button
                                            val q = quantityText.toDoubleOrNull() ?: 1.0
                                            val estPaise = estimatedPriceText.toDoubleOrNull()?.let { (it * 100).toLong() }
                                            viewModel.addOrMergeOrderItem(
                                                participantId = pId,
                                                orderId = orderId,
                                                productName = productName,
                                                quantity = q,
                                                unit = selectedUnit,
                                                estimatedUnitPricePaise = estPaise,
                                                notes = notesText.ifBlank { null }
                                            )
                                            productName = ""
                                            quantityText = "1"
                                            estimatedPriceText = ""
                                            notesText = ""
                                            focusManager.clearFocus()
                                        },
                                        enabled = productName.isNotBlank(),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Add to Order", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                // Participant's Added Items List
                if (currentParticipantItems.isEmpty()) {
                    item {
                        EmptyQuickOrderView(
                            icon = Icons.Default.AddShoppingCart,
                            title = "No Items Yet",
                            subtitle = "Add products above for ${currentParticipant?.name ?: "this participant"}.",
                            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
                        )
                    }
                } else {
                    items(currentParticipantItems, key = { it.id }) { item ->
                        ParticipantOrderItemCard(
                            item = item,
                            currency = order?.currency ?: "₹",
                            onQuantityChange = { newQty ->
                                if (newQty <= 0.0) {
                                    viewModel.deleteOrderItem(item.id)
                                } else {
                                    viewModel.addOrMergeOrderItem(
                                        participantId = item.participantId,
                                        orderId = orderId,
                                        productName = item.productName,
                                        quantity = newQty - item.quantity,
                                        unit = item.unit,
                                        estimatedUnitPricePaise = item.estimatedUnitPricePaise,
                                        notes = item.notes
                                    )
                                }
                            },
                            onDelete = { viewModel.deleteOrderItem(item.id) }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }

    if (showAddParticipantDialog) {
        AlertDialog(
            onDismissRequest = { showAddParticipantDialog = false },
            title = { Text("Add Participant") },
            text = {
                OutlinedTextField(
                    value = newParticipantName,
                    onValueChange = { newParticipantName = it },
                    label = { Text("Participant Name") },
                    placeholder = { Text("e.g. Priya") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newParticipantName.isNotBlank()) {
                            viewModel.addParticipant(orderId, newParticipantName)
                            newParticipantName = ""
                            showAddParticipantDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary)
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddParticipantDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ParticipantOrderItemCard(
    item: ParticipantOrderItem,
    currency: String,
    onQuantityChange: (Double) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.productName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (item.estimatedUnitPricePaise != null && item.estimatedUnitPricePaise > 0L) {
                    val totalPaise = (item.estimatedUnitPricePaise * item.quantity).toLong()
                    Text(
                        "Est: ${SmartSplitEngine.formatPaise(item.estimatedUnitPricePaise, currency)}/ea → ${SmartSplitEngine.formatPaise(totalPaise, currency)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (!item.notes.isNullOrBlank()) {
                    Text(
                        "Note: ${item.notes}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                QuantityStepper(
                    quantity = item.quantity,
                    unit = item.unit,
                    onQuantityChange = onQuantityChange,
                    minQuantity = 0.0
                )
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "Remove",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
