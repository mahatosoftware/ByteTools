package `in`.mahato.bytetools.ui.tools.quickorder.domain.engine

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import `in`.mahato.bytetools.ui.tools.quickorder.domain.model.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.max

enum class PdfReportType {
    COMPLETE_GROUP_ORDER,
    FINAL_SHOP_BILL,
    INDIVIDUAL_SETTLEMENT,
    COMPLETE_SETTLEMENT
}

/**
 * High-performance, professional font provider for Inter typeface family.
 * Copies the font binary to local storage so Skia's PDF engine embeds the full
 * TrueType outlines directly into the output PDF for solid, crisp, vector typography.
 */
object QuickOrderFontProvider {
    private var regular: Typeface? = null
    private var medium: Typeface? = null
    private var semiBold: Typeface? = null
    private var bold: Typeface? = null

    fun getRegular(context: Context?): Typeface {
        if (regular != null) return regular!!
        regular = loadTypeface(context, "Inter-Regular.ttf", Typeface.NORMAL, "sans-serif")
        return regular!!
    }

    fun getMedium(context: Context?): Typeface {
        if (medium != null) return medium!!
        medium = loadTypeface(context, "Inter-Medium.ttf", Typeface.NORMAL, "sans-serif-medium")
        return medium!!
    }

    fun getSemiBold(context: Context?): Typeface {
        if (semiBold != null) return semiBold!!
        semiBold = loadTypeface(context, "Inter-SemiBold.ttf", Typeface.BOLD, "sans-serif-medium")
        return semiBold!!
    }

    fun getBold(context: Context?): Typeface {
        if (bold != null) return bold!!
        bold = loadTypeface(context, "Inter-Bold.ttf", Typeface.BOLD, "sans-serif")
        return bold!!
    }

    private fun loadTypeface(context: Context?, filename: String, fallbackStyle: Int, fallbackFamily: String): Typeface {
        if (context != null) {
            try {
                val fontDir = File(context.cacheDir, "fonts")
                if (!fontDir.exists()) fontDir.mkdirs()
                val fontFile = File(fontDir, filename)
                if (!fontFile.exists() || fontFile.length() == 0L) {
                    context.assets.open("fonts/$filename").use { input ->
                        FileOutputStream(fontFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                }
                if (fontFile.exists() && fontFile.length() > 0L) {
                    return Typeface.createFromFile(fontFile)
                }
            } catch (_: Exception) {}

            try {
                return Typeface.createFromAsset(context.assets, "fonts/$filename")
            } catch (_: Exception) {}
        }
        // Local file fallback for tests running on JVM
        try {
            val localFile = File("app/src/main/assets/fonts/$filename")
            if (localFile.exists()) {
                return Typeface.createFromFile(localFile)
            }
        } catch (_: Exception) {}
        return Typeface.create(fallbackFamily, fallbackStyle)
    }
}

object QuickOrderPdfGenerator {

    private const val PAGE_WIDTH = 595 // A4 standard width in points (72 DPI)
    private const val PAGE_HEIGHT = 842 // A4 standard height in points (72 DPI)
    private const val MARGIN = 36f // 0.5 inch margin
    private const val CONTENT_WIDTH = PAGE_WIDTH - (MARGIN * 2) // 523f

    // Solid High-Contrast Color Palette (No faint, transparent or blurry tones)
    private val COLOR_TEXT_SOLID_BLACK = Color.rgb(15, 23, 42)    // #0F172A (Deep Solid Black-Charcoal)
    private val COLOR_TEXT_SOLID_DARK = Color.rgb(30, 41, 59)     // #1E293B (Solid Navy Charcoal)
    private val COLOR_TEXT_SOLID_MUTED = Color.rgb(51, 65, 85)    // #334155 (Solid Secondary Charcoal)
    private val COLOR_HEADING_PURPLE = Color.rgb(91, 33, 182)     // #5B21B6 (Deep Solid Royal Purple)
    private val COLOR_PRIMARY_PURPLE = Color.rgb(103, 58, 183)    // #673AB7 (Brand Purple)
    private val COLOR_BORDER_SOLID = Color.rgb(203, 213, 225)     // #CBD5E1 (Solid Slate Border)
    private val COLOR_BORDER_SUBTLE = Color.rgb(226, 232, 240)    // #E2E8F0 (Subtle Table Border)
    private val COLOR_ROW_ALT = Color.rgb(248, 250, 252)          // #F8FAFC (Subtle Alternate Row)
    private val COLOR_PURPLE_BG = Color.rgb(237, 231, 246)        // #EDE7F6 (Highlight Fill)

    // Financial Status Colors (Solid, Vibrant, Punchy)
    private val COLOR_GREEN_TEXT = Color.rgb(21, 128, 61)         // #15803D (Solid Green)
    private val COLOR_GREEN_BG = Color.rgb(220, 252, 231)         // #DCFCE7 (Green Pill Fill)
    private val COLOR_GREEN_BORDER = Color.rgb(134, 239, 172)     // #86EFAC (Green Pill Border)

    private val COLOR_RED_TEXT = Color.rgb(185, 28, 28)           // #B91C1C (Solid Red)
    private val COLOR_RED_BG = Color.rgb(254, 226, 226)           // #FEE2E2 (Red Pill Fill)
    private val COLOR_RED_BORDER = Color.rgb(254, 202, 202)       // #FECACA (Red Pill Border)

    private val COLOR_AMBER_TEXT = Color.rgb(180, 83, 9)          // #B45309 (Solid Amber)
    private val COLOR_AMBER_BG = Color.rgb(254, 243, 199)         // #FEF3C7 (Amber Pill Fill)
    private val COLOR_AMBER_BORDER = Color.rgb(253, 230, 138)     // #FDE68A (Amber Pill Border)

    private val COLOR_BLUE_TEXT = Color.rgb(29, 78, 216)          // #1D4ED8 (Solid Blue)
    private val COLOR_BLUE_BG = Color.rgb(239, 246, 255)          // #EFF6FF (Blue Card Fill)
    private val COLOR_BLUE_BORDER = Color.rgb(191, 219, 254)      // #BFDBFE (Blue Card Border)

    // Solid Typography Scale
    private const val SIZE_TITLE = 19f
    private const val SIZE_SUBTITLE = 11f
    private const val SIZE_SECTION_TITLE = 14f
    private const val SIZE_MEMBER_NAME = 13f
    private const val SIZE_CARD_AMOUNT = 16f
    private const val SIZE_CARD_LABEL = 10f
    private const val SIZE_SUMMARY_LABEL = 11f
    private const val SIZE_SUMMARY_VALUE = 11.5f
    private const val SIZE_TABLE_HEADER = 10f
    private const val SIZE_TABLE_CONTENT = 10f
    private const val SIZE_FOOTER = 9f

    /**
     * Generates Report A: Complete Group Order Report
     */
    fun generateCompleteGroupOrderReport(
        context: Context,
        order: GroupOrder,
        participants: List<Participant>,
        orderItems: List<ParticipantOrderItem>,
        consolidatedItems: List<ConsolidatedItem>
    ): File {
        val curr = order.currency
        val orderDateStr = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.getDefault()).format(Date(order.createdAt))

        return PdfReportRenderer.render(
            context = context,
            orderName = order.orderName,
            reportTitle = "Complete Group Order Report",
            filename = "GroupOrder_${sanitizeFilename(order.orderName)}.pdf"
        ) { renderer ->
            renderer.addSectionTitle("1. Order & Organizer Details")
            renderer.addKeyValuePairs(
                listOf(
                    "Order Name" to order.orderName,
                    "Organizer" to order.organizerName,
                    "Created Date" to orderDateStr,
                    "Total Participants" to "${participants.size} members",
                    "Description" to (order.description ?: "None")
                )
            )

            renderer.addSpacing(14f)
            renderer.addSectionTitle("2. Consolidated Shopping List")
            val consHeaders = listOf("Product", "Quantity", "Unit", "Est. Unit Price", "Ordered By")
            val consColWidths = floatArrayOf(150f, 55f, 45f, 95f, 178f)
            val consAlignments = listOf(Paint.Align.LEFT, Paint.Align.RIGHT, Paint.Align.LEFT, Paint.Align.RIGHT, Paint.Align.LEFT)

            val consRows = consolidatedItems.map { item ->
                val qtyStr = if (item.totalOrderedQuantity % 1.0 == 0.0) item.totalOrderedQuantity.toInt().toString() else String.format(Locale.US, "%.1f", item.totalOrderedQuantity)
                val estStr = item.estimatedUnitPricePaise?.let { SmartSplitEngine.formatPaise(it, curr) } ?: "-"
                val buyers = item.participantOrders.joinToString(", ") {
                    val q = if (it.quantity % 1.0 == 0.0) it.quantity.toInt().toString() else String.format(Locale.US, "%.1f", it.quantity)
                    "${it.participantName} ($q)"
                }
                listOf(item.displayName, qtyStr, item.unit, estStr, buyers)
            }
            renderer.addSolidTable(consHeaders, consRows, consColWidths, consAlignments)

            renderer.addSpacing(16f)
            renderer.addSectionTitle("3. Individual Participant Orders")

            for (p in participants) {
                val pItems = orderItems.filter { it.participantId == p.id }
                val roleLabel = if (p.isOrganizer) " (Organizer)" else ""

                if (pItems.isEmpty()) {
                    renderer.addMemberHeader("${p.name}$roleLabel", "No items added", "")
                    renderer.addParagraph("No items ordered.")
                    renderer.addDivider()
                } else {
                    renderer.addMemberHeader("${p.name}$roleLabel", "${pItems.size} items", "")
                    val pHeaders = listOf("Product", "Qty", "Unit", "Est. Unit Price", "Est. Total", "Notes")
                    val pColWidths = floatArrayOf(150f, 45f, 45f, 95f, 95f, 93f)
                    val pAlignments = listOf(Paint.Align.LEFT, Paint.Align.RIGHT, Paint.Align.LEFT, Paint.Align.RIGHT, Paint.Align.RIGHT, Paint.Align.LEFT)

                    val pRows = pItems.map { item ->
                        val qStr = if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else String.format(Locale.US, "%.1f", item.quantity)
                        val estUnitPriceStr = item.estimatedUnitPricePaise?.let { SmartSplitEngine.formatPaise(it, curr) } ?: "-"
                        val estTotalStr = item.estimatedUnitPricePaise?.let { SmartSplitEngine.formatPaise((it * item.quantity).toLong(), curr) } ?: "-"
                        listOf(item.productName, qStr, item.unit, estUnitPriceStr, estTotalStr, item.notes ?: "-")
                    }
                    renderer.addSolidTable(pHeaders, pRows, pColWidths, pAlignments)
                    renderer.addDivider()
                }
            }
        }
    }

    /**
     * Generates Report B: Final Shopkeeper Bill Report
     */
    fun generateFinalShopBillReport(
        context: Context,
        order: GroupOrder,
        billItems: List<BillItem>,
        summary: BillSummary
    ): File {
        val curr = order.currency
        val dateStr = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.getDefault()).format(Date(System.currentTimeMillis()))

        return PdfReportRenderer.render(
            context = context,
            orderName = order.orderName,
            reportTitle = "Shopkeeper's Final Bill Report",
            filename = "ShopBill_${sanitizeFilename(order.orderName)}.pdf"
        ) { renderer ->
            renderer.addSectionTitle("1. Bill Summary & Store Details")
            renderer.addKeyValuePairs(
                listOf(
                    "Order Name" to order.orderName,
                    "Organizer / Buyer" to order.organizerName,
                    "Purchase Date" to dateStr,
                    "Total Purchased Items" to "${billItems.size} unique products",
                    "Final Bill Amount" to SmartSplitEngine.formatPaise(summary.finalBillAmountPaise, curr)
                )
            )

            renderer.addSpacing(14f)
            renderer.addSectionTitle("2. Itemized Purchased Products")
            val headers = listOf("Product", "Purchased Qty", "Unit Price", "Gross Amount", "Item Disc.", "Net Amount")
            val colWidths = floatArrayOf(155f, 60f, 75f, 78f, 75f, 80f)
            val alignments = listOf(Paint.Align.LEFT, Paint.Align.RIGHT, Paint.Align.RIGHT, Paint.Align.RIGHT, Paint.Align.RIGHT, Paint.Align.RIGHT)

            val rows = billItems.map { item ->
                val qtyStr = if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else String.format(Locale.US, "%.1f", item.quantity)
                val gross = (item.quantity * item.unitPricePaise).toLong()
                listOf(
                    "${item.productName} (${item.unit})",
                    qtyStr,
                    SmartSplitEngine.formatPaise(item.unitPricePaise, curr),
                    SmartSplitEngine.formatPaise(gross, curr),
                    if (item.itemDiscountPaise > 0L) "-${SmartSplitEngine.formatPaise(item.itemDiscountPaise, curr)}" else "-",
                    SmartSplitEngine.formatPaise(item.totalPricePaise, curr)
                )
            }
            renderer.addSolidTable(headers, rows, colWidths, alignments)

            renderer.addSpacing(14f)
            renderer.addSectionTitle("3. Bill Deductions & Extra Charges")
            val summaryRows = mutableListOf<Pair<String, String>>()
            summaryRows.add("Items Subtotal" to SmartSplitEngine.formatPaise(summary.subtotalPaise, curr))

            if (summary.itemDiscountTotalPaise > 0L) {
                summaryRows.add("Total Item-Specific Discounts" to "-${SmartSplitEngine.formatPaise(summary.itemDiscountTotalPaise, curr)}")
            }
            if (summary.overallDiscountPaise > 0L) {
                val discLabel = if (summary.overallDiscountType == "PERCENTAGE") "Overall Discount (${summary.overallDiscountValue}%)" else "Overall Discount"
                summaryRows.add(discLabel to "-${SmartSplitEngine.formatPaise(summary.overallDiscountPaise, curr)}")
            }
            if (summary.taxPaise > 0L) {
                summaryRows.add("Taxes / GST" to "+${SmartSplitEngine.formatPaise(summary.taxPaise, curr)}")
            }
            if (summary.additionalChargesPaise > 0L) {
                val note = summary.additionalChargesNote.ifBlank { "Delivery & Packaging" }
                summaryRows.add("Additional Charges ($note)" to "+${SmartSplitEngine.formatPaise(summary.additionalChargesPaise, curr)}")
            }
            summaryRows.add("Final Paid Bill Amount" to SmartSplitEngine.formatPaise(summary.finalBillAmountPaise, curr))
            renderer.addKeyValuePairs(summaryRows, highlightLast = true)
        }
    }

    /**
     * Generates Report C: Individual Settlement Report (Single Participant)
     */
    fun generateIndividualSettlementReport(
        context: Context,
        order: GroupOrder,
        settlement: ParticipantSettlement,
        payments: List<PaymentRecord>,
        settlementTransfers: List<SettlementTransfer> = emptyList()
    ): File {
        val curr = order.currency
        val dateStr = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.getDefault()).format(Date(System.currentTimeMillis()))

        return PdfReportRenderer.render(
            context = context,
            orderName = order.orderName,
            reportTitle = "Participant Settlement Statement",
            filename = "Settlement_${sanitizeFilename(settlement.participantName)}_${sanitizeFilename(order.orderName)}.pdf"
        ) { renderer ->
            renderer.addSectionTitle("1. Participant Information")
            renderer.addKeyValuePairs(
                listOf(
                    "Participant Name" to settlement.participantName,
                    "Role" to if (settlement.isOrganizer) "Organizer" else "Member",
                    "Order Name" to order.orderName,
                    "Organizer" to order.organizerName,
                    "Statement Date" to dateStr,
                    "Payment Status" to settlement.paymentStatus.name.replace("_", " ")
                )
            )

            renderer.addSpacing(14f)
            renderer.addSectionTitle("2. Allocated Products & Quantities")
            if (settlement.items.isEmpty()) {
                renderer.addParagraph("No items ordered.")
            } else {
                val headers = listOf("Product", "Allocated Qty", "Unit Price", "Gross Cost", "Disc. Share", "Net Cost")
                val colWidths = floatArrayOf(155f, 60f, 75f, 78f, 75f, 80f)
                val alignments = listOf(Paint.Align.LEFT, Paint.Align.RIGHT, Paint.Align.RIGHT, Paint.Align.RIGHT, Paint.Align.RIGHT, Paint.Align.RIGHT)

                val rows = settlement.items.map { item ->
                    val qtyStr = if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else String.format(Locale.US, "%.1f", item.quantity)
                    listOf(
                        "${item.productName} (${item.unit})",
                        qtyStr,
                        SmartSplitEngine.formatPaise(item.unitPricePaise, curr),
                        SmartSplitEngine.formatPaise(item.grossAmountPaise, curr),
                        if (item.itemDiscountPaise > 0L) "-${SmartSplitEngine.formatPaise(item.itemDiscountPaise, curr)}" else "-",
                        SmartSplitEngine.formatPaise(item.netAmountPaise, curr)
                    )
                }
                renderer.addSolidTable(headers, rows, colWidths, alignments)
            }

            renderer.addSpacing(14f)
            renderer.addSectionTitle("3. Cost Allocation & Net Balance")
            val calcRows = mutableListOf(
                "Original Subtotal" to SmartSplitEngine.formatPaise(settlement.originalSubtotalPaise, curr)
            )
            if (settlement.itemDiscountPaise > 0L) {
                calcRows.add("Item-Specific Discount Savings" to "-${SmartSplitEngine.formatPaise(settlement.itemDiscountPaise, curr)}")
            }
            if (settlement.overallDiscountSharePaise > 0L) {
                calcRows.add("Proportional Overall Discount Share" to "-${SmartSplitEngine.formatPaise(settlement.overallDiscountSharePaise, curr)}")
            }
            if (settlement.taxSharePaise > 0L) {
                calcRows.add("Tax / GST Share" to "+${SmartSplitEngine.formatPaise(settlement.taxSharePaise, curr)}")
            }
            if (settlement.additionalChargesSharePaise > 0L) {
                calcRows.add("Additional Charges Share" to "+${SmartSplitEngine.formatPaise(settlement.additionalChargesSharePaise, curr)}")
            }
            calcRows.add("Cost Share (Final Payable)" to SmartSplitEngine.formatPaise(settlement.finalPayablePaise, curr))
            if (settlement.paidForPurchasePaise > 0L) {
                calcRows.add("Paid Upfront for Purchase" to SmartSplitEngine.formatPaise(settlement.paidForPurchasePaise, curr))
            }
            val netText = when {
                settlement.netBalancePaise > 0L -> "Gets back +${SmartSplitEngine.formatPaise(settlement.netBalancePaise, curr)}"
                settlement.netBalancePaise < 0L -> "Owes -${SmartSplitEngine.formatPaise(abs(settlement.netBalancePaise), curr)}"
                else -> "Settled Up (0)"
            }
            calcRows.add("Net Settlement Balance" to netText)
            calcRows.add("Settlement Paid / Sent" to SmartSplitEngine.formatPaise(settlement.amountPaidPaise, curr))
            if (settlement.amountReceivedPaise > 0L) {
                calcRows.add("Settlement Received" to SmartSplitEngine.formatPaise(settlement.amountReceivedPaise, curr))
            }
            calcRows.add("Remaining Balance Due" to SmartSplitEngine.formatPaise(settlement.outstandingBalancePaise, curr))
            renderer.addKeyValuePairs(calcRows, highlightLast = true)

            // Peer-to-peer settlement transfers
            val myTransfers = settlementTransfers.filter {
                it.fromParticipantId == settlement.participantId || it.toParticipantId == settlement.participantId
            }
            var nextSectionNum = 4
            if (myTransfers.isNotEmpty()) {
                renderer.addSpacing(14f)
                renderer.addSectionTitle("$nextSectionNum. Settlement Transfers (Who to Pay / Receive From)")
                nextSectionNum++
                val stHeaders = listOf("Action", "Counterparty", "Total Transfer", "Settled", "Remaining", "Status")
                val stColWidths = floatArrayOf(85f, 130f, 85f, 75f, 80f, 68f)
                val stAlignments = listOf(Paint.Align.LEFT, Paint.Align.LEFT, Paint.Align.RIGHT, Paint.Align.RIGHT, Paint.Align.RIGHT, Paint.Align.CENTER)
                val stRows = myTransfers.map { t ->
                    val isDebtor = t.fromParticipantId == settlement.participantId
                    listOf(
                        if (isDebtor) "Pay To" else "Receive From",
                        if (isDebtor) t.toParticipantName else t.fromParticipantName,
                        SmartSplitEngine.formatPaise(t.amountPaise, curr),
                        SmartSplitEngine.formatPaise(t.settledPaise, curr),
                        SmartSplitEngine.formatPaise(t.remainingPaise, curr),
                        if (t.isFullySettled) "SETTLED" else "PENDING"
                    )
                }
                renderer.addSolidTable(stHeaders, stRows, stColWidths, stAlignments)
            }

            // Payment History section
            val participantPayments = payments.filter { it.participantId == settlement.participantId }
            if (participantPayments.isNotEmpty()) {
                renderer.addSpacing(14f)
                renderer.addSectionTitle("$nextSectionNum. Payment Records")
                val pmtHeaders = listOf("Date", "Paid To", "Amount", "Notes")
                val pmtColWidths = floatArrayOf(135f, 115f, 105f, 168f)
                val pmtAlignments = listOf(Paint.Align.LEFT, Paint.Align.LEFT, Paint.Align.RIGHT, Paint.Align.LEFT)

                val pmtRows = participantPayments.map { pmt ->
                    val pmtDateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(pmt.paymentDate))
                    val toName = if (pmt.toParticipantId != null) {
                        settlementTransfers.firstOrNull { it.toParticipantId == pmt.toParticipantId }?.toParticipantName ?: "Member"
                    } else order.organizerName
                    listOf(
                        pmtDateStr,
                        toName,
                        SmartSplitEngine.formatPaise(pmt.amountPaise, curr),
                        listOfNotNull(pmt.notes?.ifBlank { null }, pmt.transactionReference?.ifBlank { null }).joinToString(" - ").ifBlank { "Recorded" }
                    )
                }
                renderer.addSolidTable(pmtHeaders, pmtRows, pmtColWidths, pmtAlignments)
            }
        }
    }

    /**
     * Generates Report D: Complete Settlement Report (All Participants Combined)
     * Features:
     * - Solid Inter fonts (Bold 700 & SemiBold 600) for complete clarity and sharpness.
     * - Executive summary cards in a solid 2x2 financial grid.
     * - Participant settlement ledger with solid table headers and status pill badges.
     * - Individual member breakdowns with smart zero-item hiding.
     * - Intelligent pagination with automatic table header repeats.
     */
    fun generateCompleteSettlementReport(
        context: Context,
        order: GroupOrder,
        summary: BillSummary,
        settlements: List<ParticipantSettlement>,
        purchasePayments: List<PurchasePayment> = emptyList(),
        settlementTransfers: List<SettlementTransfer> = emptyList()
    ): File {
        val curr = order.currency
        val dateStr = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.getDefault()).format(Date(System.currentTimeMillis()))
        val totalCollected = settlements.sumOf { it.amountPaidPaise }
        val totalOutstanding = settlements.sumOf { it.outstandingBalancePaise }
        val totalSavings = summary.overallDiscountPaise + summary.itemDiscountTotalPaise

        return PdfReportRenderer.render(
            context = context,
            orderName = order.orderName,
            reportTitle = "Complete Group Settlement Report",
            filename = "CompleteSettlement_${sanitizeFilename(order.orderName)}.pdf"
        ) { renderer ->
            var sectionIndex = 1

            // Section 1: Executive Summary
            renderer.addSectionTitle("$sectionIndex. Executive Summary")
            sectionIndex++

            // Metadata & Audit Status
            renderer.addMetadataRow(
                organizer = order.organizerName,
                dateStr = dateStr,
                participantCount = settlements.size,
                isFullySettled = (totalOutstanding == 0L)
            )

            renderer.addSpacing(10f)

            // Prominent 2x2 Financial Cards with solid amounts
            renderer.addExecutiveSummaryCards(
                billPaise = summary.finalBillAmountPaise,
                savingsPaise = totalSavings,
                collectedPaise = totalCollected,
                outstandingPaise = totalOutstanding,
                currency = curr
            )

            renderer.addSpacing(16f)

            // Upfront Purchase Contributions (if specified)
            if (purchasePayments.isNotEmpty()) {
                renderer.addSectionTitle("$sectionIndex. Purchase Contributions (Paid Upfront)")
                sectionIndex++

                val ppHeaders = listOf("Participant", "Amount Paid", "Share of Bill")
                val ppColWidths = floatArrayOf(240f, 140f, 143f)
                val ppAlignments = listOf(Paint.Align.LEFT, Paint.Align.RIGHT, Paint.Align.RIGHT)

                val ppRows = purchasePayments.map { pp ->
                    val pctStr = if (summary.finalBillAmountPaise > 0L) {
                        String.format(Locale.US, "%.1f%%", (pp.amountPaise.toDouble() / summary.finalBillAmountPaise.toDouble()) * 100)
                    } else "100%"
                    listOf(
                        pp.participantName,
                        SmartSplitEngine.formatPaise(pp.amountPaise, curr),
                        pctStr
                    )
                }
                renderer.addSolidTable(ppHeaders, ppRows, ppColWidths, ppAlignments)
                renderer.addSpacing(16f)
            }

            // Participant Settlement Ledger
            renderer.addSectionTitle("$sectionIndex. Participant Settlement Ledger")
            sectionIndex++

            val ledgerHeaders = listOf("Participant", "Role", "Subtotal", "Discount", "Taxes/Extra", "Payable", "Paid", "Balance", "Status")
            val ledgerColWidths = floatArrayOf(95f, 52f, 56f, 54f, 54f, 62f, 50f, 50f, 50f)
            val ledgerAlignments = listOf(
                Paint.Align.LEFT, Paint.Align.LEFT, Paint.Align.RIGHT, Paint.Align.RIGHT,
                Paint.Align.RIGHT, Paint.Align.RIGHT, Paint.Align.RIGHT, Paint.Align.RIGHT, Paint.Align.CENTER
            )

            val ledgerRows = settlements.map { s ->
                val totalDisc = s.itemDiscountPaise + s.overallDiscountSharePaise
                val extra = s.taxSharePaise + s.additionalChargesSharePaise
                val statusAbbr = when (s.paymentStatus) {
                    PaymentStatus.PAID -> "PAID"
                    PaymentStatus.PARTIALLY_PAID -> "PARTIAL"
                    PaymentStatus.PENDING -> "DUE"
                }
                listOf(
                    s.participantName,
                    if (s.isOrganizer) "Organizer" else "Member",
                    SmartSplitEngine.formatPaise(s.originalSubtotalPaise, curr, includeDecimals = s.originalSubtotalPaise % 100 != 0L),
                    if (totalDisc > 0L) "-${SmartSplitEngine.formatPaise(totalDisc, curr, includeDecimals = totalDisc % 100 != 0L)}" else "0",
                    if (extra > 0L) "+${SmartSplitEngine.formatPaise(extra, curr, includeDecimals = extra % 100 != 0L)}" else "0",
                    SmartSplitEngine.formatPaise(s.finalPayablePaise, curr, includeDecimals = s.finalPayablePaise % 100 != 0L),
                    SmartSplitEngine.formatPaise(s.amountPaidPaise, curr, includeDecimals = s.amountPaidPaise % 100 != 0L),
                    SmartSplitEngine.formatPaise(s.outstandingBalancePaise, curr, includeDecimals = s.outstandingBalancePaise % 100 != 0L),
                    statusAbbr
                )
            }

            val ledgerStatuses = settlements.map { it.paymentStatus }
            renderer.addLedgerTable(ledgerHeaders, ledgerRows, ledgerColWidths, ledgerAlignments, ledgerStatuses)

            // Settlement Plan (Who Pays Whom)
            if (settlementTransfers.isNotEmpty()) {
                renderer.addSpacing(16f)
                renderer.addSectionTitle("$sectionIndex. Settlement Plan (Who Pays Whom)")
                sectionIndex++

                val stHeaders = listOf("From (Debtor)", "To (Creditor)", "Transfer Amount", "Settled", "Remaining", "Status")
                val stColWidths = floatArrayOf(120f, 120f, 85f, 75f, 75f, 48f)
                val stAlignments = listOf(Paint.Align.LEFT, Paint.Align.LEFT, Paint.Align.RIGHT, Paint.Align.RIGHT, Paint.Align.RIGHT, Paint.Align.CENTER)

                val stRows = settlementTransfers.map { t ->
                    listOf(
                        t.fromParticipantName,
                        t.toParticipantName,
                        SmartSplitEngine.formatPaise(t.amountPaise, curr),
                        SmartSplitEngine.formatPaise(t.settledPaise, curr),
                        SmartSplitEngine.formatPaise(t.remainingPaise, curr),
                        if (t.isFullySettled) "SETTLED" else "PENDING"
                    )
                }
                renderer.addSolidTable(stHeaders, stRows, stColWidths, stAlignments)
            }

            renderer.addSpacing(18f)

            // Detailed Individual Member Breakdowns
            renderer.addSectionTitle("$sectionIndex. Detailed Individual Member Breakdowns")

            val itemHeaders = listOf("Product", "Quantity", "Actual Price", "Gross Amount", "Net Amount")
            val itemColWidths = floatArrayOf(170f, 65f, 95f, 95f, 98f)
            val itemAlignments = listOf(Paint.Align.LEFT, Paint.Align.RIGHT, Paint.Align.RIGHT, Paint.Align.RIGHT, Paint.Align.RIGHT)

            for (s in settlements) {
                val roleStr = if (s.isOrganizer) "Organizer" else "Member"
                val payableFormatted = "Total Payable: " + SmartSplitEngine.formatPaise(s.finalPayablePaise, curr)

                val itemRows = s.items.map { itm ->
                    val qStr = if (itm.quantity % 1.0 == 0.0) itm.quantity.toInt().toString() else String.format(Locale.US, "%.1f", itm.quantity)
                    listOf(
                        "${itm.productName} (${itm.unit})",
                        qStr,
                        SmartSplitEngine.formatPaise(itm.unitPricePaise, curr),
                        SmartSplitEngine.formatPaise(itm.grossAmountPaise, curr),
                        SmartSplitEngine.formatPaise(itm.netAmountPaise, curr)
                    )
                }

                renderer.addMemberBreakdown(
                    memberName = s.participantName,
                    role = roleStr,
                    payableText = payableFormatted,
                    headers = itemHeaders,
                    rows = itemRows,
                    colWidths = itemColWidths,
                    alignments = itemAlignments
                )
            }
        }
    }

    private fun sanitizeFilename(name: String): String {
        return name.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(30)
    }

    // =========================================================================
    // Multi-page Canvas PDF Renderer with Solid Fonts & Exact Two-Pass Pagination
    // =========================================================================
    private class PdfReportRenderer private constructor(
        private val context: Context,
        private val orderName: String,
        private val reportTitle: String,
        private val isMeasurePass: Boolean,
        private val totalPageCount: Int = 1
    ) {
        var pageCount: Int = 1
            private set

        private val document: PdfDocument? = if (!isMeasurePass) PdfDocument() else null
        private var currentPage: PdfDocument.Page? = null
        private var currentCanvas: Canvas? = null
        private var currentY = MARGIN + 80f

        // Solid Font Hierarchy (Heavy, dense strokes for 100% vector sharpness)
        private val fontMedium: Typeface = QuickOrderFontProvider.getMedium(context)
        private val fontSemiBold: Typeface = QuickOrderFontProvider.getSemiBold(context)
        private val fontBold: Typeface = QuickOrderFontProvider.getBold(context)

        private val paint = Paint().apply {
            isAntiAlias = true
            isSubpixelText = true
            isLinearText = true
            isFilterBitmap = true
            hinting = Paint.HINTING_OFF
            style = Paint.Style.FILL
            strokeWidth = 0f
        }

        companion object {
            private const val MAX_CONTENT_Y = PAGE_HEIGHT - MARGIN - 30f // 776f

            fun render(
                context: Context,
                orderName: String,
                reportTitle: String,
                filename: String,
                buildDocument: (PdfReportRenderer) -> Unit
            ): File {
                // Pass 1: Measure total pages dynamically
                val measureRenderer = PdfReportRenderer(context, orderName, reportTitle, isMeasurePass = true)
                buildDocument(measureRenderer)
                val computedTotalPages = max(1, measureRenderer.pageCount)

                // Pass 2: Draw actual vector PDF with exact "Page X of Y" and solid typography
                val drawRenderer = PdfReportRenderer(
                    context = context,
                    orderName = orderName,
                    reportTitle = reportTitle,
                    isMeasurePass = false,
                    totalPageCount = computedTotalPages
                )
                drawRenderer.startNewPage()
                buildDocument(drawRenderer)
                return drawRenderer.finishAndSave(filename)
            }
        }

        private fun startNewPage() {
            if (!isMeasurePass) {
                currentPage?.let {
                    drawPageFooter(currentCanvas!!, pageCount)
                    document?.finishPage(it)
                }
                val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageCount).create()
                val page = document!!.startPage(pageInfo)
                currentPage = page
                currentCanvas = page.canvas
                drawPageHeader(currentCanvas!!)
            }
            currentY = MARGIN + 80f
        }

        private fun ensureSpace(heightNeeded: Float) {
            if (currentY + heightNeeded > MAX_CONTENT_Y) {
                pageCount++
                startNewPage()
            }
        }

        private fun drawPageHeader(canvas: Canvas) {
            // Header top brand bar (Solid Purple Primary #5B21B6)
            paint.color = COLOR_HEADING_PURPLE
            paint.style = Paint.Style.FILL
            canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 48f, paint)

            // Brand title (Solid Inter Bold)
            paint.color = Color.WHITE
            paint.textSize = 14f
            paint.typeface = fontBold
            paint.textAlign = Paint.Align.LEFT
            canvas.drawText("ByteTools • Group Shopping with Smart Split", MARGIN, 29f, paint)

            // Report title (Solid Inter SemiBold)
            paint.textSize = SIZE_SUBTITLE
            paint.typeface = fontSemiBold
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText(reportTitle, PAGE_WIDTH - MARGIN, 29f, paint)

            // Sub-header Order Name (Solid Inter Bold in Solid Black)
            paint.color = COLOR_TEXT_SOLID_BLACK
            paint.textSize = SIZE_TITLE
            paint.typeface = fontBold
            paint.textAlign = Paint.Align.LEFT
            canvas.drawText(orderName, MARGIN, 70f, paint)

            paint.color = COLOR_BORDER_SOLID
            paint.strokeWidth = 1f
            paint.style = Paint.Style.STROKE
            canvas.drawLine(MARGIN, 76f, PAGE_WIDTH - MARGIN, 76f, paint)
        }

        private fun drawPageFooter(canvas: Canvas, pageNum: Int) {
            val footerY = PAGE_HEIGHT - 20f
            paint.color = COLOR_BORDER_SOLID
            paint.strokeWidth = 1f
            paint.style = Paint.Style.STROKE
            canvas.drawLine(MARGIN, footerY - 10f, PAGE_WIDTH - MARGIN, footerY - 10f, paint)

            paint.color = COLOR_TEXT_SOLID_MUTED
            paint.textSize = SIZE_FOOTER
            paint.style = Paint.Style.FILL
            paint.textAlign = Paint.Align.LEFT
            paint.typeface = fontSemiBold
            canvas.drawText("Generated via ByteTools • Group Shopping with Smart Split", MARGIN, footerY, paint)

            paint.textAlign = Paint.Align.RIGHT
            val pageStr = "Page $pageNum of $totalPageCount"
            canvas.drawText(pageStr, PAGE_WIDTH - MARGIN, footerY, paint)
        }

        fun addSpacing(space: Float) {
            currentY += space
        }

        fun addSectionTitle(title: String) {
            ensureSpace(32f)
            if (!isMeasurePass) {
                paint.style = Paint.Style.FILL
                paint.color = COLOR_HEADING_PURPLE
                paint.textSize = SIZE_SECTION_TITLE
                paint.typeface = fontBold
                paint.textAlign = Paint.Align.LEFT
                currentCanvas?.drawText(title, MARGIN, currentY + 16f, paint)
            }
            currentY += 26f
        }

        fun addParagraph(text: String) {
            ensureSpace(20f)
            if (!isMeasurePass) {
                paint.style = Paint.Style.FILL
                paint.color = COLOR_TEXT_SOLID_MUTED
                paint.textSize = SIZE_SUMMARY_LABEL
                paint.typeface = fontMedium
                paint.textAlign = Paint.Align.LEFT
                currentCanvas?.drawText(text, MARGIN, currentY + 13f, paint)
            }
            currentY += 18f
        }

        fun addDivider() {
            ensureSpace(12f)
            if (!isMeasurePass) {
                paint.color = COLOR_BORDER_SOLID
                paint.strokeWidth = 0.8f
                paint.style = Paint.Style.STROKE
                currentCanvas?.drawLine(MARGIN, currentY + 5f, MARGIN + CONTENT_WIDTH, currentY + 5f, paint)
            }
            currentY += 12f
        }

        fun addMetadataRow(organizer: String, dateStr: String, participantCount: Int, isFullySettled: Boolean) {
            ensureSpace(36f)
            if (!isMeasurePass) {
                paint.style = Paint.Style.FILL
                paint.color = COLOR_TEXT_SOLID_MUTED
                paint.textSize = SIZE_SUMMARY_LABEL
                paint.typeface = fontMedium
                paint.textAlign = Paint.Align.LEFT
                currentCanvas?.drawText("Organizer: ", MARGIN, currentY + 14f, paint)

                val orgLabelW = paint.measureText("Organizer: ")
                paint.color = COLOR_TEXT_SOLID_BLACK
                paint.typeface = fontBold
                currentCanvas?.drawText("$organizer  •  $participantCount Members  •  $dateStr", MARGIN + orgLabelW, currentY + 14f, paint)

                // Audit Status Pill on Right (Solid borders & deep text)
                val pillText = if (isFullySettled) "✓ 100% Fully Settled" else "● Pending Collection"
                val pillTextW = paint.measureText(pillText)
                val pillW = pillTextW + 16f
                val pillH = 20f
                val pillX = MARGIN + CONTENT_WIDTH - pillW
                val pillY = currentY + 1f

                paint.style = Paint.Style.FILL
                paint.color = if (isFullySettled) COLOR_GREEN_BG else COLOR_AMBER_BG
                val pillRect = RectF(pillX, pillY, pillX + pillW, pillY + pillH)
                currentCanvas?.drawRoundRect(pillRect, 10f, 10f, paint)

                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 1f
                paint.color = if (isFullySettled) COLOR_GREEN_BORDER else COLOR_AMBER_BORDER
                currentCanvas?.drawRoundRect(pillRect, 10f, 10f, paint)

                paint.style = Paint.Style.FILL
                paint.color = if (isFullySettled) COLOR_GREEN_TEXT else COLOR_AMBER_TEXT
                paint.textSize = 9.5f
                paint.typeface = fontBold
                paint.textAlign = Paint.Align.CENTER
                currentCanvas?.drawText(pillText, pillX + (pillW / 2f), pillY + 14f, paint)
            }
            currentY += 28f
        }

        fun addExecutiveSummaryCards(
            billPaise: Long,
            savingsPaise: Long,
            collectedPaise: Long,
            outstandingPaise: Long,
            currency: String
        ) {
            val cardH = 50f
            val cardW = (CONTENT_WIDTH - 12f) / 2f
            val totalH = (cardH * 2f) + 8f
            ensureSpace(totalH)

            if (!isMeasurePass) {
                fun drawCard(
                    x: Float,
                    y: Float,
                    label: String,
                    amount: String,
                    bgCol: Int,
                    borderCol: Int,
                    labelCol: Int,
                    amtCol: Int
                ) {
                    val rect = RectF(x, y, x + cardW, y + cardH)
                    paint.style = Paint.Style.FILL
                    paint.color = bgCol
                    currentCanvas?.drawRoundRect(rect, 6f, 6f, paint)

                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 1f
                    paint.color = borderCol
                    currentCanvas?.drawRoundRect(rect, 6f, 6f, paint)

                    paint.style = Paint.Style.FILL
                    paint.color = labelCol
                    paint.textSize = SIZE_CARD_LABEL
                    paint.typeface = fontBold
                    paint.textAlign = Paint.Align.LEFT
                    currentCanvas?.drawText(label, x + 12f, y + 17f, paint)

                    paint.color = amtCol
                    paint.textSize = SIZE_CARD_AMOUNT
                    paint.typeface = fontBold
                    currentCanvas?.drawText(amount, x + 12f, y + 38f, paint)
                }

                // Row 1
                val billStr = SmartSplitEngine.formatPaise(billPaise, currency)
                val savingsStr = SmartSplitEngine.formatPaise(savingsPaise, currency)
                drawCard(
                    MARGIN, currentY,
                    "TOTAL SHOPKEEPER BILL", billStr,
                    COLOR_ROW_ALT, COLOR_BORDER_SOLID, COLOR_TEXT_SOLID_MUTED, COLOR_TEXT_SOLID_BLACK
                )
                drawCard(
                    MARGIN + cardW + 12f, currentY,
                    "TOTAL DISCOUNT SAVINGS", savingsStr,
                    COLOR_GREEN_BG, COLOR_GREEN_BORDER, COLOR_GREEN_TEXT, COLOR_GREEN_TEXT
                )

                // Row 2
                val collectedStr = SmartSplitEngine.formatPaise(collectedPaise, currency)
                val outStr = SmartSplitEngine.formatPaise(outstandingPaise, currency)
                val isSettled = (outstandingPaise == 0L)
                drawCard(
                    MARGIN, currentY + cardH + 8f,
                    "TOTAL COLLECTED", collectedStr,
                    COLOR_BLUE_BG, COLOR_BLUE_BORDER, COLOR_BLUE_TEXT, COLOR_BLUE_TEXT
                )
                drawCard(
                    MARGIN + cardW + 12f, currentY + cardH + 8f,
                    "TOTAL OUTSTANDING DUE", outStr,
                    if (isSettled) COLOR_GREEN_BG else COLOR_RED_BG,
                    if (isSettled) COLOR_GREEN_BORDER else COLOR_RED_BORDER,
                    if (isSettled) COLOR_GREEN_TEXT else COLOR_RED_TEXT,
                    if (isSettled) COLOR_GREEN_TEXT else COLOR_RED_TEXT
                )
            }
            currentY += totalH
        }

        fun addKeyValuePairs(pairs: List<Pair<String, String>>, highlightLast: Boolean = false) {
            for ((idx, pair) in pairs.withIndex()) {
                val isLastHighlight = highlightLast && (idx == pairs.size - 1)
                val rowHeight = if (isLastHighlight) 32f else 22f
                ensureSpace(rowHeight)

                if (!isMeasurePass) {
                    if (isLastHighlight) {
                        val rect = RectF(MARGIN, currentY, MARGIN + CONTENT_WIDTH, currentY + 28f)
                        paint.color = COLOR_PURPLE_BG
                        paint.style = Paint.Style.FILL
                        currentCanvas?.drawRoundRect(rect, 6f, 6f, paint)

                        paint.color = COLOR_HEADING_PURPLE
                        paint.style = Paint.Style.STROKE
                        paint.strokeWidth = 1f
                        currentCanvas?.drawRoundRect(rect, 6f, 6f, paint)

                        paint.style = Paint.Style.FILL
                        paint.color = COLOR_HEADING_PURPLE
                        paint.textSize = SIZE_MEMBER_NAME
                        paint.typeface = fontBold
                        paint.textAlign = Paint.Align.LEFT
                        currentCanvas?.drawText(pair.first, MARGIN + 10f, currentY + 18f, paint)

                        paint.textAlign = Paint.Align.RIGHT
                        currentCanvas?.drawText(pair.second, MARGIN + CONTENT_WIDTH - 10f, currentY + 18f, paint)
                    } else {
                        paint.style = Paint.Style.FILL
                        paint.color = COLOR_TEXT_SOLID_MUTED
                        paint.textSize = SIZE_SUMMARY_LABEL
                        paint.typeface = fontSemiBold
                        paint.textAlign = Paint.Align.LEFT
                        currentCanvas?.drawText(pair.first, MARGIN, currentY + 15f, paint)

                        paint.color = COLOR_TEXT_SOLID_BLACK
                        paint.textSize = SIZE_SUMMARY_VALUE
                        paint.typeface = fontBold
                        paint.textAlign = Paint.Align.RIGHT
                        currentCanvas?.drawText(pair.second, MARGIN + CONTENT_WIDTH, currentY + 15f, paint)
                    }
                }
                currentY += rowHeight
            }
        }

        fun addSolidTable(
            headers: List<String>,
            rows: List<List<String>>,
            colWidths: FloatArray,
            alignments: List<Paint.Align>
        ) {
            val headerH = 26f
            val rowH = 22f

            fun drawTableHeader() {
                if (isMeasurePass) return
                // Solid Deep Purple Header Background with Solid White Bold Text
                paint.color = COLOR_HEADING_PURPLE
                paint.style = Paint.Style.FILL
                currentCanvas?.drawRect(MARGIN, currentY, MARGIN + CONTENT_WIDTH, currentY + headerH, paint)

                paint.color = Color.WHITE
                paint.textSize = SIZE_TABLE_HEADER
                paint.typeface = fontBold

                var cx = MARGIN
                for (i in headers.indices) {
                    val w = colWidths[i]
                    val align = alignments[i]
                    paint.textAlign = align
                    val x = when (align) {
                        Paint.Align.LEFT -> cx + 6f
                        Paint.Align.RIGHT -> cx + w - 6f
                        else -> cx + (w / 2f)
                    }
                    val text = truncate(headers[i], w - 8f, paint)
                    currentCanvas?.drawText(text, x, currentY + 17f, paint)
                    cx += w
                }
            }

            ensureSpace(headerH + rowH)
            drawTableHeader()
            currentY += headerH

            for ((rowIdx, row) in rows.withIndex()) {
                if (currentY + rowH > MAX_CONTENT_Y) {
                    pageCount++
                    startNewPage()
                    drawTableHeader()
                    currentY += headerH
                }

                if (!isMeasurePass) {
                    if (rowIdx % 2 == 1) {
                        paint.color = COLOR_ROW_ALT
                        paint.style = Paint.Style.FILL
                        currentCanvas?.drawRect(MARGIN, currentY, MARGIN + CONTENT_WIDTH, currentY + rowH, paint)
                    }

                    paint.color = COLOR_BORDER_SUBTLE
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 0.6f
                    currentCanvas?.drawLine(MARGIN, currentY + rowH, MARGIN + CONTENT_WIDTH, currentY + rowH, paint)

                    paint.style = Paint.Style.FILL
                    paint.color = COLOR_TEXT_SOLID_BLACK
                    paint.textSize = SIZE_TABLE_CONTENT
                    paint.typeface = fontSemiBold

                    var cx = MARGIN
                    for (colIdx in row.indices) {
                        val w = colWidths[colIdx]
                        val align = alignments[colIdx]
                        paint.textAlign = align
                        val x = when (align) {
                            Paint.Align.LEFT -> cx + 6f
                            Paint.Align.RIGHT -> cx + w - 6f
                            else -> cx + (w / 2f)
                        }
                        val cellText = truncate(row[colIdx], w - 8f, paint)
                        currentCanvas?.drawText(cellText, x, currentY + 15f, paint)
                        cx += w
                    }
                }
                currentY += rowH
            }
        }

        fun addLedgerTable(
            headers: List<String>,
            rows: List<List<String>>,
            colWidths: FloatArray,
            alignments: List<Paint.Align>,
            statuses: List<PaymentStatus>
        ) {
            val headerH = 26f
            val rowH = 22f

            fun drawHeader() {
                if (isMeasurePass) return
                // Solid Deep Purple Header Background with Solid White Bold Text
                paint.color = COLOR_HEADING_PURPLE
                paint.style = Paint.Style.FILL
                currentCanvas?.drawRect(MARGIN, currentY, MARGIN + CONTENT_WIDTH, currentY + headerH, paint)

                paint.color = Color.WHITE
                paint.textSize = SIZE_TABLE_HEADER
                paint.typeface = fontBold

                var cx = MARGIN
                for (i in headers.indices) {
                    val w = colWidths[i]
                    val align = alignments[i]
                    paint.textAlign = align
                    val x = when (align) {
                        Paint.Align.LEFT -> cx + 6f
                        Paint.Align.RIGHT -> cx + w - 6f
                        else -> cx + (w / 2f)
                    }
                    val text = truncate(headers[i], w - 6f, paint)
                    currentCanvas?.drawText(text, x, currentY + 17f, paint)
                    cx += w
                }
            }

            ensureSpace(headerH + rowH)
            drawHeader()
            currentY += headerH

            for (rowIdx in rows.indices) {
                if (currentY + rowH > MAX_CONTENT_Y) {
                    pageCount++
                    startNewPage()
                    drawHeader()
                    currentY += headerH
                }

                if (!isMeasurePass) {
                    val row = rows[rowIdx]
                    val status = statuses.getOrElse(rowIdx) { PaymentStatus.PENDING }

                    if (rowIdx % 2 == 1) {
                        paint.color = COLOR_ROW_ALT
                        paint.style = Paint.Style.FILL
                        currentCanvas?.drawRect(MARGIN, currentY, MARGIN + CONTENT_WIDTH, currentY + rowH, paint)
                    }

                    paint.color = COLOR_BORDER_SUBTLE
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 0.6f
                    currentCanvas?.drawLine(MARGIN, currentY + rowH, MARGIN + CONTENT_WIDTH, currentY + rowH, paint)

                    var cx = MARGIN
                    for (colIdx in row.indices) {
                        val w = colWidths[colIdx]
                        val align = alignments[colIdx]

                        if (colIdx == row.size - 1) {
                            // Status Pill
                            val pillW = 44f
                            val pillH = 15f
                            val pillX = cx + ((w - pillW) / 2f)
                            val pillY = currentY + 3.5f

                            val (bg, border, txtCol, label) = when (status) {
                                PaymentStatus.PAID -> Quad(COLOR_GREEN_BG, COLOR_GREEN_BORDER, COLOR_GREEN_TEXT, "PAID")
                                PaymentStatus.PARTIALLY_PAID -> Quad(COLOR_AMBER_BG, COLOR_AMBER_BORDER, COLOR_AMBER_TEXT, "PARTIAL")
                                PaymentStatus.PENDING -> Quad(COLOR_RED_BG, COLOR_RED_BORDER, COLOR_RED_TEXT, "DUE")
                            }

                            paint.style = Paint.Style.FILL
                            paint.color = bg
                            val rect = RectF(pillX, pillY, pillX + pillW, pillY + pillH)
                            currentCanvas?.drawRoundRect(rect, 4f, 4f, paint)

                            paint.style = Paint.Style.STROKE
                            paint.strokeWidth = 0.8f
                            paint.color = border
                            currentCanvas?.drawRoundRect(rect, 4f, 4f, paint)

                            paint.style = Paint.Style.FILL
                            paint.color = txtCol
                            paint.textSize = 8.5f
                            paint.typeface = fontBold
                            paint.textAlign = Paint.Align.CENTER
                            currentCanvas?.drawText(label, pillX + (pillW / 2f), pillY + 11f, paint)
                        } else {
                            paint.style = Paint.Style.FILL
                            paint.textSize = SIZE_TABLE_CONTENT
                            paint.textAlign = align

                            when (colIdx) {
                                0 -> { // Participant Name
                                    paint.color = COLOR_TEXT_SOLID_BLACK
                                    paint.typeface = fontBold
                                }
                                3 -> { // Discount
                                    paint.color = COLOR_GREEN_TEXT
                                    paint.typeface = fontBold
                                }
                                5 -> { // Payable
                                    paint.color = COLOR_HEADING_PURPLE
                                    paint.typeface = fontBold
                                }
                                7 -> { // Balance
                                    paint.color = if (row[7] == "0" || row[7] == "₹0") COLOR_TEXT_SOLID_BLACK else COLOR_RED_TEXT
                                    paint.typeface = fontBold
                                }
                                1, 4 -> { // Role, Extra
                                    paint.color = COLOR_TEXT_SOLID_MUTED
                                    paint.typeface = fontSemiBold
                                }
                                else -> {
                                    paint.color = COLOR_TEXT_SOLID_BLACK
                                    paint.typeface = fontSemiBold
                                }
                            }

                            val x = when (align) {
                                Paint.Align.LEFT -> cx + 6f
                                Paint.Align.RIGHT -> cx + w - 6f
                                else -> cx + (w / 2f)
                            }
                            val cellText = truncate(row[colIdx], w - 6f, paint)
                            currentCanvas?.drawText(cellText, x, currentY + 15f, paint)
                        }
                        cx += w
                    }
                }
                currentY += rowH
            }
        }

        fun addMemberHeader(name: String, roleOrCount: String, payableText: String) {
            ensureSpace(26f)
            if (!isMeasurePass) {
                paint.style = Paint.Style.FILL
                paint.color = COLOR_TEXT_SOLID_BLACK
                paint.textSize = SIZE_MEMBER_NAME
                paint.typeface = fontBold
                paint.textAlign = Paint.Align.LEFT
                currentCanvas?.drawText(name, MARGIN, currentY + 16f, paint)

                if (roleOrCount.isNotBlank()) {
                    val nameW = paint.measureText(name)
                    paint.color = COLOR_TEXT_SOLID_MUTED
                    paint.textSize = 10f
                    paint.typeface = fontSemiBold
                    currentCanvas?.drawText(" ($roleOrCount)", MARGIN + nameW, currentY + 16f, paint)
                }

                if (payableText.isNotBlank()) {
                    paint.color = COLOR_HEADING_PURPLE
                    paint.textSize = SIZE_MEMBER_NAME
                    paint.typeface = fontBold
                    paint.textAlign = Paint.Align.RIGHT
                    currentCanvas?.drawText(payableText, MARGIN + CONTENT_WIDTH, currentY + 16f, paint)
                }
            }
            currentY += 22f
        }

        fun addMemberBreakdown(
            memberName: String,
            role: String,
            payableText: String,
            headers: List<String>,
            rows: List<List<String>>,
            colWidths: FloatArray,
            alignments: List<Paint.Align>
        ) {
            val minHeightNeeded = if (rows.isEmpty()) 48f else 68f
            ensureSpace(minHeightNeeded)

            addMemberHeader(memberName, role, payableText)

            if (rows.isEmpty()) {
                addParagraph("No items ordered.")
                addDivider()
            } else {
                addSolidTable(headers, rows, colWidths, alignments)
                addDivider()
            }
        }

        private fun truncate(text: String, maxWidth: Float, p: Paint): String {
            if (p.measureText(text) <= maxWidth) return text
            var truncated = text
            while (truncated.length > 2 && p.measureText("$truncated…") > maxWidth) {
                truncated = truncated.substring(0, truncated.length - 1)
            }
            return "$truncated…"
        }

        private fun finishAndSave(filename: String): File {
            currentPage?.let {
                drawPageFooter(currentCanvas!!, pageCount)
                document?.finishPage(it)
            }

            val reportsDir = File(context.cacheDir, "quick_order_reports")
            if (!reportsDir.exists()) reportsDir.mkdirs()

            val file = File(reportsDir, filename)
            FileOutputStream(file).use { out ->
                document?.writeTo(out)
            }
            document?.close()
            return file
        }
    }

    private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}
