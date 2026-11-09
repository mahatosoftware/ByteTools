package `in`.mahato.bytetools.ui.tools.quickorder.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.mahato.bytetools.ui.tools.quickorder.domain.engine.PdfReportType
import `in`.mahato.bytetools.ui.tools.quickorder.domain.engine.QuickOrderPdfGenerator
import `in`.mahato.bytetools.ui.tools.quickorder.domain.engine.QuickOrderPdfManager
import `in`.mahato.bytetools.ui.tools.quickorder.domain.engine.SmartSplitEngine
import `in`.mahato.bytetools.ui.tools.quickorder.domain.model.*
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickOrderPdfExportBottomSheet(
    order: GroupOrder,
    summary: BillSummary?,
    settlements: List<ParticipantSettlement>,
    participants: List<Participant>,
    orderItems: List<ParticipantOrderItem>,
    consolidatedItems: List<ConsolidatedItem>,
    billItems: List<BillItem>,
    payments: List<PaymentRecord>,
    purchasePayments: List<PurchasePayment> = emptyList(),
    settlementTransfers: List<SettlementTransfer> = emptyList(),
    navController: androidx.navigation.NavController? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    var selectedReportType by remember { mutableStateOf(PdfReportType.COMPLETE_SETTLEMENT) }
    var selectedParticipantId by remember { mutableStateOf(participants.firstOrNull()?.id ?: 0L) }
    var generatedFile by remember { mutableStateOf<File?>(null) }
    var isGenerating by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    // Validation: Ensure final payable amounts match the final bill
    val totalBill = summary?.finalBillAmountPaise ?: 0L
    val totalAllocated = settlements.sumOf { it.finalPayablePaise }
    val unallocatedDiff = totalBill - totalAllocated

    fun generatePdf(action: (File) -> Unit) {
        // Validation check for settlement reports
        if (selectedReportType == PdfReportType.COMPLETE_SETTLEMENT || selectedReportType == PdfReportType.INDIVIDUAL_SETTLEMENT) {
            if (settlements.isEmpty() || summary == null) {
                validationError = "Settlement is not finalized yet. Please enter the bill and reconcile first."
                return
            }
            if (kotlin.math.abs(unallocatedDiff) > 0L) {
                validationError = "Cannot export: Final payable amounts have an unallocated difference of ${SmartSplitEngine.formatPaise(unallocatedDiff, order.currency)}. Please balance allocations first."
                return
            }
        }

        validationError = null
        isGenerating = true

        try {
            val file = when (selectedReportType) {
                PdfReportType.COMPLETE_SETTLEMENT -> {
                    QuickOrderPdfGenerator.generateCompleteSettlementReport(
                        context = context,
                        order = order,
                        summary = summary!!,
                        settlements = settlements,
                        purchasePayments = purchasePayments,
                        settlementTransfers = settlementTransfers
                    )
                }
                PdfReportType.INDIVIDUAL_SETTLEMENT -> {
                    val pSettlement = settlements.firstOrNull { it.participantId == selectedParticipantId }
                        ?: settlements.first()
                    QuickOrderPdfGenerator.generateIndividualSettlementReport(
                        context = context,
                        order = order,
                        settlement = pSettlement,
                        payments = payments,
                        settlementTransfers = settlementTransfers
                    )
                }
                PdfReportType.FINAL_SHOP_BILL -> {
                    QuickOrderPdfGenerator.generateFinalShopBillReport(
                        context = context,
                        order = order,
                        billItems = billItems,
                        summary = summary ?: BillSummary(groupOrderId = order.id, subtotalPaise = 0L, finalBillAmountPaise = 0L)
                    )
                }
                PdfReportType.COMPLETE_GROUP_ORDER -> {
                    QuickOrderPdfGenerator.generateCompleteGroupOrderReport(
                        context = context,
                        order = order,
                        participants = participants,
                        orderItems = orderItems,
                        consolidatedItems = consolidatedItems
                    )
                }
            }
            generatedFile = file
            action(file)
        } catch (e: Exception) {
            validationError = "Error generating PDF: ${e.localizedMessage}"
        } finally {
            isGenerating = false
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
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
                            .background(PurpleBackground),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = PurplePrimary, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("PDF Export & Printable Reports", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Report Option Selectors
            Text("Select Report Type", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            ReportOptionTile(
                title = "Complete Settlement Report",
                subtitle = "Combined executive summary, all members' payables & item breakdowns",
                icon = Icons.Default.Assessment,
                isSelected = selectedReportType == PdfReportType.COMPLETE_SETTLEMENT,
                onClick = { selectedReportType = PdfReportType.COMPLETE_SETTLEMENT; validationError = null }
            )

            Spacer(modifier = Modifier.height(8.dp))

            ReportOptionTile(
                title = "Individual Settlement Statement",
                subtitle = "Personalized receipt & calculation statement for a specific participant",
                icon = Icons.Default.Person,
                isSelected = selectedReportType == PdfReportType.INDIVIDUAL_SETTLEMENT,
                onClick = { selectedReportType = PdfReportType.INDIVIDUAL_SETTLEMENT; validationError = null }
            )

            // Participant selector if Individual chosen
            if (selectedReportType == PdfReportType.INDIVIDUAL_SETTLEMENT && participants.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Select Member:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            participants.forEach { p ->
                                val isSel = selectedParticipantId == p.id
                                FilterChip(
                                    selected = isSel,
                                    onClick = { selectedParticipantId = p.id },
                                    label = { Text(p.name, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = PurplePrimary,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            ReportOptionTile(
                title = "Shopkeeper's Final Bill",
                subtitle = "Full purchased items list, actual prices, discounts, and charges",
                icon = Icons.Default.Receipt,
                isSelected = selectedReportType == PdfReportType.FINAL_SHOP_BILL,
                onClick = { selectedReportType = PdfReportType.FINAL_SHOP_BILL; validationError = null }
            )

            Spacer(modifier = Modifier.height(8.dp))

            ReportOptionTile(
                title = "Complete Group Order Report",
                subtitle = "Initial collection, organizer details & consolidated shopping list",
                icon = Icons.AutoMirrored.Filled.ListAlt,
                isSelected = selectedReportType == PdfReportType.COMPLETE_GROUP_ORDER,
                onClick = { selectedReportType = PdfReportType.COMPLETE_GROUP_ORDER; validationError = null }
            )

            if (validationError != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = RedBackground,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        validationError ?: "",
                        color = RedDanger,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            if (isGenerating) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = PurplePrimary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Generating ByteTools PDF...", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
            } else {
                // PDF Action Buttons Grid
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                generatePdf { file ->
                                    QuickOrderPdfManager.sharePdf(context, file)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(46.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share PDF", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        Button(
                            onClick = {
                                generatePdf { file ->
                                    QuickOrderPdfManager.printPdf(context, file, order.orderName)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0369A1)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(46.dp)
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Print PDF", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                generatePdf { file ->
                                    QuickOrderPdfManager.savePdfToDownloads(context, file)
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(46.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save to Device", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                generatePdf { file ->
                                    QuickOrderPdfManager.previewPdf(context, file, navController)
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(46.dp)
                        ) {
                            Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Preview PDF", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun ReportOptionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) PurpleBackground else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, PurplePrimary) else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) PurplePrimary else MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) PurplePrimary else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 14.sp
                )
            }
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(selectedColor = PurplePrimary)
            )
        }
    }
}
