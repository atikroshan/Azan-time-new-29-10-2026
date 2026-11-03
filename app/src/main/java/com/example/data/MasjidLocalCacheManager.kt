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

                if (directPhotoUrl.isNotBlank() && directPhotoUrl.startsWith("http")) {
                    val lastPhotoUrl = if (photoUrlTracker.exists()) photoUrlTracker.readText().trim() else ""
                    if (lastPhotoUrl != directPhotoUrl || !photoFile.exists() || photoFile.length() == 0L) {
                        downloadAndSaveAsWebP(directPhotoUrl, photoFile)
                        try { photoUrlTracker.writeText(directPhotoUrl) } catch (_: Exception) {}
                    }
                } else {
                    // Photo was removed from sheet - clean up stale local file
                    if (photoFile.exists()) photoFile.delete()
                    if (photoUrlTracker.exists()) photoUrlTracker.delete()
                }

                // 2. QR sync & WebP compression with URL change detection
                val directQrUrl = GoogleSheetMasjidSync.extractGoogleDriveDirectUrl(masjid.qrUrl)
                val qrFile = File(masjidDir, QR_FILE_NAME)
                val qrUrlTracker = File(masjidDir, "qr_url.txt")

                if (directQrUrl.isNotBlank() && directQrUrl.startsWith("http")) {
                    val lastQrUrl = if (qrUrlTracker.exists()) qrUrlTracker.readText().trim() else ""
                    if (lastQrUrl != directQrUrl || !qrFile.exists() || qrFile.length() == 0L) {
                        downloadAndSaveAsWebP(directQrUrl, qrFile)
                        try { qrUrlTracker.writeText(directQrUrl) } catch (_: Exception) {}
                    }
                } else {
                    // QR was removed from sheet - clean up stale local file
                    if (qrFile.exists()) qrFile.delete()
                    if (qrUrlTracker.exists()) qrUrlTracker.delete()
                }
            }
        }
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
