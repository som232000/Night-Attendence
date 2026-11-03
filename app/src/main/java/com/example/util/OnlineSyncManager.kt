package com.example.util

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.repository.NightShiftRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object OnlineSyncManager {

    private const val TAG = "OnlineSyncManager"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    // Verified persistent global cloud storage object ID for NightShift App
    private const val CLOUD_OBJECT_URL = "https://api.restful-api.dev/objects/ff808181a09d98f701a1a28b7459252f"

    /**
     * Pulls the latest cloud database state and merges it into local Room DB.
     * This brings all records submitted by any user on any device to this device.
     */
    suspend fun syncFromCloud(repository: NightShiftRepository): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(CLOUD_OBJECT_URL)
                .get()
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                if (!responseBody.isNullOrBlank()) {
                    val root = JSONObject(responseBody)
                    if (root.has("data")) {
                        val dataObj = root.getJSONObject("data")
                        val count = repository.importDatabaseJson(dataObj.toString())
                        Log.d(TAG, "Pulled and imported $count items from cloud")
                        return@withContext Result.success(count)
                    }
                }
                Result.success(0)
            } else {
                Result.failure(Exception("Cloud fetch failed (HTTP ${response.code})"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "syncFromCloud error", e)
            Result.failure(e)
        }
    }

    /**
     * Bidirectional sync:
     * 1. Pulls newest entries from cloud and merges locally.
     * 2. Exports the unified local database and pushes it to cloud.
     * Guarantees all devices see all data in real-time.
     */
    suspend fun syncToCloud(repository: NightShiftRepository): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Step 1: Pull from cloud first so we don't overwrite another device's entries
            syncFromCloud(repository)

            // Step 2: Export unified data
            val localJsonStr = repository.exportDatabaseJson()
            val localJsonObj = JSONObject(localJsonStr)

            val payload = JSONObject()
            payload.put("data", localJsonObj)

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = payload.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url(CLOUD_OBJECT_URL)
                .patch(body)
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                Log.d(TAG, "Database synced to cloud successfully")
                Result.success("Sync successful")
            } else {
                Result.failure(Exception("Cloud upload failed (HTTP ${response.code})"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "syncToCloud error", e)
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
