package `in`.mahato.bytetools.nfc

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import java.nio.charset.Charset

enum class RecordType {
    TEXT, URI, VCARD, WIFI, APP_LAUNCH, MIME, AUTOMATION, UNKNOWN, EMPTY
}

data class ParsedNdefRecord(
    val type: RecordType,
    val data: String
)

object NfcUtils {

    fun readTagMetadata(tag: Tag): String {
        val ndef = Ndef.get(tag)
        if (ndef == null) {
            val formatable = NdefFormatable.get(tag)
            return if (formatable != null) {
                "Tag is unformatted, but can be formatted for NDEF.\nWritable: ${true}\nSize: Unknown"
            } else {
                "Tag is not NDEF formatted and cannot be formatted as NDEF."
            }
        }
        
        try {
            ndef.connect()
            val type = ndef.type ?: "Unknown"
            val maxSize = ndef.maxSize
            val isWritable = ndef.isWritable
            
            val usedSize = try {
                ndef.ndefMessage?.toByteArray()?.size ?: ndef.cachedNdefMessage?.toByteArray()?.size ?: 0
            } catch (e: Exception) {
                ndef.cachedNdefMessage?.toByteArray()?.size ?: 0
            }
            
            return "Type: $type\nSize: $usedSize / $maxSize Bytes\nWritable: $isWritable"
        } catch (e: Exception) {
            return "Error reading metadata: ${e.message}"
        } finally {
            try { ndef.close() } catch (_: Exception) {}
        }
    }

    fun readNdefMessage(tag: Tag): String? {
        val ndef = Ndef.get(tag)
        if (ndef == null) {
            val formatable = NdefFormatable.get(tag)
            return if (formatable != null) "Tag is empty/unformatted." else "NFC tag is unformatted or unsupported."
        }
        try {
            ndef.connect()
            val ndefMessage = ndef.cachedNdefMessage ?: return "Tag is empty."
            val records = ndefMessage.records
            if (records.isNullOrEmpty()) return "No records found."
            
            val sb = java.lang.StringBuilder()
            for ((index, record) in records.withIndex()) {
                sb.append("Record ${index + 1}:\n").append(parseRecord(record)).append("\n\n")
            }
            return sb.toString().trim()
        } catch (e: Exception) {
            return "Error reading tag records: ${e.message}"
        } finally {
            try { ndef.close() } catch (_: Exception) {}
        }
    }

    fun getParsedNdefRecords(tag: Tag): List<ParsedNdefRecord> {
        val ndef = Ndef.get(tag) ?: return emptyList()
        try {
            ndef.connect()
            val ndefMessage = ndef.cachedNdefMessage ?: return emptyList()
            val records = ndefMessage.records ?: return emptyList()
            
            return records.map { parseToRecord(it) }
        } catch (e: Exception) {
            return emptyList()
        } finally {
            try { ndef.close() } catch (_: Exception) {}
        }
    }

    private fun parseToRecord(record: NdefRecord): ParsedNdefRecord {
        val payload = record.payload
        if (payload == null || payload.isEmpty()) return ParsedNdefRecord(RecordType.EMPTY, "Empty Record")
        
        when (record.tnf) {
            NdefRecord.TNF_WELL_KNOWN -> {
                if (record.type.contentEquals(NdefRecord.RTD_TEXT)) {
                    val languageCodeLength = (payload[0].toInt() and 0x3F)
                    val text = String(payload, languageCodeLength + 1, payload.size - languageCodeLength - 1, Charset.forName("UTF-8"))
                    if (text.startsWith("AUTO_TASK:")) {
                        return ParsedNdefRecord(RecordType.AUTOMATION, text.removePrefix("AUTO_TASK:").trim())
                    }
                    return ParsedNdefRecord(RecordType.TEXT, text)
                } else if (record.type.contentEquals(NdefRecord.RTD_URI)) {
                    val uriPrefixes = arrayOf(
                        "", "http://www.", "https://www.", "http://", "https://", "tel:", "mailto:",
                        "ftp://anonymous:anonymous@", "ftp://ftp.", "ftps://", "sftp://", "smb://",
                        "nfs://", "ftp://", "dav://", "news:", "telnet://", "imap:", "rtsp://", "urn:",
                        "pop:", "sip:", "sips:", "tftp:", "btspp://", "btl2cap://", "btgoep://",
                        "tcpobex://", "irdaobex://", "file://", "urn:epc:id:", "urn:epc:tag:",
                        "urn:epc:pat:", "urn:epc:raw:", "urn:epc:", "urn:nfc:"
                    )
                    val prefixIndex = payload[0].toInt()
                    val prefix = if (prefixIndex in uriPrefixes.indices) uriPrefixes[prefixIndex] else ""
                    val uri = prefix + String(payload, 1, payload.size - 1, Charset.forName("UTF-8"))
                    return ParsedNdefRecord(RecordType.URI, uri)
                }
            }
            NdefRecord.TNF_MIME_MEDIA -> {
                val mimeType = String(record.type, Charset.forName("UTF-8"))
                val content = String(payload, Charset.forName("UTF-8"))
                when {
                    mimeType.equals("text/x-vcard", ignoreCase = true) || content.startsWith("BEGIN:VCARD") -> {
                        return ParsedNdefRecord(RecordType.VCARD, content)
                    }
                    mimeType.equals("application/vnd.wfa.wsc", ignoreCase = true) || mimeType.equals("application/x-wifi-config", ignoreCase = true) -> {
                        return ParsedNdefRecord(RecordType.WIFI, content)
                    }
                    else -> {
                        if (content.startsWith("WIFI:S:")) {
                            return ParsedNdefRecord(RecordType.WIFI, content)
                        }
                        return ParsedNdefRecord(RecordType.MIME, "MIME: $mimeType\n$content")
                    }
                }
            }
            NdefRecord.TNF_ABSOLUTE_URI -> {
                 val content = String(payload, Charset.forName("UTF-8"))
                 return ParsedNdefRecord(RecordType.URI, content)
            }
            NdefRecord.TNF_EXTERNAL_TYPE -> {
                 val typeStr = String(record.type, Charset.forName("UTF-8"))
                 val content = String(payload, Charset.forName("UTF-8"))
                 if (typeStr == "android.com:pkg") {
                     return ParsedNdefRecord(RecordType.APP_LAUNCH, content)
                 }
                 return ParsedNdefRecord(RecordType.UNKNOWN, "External: $typeStr\n$content")
            }
        }
        val content = String(payload, Charset.forName("UTF-8"))
        if (content.startsWith("AUTO_TASK:")) {
            return ParsedNdefRecord(RecordType.AUTOMATION, content.removePrefix("AUTO_TASK:").trim())
        }
        if (content.startsWith("WIFI:S:")) {
            return ParsedNdefRecord(RecordType.WIFI, content)
        }
        return ParsedNdefRecord(RecordType.UNKNOWN, content)
    }

    private fun parseRecord(record: NdefRecord): String {
        val payload = record.payload
        if (payload == null || payload.isEmpty()) return "Empty Record"
        
        when (record.tnf) {
            NdefRecord.TNF_WELL_KNOWN -> {
                if (record.type.contentEquals(NdefRecord.RTD_TEXT)) {
                    val languageCodeLength = (payload[0].toInt() and 0x3F)
                    return String(payload, languageCodeLength + 1, payload.size - languageCodeLength - 1, Charset.forName("UTF-8"))
                } else if (record.type.contentEquals(NdefRecord.RTD_URI)) {
                    val uriPrefixes = arrayOf(
                        "", "http://www.", "https://www.", "http://", "https://", "tel:", "mailto:",
                        "ftp://anonymous:anonymous@", "ftp://ftp.", "ftps://", "sftp://", "smb://",
                        "nfs://", "ftp://", "dav://", "news:", "telnet://", "imap:", "rtsp://", "urn:",
                        "pop:", "sip:", "sips:", "tftp:", "btspp://", "btl2cap://", "btgoep://",
                        "tcpobex://", "irdaobex://", "file://", "urn:epc:id:", "urn:epc:tag:",
                        "urn:epc:pat:", "urn:epc:raw:", "urn:epc:", "urn:nfc:"
                    )
                    val prefixIndex = payload[0].toInt()
                    val prefix = if (prefixIndex in uriPrefixes.indices) uriPrefixes[prefixIndex] else ""
                    val uri = String(payload, 1, payload.size - 1, Charset.forName("UTF-8"))
                    return prefix + uri
                }
            }
            NdefRecord.TNF_MIME_MEDIA -> return String(payload, Charset.forName("UTF-8"))
        }
        return String(payload, Charset.forName("UTF-8"))
    }

    fun writeNdefMessage(tag: Tag, ndefMessage: NdefMessage): Boolean {
        try {
            val ndef = Ndef.get(tag)
            if (ndef != null) {
                ndef.connect()
                if (!ndef.isWritable) {
                    return false
                }
                if (ndef.maxSize < ndefMessage.toByteArray().size) {
                    return false
                }
                ndef.writeNdefMessage(ndefMessage)
                ndef.close()
                return true
            } else {
                val format = NdefFormatable.get(tag)
                if (format != null) {
                    try {
                        format.connect()
                        format.format(ndefMessage)
                        format.close()
                        return true
                    } catch (e: Exception) {
                        return false
                    }
                } else {
                    return false
                }
            }
        } catch (e: Exception) {
            return false
        }
    }

    fun createTextRecord(text: String): NdefRecord {
        val langBytes = "en".toByteArray(Charset.forName("UTF-8"))
        val textBytes = text.toByteArray(Charset.forName("UTF-8"))
        val payload = ByteArray(1 + langBytes.size + textBytes.size)
        payload[0] = langBytes.size.toByte()
        System.arraycopy(langBytes, 0, payload, 1, langBytes.size)
        System.arraycopy(textBytes, 0, payload, 1 + langBytes.size, textBytes.size)
        return NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, ByteArray(0), payload)
    }

    fun createUriRecord(uriString: String): NdefRecord {
        return NdefRecord.createUri(uriString)
    }

    fun createMimeRecord(mimeType: String, content: String): NdefRecord {
        val payload = content.toByteArray(Charset.forName("UTF-8"))
        return NdefRecord.createMime(mimeType, payload)
    }

    fun formatTag(tag: Tag): Boolean {
        try {
            val ndef = Ndef.get(tag)
            if (ndef != null) {
                ndef.connect()
                val emptyMessage = NdefMessage(arrayOf(NdefRecord(NdefRecord.TNF_EMPTY, null, null, null)))
                ndef.writeNdefMessage(emptyMessage)
                ndef.close()
                return true
            } else {
                val format = NdefFormatable.get(tag)
                if (format != null) {
                    format.connect()
                    val emptyMessage = NdefMessage(arrayOf(NdefRecord(NdefRecord.TNF_EMPTY, null, null, null)))
                    format.format(emptyMessage)
                    format.close()
                    return true
                }
            }
        } catch (_: Exception) {}
        return false
    }

    fun lockTag(tag: Tag): Boolean {
        try {
            val ndef = Ndef.get(tag) ?: return false
            ndef.connect()
            if (ndef.canMakeReadOnly()) {
                val success = ndef.makeReadOnly()
                ndef.close()
                return success
            }
            ndef.close()
        } catch (_: Exception) {}
        return false
    }

    fun readEmvCard(tag: Tag): String? {
        val isoDep = android.nfc.tech.IsoDep.get(tag) ?: return null
        try {
            isoDep.connect()
            // Select PPSE (Proximity Payment System Environment)
            val ppseCommand = byteArrayOf(
                0x00, 0xA4.toByte(), 0x04, 0x00, 0x0E,
                '2'.code.toByte(), 'P'.code.toByte(), 'A'.code.toByte(), 'Y'.code.toByte(), '.'.code.toByte(),
                'S'.code.toByte(), 'Y'.code.toByte(), 'S'.code.toByte(), '.'.code.toByte(),
                'D'.code.toByte(), 'D'.code.toByte(), 'F'.code.toByte(), '0'.code.toByte(), '1'.code.toByte(),
                0x00
            )
            val ppseResponse = isoDep.transceive(ppseCommand)
            if (ppseResponse == null || !isSuccess(ppseResponse)) {
                return null // Not an EMV payment card
            }
            
            val aid = extractAid(ppseResponse) ?: return "Payment Card detected, but no AID found."
            
            val schemeHex = bytesToHex(aid)
            val schemeName = when {
                schemeHex.startsWith("A000000003") -> "Visa"
                schemeHex.startsWith("A0000000041010") -> "Mastercard"
                schemeHex.startsWith("A0000000043060") -> "Maestro"
                schemeHex.startsWith("A000000524") -> "RuPay"
                else -> "Unknown / Other"
            }
            
            // Select AID
            val selectAidCommand = ByteArray(6 + aid.size)
            selectAidCommand[0] = 0x00
            selectAidCommand[1] = 0xA4.toByte()
            selectAidCommand[2] = 0x04
            selectAidCommand[3] = 0x00
            selectAidCommand[4] = aid.size.toByte()
            System.arraycopy(aid, 0, selectAidCommand, 5, aid.size)
            selectAidCommand[selectAidCommand.size - 1] = 0x00
            
            val aidResponse = isoDep.transceive(selectAidCommand)
            if (aidResponse == null || !isSuccess(aidResponse)) return "Failed to select payment application."
            
            // Get Processing Options
            val gpoCommand = byteArrayOf(0x80.toByte(), 0xA8.toByte(), 0x00, 0x00, 0x02, 0x83.toByte(), 0x00, 0x00)
            var gpoResponse = isoDep.transceive(gpoCommand)
            if (gpoResponse == null || !isSuccess(gpoResponse)) {
                // Try dynamic PDOL
                val pdolGpo = constructGpoCommand(aidResponse)
                if (pdolGpo != null) {
                    gpoResponse = isoDep.transceive(pdolGpo)
                }
                
                if (gpoResponse == null || !isSuccess(gpoResponse)) {
                    // Static Fallback
                    val fallbackGpo = byteArrayOf(0x80.toByte(), 0xA8.toByte(), 0x00, 0x00, 0x23, 0x83.toByte(), 0x21) + ByteArray(33) + byteArrayOf(0x00)
                    gpoResponse = isoDep.transceive(fallbackGpo)
                    if (gpoResponse == null || !isSuccess(gpoResponse)) {
                        return "Payment Card detected ($schemeName), but reading is blocked/encrypted."
                    }
                }
            }
            
            var cardPan: String? = null
            var cardExpiry: String? = null
            var cardName: String? = null
            
            // Visa often returns Track 2 directly in GPO response
            if (gpoResponse != null && isSuccess(gpoResponse)) {
                val track2 = extractTrack2(gpoResponse)
                if (track2 != null) {
                    val parts = track2.split(Regex("[D=]"))
                    if (parts.size >= 2) {
                        cardPan = parts[0]
                        cardExpiry = parts[1].take(4)
                    }
                }
                if (cardName == null) cardName = extractCardholderName(gpoResponse)
            }
            
            val afl = extractAfl(gpoResponse)
            
            if (afl != null) {
                // Read records
                for (i in afl.indices step 4) {
                    if (i + 2 >= afl.size) break
                    val sfi = afl[i].toInt() shr 3
                    val firstRecord = afl[i + 1].toInt()
                    val lastRecord = afl[i + 2].toInt()
                    
                    for (rec in firstRecord..lastRecord) {
                        val readCommand = byteArrayOf(
                            0x00, 0xB2.toByte(), rec.toByte(), ((sfi shl 3) or 4).toByte(), 0x00
                        )
                        val recResponse = isoDep.transceive(readCommand)
                        if (recResponse != null && isSuccess(recResponse)) {
                            if (cardPan == null) {
                                val track2 = extractTrack2(recResponse)
                                if (track2 != null) {
                                    val parts = track2.split(Regex("[D=]"))
                                    if (parts.size >= 2) {
                                        cardPan = parts[0]
                                        cardExpiry = parts[1].take(4)
                                    }
                                }
                            }
                            if (cardName == null) {
                                cardName = extractCardholderName(recResponse)
                            }
                        }
                    }
                }
            }
            
            
            if (cardPan != null && cardExpiry != null && cardExpiry!!.length >= 4) {
                val sb = StringBuilder()
                sb.append("Card Scheme: ").append(schemeName).append("\n\n")
                
                val maskedPan = if (cardPan!!.length >= 12) {
                    "${cardPan!!.substring(0, 4)} **** **** ${cardPan!!.takeLast(4)}"
                } else {
                    cardPan!!
                }
                
                sb.append("Card Number: \n").append(maskedPan).append("\n\n")
                if (!cardName.isNullOrBlank()) {
                    sb.append("Cardholder Name: \n").append(cardName).append("\n\n")
                }
                sb.append("Expiry Date: ").append(cardExpiry!!.substring(2, 4)).append("/").append(cardExpiry!!.substring(0, 2)).append("\n")
                return sb.toString()
            }
            
            return "Payment Card detected ($schemeName). Track 2 data is fully encrypted or hidden."

        } catch (e: Exception) {
            return "EMV Read Error: ${e.message}"
        } finally {
            try { isoDep.close() } catch (e: Exception) {}
        }
    }

    private fun isSuccess(response: ByteArray): Boolean {
        if (response.size < 2) return false
        val sw1 = response[response.size - 2].toInt() and 0xFF
        val sw2 = response[response.size - 1].toInt() and 0xFF
        return sw1 == 0x90 && sw2 == 0x00
    }

    private fun extractAid(data: ByteArray): ByteArray? {
        for (i in 0 until data.size - 2) {
            if (data[i] == 0x4F.toByte()) {
                val len = data[i + 1].toInt() and 0xFF
                if (i + 2 + len <= data.size) {
                    val aid = ByteArray(len)
                    System.arraycopy(data, i + 2, aid, 0, len)
                    return aid
                }
            }
        }
        return null
    }

    private fun extractAfl(data: ByteArray): ByteArray? {
        if (data.size > 2 && data[0] == 0x80.toByte()) {
            val len = data[1].toInt() and 0xFF
            if (2 + len <= data.size) {
                val afl = ByteArray(len)
                System.arraycopy(data, 2, afl, 0, len)
                return afl
            }
        } else if (data.size > 2 && data[0] == 0x77.toByte()) {
            for (i in 0 until data.size - 2) {
                if (data[i] == 0x94.toByte()) {
                    val len = data[i + 1].toInt() and 0xFF
                    if (i + 2 + len <= data.size) {
                        val afl = ByteArray(len)
                        System.arraycopy(data, i + 2, afl, 0, len)
                        return afl
                    }
                }
            }
        }
        return null
    }

    private fun extractTrack2(data: ByteArray): String? {
        for (i in 0 until data.size - 2) {
            if (data[i] == 0x57.toByte()) {
                val len = data[i + 1].toInt() and 0xFF
                if (i + 2 + len <= data.size) {
                    val track2Bytes = ByteArray(len)
                    System.arraycopy(data, i + 2, track2Bytes, 0, len)
                    return bytesToHex(track2Bytes).replace("F", "") // padding out
                }
            }
        }
        return null
    }

    private fun extractCardholderName(data: ByteArray): String? {
        // Find Tag 5F 20 (Cardholder Name)
        for (i in 0 until data.size - 2) {
            if (data[i] == 0x5F.toByte() && data[i + 1] == 0x20.toByte()) {
                val len = data[i + 2].toInt() and 0xFF
                if (i + 3 + len <= data.size) {
                    val nameBytes = ByteArray(len)
                    System.arraycopy(data, i + 3, nameBytes, 0, len)
                    val name = String(nameBytes, Charset.forName("UTF-8")).trim()
                    val cleaned = name.replace(Regex("[^A-Za-z0-9 /]"), "").trim()
                    if (cleaned.isNotBlank() && cleaned.replace("/", "").isNotBlank()) return cleaned.replace("/", " ").trim()
                }
            }
        }
        
        // Find Tag 9F 0B (Cardholder Name Extended)
        for (i in 0 until data.size - 2) {
            if (data[i] == 0x9F.toByte() && data[i + 1] == 0x0B.toByte()) {
                val len = data[i + 2].toInt() and 0xFF
                if (i + 3 + len <= data.size) {
                    val nameBytes = ByteArray(len)
                    System.arraycopy(data, i + 3, nameBytes, 0, len)
                    val name = String(nameBytes, Charset.forName("UTF-8")).trim()
                    val cleaned = name.replace(Regex("[^A-Za-z0-9 /]"), "").trim()
                    if (cleaned.isNotBlank() && cleaned.replace("/", "").isNotBlank()) return cleaned.replace("/", " ").trim()
                }
            }
        }

        // Find Tag 56 (Track 1 Data) which contains B[PAN]^[NAME]^[EXP]
        for (i in 0 until data.size - 1) {
            if (data[i] == 0x56.toByte()) { // Tag 56 is only 1 byte
                val len = data[i + 1].toInt() and 0xFF
                // Ensure length makes sense (usually ~30-76 bytes)
                if (i + 2 + len <= data.size) {
                    val track1Bytes = ByteArray(len)
                    System.arraycopy(data, i + 2, track1Bytes, 0, len)
                    val track1Str = String(track1Bytes, Charset.forName("UTF-8"))
                    val parts = track1Str.split("^")
                    if (parts.size >= 2) {
                        val name = parts[1].trim()
                        val cleaned = name.replace(Regex("[^A-Za-z0-9 /]"), "").trim()
                        if (cleaned.isNotBlank() && cleaned.replace("/", "").isNotBlank()) return cleaned.replace("/", " ").trim()
                    }
                }
            }
        }

        return null
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val sb = java.lang.StringBuilder()
        for (b in bytes) {
            sb.append(String.format("%02X", b))
        }
        return sb.toString()
    }

    private fun constructGpoCommand(aidResponse: ByteArray): ByteArray? {
        var pdolStart = -1
        var pdolLen = 0
        for (i in 0 until aidResponse.size - 2) {
            if (aidResponse[i] == 0x9F.toByte() && aidResponse[i + 1] == 0x38.toByte()) {
                pdolLen = aidResponse[i + 2].toInt() and 0xFF
                pdolStart = i + 3
                break
            }
        }
        if (pdolStart == -1) return null

        val pdolValue = java.io.ByteArrayOutputStream()
        var i = pdolStart
        val end = pdolStart + pdolLen

        while (i < end && i < aidResponse.size) {
            val tag1 = aidResponse[i].toInt() and 0xFF
            var tag2 = 0
            if ((tag1 and 0x1F) == 0x1F) {
                if (i + 1 < aidResponse.size) {
                    tag2 = aidResponse[i + 1].toInt() and 0xFF
                }
                i += 2
            } else {
                i += 1
            }

            if (i < aidResponse.size) {
                val len = aidResponse[i].toInt() and 0xFF
                if (tag1 == 0x9F && tag2 == 0x66 && len == 4) { // Terminal Transaction Qualifiers (TTQ)
                    pdolValue.write(byteArrayOf(0x36.toByte(), 0x00, 0x40.toByte(), 0x00))
                } else if (tag1 == 0x5F && tag2 == 0x2A && len == 2) { // Transaction Currency Code
                    pdolValue.write(byteArrayOf(0x08, 0x40.toByte())) // USD
                } else {
                    pdolValue.write(ByteArray(len)) // Fill with zeros
                }
                i += 1
            } else {
                break
            }
        }

        val pdolBytes = pdolValue.toByteArray()
        val cdata = ByteArray(pdolBytes.size + 2)
        cdata[0] = 0x83.toByte()
        cdata[1] = pdolBytes.size.toByte()
        System.arraycopy(pdolBytes, 0, cdata, 2, pdolBytes.size)

        val gpo = ByteArray(cdata.size + 6)
        gpo[0] = 0x80.toByte()
        gpo[1] = 0xA8.toByte()
        gpo[2] = 0x00
        gpo[3] = 0x00
        gpo[4] = cdata.size.toByte()
        System.arraycopy(cdata, 0, gpo, 5, cdata.size)
        gpo[gpo.size - 1] = 0x00
        return gpo
    }

    fun readOtherCardStats(tag: Tag): String? {
        val techList = tag.techList.asList()
        val sb = java.lang.StringBuilder()
        
        var foundType = false

        if (techList.contains(android.nfc.tech.MifareClassic::class.java.name)) {
            try {
                val mifare = android.nfc.tech.MifareClassic.get(tag)
                if (mifare != null) {
                    val type = when(mifare.type) {
                        android.nfc.tech.MifareClassic.TYPE_CLASSIC -> "Classic"
                        android.nfc.tech.MifareClassic.TYPE_PLUS -> "Plus"
                        android.nfc.tech.MifareClassic.TYPE_PRO -> "Pro"
                        else -> "Unknown"
                    }
                    sb.append("Card Family: Mifare $type\n")
                    sb.append("Size: ${mifare.size} Bytes\n")
                    sb.append("Details: Sectors: ${mifare.sectorCount}, Blocks: ${mifare.blockCount}\n")
                    sb.append("Likely Use Case: Metro/Transit Cards, Access Control, Corporate IDs\n")
                    foundType = true
                }
            } catch (e: Exception) {}
        }
        
        if (!foundType && techList.contains(android.nfc.tech.MifareUltralight::class.java.name)) {
            try {
                val mu = android.nfc.tech.MifareUltralight.get(tag)
                if (mu != null) {
                    val type = when(mu.type) {
                        android.nfc.tech.MifareUltralight.TYPE_ULTRALIGHT -> "Ultralight"
                        android.nfc.tech.MifareUltralight.TYPE_ULTRALIGHT_C -> "Ultralight C"
                        else -> "Unknown"
                    }
                    sb.append("Card Family: Mifare $type\n")
                    sb.append("Likely Use Case: Disposable Transit Tickets, Event Passes, Hotel Keys\n")
                    foundType = true
                }
            } catch (e: Exception) {}
        }

        if (!foundType && techList.contains(android.nfc.tech.IsoDep::class.java.name)) {
             sb.append("Card Family: ISO-DEP / ISO 14443-4\n")
             sb.append("Likely Use Case: Modern Metro/Transit Cards (e.g. Mifare DESFire), Identity Cards, Passports\n")
             foundType = true
        }

        if (!foundType && techList.contains(android.nfc.tech.NfcF::class.java.name)) {
             sb.append("Card Family: FeliCa (NfcF)\n")
             sb.append("Likely Use Case: Transport and E-money (Common in Asia/Japan)\n")
             foundType = true
        }
        
        if (!foundType && techList.contains(android.nfc.tech.NfcV::class.java.name)) {
             sb.append("Card Family: ISO 15693 (NfcV)\n")
             sb.append("Likely Use Case: Ski passes, Library Tags, Vicinity Cards\n")
             foundType = true
        }

        return if (foundType) sb.toString() else null
    }
}
