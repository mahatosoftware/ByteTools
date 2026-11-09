package `in`.mahato.bytetools.ui.tools.quickorder.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface QuickOrderDao {

    // Group Orders
    @Query("SELECT * FROM quick_group_orders ORDER BY updatedAt DESC")
    fun getAllOrders(): Flow<List<GroupOrderEntity>>

    @Query("SELECT * FROM quick_group_orders WHERE id = :orderId")
    suspend fun getOrderById(orderId: Long): GroupOrderEntity?

    @Query("SELECT * FROM quick_group_orders WHERE id = :orderId")
    fun observeOrderById(orderId: Long): Flow<GroupOrderEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: GroupOrderEntity): Long

    @Update
    suspend fun updateOrder(order: GroupOrderEntity)

    @Query("DELETE FROM quick_group_orders WHERE id = :orderId")
    suspend fun deleteOrder(orderId: Long)

    // Participants
    @Query("SELECT * FROM quick_participants WHERE groupOrderId = :orderId ORDER BY id ASC")
    fun observeParticipants(orderId: Long): Flow<List<ParticipantEntity>>

    @Query("SELECT * FROM quick_participants WHERE groupOrderId = :orderId ORDER BY id ASC")
    suspend fun getParticipants(orderId: Long): List<ParticipantEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParticipant(participant: ParticipantEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParticipants(participants: List<ParticipantEntity>): List<Long>

    @Delete
    suspend fun deleteParticipant(participant: ParticipantEntity)

    @Query("DELETE FROM quick_participants WHERE id = :participantId")
    suspend fun deleteParticipantById(participantId: Long)

    // Order Items
    @Query("SELECT * FROM quick_order_items WHERE groupOrderId = :orderId ORDER BY id ASC")
    fun observeOrderItems(orderId: Long): Flow<List<ParticipantOrderItemEntity>>

    @Query("SELECT * FROM quick_order_items WHERE groupOrderId = :orderId ORDER BY id ASC")
    suspend fun getOrderItems(orderId: Long): List<ParticipantOrderItemEntity>

    @Query("SELECT * FROM quick_order_items WHERE participantId = :participantId ORDER BY id ASC")
    fun observeOrderItemsForParticipant(participantId: Long): Flow<List<ParticipantOrderItemEntity>>

    @Query("SELECT DISTINCT productName FROM quick_order_items WHERE groupOrderId = :orderId")
    suspend fun getDistinctProductNames(orderId: Long): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrderItem(item: ParticipantOrderItemEntity): Long

    @Update
    suspend fun updateOrderItem(item: ParticipantOrderItemEntity)

    @Delete
    suspend fun deleteOrderItem(item: ParticipantOrderItemEntity)

    @Query("DELETE FROM quick_order_items WHERE id = :itemId")
    suspend fun deleteOrderItemById(itemId: Long)

    @Query("DELETE FROM quick_order_items WHERE participantId = :participantId")
    suspend fun deleteOrderItemsForParticipant(participantId: Long)

    // Bill Items
    @Query("SELECT * FROM quick_bill_items WHERE groupOrderId = :orderId ORDER BY id ASC")
    fun observeBillItems(orderId: Long): Flow<List<BillItemEntity>>

    @Query("SELECT * FROM quick_bill_items WHERE groupOrderId = :orderId ORDER BY id ASC")
    suspend fun getBillItems(orderId: Long): List<BillItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBillItem(item: BillItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBillItems(items: List<BillItemEntity>): List<Long>

    @Update
    suspend fun updateBillItem(item: BillItemEntity)

    @Query("DELETE FROM quick_bill_items WHERE id = :itemId")
    suspend fun deleteBillItemById(itemId: Long)

    @Query("DELETE FROM quick_bill_items WHERE groupOrderId = :orderId")
    suspend fun deleteBillItemsForOrder(orderId: Long)

    // Item Allocations
    @Query("SELECT * FROM quick_item_allocations WHERE groupOrderId = :orderId")
    fun observeAllocations(orderId: Long): Flow<List<ItemAllocationEntity>>

    @Query("SELECT * FROM quick_item_allocations WHERE groupOrderId = :orderId")
    suspend fun getAllocations(orderId: Long): List<ItemAllocationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllocations(allocations: List<ItemAllocationEntity>)

    @Query("DELETE FROM quick_item_allocations WHERE groupOrderId = :orderId")
    suspend fun deleteAllocationsForOrder(orderId: Long)

    // Bill Summary
    @Query("SELECT * FROM quick_bill_summaries WHERE groupOrderId = :orderId LIMIT 1")
    fun observeBillSummary(orderId: Long): Flow<BillSummaryEntity?>

    @Query("SELECT * FROM quick_bill_summaries WHERE groupOrderId = :orderId LIMIT 1")
    suspend fun getBillSummary(orderId: Long): BillSummaryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateBillSummary(summary: BillSummaryEntity): Long

    @Query("DELETE FROM quick_bill_summaries WHERE groupOrderId = :orderId")
    suspend fun deleteBillSummaryForOrder(orderId: Long)

    // Payment Records
    @Query("SELECT * FROM quick_payment_records WHERE groupOrderId = :orderId ORDER BY paymentDate DESC")
    fun observePayments(orderId: Long): Flow<List<PaymentRecordEntity>>

    @Query("SELECT * FROM quick_payment_records WHERE groupOrderId = :orderId")
    suspend fun getPayments(orderId: Long): List<PaymentRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(record: PaymentRecordEntity): Long

    @Delete
    suspend fun deletePayment(record: PaymentRecordEntity)

    @Query("DELETE FROM quick_payment_records WHERE id = :paymentId")
    suspend fun deletePaymentById(paymentId: Long)

    @Query("DELETE FROM quick_payment_records WHERE groupOrderId = :orderId")
    suspend fun deletePaymentsForOrder(orderId: Long)

    // Purchase Payments (Who paid the purchase bill upfront)
    @Query("SELECT * FROM quick_purchase_payments WHERE groupOrderId = :orderId ORDER BY id ASC")
    fun observePurchasePayments(orderId: Long): Flow<List<PurchasePaymentEntity>>

    @Query("SELECT * FROM quick_purchase_payments WHERE groupOrderId = :orderId ORDER BY id ASC")
    suspend fun getPurchasePayments(orderId: Long): List<PurchasePaymentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchasePayments(payments: List<PurchasePaymentEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchasePayment(payment: PurchasePaymentEntity): Long

    @Query("DELETE FROM quick_purchase_payments WHERE groupOrderId = :orderId")
    suspend fun deletePurchasePaymentsForOrder(orderId: Long)

    @Query("DELETE FROM quick_purchase_payments WHERE id = :paymentId")
    suspend fun deletePurchasePaymentById(paymentId: Long)

    // Cascading delete for entire order
    @Transaction
    suspend fun deleteEntireGroupOrder(orderId: Long) {
        deleteAllocationsForOrder(orderId)
        deleteBillSummaryForOrder(orderId)
        deleteBillItemsForOrder(orderId)
        deletePurchasePaymentsForOrder(orderId)
        deletePaymentsForOrder(orderId)
        deleteOrder(orderId)
    }
}
