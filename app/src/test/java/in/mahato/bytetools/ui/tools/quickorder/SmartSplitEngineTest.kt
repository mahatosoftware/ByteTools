package `in`.mahato.bytetools.ui.tools.quickorder

import `in`.mahato.bytetools.ui.tools.quickorder.domain.engine.SmartSplitEngine
import `in`.mahato.bytetools.ui.tools.quickorder.domain.model.*
import org.junit.Assert.*
import org.junit.Test

class SmartSplitEngineTest {

    // =========================================================================
    // Test 1: Basic order collection & consolidation
    // Debasish: 3 Flower Pots, 2 Chakkar
    // Amit: 2 Rockets, 4 Chakkar
    // Expected Consolidated: 3 Flower Pots, 6 Chakkar, 2 Rockets
    // =========================================================================
    @Test
    fun test1_basicOrderCollectionAndConsolidation() {
        val debasish = Participant(id = 1L, groupOrderId = 1L, name = "Debasish", isOrganizer = true)
        val amit = Participant(id = 2L, groupOrderId = 1L, name = "Amit", isOrganizer = false)
        val participants = listOf(debasish, amit)

        val orders = listOf(
            ParticipantOrderItem(id = 1L, participantId = 1L, groupOrderId = 1L, productName = "Flower Pots", quantity = 3.0, unit = "pcs"),
            ParticipantOrderItem(id = 2L, participantId = 1L, groupOrderId = 1L, productName = "Chakkar", quantity = 2.0, unit = "pcs"),
            ParticipantOrderItem(id = 3L, participantId = 2L, groupOrderId = 1L, productName = "Rocket", quantity = 2.0, unit = "pcs"),
            ParticipantOrderItem(id = 4L, participantId = 2L, groupOrderId = 1L, productName = "Chakkar", quantity = 4.0, unit = "pcs")
        )

        val consolidated = SmartSplitEngine.consolidateOrders(orders, participants)
        assertEquals(3, consolidated.size)

        val flowerPots = consolidated.first { it.normalizedProductName == "flower pots" }
        assertEquals(3.0, flowerPots.totalOrderedQuantity, 0.001)
        assertEquals(1, flowerPots.participantOrders.size)
        assertEquals(3.0, flowerPots.participantOrders[0].quantity, 0.001)

        val chakkar = consolidated.first { it.normalizedProductName == "chakkar" }
        assertEquals(6.0, chakkar.totalOrderedQuantity, 0.001)
        assertEquals(2, chakkar.participantOrders.size)
        val debasishChakkar = chakkar.participantOrders.first { it.participantId == 1L }
        val amitChakkar = chakkar.participantOrders.first { it.participantId == 2L }
        assertEquals(2.0, debasishChakkar.quantity, 0.001)
        assertEquals(4.0, amitChakkar.quantity, 0.001)

        val rocket = consolidated.first { it.normalizedProductName == "rocket" }
        assertEquals(2.0, rocket.totalOrderedQuantity, 0.001)
        assertEquals(1, rocket.participantOrders.size)
        assertEquals(2.0, rocket.participantOrders[0].quantity, 0.001)
    }

    // =========================================================================
    // Test 2: Proportional discount
    // Total bill ₹1,800. Discount ₹180.
    // Debasish: (3 * 200) + (2 * 100) = ₹800 -> Discount ₹80 -> Pays ₹720
    // Amit: (2 * 300) + (4 * 100) = ₹1,000 -> Discount ₹100 -> Pays ₹900
    // =========================================================================
    @Test
    fun test2_proportionalDiscountDistribution() {
        val debasish = Participant(id = 1L, groupOrderId = 1L, name = "Debasish", isOrganizer = true)
        val amit = Participant(id = 2L, groupOrderId = 1L, name = "Amit", isOrganizer = false)
        val participants = listOf(debasish, amit)

        val billItems = listOf(
            BillItem(id = 1L, groupOrderId = 1L, productName = "Flower Pots", quantity = 3.0, unitPricePaise = 20000L), // ₹600
            BillItem(id = 2L, groupOrderId = 1L, productName = "Chakkar", quantity = 6.0, unitPricePaise = 10000L),    // ₹600
            BillItem(id = 3L, groupOrderId = 1L, productName = "Rocket", quantity = 2.0, unitPricePaise = 30000L)      // ₹600
        )

        val allocations = listOf(
            ItemAllocation(id = 1L, groupOrderId = 1L, billItemId = 1L, participantId = 1L, allocatedQuantity = 3.0),
            ItemAllocation(id = 2L, groupOrderId = 1L, billItemId = 2L, participantId = 1L, allocatedQuantity = 2.0),
            ItemAllocation(id = 3L, groupOrderId = 1L, billItemId = 2L, participantId = 2L, allocatedQuantity = 4.0),
            ItemAllocation(id = 4L, groupOrderId = 1L, billItemId = 3L, participantId = 2L, allocatedQuantity = 2.0)
        )

        val summary = BillSummary(
            id = 1L,
            groupOrderId = 1L,
            subtotalPaise = 180000L,       // ₹1,800
            overallDiscountPaise = 18000L, // ₹180
            finalBillAmountPaise = 162000L,// ₹1,620
            discountSplitMethod = SplitMethod.PROPORTIONAL
        )

        val settlements = SmartSplitEngine.calculateSettlement(billItems, allocations, summary, participants)

        val debasishSettlement = settlements.first { it.participantId == 1L }
        val amitSettlement = settlements.first { it.participantId == 2L }

        assertEquals(80000L, debasishSettlement.originalSubtotalPaise) // ₹800
        assertEquals(8000L, debasishSettlement.overallDiscountSharePaise) // ₹80
        assertEquals(72000L, debasishSettlement.finalPayablePaise)     // ₹720

        assertEquals(100000L, amitSettlement.originalSubtotalPaise)   // ₹1,000
        assertEquals(10000L, amitSettlement.overallDiscountSharePaise) // ₹100
        assertEquals(90000L, amitSettlement.finalPayablePaise)        // ₹900

        // Total check
        assertEquals(162000L, debasishSettlement.finalPayablePaise + amitSettlement.finalPayablePaise)
    }

    // =========================================================================
    // Test 3: Actual price overrides estimated price
    // Estimated: Flower Pots ₹150, Actual ₹200. Final settlement uses ₹200.
    // =========================================================================
    @Test
    fun test3_actualPriceOverridesEstimatedPrice() {
        val user = Participant(id = 1L, groupOrderId = 1L, name = "Karan", isOrganizer = true)
        val participants = listOf(user)

        // Estimated was ₹150 (15000 paise)
        val billItems = listOf(
            BillItem(id = 1L, groupOrderId = 1L, productName = "Flower Pots", quantity = 3.0, unitPricePaise = 20000L) // Actual ₹200
        )
        val allocations = listOf(
            ItemAllocation(id = 1L, groupOrderId = 1L, billItemId = 1L, participantId = 1L, allocatedQuantity = 3.0)
        )
        val summary = BillSummary(
            id = 1L,
            groupOrderId = 1L,
            subtotalPaise = 60000L,
            overallDiscountPaise = 0L,
            finalBillAmountPaise = 60000L
        )

        val settlements = SmartSplitEngine.calculateSettlement(billItems, allocations, summary, participants)
        assertEquals(60000L, settlements[0].finalPayablePaise)
    }

    // =========================================================================
    // Test 4: Partial quantity availability (Shortfall)
    // Ordered 10 units of Sparklers. Only 8 purchased.
    // Organizer allocates 5 to Debasish and 3 to Amit.
    // =========================================================================
    @Test
    fun test4_partialQuantityAvailability() {
        val debasish = Participant(id = 1L, groupOrderId = 1L, name = "Debasish", isOrganizer = true)
        val amit = Participant(id = 2L, groupOrderId = 1L, name = "Amit", isOrganizer = false)
        val participants = listOf(debasish, amit)

        // Ordered 10, only 8 purchased at ₹50 each (5000 paise)
        val billItems = listOf(
            BillItem(id = 1L, groupOrderId = 1L, productName = "Sparklers", quantity = 8.0, unitPricePaise = 5000L)
        )
        val allocations = listOf(
            ItemAllocation(id = 1L, groupOrderId = 1L, billItemId = 1L, participantId = 1L, allocatedQuantity = 5.0),
            ItemAllocation(id = 2L, groupOrderId = 1L, billItemId = 1L, participantId = 2L, allocatedQuantity = 3.0)
        )
        val summary = BillSummary(
            id = 1L,
            groupOrderId = 1L,
            subtotalPaise = 40000L, // 8 * 50 = ₹400
            finalBillAmountPaise = 40000L
        )

        val settlements = SmartSplitEngine.calculateSettlement(billItems, allocations, summary, participants)
        assertEquals(25000L, settlements.first { it.participantId == 1L }.finalPayablePaise) // 5 * 50 = ₹250
        assertEquals(15000L, settlements.first { it.participantId == 2L }.finalPayablePaise) // 3 * 50 = ₹150
        assertEquals(40000L, settlements.sumOf { it.finalPayablePaise })
    }

    // =========================================================================
    // Test 5: Additional product purchased outside original order
    // Add "Sweets" (₹500) assigned to Rahul.
    // =========================================================================
    @Test
    fun test5_additionalProductPurchased() {
        val debasish = Participant(id = 1L, groupOrderId = 1L, name = "Debasish")
        val rahul = Participant(id = 2L, groupOrderId = 1L, name = "Rahul")
        val participants = listOf(debasish, rahul)

        val billItems = listOf(
            BillItem(id = 1L, groupOrderId = 1L, productName = "Crackers", quantity = 1.0, unitPricePaise = 100000L),
            BillItem(id = 2L, groupOrderId = 1L, productName = "Sweets", quantity = 1.0, unitPricePaise = 50000L)
        )
        val allocations = listOf(
            ItemAllocation(id = 1L, groupOrderId = 1L, billItemId = 1L, participantId = 1L, allocatedQuantity = 1.0),
            ItemAllocation(id = 2L, groupOrderId = 1L, billItemId = 2L, participantId = 2L, allocatedQuantity = 1.0)
        )
        val summary = BillSummary(
            id = 1L,
            groupOrderId = 1L,
            subtotalPaise = 150000L,
            finalBillAmountPaise = 150000L
        )

        val settlements = SmartSplitEngine.calculateSettlement(billItems, allocations, summary, participants)
        assertEquals(100000L, settlements.first { it.participantId == 1L }.finalPayablePaise)
        assertEquals(50000L, settlements.first { it.participantId == 2L }.finalPayablePaise)
    }

    // =========================================================================
    // Test 6: Additional charges (Packaging ₹100 distributed proportionately)
    // Debasish subtotal ₹800, Amit subtotal ₹1,000. Total ₹1,800. Packaging ₹100.
    // =========================================================================
    @Test
    fun test6_additionalChargesProportionalDistribution() {
        val debasish = Participant(id = 1L, groupOrderId = 1L, name = "Debasish")
        val amit = Participant(id = 2L, groupOrderId = 1L, name = "Amit")
        val participants = listOf(debasish, amit)

        val billItems = listOf(
            BillItem(id = 1L, groupOrderId = 1L, productName = "Item A", quantity = 1.0, unitPricePaise = 80000L),
            BillItem(id = 2L, groupOrderId = 1L, productName = "Item B", quantity = 1.0, unitPricePaise = 100000L)
        )
        val allocations = listOf(
            ItemAllocation(id = 1L, groupOrderId = 1L, billItemId = 1L, participantId = 1L, allocatedQuantity = 1.0),
            ItemAllocation(id = 2L, groupOrderId = 1L, billItemId = 2L, participantId = 2L, allocatedQuantity = 1.0)
        )
        val summary = BillSummary(
            id = 1L,
            groupOrderId = 1L,
            subtotalPaise = 180000L,
            additionalChargesPaise = 10000L, // ₹100
            finalBillAmountPaise = 190000L,   // ₹1,900
            taxChargesSplitMethod = SplitMethod.PROPORTIONAL
        )

        val settlements = SmartSplitEngine.calculateSettlement(billItems, allocations, summary, participants)
        val debasishSettlement = settlements.first { it.participantId == 1L }
        val amitSettlement = settlements.first { it.participantId == 2L }

        // ₹100 * (800 / 1800) = 44.44 -> 44 paise; Amit gets 55.55 -> 56 paise
        assertEquals(4444L, debasishSettlement.additionalChargesSharePaise)
        assertEquals(5556L, amitSettlement.additionalChargesSharePaise)
        assertEquals(10000L, debasishSettlement.additionalChargesSharePaise + amitSettlement.additionalChargesSharePaise)

        // Final payables sum must equal exactly ₹1,900
        assertEquals(190000L, debasishSettlement.finalPayablePaise + amitSettlement.finalPayablePaise)
    }

    // =========================================================================
    // Test 7: Multiple participants scalability (2, 7, 20, 50 participants)
    // Ensures exact zero paise discrepancy across any group size
    // =========================================================================
    @Test
    fun test7_multipleParticipantsScaleAndRounding() {
        for (count in listOf(2, 7, 20, 50)) {
            val participants = (1..count).map {
                Participant(id = it.toLong(), groupOrderId = 100L, name = "Member $it", isOrganizer = it == 1)
            }
            val billItems = participants.mapIndexed { idx, p ->
                BillItem(id = (idx + 1).toLong(), groupOrderId = 100L, productName = "Item $idx", quantity = 1.0, unitPricePaise = (10000L + idx * 537L))
            }
            val allocations = participants.mapIndexed { idx, _ ->
                ItemAllocation(id = (idx + 1).toLong(), groupOrderId = 100L, billItemId = (idx + 1).toLong(), participantId = participants[idx].id, allocatedQuantity = 1.0)
            }

            val subtotal = billItems.sumOf { it.totalPricePaise }
            val discount = 12345L // ₹123.45 discount
            val tax = 2345L      // ₹23.45 tax
            val finalBill = subtotal - discount + tax

            val summary = BillSummary(
                id = 1L,
                groupOrderId = 100L,
                subtotalPaise = subtotal,
                overallDiscountPaise = discount,
                taxPaise = tax,
                finalBillAmountPaise = finalBill,
                discountSplitMethod = SplitMethod.PROPORTIONAL
            )

            val settlements = SmartSplitEngine.calculateSettlement(billItems, allocations, summary, participants)

            val sumPayables = settlements.sumOf { it.finalPayablePaise }
            val sumDiscounts = settlements.sumOf { it.overallDiscountSharePaise }
            val sumTaxes = settlements.sumOf { it.taxSharePaise }

            assertEquals("Discounts must sum to total discount for $count participants", discount, sumDiscounts)
            assertEquals("Taxes must sum to total taxes for $count participants", tax, sumTaxes)
            assertEquals("Payables must equal final bill amount exactly for $count participants", finalBill, sumPayables)
        }
    }

    // =========================================================================
    // Test 8: Deterministic Rounding - Fractional paise distribution
    // 3 participants each ordering ₹100 item (total ₹300).
    // Discount is ₹10 (1000 paise). 1000 / 3 = 333.333 paise.
    // The largest remainder method should give 334, 333, 333 paise, summing to 1000 paise exactly.
    // =========================================================================
    @Test
    fun test8_roundingRemainderDistribution() {
        val weights = mapOf(1L to 10000L, 2L to 10000L, 3L to 10000L)
        val distributed = SmartSplitEngine.distributePaiseProportionally(1000L, weights)

        assertEquals(1000L, distributed.values.sum())
        assertTrue(distributed.values.contains(334L))
        assertEquals(2, distributed.values.count { it == 333L })
    }

    // =========================================================================
    // Test 9: Name Variation & Fuzzy Matching
    // "Chakri" matches "Chakkar", "Flowerpot" matches "Flower Pots", "Rockets" matches "Rocket"
    // =========================================================================
    @Test
    fun test9_fuzzyNameMatching() {
        val consolidated = listOf(
            ConsolidatedItem(normalizedProductName = "chakkar", displayName = "Chakkar", unit = "pcs", totalOrderedQuantity = 6.0),
            ConsolidatedItem(normalizedProductName = "flower pots", displayName = "Flower Pots", unit = "pcs", totalOrderedQuantity = 3.0),
            ConsolidatedItem(normalizedProductName = "rocket", displayName = "Rocket", unit = "pcs", totalOrderedQuantity = 2.0)
        )

        val match1 = SmartSplitEngine.findBestMatch("Chakri", consolidated)
        assertNotNull(match1)
        assertEquals("chakkar", match1?.normalizedProductName)

        val match2 = SmartSplitEngine.findBestMatch("Flowerpot", consolidated)
        assertNotNull(match2)
        assertEquals("flower pots", match2?.normalizedProductName)

        val match3 = SmartSplitEngine.findBestMatch("Rockets", consolidated)
        assertNotNull(match3)
        assertEquals("rocket", match3?.normalizedProductName)
    }

    // =========================================================================
    // Test 10: Equal Discount allocation option
    // =========================================================================
    @Test
    fun test10_equalDiscountOption() {
        val p1 = Participant(id = 1L, groupOrderId = 1L, name = "P1")
        val p2 = Participant(id = 2L, groupOrderId = 1L, name = "P2")
        val participants = listOf(p1, p2)

        val billItems = listOf(
            BillItem(id = 1L, groupOrderId = 1L, productName = "A", quantity = 1.0, unitPricePaise = 50000L),
            BillItem(id = 2L, groupOrderId = 1L, productName = "B", quantity = 1.0, unitPricePaise = 150000L)
        )
        val allocations = listOf(
            ItemAllocation(id = 1L, groupOrderId = 1L, billItemId = 1L, participantId = 1L, allocatedQuantity = 1.0),
            ItemAllocation(id = 2L, groupOrderId = 1L, billItemId = 2L, participantId = 2L, allocatedQuantity = 1.0)
        )
        val summary = BillSummary(
            id = 1L,
            groupOrderId = 1L,
            subtotalPaise = 200000L,
            overallDiscountPaise = 20000L, // ₹200 discount
            finalBillAmountPaise = 180000L,
            discountSplitMethod = SplitMethod.EQUAL
        )

        val settlements = SmartSplitEngine.calculateSettlement(billItems, allocations, summary, participants)
        // With EQUAL split, both get ₹100 (10000 paise) discount regardless of original bill share
        assertEquals(10000L, settlements[0].overallDiscountSharePaise)
        assertEquals(10000L, settlements[1].overallDiscountSharePaise)
        assertEquals(40000L, settlements[0].finalPayablePaise)
        assertEquals(140000L, settlements[1].finalPayablePaise)
        assertEquals(180000L, settlements.sumOf { it.finalPayablePaise })
    }

    // =========================================================================
    // Test 11: Bill line total split across purchased quantity
    // e.g. 6 Chakkar bought for ₹600 -> ₹100/unit
    // 3 Flower Pots bought for ₹600 -> ₹200/unit
    // =========================================================================
    @Test
    fun test11_billLineTotalSplitByQuantity() {
        val totalChakkarPaise = 60000L // ₹600
        val qtyChakkar = 6.0
        val unitPriceChakkar = (totalChakkarPaise / qtyChakkar).toLong()
        assertEquals(10000L, unitPriceChakkar) // ₹100

        val totalPotsPaise = 60000L // ₹600
        val qtyPots = 3.0
        val unitPricePots = (totalPotsPaise / qtyPots).toLong()
        assertEquals(20000L, unitPricePots) // ₹200

        val billItems = listOf(
            BillItem(id = 1L, groupOrderId = 1L, productName = "Flower Pots", quantity = qtyPots, unitPricePaise = unitPricePots, totalPricePaise = totalPotsPaise),
            BillItem(id = 2L, groupOrderId = 1L, productName = "Chakkar", quantity = qtyChakkar, unitPricePaise = unitPriceChakkar, totalPricePaise = totalChakkarPaise)
        )

        assertEquals(120000L, billItems.sumOf { (it.quantity * it.unitPricePaise).toLong() })
    }

    // =========================================================================
    // Test 12: Complex 5-participant scenario (zero-order member, long names)
    // =========================================================================
    @Test
    fun test12_fiveParticipantsWithZeroOrdersAndLongNames() {
        val p1 = Participant(id = 1L, groupOrderId = 1L, name = "Debasish", isOrganizer = true)
        val p2 = Participant(id = 2L, groupOrderId = 1L, name = "Amit", isOrganizer = false)
        val p3 = Participant(id = 3L, groupOrderId = 1L, name = "Rahul (No Orders)", isOrganizer = false)
        val p4 = Participant(id = 4L, groupOrderId = 1L, name = "SumanwithVeryLongParticipantNameForTestingLayout", isOrganizer = false)
        val p5 = Participant(id = 5L, groupOrderId = 1L, name = "Priya", isOrganizer = false)
        val participants = listOf(p1, p2, p3, p4, p5)

        val longProduct = "Special Handcrafted Multicolored Eco-Friendly Sparklers Deluxe Box"
        val billItems = listOf(
            BillItem(id = 1L, groupOrderId = 1L, productName = "Flower Pots", quantity = 3.0, unitPricePaise = 20000L), // ₹600
            BillItem(id = 2L, groupOrderId = 1L, productName = "Chakkar", quantity = 6.0, unitPricePaise = 10000L),    // ₹600
            BillItem(id = 3L, groupOrderId = 1L, productName = "Rocket", quantity = 2.0, unitPricePaise = 30000L),     // ₹600
            BillItem(id = 4L, groupOrderId = 1L, productName = longProduct, quantity = 1.0, unitPricePaise = 50000L)   // ₹500
        )

        val allocations = listOf(
            // Debasish: 3 Flower Pots, 2 Chakkar
            ItemAllocation(id = 1L, groupOrderId = 1L, billItemId = 1L, participantId = 1L, allocatedQuantity = 3.0),
            ItemAllocation(id = 2L, groupOrderId = 1L, billItemId = 2L, participantId = 1L, allocatedQuantity = 2.0),
            // Amit: 4 Chakkar, 1 Rocket
            ItemAllocation(id = 3L, groupOrderId = 1L, billItemId = 2L, participantId = 2L, allocatedQuantity = 4.0),
            ItemAllocation(id = 4L, groupOrderId = 1L, billItemId = 3L, participantId = 2L, allocatedQuantity = 1.0),
            // Rahul: ZERO allocations
            // Suman: 1 Long Product
            ItemAllocation(id = 5L, groupOrderId = 1L, billItemId = 4L, participantId = 4L, allocatedQuantity = 1.0),
            // Priya: 1 Rocket
            ItemAllocation(id = 6L, groupOrderId = 1L, billItemId = 3L, participantId = 5L, allocatedQuantity = 1.0)
        )

        val summary = BillSummary(
            id = 1L,
            groupOrderId = 1L,
            subtotalPaise = 230000L, // ₹2,300
            overallDiscountPaise = 23000L, // 10% discount = ₹230
            finalBillAmountPaise = 207000L, // ₹2,070
            discountSplitMethod = SplitMethod.PROPORTIONAL
        )

        val settlements = SmartSplitEngine.calculateSettlement(billItems, allocations, summary, participants)
        assertEquals(5, settlements.size)

        // Rahul has 0 orders, 0 payable
        val rahulSettlement = settlements.first { it.participantId == 3L }
        assertEquals(0L, rahulSettlement.originalSubtotalPaise)
        assertEquals(0L, rahulSettlement.finalPayablePaise)
        assertEquals(0, rahulSettlement.items.size)
        assertEquals(PaymentStatus.PAID, rahulSettlement.paymentStatus)

        // Debasish subtotal = 3 * 200 + 2 * 100 = ₹800 (80000 paise)
        val debasishSettlement = settlements.first { it.participantId == 1L }
        assertEquals(80000L, debasishSettlement.originalSubtotalPaise)
        assertEquals(2, debasishSettlement.items.size)

        // Suman subtotal = 1 * 500 = ₹500 (50000 paise)
        val sumanSettlement = settlements.first { it.participantId == 4L }
        assertEquals(50000L, sumanSettlement.originalSubtotalPaise)
        assertEquals(1, sumanSettlement.items.size)
        assertEquals(longProduct, sumanSettlement.items[0].productName)

        // Total payable across all 5 participants must match final bill exactly!
        val sumPayable = settlements.sumOf { it.finalPayablePaise }
        assertEquals(207000L, sumPayable)
    }

    // =========================================================================
    // Test 13: Multiple Payers Upfront for Purchase & Net Settlement Balances
    // Bill ₹3,000 total.
    // Alice owes ₹1,000, Bob owes ₹1,000, Charlie owes ₹1,000.
    // Upfront bill payment: Alice paid ₹500, Bob paid ₹2,500, Charlie paid ₹0.
    // Net Balances:
    // Alice: paid ₹500 - owes ₹1,000 = -₹500 (Owes ₹500)
    // Bob: paid ₹2,500 - owes ₹1,000 = +₹1,500 (Gets back ₹1,500)
    // Charlie: paid ₹0 - owes ₹1,000 = -₹1,000 (Owes ₹1,000)
    // Sum of net balances must be exactly 0 (Conservation of money).
    // =========================================================================
    @Test
    fun test13_multiplePayersUpfrontAndNetSettlementBalances() {
        val alice = Participant(id = 1L, groupOrderId = 1L, name = "Alice", isOrganizer = true)
        val bob = Participant(id = 2L, groupOrderId = 1L, name = "Bob", isOrganizer = false)
        val charlie = Participant(id = 3L, groupOrderId = 1L, name = "Charlie", isOrganizer = false)
        val participants = listOf(alice, bob, charlie)

        val billItems = listOf(
            BillItem(id = 1L, groupOrderId = 1L, productName = "Item A", quantity = 1.0, unitPricePaise = 100000L), // ₹1,000
            BillItem(id = 2L, groupOrderId = 1L, productName = "Item B", quantity = 1.0, unitPricePaise = 100000L), // ₹1,000
            BillItem(id = 3L, groupOrderId = 1L, productName = "Item C", quantity = 1.0, unitPricePaise = 100000L)  // ₹1,000
        )

        val allocations = listOf(
            ItemAllocation(id = 1L, groupOrderId = 1L, billItemId = 1L, participantId = 1L, allocatedQuantity = 1.0),
            ItemAllocation(id = 2L, groupOrderId = 1L, billItemId = 2L, participantId = 2L, allocatedQuantity = 1.0),
            ItemAllocation(id = 3L, groupOrderId = 1L, billItemId = 3L, participantId = 3L, allocatedQuantity = 1.0)
        )

        val summary = BillSummary(
            id = 1L,
            groupOrderId = 1L,
            subtotalPaise = 300000L,
            overallDiscountPaise = 0L,
            finalBillAmountPaise = 300000L
        )

        val purchasePayments = listOf(
            PurchasePayment(id = 1L, groupOrderId = 1L, participantId = 1L, participantName = "Alice", amountPaise = 50000L, paymentMethod = "UPI"),
            PurchasePayment(id = 2L, groupOrderId = 1L, participantId = 2L, participantName = "Bob", amountPaise = 250000L, paymentMethod = "Card")
        )

        val settlements = SmartSplitEngine.calculateSettlement(
            billItems = billItems,
            allocations = allocations,
            billSummary = summary,
            participants = participants,
            payments = emptyList(),
            purchasePayments = purchasePayments
        )

        val aliceS = settlements.first { it.participantId == 1L }
        val bobS = settlements.first { it.participantId == 2L }
        val charlieS = settlements.first { it.participantId == 3L }

        // Final payables (Cost shares)
        assertEquals(100000L, aliceS.finalPayablePaise)
        assertEquals(100000L, bobS.finalPayablePaise)
        assertEquals(100000L, charlieS.finalPayablePaise)

        // Paid upfront for purchase
        assertEquals(50000L, aliceS.paidForPurchasePaise)
        assertEquals(250000L, bobS.paidForPurchasePaise)
        assertEquals(0L, charlieS.paidForPurchasePaise)

        // Net settlement balances
        assertEquals(-50000L, aliceS.netBalancePaise)  // Alice owes ₹500
        assertEquals(150000L, bobS.netBalancePaise)    // Bob gets back ₹1,500
        assertEquals(-100000L, charlieS.netBalancePaise)// Charlie owes ₹1,000

        // Conservation of money: Sum of net balances = 0
        assertEquals(0L, settlements.sumOf { it.netBalancePaise })

        // Initial outstanding balances
        assertEquals(50000L, aliceS.outstandingBalancePaise)
        assertEquals(150000L, bobS.outstandingBalancePaise)
        assertEquals(100000L, charlieS.outstandingBalancePaise)
    }

    // =========================================================================
    // Test 14: Debt Simplification Settlement Transfers
    // Charlie owes ₹1,000, Alice owes ₹500 -> Bob gets back ₹1,500.
    // Minimum transfers:
    // Transfer 1: Charlie pays Bob ₹1,000
    // Transfer 2: Alice pays Bob ₹500
    // Total transactions = 2.
    // =========================================================================
    @Test
    fun test14_settlementTransfersDebtSimplification() {
        val alice = Participant(id = 1L, groupOrderId = 1L, name = "Alice", isOrganizer = true)
        val bob = Participant(id = 2L, groupOrderId = 1L, name = "Bob", isOrganizer = false)
        val charlie = Participant(id = 3L, groupOrderId = 1L, name = "Charlie", isOrganizer = false)
        val participants = listOf(alice, bob, charlie)

        val billItems = listOf(
            BillItem(id = 1L, groupOrderId = 1L, productName = "Item A", quantity = 1.0, unitPricePaise = 100000L),
            BillItem(id = 2L, groupOrderId = 1L, productName = "Item B", quantity = 1.0, unitPricePaise = 100000L),
            BillItem(id = 3L, groupOrderId = 1L, productName = "Item C", quantity = 1.0, unitPricePaise = 100000L)
        )

        val allocations = listOf(
            ItemAllocation(id = 1L, groupOrderId = 1L, billItemId = 1L, participantId = 1L, allocatedQuantity = 1.0),
            ItemAllocation(id = 2L, groupOrderId = 1L, billItemId = 2L, participantId = 2L, allocatedQuantity = 1.0),
            ItemAllocation(id = 3L, groupOrderId = 1L, billItemId = 3L, participantId = 3L, allocatedQuantity = 1.0)
        )

        val summary = BillSummary(id = 1L, groupOrderId = 1L, subtotalPaise = 300000L, overallDiscountPaise = 0L, finalBillAmountPaise = 300000L)
        val purchasePayments = listOf(
            PurchasePayment(id = 1L, groupOrderId = 1L, participantId = 1L, participantName = "Alice", amountPaise = 50000L, paymentMethod = "UPI"),
            PurchasePayment(id = 2L, groupOrderId = 1L, participantId = 2L, participantName = "Bob", amountPaise = 250000L, paymentMethod = "Card")
        )

        val settlements = SmartSplitEngine.calculateSettlement(billItems, allocations, summary, participants, emptyList(), purchasePayments)
        val transfers = SmartSplitEngine.calculateSettlementTransfers(settlements, emptyList())

        assertEquals(2, transfers.size)

        // Charlie (largest debtor: ₹1,000) pays Bob
        val t1 = transfers[0]
        assertEquals(3L, t1.fromParticipantId)
        assertEquals("Charlie", t1.fromParticipantName)
        assertEquals(2L, t1.toParticipantId)
        assertEquals("Bob", t1.toParticipantName)
        assertEquals(100000L, t1.amountPaise)
        assertEquals(0L, t1.settledPaise)
        assertEquals(100000L, t1.remainingPaise)
        assertFalse(t1.isFullySettled)

        // Alice (debtor: ₹500) pays Bob
        val t2 = transfers[1]
        assertEquals(1L, t2.fromParticipantId)
        assertEquals("Alice", t2.fromParticipantName)
        assertEquals(2L, t2.toParticipantId)
        assertEquals("Bob", t2.toParticipantName)
        assertEquals(50000L, t2.amountPaise)
        assertEquals(0L, t2.settledPaise)
        assertEquals(50000L, t2.remainingPaise)
        assertFalse(t2.isFullySettled)
    }

    // =========================================================================
    // Test 15: Settlement Payments Tracking and Partial / Full Settlement
    // Alice records a settlement payment of ₹500 directly to Bob.
    // Transfer from Alice to Bob should become fully settled.
    // Charlie's transfer to Bob remains pending.
    // =========================================================================
    @Test
    fun test15_settlementPaymentsDirectlySettleTransfers() {
        val alice = Participant(id = 1L, groupOrderId = 1L, name = "Alice", isOrganizer = true)
        val bob = Participant(id = 2L, groupOrderId = 1L, name = "Bob", isOrganizer = false)
        val charlie = Participant(id = 3L, groupOrderId = 1L, name = "Charlie", isOrganizer = false)
        val participants = listOf(alice, bob, charlie)

        val billItems = listOf(
            BillItem(id = 1L, groupOrderId = 1L, productName = "Item A", quantity = 1.0, unitPricePaise = 100000L),
            BillItem(id = 2L, groupOrderId = 1L, productName = "Item B", quantity = 1.0, unitPricePaise = 100000L),
            BillItem(id = 3L, groupOrderId = 1L, productName = "Item C", quantity = 1.0, unitPricePaise = 100000L)
        )

        val allocations = listOf(
            ItemAllocation(id = 1L, groupOrderId = 1L, billItemId = 1L, participantId = 1L, allocatedQuantity = 1.0),
            ItemAllocation(id = 2L, groupOrderId = 1L, billItemId = 2L, participantId = 2L, allocatedQuantity = 1.0),
            ItemAllocation(id = 3L, groupOrderId = 1L, billItemId = 3L, participantId = 3L, allocatedQuantity = 1.0)
        )

        val summary = BillSummary(id = 1L, groupOrderId = 1L, subtotalPaise = 300000L, overallDiscountPaise = 0L, finalBillAmountPaise = 300000L)
        val purchasePayments = listOf(
            PurchasePayment(id = 1L, groupOrderId = 1L, participantId = 1L, participantName = "Alice", amountPaise = 50000L, paymentMethod = "UPI"),
            PurchasePayment(id = 2L, groupOrderId = 1L, participantId = 2L, participantName = "Bob", amountPaise = 250000L, paymentMethod = "Card")
        )

        // Alice pays Bob ₹500 via UPI
        val payments = listOf(
            PaymentRecord(
                id = 1L,
                groupOrderId = 1L,
                participantId = 1L,
                toParticipantId = 2L,
                amountPaise = 50000L,
                paymentMethod = "UPI"
            )
        )

        val settlements = SmartSplitEngine.calculateSettlement(billItems, allocations, summary, participants, payments, purchasePayments)
        val transfers = SmartSplitEngine.calculateSettlementTransfers(settlements, payments)

        val aliceS = settlements.first { it.participantId == 1L }
        val bobS = settlements.first { it.participantId == 2L }

        // Alice is now fully paid and has 0 outstanding balance
        assertEquals(50000L, aliceS.amountPaidPaise)
        assertEquals(0L, aliceS.outstandingBalancePaise)
        assertEquals(PaymentStatus.PAID, aliceS.paymentStatus)

        // Bob has received ₹500, outstanding balance to receive is now ₹1,000 (from Charlie)
        assertEquals(50000L, bobS.amountReceivedPaise)
        assertEquals(100000L, bobS.outstandingBalancePaise)
        assertEquals(PaymentStatus.PARTIALLY_PAID, bobS.paymentStatus)

        // Transfers check
        val aliceTransfer = transfers.first { it.fromParticipantId == 1L }
        assertEquals(50000L, aliceTransfer.settledPaise)
        assertEquals(0L, aliceTransfer.remainingPaise)
        assertTrue(aliceTransfer.isFullySettled)

        val charlieTransfer = transfers.first { it.fromParticipantId == 3L }
        assertEquals(0L, charlieTransfer.settledPaise)
        assertEquals(100000L, charlieTransfer.remainingPaise)
        assertFalse(charlieTransfer.isFullySettled)
    }

    // =========================================================================
    // Test 16: Backward Compatibility – Organizer Paid 100% when purchasePayments empty
    // If purchasePayments is omitted or empty, organizer is credited with 100% of bill.
    // =========================================================================
    @Test
    fun test16_backwardCompatibilityOrganizerPaid100Percent() {
        val alice = Participant(id = 1L, groupOrderId = 1L, name = "Alice", isOrganizer = true)
        val bob = Participant(id = 2L, groupOrderId = 1L, name = "Bob", isOrganizer = false)
        val participants = listOf(alice, bob)

        val billItems = listOf(
            BillItem(id = 1L, groupOrderId = 1L, productName = "Item A", quantity = 1.0, unitPricePaise = 100000L), // ₹1,000
            BillItem(id = 2L, groupOrderId = 1L, productName = "Item B", quantity = 1.0, unitPricePaise = 100000L)  // ₹1,000
        )

        val allocations = listOf(
            ItemAllocation(id = 1L, groupOrderId = 1L, billItemId = 1L, participantId = 1L, allocatedQuantity = 1.0),
            ItemAllocation(id = 2L, groupOrderId = 1L, billItemId = 2L, participantId = 2L, allocatedQuantity = 1.0)
        )

        val summary = BillSummary(id = 1L, groupOrderId = 1L, subtotalPaise = 200000L, overallDiscountPaise = 0L, finalBillAmountPaise = 200000L)

        // Omit purchasePayments -> defaults to emptyList()
        val settlements = SmartSplitEngine.calculateSettlement(billItems, allocations, summary, participants)

        val aliceS = settlements.first { it.participantId == 1L }
        val bobS = settlements.first { it.participantId == 2L }

        // Organizer paid full ₹2,000
        assertEquals(200000L, aliceS.paidForPurchasePaise)
        assertEquals(100000L, aliceS.finalPayablePaise)
        assertEquals(100000L, aliceS.netBalancePaise) // Gets back ₹1,000

        // Bob owes ₹1,000
        assertEquals(0L, bobS.paidForPurchasePaise)
        assertEquals(100000L, bobS.finalPayablePaise)
        assertEquals(-100000L, bobS.netBalancePaise) // Owes ₹1,000

        val transfers = SmartSplitEngine.calculateSettlementTransfers(settlements)
        assertEquals(1, transfers.size)
        assertEquals(2L, transfers[0].fromParticipantId) // Bob
        assertEquals(1L, transfers[0].toParticipantId)   // Alice
        assertEquals(100000L, transfers[0].amountPaise)
    }
}
