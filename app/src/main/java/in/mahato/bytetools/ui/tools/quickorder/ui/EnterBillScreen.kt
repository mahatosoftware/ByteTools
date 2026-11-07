package `in`.mahato.bytetools.ui.tools.quickorder.ui

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import `in`.mahato.bytetools.ui.tools.quickorder.domain.engine.ParsedReceipt
import `in`.mahato.bytetools.ui.tools.quickorder.domain.engine.SmartSplitEngine
import `in`.mahato.bytetools.ui.tools.quickorder.domain.model.BillItem
import `in`.mahato.bytetools.ui.tools.quickorder.domain.model.BillSummary
import `in`.mahato.bytetools.ui.tools.quickorder.domain.model.OrderStatus
import `in`.mahato.bytetools.ui.tools.quickorder.domain.model.SplitMethod
import kotlinx.coroutines.launch
import kotlin.math.roundToLong

data class EditableBillItemState(
    val id: Long = 0,
    val productName: String = "",
    val quantityText: String = "1",
    val unit: String = "pcs",
    val unitPriceText: String = "",
    val totalPriceText: String = "",
    val itemDiscountText: String = ""
) {
    val quantity: Double get() = quantityText.replace(',', '.').toDoubleOrNull() ?: 0.0
    val unitPricePaise: Long get() = (unitPriceText.replace(',', '.').toDoubleOrNull()?.times(100))?.roundToLong() ?: 0L
    val itemDiscountPaise: Long get() = (itemDiscountText.replace(',', '.').toDoubleOrNull()?.times(100))?.roundToLong() ?: 0L
    val lineGrossPaise: Long get() {
        val directTotalPaise = (totalPriceText.replace(',', '.').toDoubleOrNull()?.times(100))?.roundToLong()
        return if (directTotalPaise != null && directTotalPaise > 0L) {
            directTotalPaise
        } else {
            (quantity * unitPricePaise).roundToLong()
        }
    }
    val lineNetPaise: Long get() = kotlin.math.max(0L, lineGrossPaise - itemDiscountPaise)
}

private fun formatNumberInput(paise: Long): String {
    if (paise <= 0L) return ""
    return if (paise % 100L == 0L) {
        (paise / 100L).toString()
    } else {
        (paise / 100.0).toString()
    }
}

private fun sanitizeDecimalInput(input: String): String {
    val sanitized = StringBuilder()
    var hasDecimal = false
    for (char in input) {
        if (char.isDigit()) {
            sanitized.append(char)
        } else if ((char == '.' || char == ',') && !hasDecimal) {
            sanitized.append('.')
            hasDecimal = true
        }
    }
    return sanitized.toString()
}

private fun calculateUnitPrice(totalStr: String, qtyStr: String): String {
    val total = totalStr.replace(',', '.').toDoubleOrNull() ?: return ""
    val qty = qtyStr.replace(',', '.').toDoubleOrNull() ?: return ""
    if (qty <= 0.0) return ""
    val u = total / qty
    return if (u % 1.0 == 0.0) {
        u.toLong().toString()
    } else {
        String.format(java.util.Locale.US, "%.2f", u)
    }
}

private fun calculateTotalPrice(unitStr: String, qtyStr: String): String {
    val unit = unitStr.replace(',', '.').toDoubleOrNull() ?: return ""
    val qty = qtyStr.replace(',', '.').toDoubleOrNull() ?: return ""
    if (qty <= 0.0) return ""
    val t = unit * qty
    return if (t % 1.0 == 0.0) {
        t.toLong().toString()
    } else {
        String.format(java.util.Locale.US, "%.2f", t)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnterBillScreen(
    orderId: Long,
    navController: NavController,
    viewModel: QuickOrderViewModel
) {
    LaunchedEffect(orderId) {
        viewModel.selectOrder(orderId)
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val order by viewModel.activeOrder.collectAsState()
    val consolidatedItems by viewModel.consolidatedItems.collectAsState()
    val existingBillItems by viewModel.billItems.collectAsState()
    val existingBillSummary by viewModel.billSummary.collectAsState()

    var selectedMode by remember { mutableIntStateOf(0) } // 0: Manual, 1: Scan OCR

    // Bill items state
    val billItemsList = remember { mutableStateListOf<EditableBillItemState>() }

    // Summary inputs
    var overallDiscountText by remember { mutableStateOf("") }
    var isDiscountPercentage by remember { mutableStateOf(false) }
    var taxText by remember { mutableStateOf("") }
    var additionalChargesText by remember { mutableStateOf("") }
    var chargesNote by remember { mutableStateOf("") }
    var statedFinalBillText by remember { mutableStateOf("") }

    // OCR State
    var isOcrScanning by remember { mutableStateOf(false) }
    var parsedReceipt by remember { mutableStateOf<ParsedReceipt?>(null) }
    var ocrErrorMessage by remember { mutableStateOf<String?>(null) }

    // Populate from existing bill items or from consolidated items
    LaunchedEffect(existingBillItems, consolidatedItems) {
        if (billItemsList.isEmpty()) {
            if (existingBillItems.isNotEmpty()) {
                billItemsList.addAll(existingBillItems.map {
                    val grossPaise = (it.quantity * it.unitPricePaise).roundToLong()
                    EditableBillItemState(
                        id = it.id,
                        productName = it.productName,
                        quantityText = if (it.quantity % 1.0 == 0.0) it.quantity.toInt().toString() else it.quantity.toString(),
                        unit = it.unit,
                        unitPriceText = formatNumberInput(it.unitPricePaise),
                        totalPriceText = formatNumberInput(if (it.totalPricePaise > 0L) it.totalPricePaise + it.itemDiscountPaise else grossPaise),
                        itemDiscountText = formatNumberInput(it.itemDiscountPaise)
                    )
                })
            } else if (consolidatedItems.isNotEmpty()) {
                billItemsList.addAll(consolidatedItems.map {
                    val estUnit = it.estimatedUnitPricePaise
                    val estTotal = estUnit?.let { u -> (it.totalOrderedQuantity * u).roundToLong() }
                    EditableBillItemState(
                        productName = it.displayName,
                        quantityText = if (it.totalOrderedQuantity % 1.0 == 0.0) it.totalOrderedQuantity.toInt().toString() else it.totalOrderedQuantity.toString(),
                        unit = it.unit,
                        unitPriceText = estUnit?.let { p -> formatNumberInput(p) } ?: "",
                        totalPriceText = estTotal?.let { t -> formatNumberInput(t) } ?: "",
                        itemDiscountText = ""
                    )
                })
            }
        }
    }

    LaunchedEffect(existingBillSummary) {
        existingBillSummary?.let { sum ->
            if (overallDiscountText.isBlank() && sum.overallDiscountPaise > 0L) {
                overallDiscountText = if (sum.overallDiscountType == "PERCENTAGE") {
                    if (sum.overallDiscountValue % 1.0 == 0.0) sum.overallDiscountValue.toInt().toString() else sum.overallDiscountValue.toString()
                } else {
                    formatNumberInput(sum.overallDiscountPaise)
                }
                isDiscountPercentage = sum.overallDiscountType == "PERCENTAGE"
            }
            if (taxText.isBlank() && sum.taxPaise > 0L) {
                taxText = formatNumberInput(sum.taxPaise)
            }
            if (additionalChargesText.isBlank() && sum.additionalChargesPaise > 0L) {
                additionalChargesText = formatNumberInput(sum.additionalChargesPaise)
                chargesNote = sum.additionalChargesNote
            }
            if (statedFinalBillText.isBlank() && sum.finalBillAmountPaise > 0L) {
                statedFinalBillText = formatNumberInput(sum.finalBillAmountPaise)
            }
        }
    }

    // Camera / Gallery Image Pickers for OCR
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                isOcrScanning = true
                ocrErrorMessage = null
                try {
                    val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, it)) { decoder, _, _ ->
                            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                            decoder.isMutableRequired = true
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        MediaStore.Images.Media.getBitmap(context.contentResolver, it)
                    }
                    val receipt = viewModel.parseOcrReceipt(context, bitmap)
                    parsedReceipt = receipt
                } catch (e: Exception) {
                    ocrErrorMessage = "OCR failed: ${e.localizedMessage}"
                } finally {
                    isOcrScanning = false
                }
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        bitmap?.let { bmp ->
            scope.launch {
                isOcrScanning = true
                ocrErrorMessage = null
                try {
                    val receipt = viewModel.parseOcrReceipt(context, bmp)
                    parsedReceipt = receipt
                } catch (e: Exception) {
                    ocrErrorMessage = "OCR failed: ${e.localizedMessage}"
                } finally {
                    isOcrScanning = false
                }
            }
        }
    }

    // Calculations
    val subtotalPaise = billItemsList.sumOf { it.lineGrossPaise }
    val itemDiscountsTotalPaise = billItemsList.sumOf { it.itemDiscountPaise }
    val netSubtotalPaise = subtotalPaise - itemDiscountsTotalPaise

    val rawOverallDiscount = overallDiscountText.replace(',', '.').toDoubleOrNull() ?: 0.0
    val overallDiscountPaise = if (isDiscountPercentage) {
        ((netSubtotalPaise * rawOverallDiscount) / 100.0).roundToLong()
    } else {
        (rawOverallDiscount * 100).roundToLong()
    }

    val taxPaise = ((taxText.replace(',', '.').toDoubleOrNull() ?: 0.0) * 100).roundToLong()
    val additionalChargesPaise = ((additionalChargesText.replace(',', '.').toDoubleOrNull() ?: 0.0) * 100).roundToLong()

    val calculatedFinalBillPaise = netSubtotalPaise - overallDiscountPaise + taxPaise + additionalChargesPaise
    val finalBillPaise = statedFinalBillText.replace(',', '.').toDoubleOrNull()?.let { (it * 100).roundToLong() } ?: calculatedFinalBillPaise

    val isFinalized = order?.status == OrderStatus.RECONCILED || order?.status == OrderStatus.SETTLED

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(order?.orderName ?: "Enter Bill", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (isFinalized) "Shopkeeper's Final Bill (View Only)" else "Step 4: Shopkeeper's Final Bill",
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
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Final Bill Amount", style = MaterialTheme.typography.labelSmall)
                        Text(
                            SmartSplitEngine.formatPaise(finalBillPaise, order?.currency ?: "₹"),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = PurplePrimary
                        )
                    }

                    if (isFinalized) {
                        Button(
                            onClick = { navController.navigate("reconciliation/$orderId") },
                            colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("View Reconciliation", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    } else {
                        Button(
                            onClick = {
                                val itemsToSave = billItemsList.map {
                                    val lineGross = it.lineGrossPaise
                                    val uPrice = if (it.quantity > 0.0) {
                                        if (it.unitPricePaise > 0L) it.unitPricePaise else (lineGross / it.quantity).roundToLong()
                                    } else 0L
                                    BillItem(
                                        id = it.id,
                                        groupOrderId = orderId,
                                        productName = it.productName.trim(),
                                        quantity = it.quantity,
                                        unit = it.unit,
                                        unitPricePaise = uPrice,
                                        itemDiscountPaise = it.itemDiscountPaise,
                                        totalPricePaise = it.lineNetPaise
                                    )
                                }
                                val summaryToSave = BillSummary(
                                    groupOrderId = orderId,
                                    subtotalPaise = subtotalPaise,
                                    itemDiscountTotalPaise = itemDiscountsTotalPaise,
                                    overallDiscountType = if (isDiscountPercentage) "PERCENTAGE" else "FIXED",
                                    overallDiscountValue = rawOverallDiscount,
                                    overallDiscountPaise = overallDiscountPaise,
                                    taxPaise = taxPaise,
                                    additionalChargesPaise = additionalChargesPaise,
                                    additionalChargesNote = chargesNote.trim(),
                                    finalBillAmountPaise = finalBillPaise,
                                    discountSplitMethod = SplitMethod.PROPORTIONAL
                                )
                                viewModel.saveBillItemsAndSummary(orderId, itemsToSave, summaryToSave)
                                navController.navigate("reconciliation/$orderId")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Reconcile & Allocate", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
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
                currentStep = 3,
                onStepClick = { step ->
                    when (step) {
                        1 -> navController.navigate("collect_orders/$orderId")
                        2 -> navController.navigate("consolidated_list/$orderId")
                        3 -> { /* current screen */ }
                        4 -> if (isFinalized) navController.navigate("reconciliation/$orderId")
                        5 -> if (isFinalized) navController.navigate("settlement_dashboard/$orderId")
                        6 -> if (isFinalized) navController.navigate("payment_tracking/$orderId")
                    }
                },
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
                            Text("This bill is locked because settlement allocations have been finalized.", fontSize = 11.sp, color = Color(0xFF1B5E20))
                        }
                    }
                }
            }

            if (!isFinalized) {
                // Mode Selector: Manual vs Scan OCR
                TabRow(
                    selectedTabIndex = selectedMode,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = PurplePrimary
                ) {
                    Tab(
                        selected = selectedMode == 0,
                        onClick = { selectedMode = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Manual Entry", fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                    Tab(
                        selected = selectedMode == 1,
                        onClick = { selectedMode = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.DocumentScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Scan Bill (OCR)", fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                }
            }

            if (selectedMode == 1) {
                // OCR Scan Mode
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Scan Bill Receipt", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    "Capture or select an image of the physical shopkeeper's receipt to auto-extract items and prices.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { cameraLauncher.launch(null) },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.CameraAlt, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Camera")
                                    }

                                    Button(
                                        onClick = { imagePickerLauncher.launch("image/*") },
                                        colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Pick Photo")
                                    }
                                }

                                if (isOcrScanning) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = PurplePrimary)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text("Extracting bill data with OCR...", style = MaterialTheme.typography.bodyMedium)
                                    }
                                }

                                if (ocrErrorMessage != null) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(ocrErrorMessage ?: "", color = RedDanger, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }

                    // Parsed OCR Table for Verification
                    parsedReceipt?.let { receipt ->
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "Extracted Items (${receipt.items.size})",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Button(
                                            onClick = {
                                                // Transfer OCR items to manual bill items
                                                billItemsList.clear()
                                                receipt.items.forEach { item ->
                                                    billItemsList.add(
                                                        EditableBillItemState(
                                                            productName = item.productName,
                                                            quantityText = if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else item.quantity.toString(),
                                                            unit = item.unit,
                                                            unitPriceText = formatNumberInput(item.unitPricePaise),
                                                            totalPriceText = formatNumberInput(item.totalPricePaise),
                                                            itemDiscountText = ""
                                                        )
                                                    )
                                                }
                                                receipt.discountPaise?.let {
                                                    overallDiscountText = formatNumberInput(it)
                                                }
                                                receipt.taxPaise?.let {
                                                    taxText = formatNumberInput(it)
                                                }
                                                receipt.grandTotalPaise?.let {
                                                    statedFinalBillText = formatNumberInput(it)
                                                }
                                                selectedMode = 0 // Switch to manual to inspect & edit
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = GreenSuccess),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text("Import to Bill", fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        "Please verify OCR data below before importing. You can adjust prices directly in Manual mode.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))

                                    receipt.items.forEachIndexed { idx, itm ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("${idx + 1}. ${itm.productName} (${itm.quantity} ${itm.unit})", style = MaterialTheme.typography.bodyMedium)
                                            Text(
                                                SmartSplitEngine.formatPaise(itm.totalPricePaise, order?.currency ?: "₹"),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Mode 0: Manual Entry
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = PurpleBackground),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = PurplePrimary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    "Enter the Total Price from your bill — it will automatically split by quantity to calculate the unit price (or enter the unit price to calculate total).",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = PurpleDark
                                )
                            }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Purchased Products (${billItemsList.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (!isFinalized) {
                                TextButton(
                                    onClick = {
                                        billItemsList.add(
                                            EditableBillItemState(
                                                productName = "",
                                                quantityText = "1",
                                                unit = "pcs",
                                                unitPriceText = "",
                                                totalPriceText = "",
                                                itemDiscountText = ""
                                            )
                                        )
                                    }
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add Product", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    itemsIndexed(billItemsList) { index, itemState ->
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = itemState.productName,
                                        onValueChange = { newName ->
                                            if (!isFinalized) {
                                                billItemsList[index] = itemState.copy(productName = newName)
                                            }
                                        },
                                        label = { Text("Product #${index + 1}") },
                                        readOnly = isFinalized,
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    if (!isFinalized) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        IconButton(
                                            onClick = { billItemsList.removeAt(index) },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "Remove", tint = RedDanger)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Row 1: Quantity and Total Price from Bill
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = itemState.quantityText,
                                        onValueChange = { newQty ->
                                            if (!isFinalized) {
                                                val cleanQty = sanitizeDecimalInput(newQty)
                                                val updatedState = when {
                                                    itemState.totalPriceText.isNotBlank() -> {
                                                        val newUnit = calculateUnitPrice(itemState.totalPriceText, cleanQty)
                                                        itemState.copy(quantityText = cleanQty, unitPriceText = newUnit.ifBlank { itemState.unitPriceText })
                                                    }
                                                    itemState.unitPriceText.isNotBlank() -> {
                                                        val newTotal = calculateTotalPrice(itemState.unitPriceText, cleanQty)
                                                        itemState.copy(quantityText = cleanQty, totalPriceText = newTotal.ifBlank { itemState.totalPriceText })
                                                    }
                                                    else -> itemState.copy(quantityText = cleanQty)
                                                }
                                                billItemsList[index] = updatedState
                                            }
                                        },
                                        label = { Text("Qty (${itemState.unit})") },
                                        readOnly = isFinalized,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp)
                                    )

                                    OutlinedTextField(
                                        value = itemState.totalPriceText,
                                        onValueChange = { newTotalRaw ->
                                            if (!isFinalized) {
                                                val newTotal = sanitizeDecimalInput(newTotalRaw)
                                                val calculatedUnit = if (newTotal.isBlank()) "" else calculateUnitPrice(newTotal, itemState.quantityText)
                                                billItemsList[index] = itemState.copy(
                                                    totalPriceText = newTotal,
                                                    unitPriceText = if (newTotal.isBlank()) "" else (calculatedUnit.ifBlank { itemState.unitPriceText })
                                                )
                                            }
                                        },
                                        label = { Text("Total Bill Price (${order?.currency ?: "₹"})") },
                                        placeholder = { Text("e.g. 600") },
                                        readOnly = isFinalized,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        modifier = Modifier.weight(1.5f),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = PurplePrimary,
                                            focusedLabelColor = PurplePrimary
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Row 2: Actual Unit Price (calculated or direct entry) and Item Discount
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = itemState.unitPriceText,
                                        onValueChange = { newUnitRaw ->
                                            if (!isFinalized) {
                                                val newUnit = sanitizeDecimalInput(newUnitRaw)
                                                val calculatedTotal = if (newUnit.isBlank()) "" else calculateTotalPrice(newUnit, itemState.quantityText)
                                                billItemsList[index] = itemState.copy(
                                                    unitPriceText = newUnit,
                                                    totalPriceText = if (newUnit.isBlank()) "" else (calculatedTotal.ifBlank { itemState.totalPriceText })
                                                )
                                            }
                                        },
                                        label = { Text("Unit Price (${order?.currency ?: "₹"})") },
                                        placeholder = { Text("Auto-split") },
                                        readOnly = isFinalized,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        modifier = Modifier.weight(1.2f),
                                        shape = RoundedCornerShape(10.dp)
                                    )

                                    OutlinedTextField(
                                        value = itemState.itemDiscountText,
                                        onValueChange = { newDiscRaw ->
                                            if (!isFinalized) {
                                                val newDisc = sanitizeDecimalInput(newDiscRaw)
                                                billItemsList[index] = itemState.copy(itemDiscountText = newDisc)
                                            }
                                        },
                                        label = { Text("Item Disc (${order?.currency ?: "₹"})") },
                                        placeholder = { Text("0") },
                                        readOnly = isFinalized,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Row 3: Summary details and Net Amount
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val curr = order?.currency ?: "₹"
                                    Text(
                                        if (itemState.unitPricePaise > 0L) {
                                            "${itemState.quantityText.ifBlank { "0" }} ${itemState.unit} @ ${SmartSplitEngine.formatPaise(itemState.unitPricePaise, curr)}"
                                        } else {
                                            "Unit: ${itemState.unit}"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (itemState.itemDiscountPaise > 0L) {
                                            Text(
                                                "-${SmartSplitEngine.formatPaise(itemState.itemDiscountPaise, curr)} disc  ",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = GreenSuccess
                                            )
                                        }
                                        Text(
                                            "Total: ${SmartSplitEngine.formatPaise(itemState.lineNetPaise, curr)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = PurplePrimary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Discounts, Taxes & Additional Charges Section
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Discounts & Extra Charges",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                // Overall Discount
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = overallDiscountText,
                                        onValueChange = { if (!isFinalized) overallDiscountText = sanitizeDecimalInput(it) },
                                        label = { Text(if (isDiscountPercentage) "Overall Discount (%)" else "Overall Discount (${order?.currency ?: "₹"})") },
                                        placeholder = { Text("0") },
                                        readOnly = isFinalized,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp)
                                    )

                                    FilterChip(
                                        selected = isDiscountPercentage,
                                        onClick = { if (!isFinalized) isDiscountPercentage = !isDiscountPercentage },
                                        enabled = !isFinalized,
                                        label = { Text(if (isDiscountPercentage) "%" else order?.currency ?: "₹") },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = PurplePrimary,
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Taxes
                                OutlinedTextField(
                                    value = taxText,
                                    onValueChange = { if (!isFinalized) taxText = sanitizeDecimalInput(it) },
                                    label = { Text("Taxes / GST (${order?.currency ?: "₹"})") },
                                    placeholder = { Text("0") },
                                    readOnly = isFinalized,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // Delivery / Packaging Charges
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = additionalChargesText,
                                        onValueChange = { if (!isFinalized) additionalChargesText = sanitizeDecimalInput(it) },
                                        label = { Text("Extra Charges (${order?.currency ?: "₹"})") },
                                        placeholder = { Text("0") },
                                        readOnly = isFinalized,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp)
                                    )

                                    OutlinedTextField(
                                        value = chargesNote,
                                        onValueChange = { if (!isFinalized) chargesNote = it },
                                        label = { Text("Charges Reason") },
                                        placeholder = { Text("e.g. Packaging, Delivery") },
                                        readOnly = isFinalized,
                                        singleLine = true,
                                        modifier = Modifier.weight(1.2f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.height(14.dp))

                                // Bill Summary Breakdown
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Subtotal:", style = MaterialTheme.typography.bodyMedium)
                                    Text(SmartSplitEngine.formatPaise(subtotalPaise, order?.currency ?: "₹"), fontWeight = FontWeight.Bold)
                                }
                                if (overallDiscountPaise > 0L) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Discount Savings:", style = MaterialTheme.typography.bodyMedium, color = GreenSuccess)
                                        Text("-${SmartSplitEngine.formatPaise(overallDiscountPaise, order?.currency ?: "₹")}", fontWeight = FontWeight.Bold, color = GreenSuccess)
                                    }
                                }
                                if (taxPaise > 0L) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Taxes:", style = MaterialTheme.typography.bodyMedium)
                                        Text("+${SmartSplitEngine.formatPaise(taxPaise, order?.currency ?: "₹")}", fontWeight = FontWeight.Bold)
                                    }
                                }
                                if (additionalChargesPaise > 0L) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Additional Charges:", style = MaterialTheme.typography.bodyMedium)
                                        Text("+${SmartSplitEngine.formatPaise(additionalChargesPaise, order?.currency ?: "₹")}", fontWeight = FontWeight.Bold)
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = statedFinalBillText,
                                    onValueChange = { statedFinalBillText = sanitizeDecimalInput(it) },
                                    label = { Text("Stated Final Bill Amount (${order?.currency ?: "₹"})") },
                                    placeholder = { Text(SmartSplitEngine.formatPaise(calculatedFinalBillPaise, order?.currency ?: "₹")) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }
        }
    }
}
