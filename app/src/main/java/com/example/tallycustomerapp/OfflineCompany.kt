package com.example.tallycustomerapp.data

import androidx.room.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Entity(tableName = "companies")
data class OfflineCompany(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val companyName: String,
    val serialNumber: String,
    val ledgerList: List<Ledger>,
    val voucherList: List<Voucher>,
    val lastSynced: Long
)

data class Ledger(
    val name: String,
    val amount: Double
)

data class Voucher(
    val date: String,
    val type: String,
    val amount: Double
)

class ListTypeConverters {
    private val gson = Gson()

    @TypeConverter
    fun ledgerListToString(list: List<Ledger>?): String =
        gson.toJson(list)

    @TypeConverter
    fun stringToLedgerList(data: String?): List<Ledger> =
        gson.fromJson(data, object : TypeToken<List<Ledger>>() {}.type)

    @TypeConverter
    fun voucherListToString(list: List<Voucher>?): String =
        gson.toJson(list)

    @TypeConverter
    fun stringToVoucherList(data: String?): List<Voucher> =
        gson.fromJson(data, object : TypeToken<List<Voucher>>() {}.type)
}