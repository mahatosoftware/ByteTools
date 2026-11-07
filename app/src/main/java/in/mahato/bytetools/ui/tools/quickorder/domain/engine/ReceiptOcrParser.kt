package `in`.mahato.bytetools.ui.tools.quickorder.domain.engine

import java.util.regex.Pattern
import kotlin.math.roundToLong

data class ParsedReceiptItem(
    val productName: String,
    val quantity: Double,
    val unit: String = "pcs",
    val unitPricePaise: Long,
    val totalPricePaise: Long
)

data class ParsedReceipt(
    val items: List<ParsedReceiptItem>,
    val subtotalPaise: Long? = null,
    val discountPaise: Long? = null,
    val taxPaise: Long? = null,
    val additionalChargesPaise: Long? = null,
    val grandTotalPaise: Long? = null,
    val rawText: String
)

object ReceiptOcrParser {

    private val numberPattern = Pattern.compile("[-+]?\\d+(?:[.,]\\d+)?")

    fun parse(ocrText: String): ParsedReceipt {
        val lines = ocrText.lines().map { it.trim() }.filter { it.isNotBlank() }
        val items = mutableListOf<ParsedReceiptItem>()

        var subtotalPaise: Long? = null
        var discountPaise: Long? = null
        var taxPaise: Long? = null
        var additionalChargesPaise: Long? = null
        var grandTotalPaise: Long? = null

        for (line in lines) {
            val lower = line.lowercase()

            // Check for summary indicators
            if (lower.contains("subtotal") || lower.contains("sub total") || lower.contains("items total")) {
                extractLastAmount(line)?.let { subtotalPaise = it }
                continue
            }
            if (lower.contains("discount") || lower.contains("disc") || lower.contains("rebate") || lower.contains("less")) {
                extractLastAmount(line)?.let { discountPaise = it }
                continue
            }
            if (lower.contains("tax") || lower.contains("gst") || lower.contains("vat") || lower.contains("cgst") || lower.contains("sgst")) {
                extractLastAmount(line)?.let { taxPaise = (taxPaise ?: 0L) + it }
                continue
            }
            if (lower.contains("delivery") || lower.contains("packing") || lower.contains("packaging") || lower.contains("shipping") || lower.contains("service charge")) {
                extractLastAmount(line)?.let { additionalChargesPaise = (additionalChargesPaise ?: 0L) + it }
                continue
            }
            if (lower.contains("grand total") || lower.contains("net total") || lower.contains("net amount") || lower.contains("total amount") || lower.startsWith("total")) {
                extractLastAmount(line)?.let { grandTotalPaise = it }
                continue
            }

            // Attempt to parse line item
            val item = parseLineItem(line)
            if (item != null) {
                items.add(item)
            }
        }

        return ParsedReceipt(
            items = items,
            subtotalPaise = subtotalPaise ?: items.sumOf { it.totalPricePaise }.takeIf { it > 0L },
            discountPaise = discountPaise,
            taxPaise = taxPaise,
            additionalChargesPaise = additionalChargesPaise,
            grandTotalPaise = grandTotalPaise,
            rawText = ocrText
        )
    }

    private fun extractLastAmount(line: String): Long? {
        val matcher = numberPattern.matcher(line)
        var lastNum: String? = null
        while (matcher.find()) {
            lastNum = matcher.group()
        }
        return lastNum?.let { parseNumberToPaise(it) }
    }

    private fun parseNumberToPaise(str: String): Long? {
        val cleaned = str.replace(",", ".").replace("[^0-9.]".toRegex(), "")
        val doubleVal = cleaned.toDoubleOrNull() ?: return null
        return (doubleVal * 100).roundToLong()
    }

    private fun parseLineItem(line: String): ParsedReceiptItem? {
        // Find all numbers in the line
        val matcher = numberPattern.matcher(line)
        val numbers = mutableListOf<String>()
        val startIndices = mutableListOf<Int>()
        val endIndices = mutableListOf<Int>()

        while (matcher.find()) {
            numbers.add(matcher.group())
            startIndices.add(matcher.start())
            endIndices.add(matcher.end())
        }

        if (numbers.isEmpty()) return null

        // If we have at least 2 numbers (e.g. "Flower Pots 3 200" or "Rocket 2 300 600")
        if (numbers.size >= 2) {
            val nameEnd = startIndices[0]
            var rawName = line.substring(0, nameEnd).trim()
            // Clean leading numbering like "1. ", "2) "
            rawName = rawName.replace(Regex("^\\d+([.)\\-]\\s*)+"), "").trim()
            if (rawName.isBlank() || rawName.length < 2) return null

            val num1 = numbers[0].replace(",", ".").toDoubleOrNull() ?: 1.0
            val num2 = numbers[1].replace(",", ".").toDoubleOrNull() ?: 0.0

            val (qty, unitPricePaise, totalPaise) = if (numbers.size >= 3) {
                val num3 = numbers[2].replace(",", ".").toDoubleOrNull() ?: (num1 * num2)
                Triple(num1, (num2 * 100).roundToLong(), (num3 * 100).roundToLong())
            } else {
                Triple(num1, (num2 * 100).roundToLong(), (num1 * num2 * 100).roundToLong())
            }

            // Detect unit in rawName or line
            val unit = when {
                line.contains("kg", ignoreCase = true) -> "kg"
                line.contains("pkt", ignoreCase = true) || line.contains("packet", ignoreCase = true) -> "packets"
                line.contains("box", ignoreCase = true) -> "boxes"
                line.contains("ltr", ignoreCase = true) || line.contains("litre", ignoreCase = true) -> "litres"
                else -> "pcs"
            }

            return ParsedReceiptItem(
                productName = rawName,
                quantity = if (qty > 0.0) qty else 1.0,
                unit = unit,
                unitPricePaise = unitPricePaise,
                totalPricePaise = totalPaise
            )
        } else if (numbers.size == 1) {
            // e.g. "Chakkar 600"
            val rawName = line.substring(0, startIndices[0]).trim().replace(Regex("^\\d+([.)\\-]\\s*)+"), "")
            if (rawName.isBlank() || rawName.length < 2) return null
            val totalPaise = parseNumberToPaise(numbers[0]) ?: return null
            return ParsedReceiptItem(
                productName = rawName,
                quantity = 1.0,
                unit = "pcs",
                unitPricePaise = totalPaise,
                totalPricePaise = totalPaise
            )
        }

        return null
    }
}
