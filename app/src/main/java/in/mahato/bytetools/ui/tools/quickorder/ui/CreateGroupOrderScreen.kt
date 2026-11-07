package `in`.mahato.bytetools.ui.tools.quickorder.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import `in`.mahato.bytetools.ui.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateGroupOrderScreen(
    navController: NavController,
    viewModel: QuickOrderViewModel
) {
    val focusManager = LocalFocusManager.current

    var orderName by remember { mutableStateOf("") }
    var organizerName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("₹") }
    var newParticipantInput by remember { mutableStateOf("") }

    // Pre-populate with sample group members as prompt suggests
    val participants = remember { mutableStateListOf<String>() }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun addParticipant() {
        val trimmed = newParticipantInput.trim()
        if (trimmed.isNotBlank()) {
            if (participants.any { it.equals(trimmed, ignoreCase = true) } ||
                trimmed.equals(organizerName.trim(), ignoreCase = true)
            ) {
                errorMessage = "Participant '$trimmed' already added!"
            } else {
                participants.add(trimmed)
                newParticipantInput = ""
                errorMessage = null
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Group Order", fontWeight = FontWeight.Bold) },
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            OrderStepIndicator(currentStep = 0)

            Spacer(modifier = Modifier.height(20.dp))

            // Order Info Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Order Details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = orderName,
                        onValueChange = { orderName = it; errorMessage = null },
                        label = { Text("Order Name *") },
                        placeholder = { Text("e.g. Diwali Shopping, Grocery Run") },
                        leadingIcon = { Icon(Icons.Default.ShoppingBag, contentDescription = null, tint = PurplePrimary) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = organizerName,
                        onValueChange = { organizerName = it; errorMessage = null },
                        label = { Text("Organizer Name *") },
                        placeholder = { Text("e.g. Debasish") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = PurplePrimary) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Group Description (Optional)") },
                        placeholder = { Text("e.g. Crackers purchase from Sivakasi store") },
                        leadingIcon = { Icon(Icons.Default.Notes, contentDescription = null, tint = PurplePrimary) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Currency Selection
                    Text("Currency", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("₹ (INR)", "$ (USD)", "€ (EUR)", "£ (GBP)").forEach { currOption ->
                            val symbol = currOption.substring(0, 1)
                            val isSelected = currency == symbol
                            FilterChip(
                                selected = isSelected,
                                onClick = { currency = symbol },
                                label = { Text(currOption) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PurplePrimary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Participants Card
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
                        Text(
                            "Group Participants (${participants.size + if (organizerName.isNotBlank()) 1 else 0})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newParticipantInput,
                            onValueChange = { newParticipantInput = it; errorMessage = null },
                            label = { Text("Add Participant Name") },
                            placeholder = { Text("e.g. Amit, Rahul, Suman") },
                            leadingIcon = { Icon(Icons.Default.GroupAdd, contentDescription = null, tint = PurplePrimary) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                addParticipant()
                                focusManager.clearFocus()
                            }),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        FilledIconButton(
                            onClick = { addParticipant() },
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = PurplePrimary),
                            modifier = Modifier.size(52.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add")
                        }
                    }

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(errorMessage ?: "", color = RedDanger, style = MaterialTheme.typography.labelSmall)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Chips
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Organizer Chip (Fixed)
                        if (organizerName.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = PurpleBackground,
                                modifier = Modifier.height(36.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Star,
                                        contentDescription = "Organizer",
                                        tint = PurplePrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "$organizerName (Organizer)",
                                        fontWeight = FontWeight.Bold,
                                        color = PurplePrimary,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }

                        // Added Participants
                        participants.forEachIndexed { index, name ->
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.height(36.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(start = 12.dp, end = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(name, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                    IconButton(
                                        onClick = { participants.removeAt(index) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Remove",
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (participants.isEmpty() && organizerName.isBlank()) {
                        Text(
                            "Add participants who will submit items to this order.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (orderName.isBlank()) {
                        errorMessage = "Please enter an order name."
                        return@Button
                    }
                    if (organizerName.isBlank()) {
                        errorMessage = "Please enter an organizer name."
                        return@Button
                    }
                    viewModel.createOrder(
                        orderName = orderName,
                        organizerName = organizerName,
                        description = description.ifBlank { null },
                        currency = currency,
                        participants = participants.toList(),
                        onCreated = { orderId ->
                            navController.navigate("collect_orders/$orderId") {
                                popUpTo(Screen.CreateGroupOrder.route) { inclusive = true }
                            }
                        }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    "Start Collecting Orders",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color.White)
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
