package `in`.mahato.bytetools.ui.tools.quickorder.data.repository

import `in`.mahato.bytetools.ui.tools.quickorder.data.local.*
import `in`.mahato.bytetools.ui.tools.quickorder.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

interface QuickOrderRepository {
    fun getAllOrders(): Flow<List<GroupOrder>>
    suspend fun getOrderById(orderId: Long): GroupOrder?
    fun observeOrderById(orderId: Long): Flow<GroupOrder?>
    suspend fun createOrder(order: GroupOrder, participantNames: List<String>): Long
    suspend fun updateOrder(order: GroupOrder)
    suspend fun deleteOrder(orderId: Long)

    fun observeParticipants(orderId: Long): Flow<List<Participant>>
    suspend fun getParticipants(orderId: Long): List<Participant>
    suspend fun addParticipant(orderId: Long, name: String, isOrganizer: Boolean = false): Long
    suspend fun deleteParticipant(participantId: Long)

    fun observeOrderItems(orderId: Long): Flow<List<ParticipantOrderItem>>
    suspend fun getOrderItems(orderId: Long): List<ParticipantOrderItem>
    suspend fun saveOrderItem(item: ParticipantOrderItem): Long
    suspend fun deleteOrderItem(itemId: Long)
    suspend fun getDistinctProductNames(orderId: Long): List<String>

    fun observeBillItems(orderId: Long): Flow<List<BillItem>>
    suspend fun getBillItems(orderId: Long): List<BillItem>
    suspend fun saveBillItems(orderId: Long, items: List<BillItem>)
    suspend fun addBillItem(item: BillItem): Long
    suspend fun updateBillItem(item: BillItem)
    suspend fun deleteBillItem(itemId: Long)

    fun observeAllocations(orderId: Long): Flow<List<ItemAllocation>>
    suspend fun getAllocations(orderId: Long): List<ItemAllocation>
    suspend fun saveAllocations(orderId: Long, allocations: List<ItemAllocation>)

    fun observeBillSummary(orderId: Long): Flow<BillSummary?>
    suspend fun getBillSummary(orderId: Long): BillSummary?
    suspend fun saveBillSummary(summary: BillSummary): Long

    fun observePayments(orderId: Long): Flow<List<PaymentRecord>>
    suspend fun getPayments(orderId: Long): List<PaymentRecord>
    suspend fun recordPayment(payment: PaymentRecord): Long
    suspend fun deletePayment(paymentId: Long)

    fun observePurchasePayments(orderId: Long): Flow<List<PurchasePayment>>
    suspend fun getPurchasePayments(orderId: Long): List<PurchasePayment>
    suspend fun savePurchasePayments(orderId: Long, payments: List<PurchasePayment>)
    suspend fun addPurchasePayment(payment: PurchasePayment): Long
    suspend fun deletePurchasePayment(paymentId: Long)
}

@Singleton
class QuickOrderRepositoryImpl @Inject constructor(
    private val dao: QuickOrderDao
) : QuickOrderRepository {

    override fun getAllOrders(): Flow<List<GroupOrder>> {
        return dao.getAllOrders().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getOrderById(orderId: Long): GroupOrder? {
        return dao.getOrderById(orderId)?.toDomain()
    }

    override fun observeOrderById(orderId: Long): Flow<GroupOrder?> {
        return dao.observeOrderById(orderId).map { it?.toDomain() }
    }

    override suspend fun createOrder(order: GroupOrder, participantNames: List<String>): Long {
        val orderEntity = GroupOrderEntity.fromDomain(order)
        val orderId = dao.insertOrder(orderEntity)

        // Ensure organizer is first participant
        val participantsToInsert = mutableListOf<ParticipantEntity>()
        participantsToInsert.add(
            ParticipantEntity(
                groupOrderId = orderId,
                name = order.organizerName.trim(),
                isOrganizer = true
            )
        )

        for (name in participantNames) {
            val trimmed = name.trim()
            if (trimmed.isNotBlank() && !trimmed.equals(order.organizerName.trim(), ignoreCase = true)) {
                participantsToInsert.add(
                    ParticipantEntity(
                        groupOrderId = orderId,
                        name = trimmed,
                        isOrganizer = false
                    )
                )
            }
        }
        dao.insertParticipants(participantsToInsert)
        return orderId
    }

    override suspend fun updateOrder(order: GroupOrder) {
        dao.updateOrder(GroupOrderEntity.fromDomain(order.copy(updatedAt = System.currentTimeMillis())))
    }

    override suspend fun deleteOrder(orderId: Long) {
        dao.deleteEntireGroupOrder(orderId)
    }

    override fun observeParticipants(orderId: Long): Flow<List<Participant>> {
        return dao.observeParticipants(orderId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getParticipants(orderId: Long): List<Participant> {
        return dao.getParticipants(orderId).map { it.toDomain() }
    }

    override suspend fun addParticipant(orderId: Long, name: String, isOrganizer: Boolean): Long {
        return dao.insertParticipant(
            ParticipantEntity(
                groupOrderId = orderId,
                name = name.trim(),
                isOrganizer = isOrganizer
            )
        )
    }

    override suspend fun deleteParticipant(participantId: Long) {
        dao.deleteOrderItemsForParticipant(participantId)
        dao.deleteParticipantById(participantId)
    }

    override fun observeOrderItems(orderId: Long): Flow<List<ParticipantOrderItem>> {
        return dao.observeOrderItems(orderId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getOrderItems(orderId: Long): List<ParticipantOrderItem> {
        return dao.getOrderItems(orderId).map { it.toDomain() }
    }

    override suspend fun saveOrderItem(item: ParticipantOrderItem): Long {
        return dao.insertOrderItem(ParticipantOrderItemEntity.fromDomain(item))
    }

    override suspend fun deleteOrderItem(itemId: Long) {
        dao.deleteOrderItemById(itemId)
    }

    override suspend fun getDistinctProductNames(orderId: Long): List<String> {
        return dao.getDistinctProductNames(orderId)
    }

    override fun observeBillItems(orderId: Long): Flow<List<BillItem>> {
        return dao.observeBillItems(orderId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getBillItems(orderId: Long): List<BillItem> {
        return dao.getBillItems(orderId).map { it.toDomain() }
    }

    override suspend fun saveBillItems(orderId: Long, items: List<BillItem>) {
        dao.deleteBillItemsForOrder(orderId)
        dao.insertBillItems(items.map { BillItemEntity.fromDomain(it.copy(groupOrderId = orderId)) })
    }

    override suspend fun addBillItem(item: BillItem): Long {
        return dao.insertBillItem(BillItemEntity.fromDomain(item))
    }

    override suspend fun updateBillItem(item: BillItem) {
        dao.updateBillItem(BillItemEntity.fromDomain(item))
    }

    override suspend fun deleteBillItem(itemId: Long) {
        dao.deleteBillItemById(itemId)
    }

    override fun observeAllocations(orderId: Long): Flow<List<ItemAllocation>> {
        return dao.observeAllocations(orderId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getAllocations(orderId: Long): List<ItemAllocation> {
        return dao.getAllocations(orderId).map { it.toDomain() }
    }

    override suspend fun saveAllocations(orderId: Long, allocations: List<ItemAllocation>) {
        dao.deleteAllocationsForOrder(orderId)
        dao.insertAllocations(allocations.map { ItemAllocationEntity.fromDomain(it.copy(groupOrderId = orderId)) })
    }

    override fun observeBillSummary(orderId: Long): Flow<BillSummary?> {
        return dao.observeBillSummary(orderId).map { it?.toDomain() }
    }

    override suspend fun getBillSummary(orderId: Long): BillSummary? {
        return dao.getBillSummary(orderId)?.toDomain()
    }

    override suspend fun saveBillSummary(summary: BillSummary): Long {
        return dao.insertOrUpdateBillSummary(BillSummaryEntity.fromDomain(summary))
    }

    override fun observePayments(orderId: Long): Flow<List<PaymentRecord>> {
        return dao.observePayments(orderId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getPayments(orderId: Long): List<PaymentRecord> {
        return dao.getPayments(orderId).map { it.toDomain() }
    }

    override suspend fun recordPayment(payment: PaymentRecord): Long {
        return dao.insertPayment(PaymentRecordEntity.fromDomain(payment))
    }

    override suspend fun deletePayment(paymentId: Long) {
        dao.deletePaymentById(paymentId)
    }

    override fun observePurchasePayments(orderId: Long): Flow<List<PurchasePayment>> {
        return dao.observePurchasePayments(orderId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getPurchasePayments(orderId: Long): List<PurchasePayment> {
        return dao.getPurchasePayments(orderId).map { it.toDomain() }
    }

    override suspend fun savePurchasePayments(orderId: Long, payments: List<PurchasePayment>) {
        dao.deletePurchasePaymentsForOrder(orderId)
        dao.insertPurchasePayments(payments.map { PurchasePaymentEntity.fromDomain(it.copy(groupOrderId = orderId)) })
    }

    override suspend fun addPurchasePayment(payment: PurchasePayment): Long {
        return dao.insertPurchasePayment(PurchasePaymentEntity.fromDomain(payment))
    }

    override suspend fun deletePurchasePayment(paymentId: Long) {
        dao.deletePurchasePaymentById(paymentId)
    }
}
