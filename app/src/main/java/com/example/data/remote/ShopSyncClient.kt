package com.example.data.remote

import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class ShopSyncClient {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val adapter = moshi.adapter(CloudShopData::class.java)

    private val client = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .writeTimeout(8, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    // Using high-speed cloud sync REST endpoint for multi-device collaboration
    private fun getCloudUrl(shopCode: String): String {
        val sanitizedCode = shopCode.trim().uppercase()
        return "https://udhaar-khata-team-default-rtdb.firebaseio.com/shops/$sanitizedCode.json"
    }

    suspend fun fetchCloudData(shopCode: String): CloudShopData? = withContext(Dispatchers.IO) {
        try {
            val url = getCloudUrl(shopCode)
            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.w("ShopSyncClient", "Fetch failed with HTTP ${response.code}")
                return@withContext null
            }
            val body = response.body?.string()
            if (body.isNullOrBlank() || body == "null") {
                return@withContext null
            }
            adapter.fromJson(body)
        } catch (e: Exception) {
            Log.e("ShopSyncClient", "Network error during fetch: ${e.message}")
            null
        }
    }

    suspend fun pushCloudData(data: CloudShopData): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = getCloudUrl(data.shopCode)
            val json = adapter.toJson(data)
            val requestBody = json.toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url(url)
                .put(requestBody)
                .build()

            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            Log.e("ShopSyncClient", "Network error during push: ${e.message}")
            false
        }
    }
}
