package com.example.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object ImageStorageHelper {

    fun createCameraImageUri(context: Context): Uri {
        val photosDir = File(context.filesDir, "product_photos")
        if (!photosDir.exists()) {
            photosDir.mkdirs()
        }
        val file = File(photosDir, "prod_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    fun createCameraTempUri(context: Context): Pair<Uri, File> {
        val photosDir = File(context.filesDir, "product_photos")
        if (!photosDir.exists()) {
            photosDir.mkdirs()
        }
        val file = File(photosDir, "prod_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        return Pair(uri, file)
    }

    fun saveImageFromUri(context: Context, sourceUri: Uri): String? {
        return try {
            val photosDir = File(context.filesDir, "product_photos")
            if (!photosDir.exists()) {
                photosDir.mkdirs()
            }
            val targetFile = File(photosDir, "prod_${System.currentTimeMillis()}.jpg")

            context.contentResolver.openInputStream(sourceUri)?.use { input: InputStream ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }
            targetFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun saveImageToPersistentStorage(context: Context, sourceUri: Uri): String? {
        return saveImageFromUri(context, sourceUri)
    }
}
