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
}
