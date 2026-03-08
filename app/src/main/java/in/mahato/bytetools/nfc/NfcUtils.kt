package `in`.mahato.bytetools.nfc

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import java.nio.charset.Charset

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
            
            return "Type: $type\nSize: $maxSize Bytes\nWritable: $isWritable"
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
            val gpoResponse = isoDep.transceive(gpoCommand)
            if (gpoResponse == null || !isSuccess(gpoResponse)) return "Payment Card detected, but reading is blocked/encrypted."
            
            val afl = extractAfl(gpoResponse) ?: return "Failed to extract Application File Locator."
            
            val sb = StringBuilder()
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
                        val track2 = extractTrack2(recResponse)
                        if (track2 != null) {
                            val parts = track2.split(Regex("[D=]"))
                            if (parts.size >= 2) {
                                val pan = parts[0]
                                val expiry = parts[1].take(4)
                                if (expiry.length == 4) {
                                    val formattedPan = pan.chunked(4).joinToString(" ")
                                    sb.append("Card Number:\n$formattedPan\n\n")
                                    sb.append("Expiry Date: ${expiry.substring(2)}/${expiry.substring(0, 2)}\n")
                                    return sb.toString()
                                }
                            }
                        }
                    }
                }
            }
            return "Payment Card detected. Track 2 data is fully encrypted or hidden."

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

    private fun bytesToHex(bytes: ByteArray): String {
        val sb = java.lang.StringBuilder()
        for (b in bytes) {
            sb.append(String.format("%02X", b))
        }
        return sb.toString()
    }
}
