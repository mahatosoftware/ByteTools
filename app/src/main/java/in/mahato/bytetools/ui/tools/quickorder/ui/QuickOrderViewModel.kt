package `in`.mahato.bytetools.ui.tools.quickorder.ui

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.mahato.bytetools.ui.tools.quickorder.data.repository.QuickOrderRepository
import `in`.mahato.bytetools.ui.tools.quickorder.domain.engine.ParsedReceipt
import `in`.mahato.bytetools.ui.tools.quickorder.domain.engine.ReceiptOcrParser
import `in`.mahato.bytetools.ui.tools.quickorder.domain.engine.SmartSplitEngine
import `in`.mahato.bytetools.ui.tools.quickorder.domain.model.*
import `in`.mahato.bytetools.utils.OCRManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class QuickOrderViewModel @Inject constructor(
    private val repository: QuickOrderRepository
) : ViewModel() {

    val allOrders: StateFlow<List<GroupOrder>> = repository.getAllOrders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentOrderId = MutableStateFlow<Long?>(null)
    val currentOrderId: StateFlow<Long?> = _currentOrderId.asStateFlow()

    // Active order streams
    val activeOrder: StateFlow<GroupOrder?> = _currentOrderId.flatMapLatest { id ->
        if (id == null) flowOf(null) else repository.observeOrderById(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val participants: StateFlow<List<Participant>> = _currentOrderId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repository.observeParticipants(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val orderItems: StateFlow<List<ParticipantOrderItem>> = _currentOrderId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repository.observeOrderItems(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val consolidatedItems: StateFlow<List<ConsolidatedItem>> = combine(orderItems, participants) { items, parts ->
        SmartSplitEngine.consolidateOrders(items, parts)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val billItems: StateFlow<List<BillItem>> = _currentOrderId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repository.observeBillItems(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allocations: StateFlow<List<ItemAllocation>> = _currentOrderId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repository.observeAllocations(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val billSummary: StateFlow<BillSummary?> = _currentOrderId.flatMapLatest { id ->
        if (id == null) flowOf(null) else repository.observeBillSummary(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val payments: StateFlow<List<PaymentRecord>> = _currentOrderId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repository.observePayments(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val purchasePayments: StateFlow<List<PurchasePayment>> = _currentOrderId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repository.observePurchasePayments(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Calculated Settlements
    val settlements: StateFlow<List<ParticipantSettlement>> = combine(
        combine(billItems, allocations, billSummary) { items, allocs, sum -> Triple(items, allocs, sum) },
        participants,
        payments,
        purchasePayments
    ) { (items, allocs, summary), parts, pmts, pPayments ->
        if (summary != null && parts.isNotEmpty()) {
            SmartSplitEngine.calculateSettlement(items, allocs, summary, parts, pmts, pPayments)
        } else {
            emptyList()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Calculated Settlement Transfers (Who pays whom)
    val settlementTransfers: StateFlow<List<SettlementTransfer>> = combine(
        settlements,
        payments
    ) { setts, pmts ->
        SmartSplitEngine.calculateSettlementTransfers(setts, pmts)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Reconciliation state
    val reconciliationItems: StateFlow<List<ReconciliationItem>> = combine(
        billItems,
        consolidatedItems,
        participants
    ) { bItems, cItems, parts ->
        SmartSplitEngine.reconcileBill(bItems, cItems, parts)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _distinctProducts = MutableStateFlow<List<String>>(emptyList())
    val distinctProducts: StateFlow<List<String>> = _distinctProducts.asStateFlow()

    fun selectOrder(orderId: Long) {
        _currentOrderId.value = orderId
        viewModelScope.launch {
            _distinctProducts.value = repository.getDistinctProductNames(orderId)
        }
    }

    fun createOrder(
        orderName: String,
        organizerName: String,
        description: String?,
        currency: String = "₹",
        deadline: Long? = null,
        participants: List<String>,
        onCreated: (Long) -> Unit
    ) {
        viewModelScope.launch {
            val newOrder = GroupOrder(
                orderName = orderName.trim(),
                organizerName = organizerName.trim(),
                description = description?.trim(),
                currency = currency,
                deadline = deadline,
                status = OrderStatus.DRAFTING
            )
            val orderId = repository.createOrder(newOrder, participants)
            _currentOrderId.value = orderId
            onCreated(orderId)
        }
    }

    fun addParticipant(orderId: Long, name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.addParticipant(orderId, name.trim(), isOrganizer = false)
        }
    }

    fun deleteParticipant(participantId: Long) {
        viewModelScope.launch {
            repository.deleteParticipant(participantId)
        }
    }

    /**
     * Adds an order item. If an identical product exists for this participant,
     * it automatically merges the quantity.
     */
    fun addOrMergeOrderItem(
        participantId: Long,
        orderId: Long,
        productName: String,
        quantity: Double,
        unit: String,
        estimatedUnitPricePaise: Long?,
        notes: String?
    ) {
        if (productName.isBlank() || quantity <= 0.0) return
        viewModelScope.launch {
            val existing = repository.getOrderItems(orderId)
                .firstOrNull { it.participantId == participantId && it.normalizedProductName == productName.trim().lowercase() }

            if (existing != null) {
                // Merge quantity
                val merged = existing.copy(
                    quantity = existing.quantity + quantity,
                    estimatedUnitPricePaise = estimatedUnitPricePaise ?: existing.estimatedUnitPricePaise,
                    notes = listOfNotNull(existing.notes, notes).joinToString("; ").ifBlank { null }
                )
                repository.saveOrderItem(merged)
            } else {
                val newItem = ParticipantOrderItem(
                    participantId = participantId,
                    groupOrderId = orderId,
                    productName = productName.trim(),
                    quantity = quantity,
                    unit = unit.trim(),
                    estimatedUnitPricePaise = estimatedUnitPricePaise,
                    notes = notes?.trim()
                )
                repository.saveOrderItem(newItem)
            }
            _distinctProducts.value = repository.getDistinctProductNames(orderId)
        }
    }

    fun deleteOrderItem(itemId: Long) {
        viewModelScope.launch {
            repository.deleteOrderItem(itemId)
        }
    }

    fun lockOrders(orderId: Long, isLocked: Boolean) {
        viewModelScope.launch {
            val order = repository.getOrderById(orderId) ?: return@launch
            val nextStatus = if (isLocked) OrderStatus.LOCKED else OrderStatus.DRAFTING
            repository.updateOrder(order.copy(isLocked = isLocked, status = nextStatus))
        }
    }

    fun saveBillItemsAndSummary(
        orderId: Long,
        items: List<BillItem>,
        summary: BillSummary
    ) {
        viewModelScope.launch {
            repository.saveBillItems(orderId, items)
            repository.saveBillSummary(summary)
            val order = repository.getOrderById(orderId)
            if (order != null) {
                repository.updateOrder(
                    order.copy(
                        status = OrderStatus.SHOPPING,
                        finalBillAmountPaise = summary.finalBillAmountPaise
                    )
                )
            }
        }
    }

    fun confirmReconciliation(
        orderId: Long,
        allocations: List<ItemAllocation>,
        updatedBillItems: List<BillItem>,
        updatedSummary: BillSummary
    ) {
        viewModelScope.launch {
            repository.saveBillItems(orderId, updatedBillItems)
            repository.saveAllocations(orderId, allocations)
            repository.saveBillSummary(updatedSummary)
            val order = repository.getOrderById(orderId)
            if (order != null) {
                repository.updateOrder(
                    order.copy(
                        status = OrderStatus.SETTLED,
                        finalBillAmountPaise = updatedSummary.finalBillAmountPaise
                    )
                )
            }
        }
    }

    fun updateSplitMethod(orderId: Long, method: SplitMethod) {
        viewModelScope.launch {
            val currentSummary = repository.getBillSummary(orderId) ?: return@launch
            val updated = currentSummary.copy(discountSplitMethod = method)
            repository.saveBillSummary(updated)
        }
    }

    fun savePurchasePayments(orderId: Long, payments: List<PurchasePayment>) {
        viewModelScope.launch {
            repository.savePurchasePayments(orderId, payments)
        }
    }

    fun deletePurchasePayment(paymentId: Long) {
        viewModelScope.launch {
            repository.deletePurchasePayment(paymentId)
        }
    }

    fun recordPayment(
        orderId: Long,
        participantId: Long,
        toParticipantId: Long? = null,
        amountPaise: Long,
        method: String = "Paid",
        reference: String? = null,
        notes: String? = null
    ) {
        if (amountPaise <= 0L) return
        viewModelScope.launch {
            repository.recordPayment(
                PaymentRecord(
                    groupOrderId = orderId,
                    participantId = participantId,
                    toParticipantId = toParticipantId,
                    amountPaise = amountPaise,
                    paymentMethod = method,
                    transactionReference = reference?.trim(),
                    notes = notes?.trim()
                )
            )
        }
    }

    fun deletePayment(paymentId: Long) {
        viewModelScope.launch {
            repository.deletePayment(paymentId)
        }
    }

    fun deleteGroupOrder(orderId: Long) {
        viewModelScope.launch {
            repository.deleteOrder(orderId)
            if (_currentOrderId.value == orderId) {
                _currentOrderId.value = null
            }
        }
    }

    suspend fun parseOcrReceipt(context: Context, bitmap: Bitmap): ParsedReceipt = withContext(Dispatchers.IO) {
        val ocrManager = OCRManager(context)
        try {
            val text = ocrManager.recognizeText(bitmap)
            ReceiptOcrParser.parse(text)
        } finally {
            ocrManager.close()
        }
    }
}
