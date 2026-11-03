package com.example.util

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.repository.NightShiftRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object OnlineSyncManager {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    // Shared cloud backup sync endpoint for NightShift App devices
    private const val CLOUD_SYNC_URL = "https://kvdb.io/4y9q9rXyX1y3m3g3q3t2b1/night_shift_global_sync"

    /**
     * Pushes local database JSON to cloud endpoint so any other device can retrieve it.
     */
    suspend fun syncToCloud(repository: NightShiftRepository): Result<String> = withContext(Dispatchers.IO) {
        try {
            val jsonPayload = repository.exportDatabaseJson()
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = jsonPayload.toRequestBody(mediaType)

            val request = Request.Builder()
                .url(CLOUD_SYNC_URL)
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                Result.success("Cloud sync uploaded successfully")
            } else {
                Result.failure(Exception("Cloud server responded with code ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Pulls latest database JSON from cloud endpoint and merges into local Room DB.
     */
    suspend fun syncFromCloud(repository: NightShiftRepository): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(CLOUD_SYNC_URL)
                .get()
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                if (!responseBody.isNullOrBlank()) {
                    val count = repository.importDatabaseJson(responseBody)
                    Result.success(count)
                } else {
                    Result.success(0)
                }
            } else {
                Result.failure(Exception("Could not fetch cloud data (${response.code})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Exports full database as a .json backup file and triggers Android share intent.
     */
    suspend fun exportBackupFile(context: Context, repository: NightShiftRepository, language: AppLanguage) {
        try {
            val json = repository.exportDatabaseJson()
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "NightShift_Backup_$timeStamp.json"

            val exportDir = File(context.cacheDir, "backups")
            if (!exportDir.exists()) exportDir.mkdirs()

            val file = File(exportDir, fileName)
            FileOutputStream(file).use { it.write(json.toByteArray(Charsets.UTF_8)) }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_SUBJECT, "Night Shift App Database Backup ($timeStamp)")
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(
                Intent.createChooser(
                    shareIntent,
                    if (language == AppLanguage.BENGALI) "ব্যাকআপ ফাইল শেয়ার করুন" else "Share Database Backup"
                )
            )
        } catch (e: Exception) {
            Toast.makeText(context, "Backup failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
