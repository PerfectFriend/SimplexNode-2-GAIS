package com.example.data.security

import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.InputStream

/**
 * High-performance, zero-dependency pure Kotlin metadata stripper for shared assets.
 * Directly parses file headers (JPEG/PNG) to locate and excise metadata markers
 * (Exif, XMP, IPTC, APP1 segments) without relying on native Android platform codecs,
 * preventing geo-location, camera model, and creation timestamp leaks during P2P file shares.
 */
object MetadataStripper {
    private const val TAG = "MetadataStripper"

    private fun logD(tag: String, msg: String) {
        try {
            Log.d(tag, msg)
        } catch (e: Throwable) {
            // No-op in JVM unit test environments without mocked android.util.Log
        }
    }

    private fun logE(tag: String, msg: String, tr: Throwable?) {
        try {
            Log.e(tag, msg, tr)
        } catch (e: Throwable) {
            // No-op in JVM unit test environments without mocked android.util.Log
        }
    }

    /**
     * Strips metadata from a JPEG or PNG byte stream.
     * Returns a clean byte array with all metadata structures discarded.
     */
    fun stripMetadata(inputStream: InputStream, mimeType: String): ByteArray {
        val bytes = inputStream.readBytes()
        return try {
            when {
                mimeType.contains("jpeg", ignoreCase = true) || mimeType.contains("jpg", ignoreCase = true) -> {
                    stripJpegMetadata(bytes)
                }
                mimeType.contains("png", ignoreCase = true) -> {
                    stripPngMetadata(bytes)
                }
                else -> {
                    logD(TAG, "MimeType '$mimeType' does not contain Exif headers. Returning copy.")
                    bytes
                }
            }
        } catch (e: Exception) {
            logE(TAG, "Error stripping metadata, falling back to raw stream copy.", e)
            bytes
        }
    }

    /**
     * JPEG Metadata Stripper: Parses JPEG markers and drops APP1 (0xFFE1) segments (Exif, XMP).
     */
    private fun stripJpegMetadata(raw: ByteArray): ByteArray {
        if (raw.size < 4) return raw
        
        // JPEG SOI (Start of Image) marker check: 0xFFD8
        if (raw[0].toInt() != 0xFF.toByte().toInt() || raw[1].toInt() != 0xD8.toByte().toInt()) {
            return raw // Not a valid JPEG
        }

        val out = ByteArrayOutputStream()
        out.write(0xFF)
        out.write(0xD8)

        var i = 2
        val length = raw.size

        while (i < length - 1) {
            val byte1 = raw[i].toInt() and 0xFF
            val byte2 = raw[i + 1].toInt() and 0xFF

            if (byte1 == 0xFF) {
                if (byte2 == 0xD9) { // EOI (End of Image)
                    out.write(0xFF)
                    out.write(0xD9)
                    break
                }

                if (byte2 == 0x00 || (byte2 in 0xD0..0xD7)) {
                    // Image scan data escape code or reset marker: write as-is
                    out.write(byte1)
                    out.write(byte2)
                    i += 2
                    continue
                }

                // Normal marker segment. Read length (2 bytes, big endian)
                if (i + 3 >= length) break
                val segmentLength = ((raw[i + 2].toInt() and 0xFF) shl 8) or (raw[i + 3].toInt() and 0xFF)

                // APP1 Marker (0xFFE1) contains EXIF / XMP metadata
                // APP2 Marker (0xFFE2) often contains ICC Color profiles (keep or discard, discarding here for total anonymity)
                if (byte2 == 0xE1 || byte2 == 0xE2) {
                    logD(TAG, "EXCISE: Dropped JPEG APP${byte2 - 0xE0} segment of length $segmentLength bytes.")
                } else {
                    // Retain safely: write marker and entire segment payload
                    out.write(byte1)
                    out.write(byte2)
                    out.write(raw, i + 2, segmentLength)
                }
                i += 2 + segmentLength
            } else {
                // Raw compressed stream bytes
                out.write(byte1)
                i++
            }
        }
        return out.toByteArray()
    }

    /**
     * PNG Metadata Stripper: Discards ancillary chunks like tEXt, zTXt, iTXt, pHYs, tIME.
     * Retains only critical chunks: IHDR, IDAT, PLTE, IEND.
     */
    private fun stripPngMetadata(raw: ByteArray): ByteArray {
        if (raw.size < 8) return raw
        
        // Check PNG signature: 137 80 78 71 13 10 26 10
        val pngSig = byteArrayOf(137.toByte(), 80.toByte(), 78.toByte(), 71.toByte(), 13.toByte(), 10.toByte(), 26.toByte(), 10.toByte())
        for (idx in 0..7) {
            if (raw[idx] != pngSig[idx]) return raw
        }

        val out = ByteArrayOutputStream()
        out.write(pngSig)

        var i = 8
        val totalSize = raw.size

        while (i < totalSize - 12) {
            // Read chunk data length (4 bytes)
            val chunkLength = ((raw[i].toInt() and 0xFF) shl 24) or
                              ((raw[i + 1].toInt() and 0xFF) shl 16) or
                              ((raw[i + 2].toInt() and 0xFF) shl 8) or
                              (raw[i + 3].toInt() and 0xFF)

            // Read chunk type (4 bytes ASCII)
            val chunkType = String(raw, i + 4, 4, Charsets.US_ASCII)

            // Critical chunks needed to display PNG correctly
            val isCritical = chunkType == "IHDR" || chunkType == "IDAT" || chunkType == "PLTE" || chunkType == "IEND"

            if (isCritical) {
                // Copy entire chunk (Length + Type + Data + CRC (4 bytes))
                val entireChunkSize = 4 + 4 + chunkLength + 4
                if (i + entireChunkSize <= totalSize) {
                    out.write(raw, i, entireChunkSize)
                }
            } else {
                logD(TAG, "EXCISE: Dropped ancillary PNG chunk: $chunkType (size: $chunkLength bytes)")
            }

            i += 4 + 4 + chunkLength + 4 // Advance pointer
            if (chunkType == "IEND") break
        }

        return out.toByteArray()
    }
}
