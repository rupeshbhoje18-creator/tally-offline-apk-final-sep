package com.example.tallycustomerapp.web

import android.content.Context
import android.widget.Toast
import com.example.tallycustomerapp.data.*
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface

class WebAppInterface(
    private val context: Context,
    private val db: AppDatabase
) {
    private val gson = Gson()

    @JavascriptInterface
    fun saveCompanyDataOffline(dataJson: String) {
        try {
            val data = gson.fromJson(dataJson, CompanySyncPayload::class.java)
            CoroutineScope(Dispatchers.IO).launch {
                db.companyDao().insertOrUpdate(
                    OfflineCompany(
                        companyName = data.companyName,
                        serialNumber = data.serialNumber,
                        ledgerList = data.ledgerList,
                        voucherList = data.voucherList,
                        lastSynced = System.currentTimeMillis()
                    )
                )
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, "Data Synced Offline!", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, "Failed to Sync Data: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    data class CompanySyncPayload(
        val companyName: String,
        val serialNumber: String,
        val ledgerList: List<Ledger>,
        val voucherList: List<Voucher>
    )
}