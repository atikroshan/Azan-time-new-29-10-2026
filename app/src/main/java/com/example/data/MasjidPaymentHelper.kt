package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import android.net.Uri
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.EncodeHintType
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/**
 * Dedicated helper to manage QR and UPI payment details for individual Masajid.
 * Ensures:
 * 1. Mohammadiya Masjid uses its verified QR payload:
 *    pa=7058855227@mahb, pn=MOHAMMADIYA MADARSA
 * 2. Skewed or tilted Google Drive photos are recreated as clean, perfectly upright, crisp QR bitmaps.
 * 3. Masajid without a QR code return null (no fallback to app support QR or developer UPI).
 */
object MasjidPaymentHelper {

    data class MasjidUpiDetails(
        val pa: String,
        val pn: String,
        val fullPayload: String,
        val isVerified: Boolean = true
    )

    // Mohammadiya Masjid verified details decoded directly from its Google Drive QR card
    private const val MOHAMMADIYA_ID = "100111111"
    private const val MOHAMMADIYA_VPA = "7058855227@mahb"
    private const val MOHAMMADIYA_PN = "MOHAMMADIYA MADARSA"
    private const val MOHAMMADIYA_PAYLOAD =
        "upi://pay?appid=bom_admin&tr=&mc=9400&pa=7058855227@mahb&pn=MOHAMMADIYA%20MADARSA&tn=&am=&cu=&bn=MOHAMMADIYA%20MADARSA"

    // Memory caches
    private var cachedMohammadiyaBitmap: Bitmap? = null
    private val decodedUrlCache = mutableMapOf<String, MasjidUpiDetails>()
    private val generatedBitmapCache = mutableMapOf<String, Bitmap>()

    init {
        try {
            cachedMohammadiyaBitmap = generateRecreatedQrBitmap(MOHAMMADIYA_PAYLOAD, 600)
        } catch (_: Throwable) {}
    }

    /**
     * Checks if a masjid has a valid, active QR code.
     */
    fun hasValidQr(masjid: MasjidItem?): Boolean {
        if (masjid == null) return false
        val mId = masjid.id.trim()
        val mName = masjid.name.trim()
        if (mId.equals("guest", ignoreCase = true) || mName.contains("guest", ignoreCase = true)) {
            return false
        }
        if (mId == MOHAMMADIYA_ID ||
            mName.contains("Mohammadiya", ignoreCase = true) ||
            mName.contains("Mohamadiya", ignoreCase = true) ||
            mName.contains("Mohomadiya", ignoreCase = true) ||
            masjid.qrUrl.contains("1V3IA8iNF5FJ-tT2zGMLYre5cebqbffpi")
        ) {
            return true
        }
        val qr = masjid.qrUrl.trim()
        if (qr.isBlank()) return false
        // Exclude generic placeholders or dummy entries
        return qr.startsWith("http://") || qr.startsWith("https://") || qr.startsWith("upi://") || qr.contains("pa=")
    }

    /**
     * Retrieves the UPI details (VPA, Payee Name, Payload) for the specified masjid.
     * Returns null if the masjid has no valid QR.
     */
    fun getMasjidUpiDetails(masjid: MasjidItem?): MasjidUpiDetails? {
        if (!hasValidQr(masjid) || masjid == null) return null

        val mId = masjid.id.trim()
        val mName = masjid.name.trim()

        // 1. Mohammadiya Masjid (100111111)
        if (mId == MOHAMMADIYA_ID ||
            mName.contains("Mohammadiya", ignoreCase = true) ||
            mName.contains("Mohamadiya", ignoreCase = true) ||
            mName.contains("Mohomadiya", ignoreCase = true) ||
            masjid.qrUrl.contains("1V3IA8iNF5FJ-tT2zGMLYre5cebqbffpi")
        ) {
            return MasjidUpiDetails(
                pa = MOHAMMADIYA_VPA,
                pn = MOHAMMADIYA_PN,
                fullPayload = MOHAMMADIYA_PAYLOAD,
                isVerified = true
            )
        }

        val qr = masjid.qrUrl.trim()

        // 2. Direct UPI payload string in qrUrl
        if (qr.contains("pa=")) {
            val matchPa = "pa=([^&]+)".toRegex().find(qr)?.groupValues?.getOrNull(1)
            val matchPn = "pn=([^&]+)".toRegex().find(qr)?.groupValues?.getOrNull(1)
            if (!matchPa.isNullOrBlank()) {
                val cleanPn = Uri.decode(matchPn ?: masjid.name)
                return MasjidUpiDetails(
                    pa = matchPa,
                    pn = cleanPn,
                    fullPayload = if (qr.startsWith("upi://")) qr else "upi://pay?$qr",
                    isVerified = true
                )
            }
        }

        // 3. Check memory cache for decoded image URL
        decodedUrlCache[qr]?.let { return it }

        // If not in cache, fallback to masjid name with null VPA until decoded
        return null
    }

    /**
     * Returns the model for displaying the QR code:
     * - A recreated, crisp Bitmap for Mohammadiya Masjid or any decoded UPI payload
     * - The direct image URL if not yet recreated into a bitmap
     * - null if the masjid has no QR code (triggering the Guest Masjid deactivated UI)
     */
    fun getMasjidQrModel(masjid: MasjidItem?): Any? {
        if (!hasValidQr(masjid) || masjid == null) return null

        val mId = masjid.id.trim()
        val mName = masjid.name.trim()

        // 1. Mohammadiya Masjid: return the recreated, upright, high-contrast QR Bitmap
        if (mId == MOHAMMADIYA_ID ||
            mName.contains("Mohammadiya", ignoreCase = true) ||
            mName.contains("Mohamadiya", ignoreCase = true) ||
            mName.contains("Mohomadiya", ignoreCase = true) ||
            masjid.qrUrl.contains("1V3IA8iNF5FJ-tT2zGMLYre5cebqbffpi")
        ) {
            if (cachedMohammadiyaBitmap == null) {
                cachedMohammadiyaBitmap = generateRecreatedQrBitmap(MOHAMMADIYA_PAYLOAD, 600)
            }
            if (cachedMohammadiyaBitmap != null) {
                return cachedMohammadiyaBitmap
            }
            // Fallback to direct Google Drive image URL if bitmap generation failed
            return GoogleSheetMasjidSync.extractGoogleDriveDirectUrl(
                "https://drive.google.com/file/d/1V3IA8iNF5FJ-tT2zGMLYre5cebqbffpi/view?usp=drivesdk"
            )
        }

        val qr = masjid.qrUrl.trim()

        // 2. If it's a direct UPI payload, generate and cache bitmap
        if (qr.startsWith("upi://") || qr.contains("pa=")) {
            generatedBitmapCache[qr]?.let { return it }
            val generated = generateRecreatedQrBitmap(qr, 600)
            if (generated != null) {
                generatedBitmapCache[qr] = generated
                return generated
            }
        }

        // 3. If decoded cache has a generated bitmap, return it
        val upi = decodedUrlCache[qr]
        if (upi != null) {
            generatedBitmapCache[upi.fullPayload]?.let { return it }
            val generated = generateRecreatedQrBitmap(upi.fullPayload, 600)
            if (generated != null) {
                generatedBitmapCache[upi.fullPayload] = generated
                return generated
            }
        }

        // 4. Return direct image URL for Coil to load
        return GoogleSheetMasjidSync.extractGoogleDriveDirectUrl(qr)
    }

    /**
     * Builds the exact UPI Uri for launching payment apps for the specified masjid.
     * Uses the exact verified merchant payload from the physical QR code so that
     * tapping 'Pay via UPI' matches scanning the QR code 1:1.
     * Returns null if the masjid has no valid QR code or UPI details.
     */
    fun buildMasjidUpiUri(masjid: MasjidItem?, amount: Int): Uri? {
        val details = getMasjidUpiDetails(masjid) ?: return null
        
        // If the verified full payload is already a valid upi://pay URI (e.g. Mohammadiya Masjid),
        // use that exact payload and inject amount to preserve all merchant parameters (mc, appid, bn, etc.)
        if (details.fullPayload.startsWith("upi://pay")) {
            val baseUri = details.fullPayload
                .replace("&am=[^&]*".toRegex(), "")
                .replace("\\?am=[^&]*&?".toRegex(), "?")
                .trimEnd('&', '?')
            val sep = if (baseUri.contains("?")) "&" else "?"
            val finalUriStr = if (amount > 0) "$baseUri${sep}am=$amount" else baseUri
            return Uri.parse(finalUriStr)
        }

        val amStr = if (amount > 0) "&am=$amount" else ""
        val encodedName = Uri.encode(details.pn)
        return Uri.parse("upi://pay?pa=${details.pa}&pn=$encodedName${amStr}&cu=INR")
    }

    /**
     * Recreates a pristine, perfectly upright, sharp QR code Bitmap from raw string content.
     */
    fun generateRecreatedQrBitmap(content: String, sizePx: Int = 600): Bitmap? {
        return try {
            val hints = HashMap<EncodeHintType, Any>()
            hints[EncodeHintType.CHARACTER_SET] = "UTF-8"
            hints[EncodeHintType.MARGIN] = 1
            hints[EncodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.H

            val bitMatrix = QRCodeWriter().encode(
                content,
                BarcodeFormat.QR_CODE,
                sizePx,
                sizePx,
                hints
            )
            val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            for (x in 0 until sizePx) {
                for (y in 0 until sizePx) {
                    bitmap.setPixel(
                        x,
                        y,
                        if (bitMatrix[x, y]) AndroidColor.BLACK else AndroidColor.WHITE
                    )
                }
            }
            bitmap
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * Decodes a QR code from an image URL in the background.
     */
    suspend fun decodeAndCacheFromUrl(imageUrl: String): MasjidUpiDetails? = withContext(Dispatchers.IO) {
        try {
            val directUrl = GoogleSheetMasjidSync.extractGoogleDriveDirectUrl(imageUrl)
            if (directUrl.isBlank()) return@withContext null

            val url = URL(directUrl)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "Mozilla/5.0")
            }
            val bitmap = conn.inputStream.use { stream: InputStream ->
                BitmapFactory.decodeStream(stream)
            } ?: return@withContext null

            val width = bitmap.width
            val height = bitmap.height
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
            val source = RGBLuminanceSource(width, height, pixels)
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            val result = QRCodeReader().decode(binaryBitmap)
            val payload = result.text?.trim() ?: return@withContext null

            if (payload.contains("pa=")) {
                val parsedUri = Uri.parse(if (payload.startsWith("upi://")) payload else "upi://pay?$payload")
                val pa = parsedUri.getQueryParameter("pa")
                val pn = parsedUri.getQueryParameter("pn") ?: ""
                if (!pa.isNullOrBlank()) {
                    val details = MasjidUpiDetails(
                        pa = pa,
                        pn = Uri.decode(pn),
                        fullPayload = payload,
                        isVerified = true
                    )
                    decodedUrlCache[imageUrl] = details
                    val cleanBitmap = generateRecreatedQrBitmap(payload, 600)
                    if (cleanBitmap != null) {
                        generatedBitmapCache[payload] = cleanBitmap
                    }
                    return@withContext details
                }
            }
        } catch (_: Throwable) {
            // Ignore decode failures
        }
        null
    }
}
