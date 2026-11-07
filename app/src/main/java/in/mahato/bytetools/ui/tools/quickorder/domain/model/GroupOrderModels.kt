package `in`.mahato.bytetools.ui.tools.quickorder.domain.model

enum class OrderStatus {
    DRAFTING,    // Collecting participant orders
    LOCKED,      // Orders locked by organizer before shopping
    SHOPPING,    // Entering bill
    RECONCILED,  // Quantities matched and confirmed
    SETTLED      // Bill split calculated, ready for payment
}

enum class SplitMethod {
    PROPORTIONAL,
    EQUAL,
    CUSTOM
}

enum class PaymentStatus {
    PENDING,
    PARTIALLY_PAID,
    PAID
}

enum class ReconciliationStatus {
    MATCHED,
    SHORTFALL,
    SURPLUS,
    UNAVAILABLE,
    NEW_ITEM
}

data class GroupOrder(
    val id: Long = 0,
    val orderName: String,
    val organizerName: String,
    val description: String? = null,
    val currency: String = "₹",
    val status: OrderStatus = OrderStatus.DRAFTING,
    val isLocked: Boolean = false,
    val deadline: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val finalBillAmountPaise: Long = 0L
)

data class Participant(
    val id: Long = 0,
    val groupOrderId: Long,
    val name: String,
    val isOrganizer: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class ParticipantOrderItem(
    val id: Long = 0,
    val participantId: Long,
    val groupOrderId: Long,
    val productName: String,
    val normalizedProductName: String = productName.trim().lowercase(),
    val quantity: Double,
    val unit: String = "pcs",
    val estimatedUnitPricePaise: Long? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class ConsolidatedItem(
    val normalizedProductName: String,
    val displayName: String,
    val unit: String,
    val totalOrderedQuantity: Double,
    val estimatedUnitPricePaise: Long? = null,
    val participantOrders: List<ParticipantOrderQuantity> = emptyList()
)

data class ParticipantOrderQuantity(
    val participantId: Long,
    val participantName: String,
    val quantity: Double,
    val unit: String
)

data class BillItem(
    val id: Long = 0,
    val groupOrderId: Long,
    val productName: String,
    val normalizedProductName: String = productName.trim().lowercase(),
    val quantity: Double,
    val unit: String = "pcs",
    val unitPricePaise: Long,
    val itemDiscountPaise: Long = 0L,
    val totalPricePaise: Long = (quantity * unitPricePaise).toLong() - itemDiscountPaise
)

data class ItemAllocation(
    val id: Long = 0,
    val groupOrderId: Long,
    val billItemId: Long,
    val participantId: Long,
    val allocatedQuantity: Double,
    val allocatedAmountPaise: Long = 0L,
    val discountSharePaise: Long = 0L
)

data class BillSummary(
    val id: Long = 0,
    val groupOrderId: Long,
    val subtotalPaise: Long,
    val itemDiscountTotalPaise: Long = 0L,
    val overallDiscountType: String = "FIXED", // FIXED or PERCENTAGE
    val overallDiscountValue: Double = 0.0,
    val overallDiscountPaise: Long = 0L,
    val taxPaise: Long = 0L,
    val additionalChargesPaise: Long = 0L,
    val additionalChargesNote: String = "",
    val finalBillAmountPaise: Long,
    val discountSplitMethod: SplitMethod = SplitMethod.PROPORTIONAL,
    val taxChargesSplitMethod: SplitMethod = SplitMethod.PROPORTIONAL,
    val finalizedAt: Long? = null
)

data class PaymentRecord(
    val id: Long = 0,
    val groupOrderId: Long,
    val participantId: Long,
    val amountPaise: Long,
    val paymentMethod: String = "Cash", // Cash, UPI, Bank Transfer, Other
    val transactionReference: String? = null,
    val notes: String? = null,
    val paymentDate: Long = System.currentTimeMillis()
)

data class SettlementItemBreakdown(
    val productName: String,
    val quantity: Double,
    val unit: String,
    val unitPricePaise: Long,
    val grossAmountPaise: Long,
    val itemDiscountPaise: Long,
    val netAmountPaise: Long
)

data class ParticipantSettlement(
    val participantId: Long,
    val participantName: String,
    val isOrganizer: Boolean,
    val items: List<SettlementItemBreakdown>,
    val originalSubtotalPaise: Long,
    val itemDiscountPaise: Long,
    val netSubtotalPaise: Long,
    val overallDiscountSharePaise: Long,
    val taxSharePaise: Long,
    val additionalChargesSharePaise: Long,
    val finalPayablePaise: Long,
    val amountPaidPaise: Long,
    val outstandingBalancePaise: Long,
    val paymentStatus: PaymentStatus
)

data class ReconciliationItem(
    val billItem: BillItem,
    val matchedConsolidatedItem: ConsolidatedItem?,
    val orderedQuantity: Double,
    val purchasedQuantity: Double,
    val status: ReconciliationStatus,
    val participantAllocations: Map<Long, Double> = emptyMap() // participantId -> allocatedQuantity
)
