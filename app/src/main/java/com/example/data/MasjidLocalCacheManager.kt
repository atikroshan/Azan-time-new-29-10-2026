package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Manages local folder-based storage and automatic WebP image caching for Masajid:
 * Storage Path: context.filesDir/masjids/{masjid_id}/
 *   - photo.webp : Local WebP compressed Masjid main photo
 *   - qr.webp    : Local WebP compressed Masjid payment QR code photo
 *
 * Provides instant 0-second offline access to photos and QR codes without requiring APK updates.
 */
object MasjidLocalCacheManager {

    private const val MASJIDS_FOLDER = "masjids"
    private const val PHOTO_FILE_NAME = "photo.webp"
    private const val QR_FILE_NAME = "qr.webp"

    /**
     * Returns the dedicated directory File for a masjid ID inside context.filesDir/masjids/{id}
     */
    fun getMasjidDir(context: Context, masjidId: String): File {
        val cleanId = masjidId.trim()
        val dir = File(context.filesDir, "$MASJIDS_FOLDER/$cleanId")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Returns the local photo.webp File object for a masjid
     */
    fun getLocalPhotoFile(context: Context, masjidId: String): File {
        return File(getMasjidDir(context, masjidId), PHOTO_FILE_NAME)
    }

    /**
     * Returns the local qr.webp File object for a masjid
     */
    fun getLocalQrFile(context: Context, masjidId: String): File {
        return File(getMasjidDir(context, masjidId), QR_FILE_NAME)
    }

    /**
     * Resolves the model to pass to Coil AsyncImage for a masjid photo.
     * Returns the local File if photo.webp exists locally, otherwise returns the direct URL or fallback drawable.
     */
    fun getPhotoModel(context: Context, masjidId: String, remoteUrl: String): Any? {
        val file = getLocalPhotoFile(context, masjidId)
        if (file.exists() && file.length() > 0) {
            return file
        }
        val directUrl = GoogleSheetMasjidSync.extractGoogleDriveDirectUrl(remoteUrl)
        if (directUrl.isNotBlank()) {
            return directUrl
        }
        return null
    }

    /**
     * Resolves the model to pass to Coil AsyncImage for a masjid QR code.
     * Returns the local File if qr.webp exists locally, otherwise returns the direct URL or null.
     */
    fun getQrModel(context: Context, masjidId: String, remoteUrl: String): Any? {
        val file = getLocalQrFile(context, masjidId)
        if (file.exists() && file.length() > 0) {
            return file
        }
        val directUrl = GoogleSheetMasjidSync.extractGoogleDriveDirectUrl(remoteUrl)
        if (directUrl.isNotBlank()) {
            return directUrl
        }
        return null
    }

    /**
     * Clears all cached masjid folders and images for a clean fresh start
     */
    fun clearAllCache(context: Context) {
        try {
            val root = File(context.filesDir, MASJIDS_FOLDER)
            if (root.exists()) {
                root.deleteRecursively()
            }
        } catch (_: Exception) {}
    }

    /**
     * Downloads remote photos/QRs when online, converts them to WebP format,
     * and saves them inside context.filesDir/masjids/{masjid_id}/.
     * Automatically updates whenever a new photo/QR is uploaded to Google Sheet,
     * and cleans up if a photo or QR is deleted from the Sheet.
     */
    suspend fun syncAllMasjidImages(context: Context, masajid: List<MasjidItem>) {
        withContext(Dispatchers.IO) {
            for (masjid in masajid) {
                if (masjid.id.isBlank() || masjid.id.equals("guest", ignoreCase = true)) continue
                val masjidDir = getMasjidDir(context, masjid.id)

                // 1. Photo sync & WebP compression with URL change detection
                val directPhotoUrl = GoogleSheetMasjidSync.extractGoogleDriveDirectUrl(masjid.photoUrl)
                val photoFile = File(masjidDir, PHOTO_FILE_NAME)
                val photoUrlTracker = File(masjidDir, "photo_url.txt")

                val lastPhotoUrl = if (photoUrlTracker.exists()) photoUrlTracker.readText().trim() else ""
                if (directPhotoUrl.isNotBlank() && directPhotoUrl.startsWith("http")) {
                    if (lastPhotoUrl != directPhotoUrl || !photoFile.exists() || photoFile.length() == 0L) {
                        downloadAndSaveAsWebP(directPhotoUrl, photoFile)
                        try { photoUrlTracker.writeText(directPhotoUrl) } catch (_: Exception) {}
                    }
                } else if (lastPhotoUrl.isNotBlank() && lastPhotoUrl.startsWith("http")) {
                    // Only remove if it was previously an HTTP image from the sheet
                    if (photoFile.exists()) photoFile.delete()
                    if (photoUrlTracker.exists()) photoUrlTracker.delete()
                }

                // 2. QR sync & WebP compression with URL change detection
                val directQrUrl = GoogleSheetMasjidSync.extractGoogleDriveDirectUrl(masjid.qrUrl)
                val qrFile = File(masjidDir, QR_FILE_NAME)
                val qrUrlTracker = File(masjidDir, "qr_url.txt")
                val lastQrUrl = if (qrUrlTracker.exists()) qrUrlTracker.readText().trim() else ""

                if (directQrUrl.isNotBlank() && directQrUrl.startsWith("http")) {
                    if (lastQrUrl != directQrUrl || !qrFile.exists() || qrFile.length() == 0L) {
                        downloadAndSaveAsWebP(directQrUrl, qrFile)
                        try { qrUrlTracker.writeText(directQrUrl) } catch (_: Exception) {}
                    }
                } else if (lastQrUrl.isNotBlank() && lastQrUrl.startsWith("http")) {
                    // Only remove if it was previously an HTTP image from the sheet
                    if (qrFile.exists()) qrFile.delete()
                    if (qrUrlTracker.exists()) qrUrlTracker.delete()
                }
            }
        }
    }

    fun hasLocalPhoto(context: Context, masjidId: String): Boolean {
        val file = getLocalPhotoFile(context, masjidId)
        return file.exists() && file.length() > 0
    }

    fun hasLocalQr(context: Context, masjidId: String): Boolean {
        val file = getLocalQrFile(context, masjidId)
        return file.exists() && file.length() > 0
    }

    /**
     * Replaces previous masjid photo with new photo immediately and evicts Coil image cache.
     */
    fun replaceMasjidPhotoLocally(context: Context, masjidId: String, sourceUri: android.net.Uri): Boolean {
        val success = saveCustomImageAsWebP(context, sourceUri, masjidId, isQr = false)
        if (success) {
            val photoUrlTracker = File(getMasjidDir(context, masjidId), "photo_url.txt")
            try { photoUrlTracker.writeText(sourceUri.toString()) } catch (_: Exception) {}
            try {
                coil.Coil.imageLoader(context).memoryCache?.clear()
                coil.Coil.imageLoader(context).diskCache?.clear()
            } catch (_: Exception) {}
        }
        return success
    }

    /**
     * Replaces previous masjid QR with new QR immediately and evicts Coil image cache.
     */
    fun replaceMasjidQrLocally(context: Context, masjidId: String, qrBitmapOrUri: Any): Boolean {
        val qrFile = getLocalQrFile(context, masjidId)
        val success = try {
            if (qrBitmapOrUri is Bitmap) {
                val tempFile = File(qrFile.parentFile, "${qrFile.name}.tmp")
                FileOutputStream(tempFile).use { out ->
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                        qrBitmapOrUri.compress(Bitmap.CompressFormat.WEBP_LOSSY, 90, out)
                    } else {
                        @Suppress("DEPRECATION")
                        qrBitmapOrUri.compress(Bitmap.CompressFormat.WEBP, 90, out)
                    }
                }
                if (tempFile.exists() && tempFile.length() > 0) {
                    if (qrFile.exists()) qrFile.delete()
                    tempFile.renameTo(qrFile)
                    true
                } else false
            } else if (qrBitmapOrUri is android.net.Uri) {
                saveCustomImageAsWebP(context, qrBitmapOrUri, masjidId, isQr = true)
            } else false
        } catch (_: Exception) {
            false
        }
        if (success) {
            val qrUrlTracker = File(getMasjidDir(context, masjidId), "qr_url.txt")
            try { qrUrlTracker.writeText("custom_local_qr") } catch (_: Exception) {}
            try {
                coil.Coil.imageLoader(context).memoryCache?.clear()
                coil.Coil.imageLoader(context).diskCache?.clear()
            } catch (_: Exception) {}
        }
        return success
    }

    /**
     * Saves a Uri (uploaded by admin in app) into the local masjid folder as WebP
     */
    fun saveCustomImageAsWebP(context: Context, sourceUri: android.net.Uri, masjidId: String, isQr: Boolean): Boolean {
        return try {
            val targetFile = if (isQr) getLocalQrFile(context, masjidId) else getLocalPhotoFile(context, masjidId)
            context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                val bitmap = BitmapFactory.decodeStream(inputStream)
                if (bitmap != null) {
                    targetFile.parentFile?.mkdirs()
                    val tempFile = File(targetFile.parentFile, "${targetFile.name}.tmp")
                    FileOutputStream(tempFile).use { out ->
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                            bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 85, out)
                        } else {
                            @Suppress("DEPRECATION")
                            bitmap.compress(Bitmap.CompressFormat.WEBP, 85, out)
                        }
                    }
                    if (tempFile.exists() && tempFile.length() > 0) {
                        if (targetFile.exists()) targetFile.delete()
                        tempFile.renameTo(targetFile)
                        return true
                    }
                }
            }
            false
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Downloads an image from HTTP/HTTPS URL, converts it to WebP format, and saves to target file.
     */
    private fun downloadAndSaveAsWebP(imageUrl: String, targetFile: File) {
        var conn: HttpURLConnection? = null
        try {
            val url = URL(imageUrl)
            conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 10000
            conn.readTimeout = 15000
            conn.instanceFollowRedirects = true
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            conn.connect()

            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                conn.inputStream.use { inputStream ->
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    if (bitmap != null) {
                        val tempFile = File(targetFile.parentFile, "${targetFile.name}.tmp")
                        FileOutputStream(tempFile).use { out ->
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                                bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 85, out)
                            } else {
                                @Suppress("DEPRECATION")
                                bitmap.compress(Bitmap.CompressFormat.WEBP, 85, out)
                            }
                        }
                        if (tempFile.exists() && tempFile.length() > 0) {
                            if (targetFile.exists()) {
                                targetFile.delete()
                            }
                            tempFile.renameTo(targetFile)
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Download/encoding error handled gracefully
        } finally {
            conn?.disconnect()
        }
    }
}
