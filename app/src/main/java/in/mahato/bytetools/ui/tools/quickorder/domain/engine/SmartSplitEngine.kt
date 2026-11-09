package `in`.mahato.bytetools.ui.tools.quickorder.domain.engine

import `in`.mahato.bytetools.ui.tools.quickorder.domain.model.*
import java.text.DecimalFormat
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong

object SmartSplitEngine {

    private val currencyFormat = DecimalFormat("#,##,##0.00")
    private val integerFormat = DecimalFormat("#,##,##0")

    fun formatPaise(paise: Long, currency: String = "₹", includeDecimals: Boolean = true): String {
        val sign = if (paise < 0) "-" else ""
        val absolutePaise = abs(paise)
        val rupees = absolutePaise / 100.0
        val formatted = if (includeDecimals || absolutePaise % 100 != 0L) {
            currencyFormat.format(rupees)
        } else {
            integerFormat.format(rupees)
        }
        return "$sign$currency$formatted"
    }

    fun parseAmountToPaise(amountString: String): Long {
        val cleaned = amountString.replace(",", "").replace("₹", "").replace("$", "").trim()
        if (cleaned.isBlank()) return 0L
        val doubleVal = cleaned.toDoubleOrNull() ?: 0.0
        return (doubleVal * 100).roundToLong()
    }

    /**
     * Consolidates individual participant order items across the whole group.
     * Merges duplicate entries from the same participant first,
     * then aggregates identical products across all participants.
     */
    fun consolidateOrders(
        orderItems: List<ParticipantOrderItem>,
        participants: List<Participant>
    ): List<ConsolidatedItem> {
        val participantMap = participants.associateBy { it.id }

        // Step 1: Pre-merge duplicate items per participant (e.g. participant added Flower Pots twice)
        val mergedPerParticipant = mutableMapOf<Pair<Long, String>, ParticipantOrderItem>()
        for (item in orderItems) {
            val key = Pair(item.participantId, item.normalizedProductName)
            val existing = mergedPerParticipant[key]
            if (existing != null) {
                mergedPerParticipant[key] = existing.copy(
                    quantity = existing.quantity + item.quantity,
                    estimatedUnitPricePaise = item.estimatedUnitPricePaise ?: existing.estimatedUnitPricePaise,
                    notes = listOfNotNull(existing.notes, item.notes).joinToString("; ").ifBlank { null }
                )
            } else {
                mergedPerParticipant[key] = item
            }
        }

        // Step 2: Aggregate by normalized product name + unit
        val consolidatedMap = mutableMapOf<String, MutableList<ParticipantOrderItem>>()
        for (item in mergedPerParticipant.values) {
            val key = "${item.normalizedProductName}:::${item.unit.trim().lowercase()}"
            consolidatedMap.getOrPut(key) { mutableListOf() }.add(item)
        }

        return consolidatedMap.values.map { items ->
            val first = items.first()
            val totalQty = items.sumOf { it.quantity }
            val breakdown = items.map { item ->
                val pName = participantMap[item.participantId]?.name ?: "Participant #${item.participantId}"
                ParticipantOrderQuantity(
                    participantId = item.participantId,
                    participantName = pName,
                    quantity = item.quantity,
                    unit = item.unit
                )
            }
            ConsolidatedItem(
                normalizedProductName = first.normalizedProductName,
                displayName = first.productName,
                unit = first.unit,
                totalOrderedQuantity = totalQty,
                estimatedUnitPricePaise = items.mapNotNull { it.estimatedUnitPricePaise }.averageOrNull(),
                participantOrders = breakdown
            )
        }.sortedBy { it.displayName.lowercase() }
    }

    private fun List<Long>.averageOrNull(): Long? {
        if (isEmpty()) return null
        return (sum().toDouble() / size).roundToLong()
    }

    /**
     * Fuzzy matching between a shopkeeper bill item and consolidated order items.
     * Handles variations like Chakri = Chakkar, Flowerpot = Flower Pots, Rockets = Rocket.
     */
    fun findBestMatch(
        billItemName: String,
        consolidatedItems: List<ConsolidatedItem>,
        threshold: Double = 0.60
    ): ConsolidatedItem? {
        val normBill = normalizeString(billItemName)
        var bestItem: ConsolidatedItem? = null
        var bestScore = 0.0

        for (item in consolidatedItems) {
            val normConsolidated = normalizeString(item.displayName)
            val score = calculateSimilarity(normBill, normConsolidated)
            if (score > bestScore && score >= threshold) {
                bestScore = score
                bestItem = item
            }
        }
        return bestItem
    }

    fun normalizeString(input: String): String {
        return input.trim().lowercase()
            .replace(Regex("[^a-z0-9\\s]"), "")
            .replace("\\s+".toRegex(), " ")
    }

    /**
     * Calculates Levenshtein-based similarity + token overlap ratio between 0.0 and 1.0.
     */
    fun calculateSimilarity(s1: String, s2: String): Double {
        if (s1 == s2) return 1.0
        if (s1.isEmpty() || s2.isEmpty()) return 0.0

        // Ignore spaces match (e.g. "flowerpot" vs "flower pots")
        val noSpace1 = s1.replace(" ", "")
        val noSpace2 = s2.replace(" ", "")
        if (noSpace1 == noSpace2) return 1.0

        // Handle plurals / stems (e.g. rocket vs rockets, pot vs pots)
        val stem1 = s1.removeSuffix("s").removeSuffix("es")
        val stem2 = s2.removeSuffix("s").removeSuffix("es")
        if (stem1 == stem2) return 0.95

        // Token overlap
        val words1 = s1.split(" ").filter { it.isNotBlank() }.toSet()
        val words2 = s2.split(" ").filter { it.isNotBlank() }.toSet()
        val commonWords = words1.intersect(words2)
        if (words1.isNotEmpty() && words2.isNotEmpty() && commonWords.size == max(words1.size, words2.size)) {
            return 0.90
        }

        // Levenshtein distance
        val maxLen = max(s1.length, s2.length)
        val dist = levenshteinDistance(s1, s2)
        val levScore = 1.0 - (dist.toDouble() / maxLen)

        // Common prefix bonus (Jaro-Winkler concept)
        var commonPrefix = 0
        val minLen = min(s1.length, s2.length)
        while (commonPrefix < minLen && s1[commonPrefix] == s2[commonPrefix]) {
            commonPrefix++
        }
        val prefixBonus = min(commonPrefix, 4) * 0.08
        val boostedScore = min(1.0, levScore + prefixBonus * (1.0 - levScore))

        val tokenScore = if (words1.isNotEmpty() && words2.isNotEmpty()) {
            (2.0 * commonWords.size) / (words1.size + words2.size)
        } else 0.0

        return max(boostedScore, tokenScore)
    }

    private fun levenshteinDistance(a: String, b: String): Int {
        val dp = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) dp[i][0] = i
        for (j in 0..b.length) dp[0][j] = j

        for (i in 1..a.length) {
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                dp[i][j] = min(
                    dp[i - 1][j] + 1,
                    min(dp[i][j - 1] + 1, dp[i - 1][j - 1] + cost)
                )
            }
        }
        return dp[a.length][b.length]
    }

    /**
     * Matches bill items against the consolidated shopping list.
     */
    fun reconcileBill(
        billItems: List<BillItem>,
        consolidatedItems: List<ConsolidatedItem>,
        participants: List<Participant>
    ): List<ReconciliationItem> {
        val result = mutableListOf<ReconciliationItem>()
        val matchedConsolidatedKeys = mutableSetOf<String>()

        for (billItem in billItems) {
            val match = findBestMatch(billItem.productName, consolidatedItems)
            if (match != null) {
                matchedConsolidatedKeys.add(match.normalizedProductName)
                val orderedQty = match.totalOrderedQuantity
                val purchasedQty = billItem.quantity

                val status = when {
                    purchasedQty == orderedQty -> ReconciliationStatus.MATCHED
                    purchasedQty < orderedQty -> ReconciliationStatus.SHORTFALL
                    else -> ReconciliationStatus.SURPLUS
                }

                // Default allocation
                val allocations = mutableMapOf<Long, Double>()
                if (status == ReconciliationStatus.MATCHED) {
                    for (order in match.participantOrders) {
                        allocations[order.participantId] = order.quantity
                    }
                } else if (status == ReconciliationStatus.SHORTFALL) {
                    // Proportional distribution of shortfall
                    val ratio = if (orderedQty > 0) purchasedQty / orderedQty else 0.0
                    for (order in match.participantOrders) {
                        allocations[order.participantId] = (order.quantity * ratio)
                    }
                } else {
                    // Surplus: give ordered qty to participants, extra remains to be shared or assigned
                    for (order in match.participantOrders) {
                        allocations[order.participantId] = order.quantity
                    }
                }

                result.add(
                    ReconciliationItem(
                        billItem = billItem,
                        matchedConsolidatedItem = match,
                        orderedQuantity = orderedQty,
                        purchasedQuantity = purchasedQty,
                        status = status,
                        participantAllocations = allocations
                    )
                )
            } else {
                // New item not ordered in the shopping list
                result.add(
                    ReconciliationItem(
                        billItem = billItem,
                        matchedConsolidatedItem = null,
                        orderedQuantity = 0.0,
                        purchasedQuantity = billItem.quantity,
                        status = ReconciliationStatus.NEW_ITEM,
                        participantAllocations = emptyMap()
                    )
                )
            }
        }

        // Consolidated items that were not found in bill items (Unavailable)
        for (consolidated in consolidatedItems) {
            if (!matchedConsolidatedKeys.contains(consolidated.normalizedProductName)) {
                val dummyBill = BillItem(
                    id = 0,
                    groupOrderId = 0,
                    productName = consolidated.displayName,
                    normalizedProductName = consolidated.normalizedProductName,
                    quantity = 0.0,
                    unit = consolidated.unit,
                    unitPricePaise = consolidated.estimatedUnitPricePaise ?: 0L,
                    itemDiscountPaise = 0L
                )
                result.add(
                    ReconciliationItem(
                        billItem = dummyBill,
                        matchedConsolidatedItem = consolidated,
                        orderedQuantity = consolidated.totalOrderedQuantity,
                        purchasedQuantity = 0.0,
                        status = ReconciliationStatus.UNAVAILABLE,
                        participantAllocations = emptyMap()
                    )
                )
            }
        }

        return result
    }

    /**
     * Distributes an integer amount of paise proportionally among participant weights
     * using the Hare-Niemeyer / Hamilton largest-remainder method.
     * Guarantees that sum(result.values) == totalToDistribute EXACTLY down to 0 paise difference.
     */
    fun distributePaiseProportionally(
        totalToDistribute: Long,
        weights: Map<Long, Long>
    ): Map<Long, Long> {
        if (weights.isEmpty()) return emptyMap()
        if (totalToDistribute == 0L) return weights.keys.associateWith { 0L }

        val totalWeight = weights.values.sum()
        if (totalWeight <= 0L) {
            return distributePaiseEqually(totalToDistribute, weights.keys.toList())
        }

        // Hare-Niemeyer Largest Remainder Method
        val baseShares = mutableMapOf<Long, Long>()
        val remainders = mutableListOf<Triple<Long, Long, Long>>() // (id, remainder, weight)

        var totalBaseAllocated = 0L
        for ((id, weight) in weights) {
            // (totalToDistribute * weight) / totalWeight
            val product = totalToDistribute * weight
            val base = product / totalWeight
            val remainder = product % totalWeight
            baseShares[id] = base
            totalBaseAllocated += base
            remainders.add(Triple(id, remainder, weight))
        }

        val leftoverPaise = (totalToDistribute - totalBaseAllocated).toInt()
        if (leftoverPaise > 0) {
            // Sort by remainder descending, then by weight descending, then by id ascending for determinism
            remainders.sortWith(
                compareByDescending<Triple<Long, Long, Long>> { it.second }
                    .thenByDescending { it.third }
                    .thenBy { it.first }
            )
            for (i in 0 until min(leftoverPaise, remainders.size)) {
                val id = remainders[i].first
                baseShares[id] = (baseShares[id] ?: 0L) + 1L
            }
        } else if (leftoverPaise < 0) {
            // For negative distribution (rare, e.g. negative discount/charge)
            val toRemove = abs(leftoverPaise)
            remainders.sortWith(
                compareBy<Triple<Long, Long, Long>> { it.second }
                    .thenBy { it.third }
                    .thenBy { it.first }
            )
            for (i in 0 until min(toRemove, remainders.size)) {
                val id = remainders[i].first
                baseShares[id] = (baseShares[id] ?: 0L) - 1L
            }
        }

        return baseShares
    }

    /**
     * Distributes an integer amount of paise equally among participant IDs
     * with deterministic largest-remainder distribution for leftovers.
     * Guarantees that sum(result.values) == totalToDistribute EXACTLY.
     */
    fun distributePaiseEqually(
        totalToDistribute: Long,
        participantIds: List<Long>
    ): Map<Long, Long> {
        if (participantIds.isEmpty()) return emptyMap()
        if (totalToDistribute == 0L) return participantIds.associateWith { 0L }

        val n = participantIds.size
        val base = totalToDistribute / n
        val leftover = (totalToDistribute % n).toInt()

        val sortedIds = participantIds.sorted()
        val result = mutableMapOf<Long, Long>()

        for (i in sortedIds.indices) {
            val id = sortedIds[i]
            val extra = if (i < leftover) 1L else 0L
            result[id] = base + extra
        }
        return result
    }

    /**
     * The Core Settlement Engine.
     *
     * 1. Calculates member item costs: Allocated Quantity * Actual Unit Price.
     * 2. Distributes item-specific discounts to members proportionally to their allocated quantity.
     * 3. Calculates each member's pre-overall-discount subtotal.
     * 4. Distributes overall discount using chosen method (Proportional by default) with deterministic rounding.
     * 5. Distributes taxes and additional charges proportionally (or equally) with deterministic rounding.
     * 6. Resolves final payable per participant:
     *    Member Payable = Member Net Subtotal - Member Overall Discount + Member Taxes + Member Charges.
     * 7. Determines each member's upfront purchase contribution (paidForPurchasePaise).
     * 8. Computes Net Settlement Balance (paidForPurchasePaise - finalPayablePaise).
     * 9. Tracks settlement payments and statuses.
     */
    fun calculateSettlement(
        billItems: List<BillItem>,
        allocations: List<ItemAllocation>,
        billSummary: BillSummary,
        participants: List<Participant>,
        payments: List<PaymentRecord> = emptyList(),
        purchasePayments: List<PurchasePayment> = emptyList()
    ): List<ParticipantSettlement> {
        if (participants.isEmpty()) return emptyList()

        val billItemMap = billItems.associateBy { it.id }

        // Step 1: Member Item Breakdown & Item-specific discounts
        val participantItems = mutableMapOf<Long, MutableList<SettlementItemBreakdown>>()
        val participantGrossSubtotals = mutableMapOf<Long, Long>()
        val participantItemDiscounts = mutableMapOf<Long, Long>()

        for (p in participants) {
            participantItems[p.id] = mutableListOf()
            participantGrossSubtotals[p.id] = 0L
            participantItemDiscounts[p.id] = 0L
        }

        // For each bill item, compute each participant's allocated cost and share of item discount
        val allocationsByBillItem = allocations.groupBy { it.billItemId }
        for ((billItemId, itemAllocs) in allocationsByBillItem) {
            val billItem = billItemMap[billItemId] ?: continue
            val totalAllocatedQty = itemAllocs.sumOf { it.allocatedQuantity }

            for (alloc in itemAllocs) {
                if (alloc.allocatedQuantity <= 0.0) continue

                // Allocated gross cost = quantity * actual unit price
                val grossCost = (alloc.allocatedQuantity * billItem.unitPricePaise).roundToLong()

                // Share of item discount (applied before overall discount)
                val itemDiscountShare = if (billItem.itemDiscountPaise > 0L && totalAllocatedQty > 0.0) {
                    ((billItem.itemDiscountPaise * alloc.allocatedQuantity) / totalAllocatedQty).roundToLong()
                } else 0L

                val netCost = grossCost - itemDiscountShare

                participantItems[alloc.participantId]?.add(
                    SettlementItemBreakdown(
                        productName = billItem.productName,
                        quantity = alloc.allocatedQuantity,
                        unit = billItem.unit,
                        unitPricePaise = billItem.unitPricePaise,
                        grossAmountPaise = grossCost,
                        itemDiscountPaise = itemDiscountShare,
                        netAmountPaise = netCost
                    )
                )

                participantGrossSubtotals[alloc.participantId] = (participantGrossSubtotals[alloc.participantId] ?: 0L) + grossCost
                participantItemDiscounts[alloc.participantId] = (participantItemDiscounts[alloc.participantId] ?: 0L) + itemDiscountShare
            }
        }

        // Net subtotals for each participant after item discounts
        val participantNetSubtotals = participants.associate { p ->
            val gross = participantGrossSubtotals[p.id] ?: 0L
            val itemDisc = participantItemDiscounts[p.id] ?: 0L
            p.id to (gross - itemDisc)
        }

        // Step 2: Distribute Overall Discount
        val discountShares = when (billSummary.discountSplitMethod) {
            SplitMethod.PROPORTIONAL -> {
                distributePaiseProportionally(
                    billSummary.overallDiscountPaise,
                    participantNetSubtotals
                )
            }
            SplitMethod.EQUAL -> {
                distributePaiseEqually(
                    billSummary.overallDiscountPaise,
                    participants.map { it.id }
                )
            }
            SplitMethod.CUSTOM -> {
                // If custom not specified, fallback to proportional
                distributePaiseProportionally(
                    billSummary.overallDiscountPaise,
                    participantNetSubtotals
                )
            }
        }

        // Step 3: Distribute Taxes & Additional Charges
        val taxShares: Map<Long, Long>
        val chargesShares: Map<Long, Long>

        if (billSummary.taxChargesSplitMethod == SplitMethod.EQUAL) {
            taxShares = distributePaiseEqually(billSummary.taxPaise, participants.map { it.id })
            chargesShares = distributePaiseEqually(billSummary.additionalChargesPaise, participants.map { it.id })
        } else {
            // Default Proportional by net subtotal
            taxShares = distributePaiseProportionally(billSummary.taxPaise, participantNetSubtotals)
            chargesShares = distributePaiseProportionally(billSummary.additionalChargesPaise, participantNetSubtotals)
        }

        // Step 4: Upfront Purchase Payments (Who paid for the purchase at the shop)
        val purchasePaidMap: Map<Long, Long> = if (purchasePayments.isNotEmpty()) {
            val map = mutableMapOf<Long, Long>()
            for (pp in purchasePayments) {
                map[pp.participantId] = (map[pp.participantId] ?: 0L) + pp.amountPaise
            }
            map
        } else {
            // Default fallback: Organizer paid 100% of final bill
            val organizer = participants.firstOrNull { it.isOrganizer } ?: participants.firstOrNull()
            if (organizer != null) {
                mapOf(organizer.id to billSummary.finalBillAmountPaise)
            } else {
                emptyMap()
            }
        }

        // Step 5: Settlement payments recorded between participants
        val paymentsSentByParticipant = payments.groupBy { it.participantId }
        val paymentsReceivedByParticipant = payments.filter { it.toParticipantId != null }.groupBy { it.toParticipantId!! }

        // Step 6: Build final settlements & ensure mathematical exactness
        val settlements = mutableListOf<ParticipantSettlement>()

        for (p in participants) {
            val originalGross = participantGrossSubtotals[p.id] ?: 0L
            val itemDisc = participantItemDiscounts[p.id] ?: 0L
            val netSubtotal = originalGross - itemDisc
            val overallDisc = discountShares[p.id] ?: 0L
            val tax = taxShares[p.id] ?: 0L
            val charges = chargesShares[p.id] ?: 0L

            val finalPayable = max(0L, netSubtotal - overallDisc + tax + charges)
            val paidForPurchase = purchasePaidMap[p.id] ?: 0L
            val netBalance = paidForPurchase - finalPayable // positive: to receive, negative: owes

            val sent = paymentsSentByParticipant[p.id]?.sumOf { it.amountPaise } ?: 0L
            val received = paymentsReceivedByParticipant[p.id]?.sumOf { it.amountPaise } ?: 0L

            val outstanding = if (netBalance < 0L) {
                max(0L, abs(netBalance) - sent)
            } else if (netBalance > 0L) {
                max(0L, netBalance - received)
            } else {
                0L
            }

            val status = when {
                netBalance == 0L -> PaymentStatus.PAID
                netBalance < 0L && outstanding == 0L -> PaymentStatus.PAID
                netBalance < 0L && sent > 0L -> PaymentStatus.PARTIALLY_PAID
                netBalance > 0L && outstanding == 0L -> PaymentStatus.PAID
                netBalance > 0L && received > 0L -> PaymentStatus.PARTIALLY_PAID
                else -> PaymentStatus.PENDING
            }

            settlements.add(
                ParticipantSettlement(
                    participantId = p.id,
                    participantName = p.name,
                    isOrganizer = p.isOrganizer,
                    items = participantItems[p.id] ?: emptyList(),
                    originalSubtotalPaise = originalGross,
                    itemDiscountPaise = itemDisc,
                    netSubtotalPaise = netSubtotal,
                    overallDiscountSharePaise = overallDisc,
                    taxSharePaise = tax,
                    additionalChargesSharePaise = charges,
                    finalPayablePaise = finalPayable,
                    paidForPurchasePaise = paidForPurchase,
                    netBalancePaise = netBalance,
                    amountPaidPaise = sent,
                    amountReceivedPaise = received,
                    outstandingBalancePaise = outstanding,
                    paymentStatus = status
                )
            )
        }

        return settlements
    }

    /**
     * Calculates peer-to-peer settlement transfers using debt simplification (greedy matching).
     * Minimizes the number of cash transactions needed across members.
     */
    fun calculateSettlementTransfers(
        settlements: List<ParticipantSettlement>,
        payments: List<PaymentRecord> = emptyList()
    ): List<SettlementTransfer> {
        val participantMap = settlements.associateBy { it.participantId }

        data class BalanceEntry(val participantId: Long, var balancePaise: Long)

        val debtors = mutableListOf<BalanceEntry>()
        val creditors = mutableListOf<BalanceEntry>()

        for (s in settlements) {
            if (s.netBalancePaise < 0L) {
                debtors.add(BalanceEntry(s.participantId, abs(s.netBalancePaise)))
            } else if (s.netBalancePaise > 0L) {
                creditors.add(BalanceEntry(s.participantId, s.netBalancePaise))
            }
        }

        // Sort largest amounts first for greedy debt minimization
        debtors.sortByDescending { it.balancePaise }
        creditors.sortByDescending { it.balancePaise }

        val transfers = mutableListOf<SettlementTransfer>()
        var dIdx = 0
        var cIdx = 0

        while (dIdx < debtors.size && cIdx < creditors.size) {
            val debtor = debtors[dIdx]
            val creditor = creditors[cIdx]

            val transferAmount = min(debtor.balancePaise, creditor.balancePaise)
            if (transferAmount > 0L) {
                val fromName = participantMap[debtor.participantId]?.participantName ?: "Participant #${debtor.participantId}"
                val toName = participantMap[creditor.participantId]?.participantName ?: "Participant #${creditor.participantId}"

                // Check recorded payments between debtor and creditor
                val settled = payments.filter {
                    it.participantId == debtor.participantId &&
                    (it.toParticipantId == creditor.participantId || (it.toParticipantId == null && participantMap[creditor.participantId]?.isOrganizer == true))
                }.sumOf { it.amountPaise }

                transfers.add(
                    SettlementTransfer(
                        fromParticipantId = debtor.participantId,
                        fromParticipantName = fromName,
                        toParticipantId = creditor.participantId,
                        toParticipantName = toName,
                        amountPaise = transferAmount,
                        settledPaise = settled
                    )
                )

                debtor.balancePaise -= transferAmount
                creditor.balancePaise -= transferAmount
            }

            if (debtor.balancePaise <= 0L) dIdx++
            if (creditor.balancePaise <= 0L) cIdx++
        }

        return transfers
    }

    // ==========================================
    // Sharing & Export Message Generators
    // ==========================================

    fun generateShoppingListShareText(
        order: GroupOrder,
        consolidatedList: List<ConsolidatedItem>
    ): String {
        val sb = StringBuilder()
        sb.append("📋 *${order.orderName.trim()} – Consolidated Shopping List*\n")
        sb.append("👤 Organizer: ${order.organizerName}\n")
        sb.append("📦 Unique Items: ${consolidatedList.size}\n")
        sb.append("─────────────────────\n\n")

        for ((idx, item) in consolidatedList.withIndex()) {
            val qtyStr = if (item.totalOrderedQuantity % 1.0 == 0.0) {
                item.totalOrderedQuantity.toInt().toString()
            } else {
                String.format(Locale.US, "%.2f", item.totalOrderedQuantity)
            }
            sb.append("${idx + 1}. *${item.displayName}*: $qtyStr ${item.unit}\n")
            val buyers = item.participantOrders.joinToString(", ") {
                val q = if (it.quantity % 1.0 == 0.0) it.quantity.toInt().toString() else String.format(Locale.US, "%.2f", it.quantity)
                "${it.participantName} ($q)"
            }
            sb.append("   ↳ For: $buyers\n")
        }

        sb.append("\n─────────────────────\n")
        sb.append("Generated with ByteTools Group Shopping with Smart Split 🚀")
        return sb.toString()
    }

    fun generateParticipantShareText(
        order: GroupOrder,
        settlement: ParticipantSettlement,
        transfers: List<SettlementTransfer> = emptyList()
    ): String {
        val curr = order.currency
        val sb = StringBuilder()
        sb.append("🛍️ *${order.orderName.trim()} – Settlement Summary*\n\n")
        sb.append("Hi *${settlement.participantName}*,\n\n")
        sb.append("Here is your detailed order breakdown:\n")

        for (item in settlement.items) {
            val q = if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else String.format(Locale.US, "%.2f", item.quantity)
            val amt = formatPaise(item.netAmountPaise, curr)
            val unitP = formatPaise(item.unitPricePaise, curr)
            sb.append("• ${item.productName}: $q ${item.unit} @ $unitP = $amt\n")
        }

        sb.append("\n*Subtotal:* ${formatPaise(settlement.originalSubtotalPaise, curr)}\n")

        val totalDiscount = settlement.itemDiscountPaise + settlement.overallDiscountSharePaise
        if (totalDiscount > 0L) {
            sb.append("🎉 *Discount Savings:* -${formatPaise(totalDiscount, curr)}\n")
        }

        val extraCharges = settlement.taxSharePaise + settlement.additionalChargesSharePaise
        if (extraCharges > 0L) {
            sb.append("➕ *Taxes & Charges:* +${formatPaise(extraCharges, curr)}\n")
        }

        sb.append("─────────────────────\n")
        sb.append("💰 *Your Cost Share:* *${formatPaise(settlement.finalPayablePaise, curr)}*\n")
        if (settlement.paidForPurchasePaise > 0L) {
            sb.append("💳 *Paid Upfront for Purchase:* ${formatPaise(settlement.paidForPurchasePaise, curr)}\n")
        }

        if (settlement.netBalancePaise < 0L) {
            val netOwed = abs(settlement.netBalancePaise)
            sb.append("🔴 *Net Settlement (To Pay):* *${formatPaise(netOwed, curr)}*\n")
            if (settlement.amountPaidPaise > 0L) {
                sb.append("✅ *Settlement Paid so far:* ${formatPaise(settlement.amountPaidPaise, curr)}\n")
                sb.append("⏳ *Remaining Balance:* ${formatPaise(settlement.outstandingBalancePaise, curr)}\n")
            }

            val myPayTransfers = transfers.filter { it.fromParticipantId == settlement.participantId }
            if (myPayTransfers.isNotEmpty()) {
                sb.append("\n*Settlement Payment Instructions:*\n")
                for (t in myPayTransfers) {
                    val statusStr = if (t.isFullySettled) " (Settled ✅)" else ""
                    sb.append("👉 Pay *${formatPaise(t.amountPaise, curr)}* to *${t.toParticipantName}*$statusStr\n")
                }
            } else {
                sb.append("\nPlease pay ${formatPaise(settlement.outstandingBalancePaise, curr)} to ${order.organizerName} (Organizer).\n")
            }
        } else if (settlement.netBalancePaise > 0L) {
            sb.append("🟢 *Net Settlement (To Receive):* *+${formatPaise(settlement.netBalancePaise, curr)}*\n")
            if (settlement.amountReceivedPaise > 0L) {
                sb.append("✅ *Received so far:* ${formatPaise(settlement.amountReceivedPaise, curr)}\n")
                sb.append("⏳ *Pending to Receive:* ${formatPaise(settlement.outstandingBalancePaise, curr)}\n")
            }

            val myReceiveTransfers = transfers.filter { it.toParticipantId == settlement.participantId }
            if (myReceiveTransfers.isNotEmpty()) {
                sb.append("\n*You will receive payments from:*\n")
                for (t in myReceiveTransfers) {
                    val statusStr = if (t.isFullySettled) " (Received ✅)" else ""
                    sb.append("👉 *${formatPaise(t.amountPaise, curr)}* from *${t.fromParticipantName}*$statusStr\n")
                }
            }
        } else {
            sb.append("🎉 *You are fully settled up! (Net: ${formatPaise(0L, curr)})*\n")
        }

        sb.append("\nThank you!\n\n")
        sb.append("Generated via ByteTools Group Shopping with Smart Split 📲")
        return sb.toString()
    }

    fun generateAllSettlementShareText(
        order: GroupOrder,
        summary: BillSummary,
        settlements: List<ParticipantSettlement>,
        purchasePayments: List<PurchasePayment> = emptyList(),
        transfers: List<SettlementTransfer> = emptyList()
    ): String {
        val curr = order.currency
        val sb = StringBuilder()
        sb.append("🧾 *${order.orderName.trim()} – Final Settlement*\n")
        sb.append("👤 Organizer: ${order.organizerName}\n")
        sb.append("💳 Final Shop Bill: ${formatPaise(summary.finalBillAmountPaise, curr)}\n")

        if (purchasePayments.isNotEmpty()) {
            sb.append("\n💳 *Purchase Paid By:*\n")
            for (p in purchasePayments) {
                val pName = settlements.firstOrNull { it.participantId == p.participantId }?.participantName ?: p.participantName
                val methodStr = if (p.paymentMethod.isNotBlank() && p.paymentMethod != "Cash" && p.paymentMethod != "Paid") " (${p.paymentMethod})" else ""
                sb.append("• $pName: ${formatPaise(p.amountPaise, curr)}$methodStr\n")
            }
        }

        if (summary.overallDiscountPaise > 0L) {
            sb.append("🎉 Total Discount: ${formatPaise(summary.overallDiscountPaise, curr)}\n")
        }
        sb.append("─────────────────────\n\n")

        sb.append("*Member Balances:*\n")
        for ((idx, s) in settlements.withIndex()) {
            val netStr = when {
                s.netBalancePaise > 0L -> "Gets back +${formatPaise(s.netBalancePaise, curr)} 🟢"
                s.netBalancePaise < 0L -> "Owes ${formatPaise(abs(s.netBalancePaise), curr)} 🔴"
                else -> "Settled ✅"
            }
            sb.append("${idx + 1}. *${s.participantName}*: $netStr\n")
            sb.append("   Share: ${formatPaise(s.finalPayablePaise, curr)} | Paid Upfront: ${formatPaise(s.paidForPurchasePaise, curr)}\n")
        }

        if (transfers.isNotEmpty()) {
            sb.append("\n─────────────────────\n")
            sb.append("🤝 *Settlement Plan (Who Pays Whom):*\n")
            for (t in transfers) {
                val check = if (t.isFullySettled) "✅" else "⏳"
                sb.append("• *${t.fromParticipantName}* ➔ pays *${formatPaise(t.amountPaise, curr)}* ➔ *${t.toParticipantName}* $check\n")
            }
        }

        sb.append("\n─────────────────────\n")
        val totalPaid = settlements.sumOf { it.amountPaidPaise }
        sb.append("💵 Total Settlement Collected: ${formatPaise(totalPaid, curr)} / ${formatPaise(summary.finalBillAmountPaise, curr)}\n")
        sb.append("Shared via ByteTools 🚀")
        return sb.toString()
    }

    fun generateCsvExport(
        order: GroupOrder,
        summary: BillSummary,
        settlements: List<ParticipantSettlement>,
        transfers: List<SettlementTransfer> = emptyList()
    ): String {
        val sb = StringBuilder()
        sb.append("Order Name,${escapeCsv(order.orderName)}\n")
        sb.append("Organizer,${escapeCsv(order.organizerName)}\n")
        sb.append("Total Bill,${summary.finalBillAmountPaise / 100.0}\n")
        sb.append("Overall Discount,${summary.overallDiscountPaise / 100.0}\n")
        sb.append("Taxes & Charges,${(summary.taxPaise + summary.additionalChargesPaise) / 100.0}\n\n")

        sb.append("Participant,Role,Original Subtotal,Discount Share,Taxes & Charges,Cost Share,Paid Upfront,Net Balance,Status\n")
        for (s in settlements) {
            sb.append("${escapeCsv(s.participantName)},")
            sb.append("${if (s.isOrganizer) "Organizer" else "Participant"},")
            sb.append("${s.originalSubtotalPaise / 100.0},")
            sb.append("${(s.overallDiscountSharePaise + s.itemDiscountPaise) / 100.0},")
            sb.append("${(s.taxSharePaise + s.additionalChargesSharePaise) / 100.0},")
            sb.append("${s.finalPayablePaise / 100.0},")
            sb.append("${s.paidForPurchasePaise / 100.0},")
            sb.append("${s.netBalancePaise / 100.0},")
            sb.append("${s.paymentStatus.name}\n")
        }

        if (transfers.isNotEmpty()) {
            sb.append("\nSettlement Plan\n")
            sb.append("From,To,Amount,Settled Amount,Status\n")
            for (t in transfers) {
                sb.append("${escapeCsv(t.fromParticipantName)},")
                sb.append("${escapeCsv(t.toParticipantName)},")
                sb.append("${t.amountPaise / 100.0},")
                sb.append("${t.settledPaise / 100.0},")
                sb.append("${if (t.isFullySettled) "SETTLED" else "PENDING"}\n")
            }
        }

        return sb.toString()
    }

    private fun escapeCsv(str: String): String {
        return if (str.contains(",") || str.contains("\"") || str.contains("\n")) {
            "\"${str.replace("\"", "\"\"")}\""
        } else str
    }
}
