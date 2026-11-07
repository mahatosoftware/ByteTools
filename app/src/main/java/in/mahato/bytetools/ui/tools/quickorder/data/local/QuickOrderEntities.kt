package `in`.mahato.bytetools.ui.tools.quickorder.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import `in`.mahato.bytetools.ui.tools.quickorder.domain.model.*

@Entity(tableName = "quick_group_orders")
data class GroupOrderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val orderName: String,
    val organizerName: String,
    val description: String? = null,
    val currency: String = "₹",
    val status: String = OrderStatus.DRAFTING.name,
    val isLocked: Boolean = false,
    val deadline: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val finalBillAmountPaise: Long = 0L
) {
    fun toDomain() = GroupOrder(
        id = id,
        orderName = orderName,
        organizerName = organizerName,
        description = description,
        currency = currency,
        status = try { OrderStatus.valueOf(status) } catch (e: Exception) { OrderStatus.DRAFTING },
        isLocked = isLocked,
        deadline = deadline,
        createdAt = createdAt,
        updatedAt = updatedAt,
        finalBillAmountPaise = finalBillAmountPaise
    )

    companion object {
        fun fromDomain(order: GroupOrder) = GroupOrderEntity(
            id = order.id,
            orderName = order.orderName,
            organizerName = order.organizerName,
            description = order.description,
            currency = order.currency,
            status = order.status.name,
            isLocked = order.isLocked,
            deadline = order.deadline,
            createdAt = order.createdAt,
            updatedAt = order.updatedAt,
            finalBillAmountPaise = order.finalBillAmountPaise
        )
    }
}

@Entity(
    tableName = "quick_participants",
    indices = [Index("groupOrderId")]
)
data class ParticipantEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val groupOrderId: Long,
    val name: String,
    val isOrganizer: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain() = Participant(
        id = id,
        groupOrderId = groupOrderId,
        name = name,
        isOrganizer = isOrganizer,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(p: Participant) = ParticipantEntity(
            id = p.id,
            groupOrderId = p.groupOrderId,
            name = p.name,
            isOrganizer = p.isOrganizer,
            createdAt = p.createdAt
        )
    }
}

@Entity(
    tableName = "quick_order_items",
    indices = [Index("groupOrderId"), Index("participantId")]
)
data class ParticipantOrderItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val participantId: Long,
    val groupOrderId: Long,
    val productName: String,
    val normalizedProductName: String,
    val quantity: Double,
    val unit: String = "pcs",
    val estimatedUnitPricePaise: Long? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toDomain() = ParticipantOrderItem(
        id = id,
        participantId = participantId,
        groupOrderId = groupOrderId,
        productName = productName,
        normalizedProductName = normalizedProductName,
        quantity = quantity,
        unit = unit,
        estimatedUnitPricePaise = estimatedUnitPricePaise,
        notes = notes,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(item: ParticipantOrderItem) = ParticipantOrderItemEntity(
            id = item.id,
            participantId = item.participantId,
            groupOrderId = item.groupOrderId,
            productName = item.productName,
            normalizedProductName = item.normalizedProductName,
            quantity = item.quantity,
            unit = item.unit,
            estimatedUnitPricePaise = item.estimatedUnitPricePaise,
            notes = item.notes,
            createdAt = item.createdAt
        )
    }
}

@Entity(
    tableName = "quick_bill_items",
    indices = [Index("groupOrderId")]
)
data class BillItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val groupOrderId: Long,
    val productName: String,
    val normalizedProductName: String,
    val quantity: Double,
    val unit: String = "pcs",
    val unitPricePaise: Long,
    val itemDiscountPaise: Long = 0L,
    val totalPricePaise: Long
) {
    fun toDomain() = BillItem(
        id = id,
        groupOrderId = groupOrderId,
        productName = productName,
        normalizedProductName = normalizedProductName,
        quantity = quantity,
        unit = unit,
        unitPricePaise = unitPricePaise,
        itemDiscountPaise = itemDiscountPaise,
        totalPricePaise = totalPricePaise
    )

    companion object {
        fun fromDomain(item: BillItem) = BillItemEntity(
            id = item.id,
            groupOrderId = item.groupOrderId,
            productName = item.productName,
            normalizedProductName = item.normalizedProductName,
            quantity = item.quantity,
            unit = item.unit,
            unitPricePaise = item.unitPricePaise,
            itemDiscountPaise = item.itemDiscountPaise,
            totalPricePaise = item.totalPricePaise
        )
    }
}

@Entity(
    tableName = "quick_item_allocations",
    indices = [Index("groupOrderId"), Index("billItemId"), Index("participantId")]
)
data class ItemAllocationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val groupOrderId: Long,
    val billItemId: Long,
    val participantId: Long,
    val allocatedQuantity: Double,
    val allocatedAmountPaise: Long = 0L,
    val discountSharePaise: Long = 0L
) {
    fun toDomain() = ItemAllocation(
        id = id,
        groupOrderId = groupOrderId,
        billItemId = billItemId,
        participantId = participantId,
        allocatedQuantity = allocatedQuantity,
        allocatedAmountPaise = allocatedAmountPaise,
        discountSharePaise = discountSharePaise
    )

    companion object {
        fun fromDomain(alloc: ItemAllocation) = ItemAllocationEntity(
            id = alloc.id,
            groupOrderId = alloc.groupOrderId,
            billItemId = alloc.billItemId,
            participantId = alloc.participantId,
            allocatedQuantity = alloc.allocatedQuantity,
            allocatedAmountPaise = alloc.allocatedAmountPaise,
            discountSharePaise = alloc.discountSharePaise
        )
    }
}

@Entity(
    tableName = "quick_bill_summaries",
    indices = [Index("groupOrderId")]
)
data class BillSummaryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val groupOrderId: Long,
    val subtotalPaise: Long,
    val itemDiscountTotalPaise: Long = 0L,
    val overallDiscountType: String = "FIXED",
    val overallDiscountValue: Double = 0.0,
    val overallDiscountPaise: Long = 0L,
    val taxPaise: Long = 0L,
    val additionalChargesPaise: Long = 0L,
    val additionalChargesNote: String = "",
    val finalBillAmountPaise: Long,
    val discountSplitMethod: String = SplitMethod.PROPORTIONAL.name,
    val taxChargesSplitMethod: String = SplitMethod.PROPORTIONAL.name,
    val finalizedAt: Long? = null
) {
    fun toDomain() = BillSummary(
        id = id,
        groupOrderId = groupOrderId,
        subtotalPaise = subtotalPaise,
        itemDiscountTotalPaise = itemDiscountTotalPaise,
        overallDiscountType = overallDiscountType,
        overallDiscountValue = overallDiscountValue,
        overallDiscountPaise = overallDiscountPaise,
        taxPaise = taxPaise,
        additionalChargesPaise = additionalChargesPaise,
        additionalChargesNote = additionalChargesNote,
        finalBillAmountPaise = finalBillAmountPaise,
        discountSplitMethod = try { SplitMethod.valueOf(discountSplitMethod) } catch (e: Exception) { SplitMethod.PROPORTIONAL },
        taxChargesSplitMethod = try { SplitMethod.valueOf(taxChargesSplitMethod) } catch (e: Exception) { SplitMethod.PROPORTIONAL },
        finalizedAt = finalizedAt
    )

    companion object {
        fun fromDomain(summary: BillSummary) = BillSummaryEntity(
            id = summary.id,
            groupOrderId = summary.groupOrderId,
            subtotalPaise = summary.subtotalPaise,
            itemDiscountTotalPaise = summary.itemDiscountTotalPaise,
            overallDiscountType = summary.overallDiscountType,
            overallDiscountValue = summary.overallDiscountValue,
            overallDiscountPaise = summary.overallDiscountPaise,
            taxPaise = summary.taxPaise,
            additionalChargesPaise = summary.additionalChargesPaise,
            additionalChargesNote = summary.additionalChargesNote,
            finalBillAmountPaise = summary.finalBillAmountPaise,
            discountSplitMethod = summary.discountSplitMethod.name,
            taxChargesSplitMethod = summary.taxChargesSplitMethod.name,
            finalizedAt = summary.finalizedAt
        )
    }
}

@Entity(
    tableName = "quick_payment_records",
    indices = [Index("groupOrderId"), Index("participantId")]
)
data class PaymentRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val groupOrderId: Long,
    val participantId: Long,
    val amountPaise: Long,
    val paymentMethod: String = "Cash",
    val transactionReference: String? = null,
    val notes: String? = null,
    val paymentDate: Long = System.currentTimeMillis()
) {
    fun toDomain() = PaymentRecord(
        id = id,
        groupOrderId = groupOrderId,
        participantId = participantId,
        amountPaise = amountPaise,
        paymentMethod = paymentMethod,
        transactionReference = transactionReference,
        notes = notes,
        paymentDate = paymentDate
    )

    companion object {
        fun fromDomain(record: PaymentRecord) = PaymentRecordEntity(
            id = record.id,
            groupOrderId = record.groupOrderId,
            participantId = record.participantId,
            amountPaise = record.amountPaise,
            paymentMethod = record.paymentMethod,
            transactionReference = record.transactionReference,
            notes = record.notes,
            paymentDate = record.paymentDate
        )
    }
}
