package com.example.tallycustomerapp.data

import androidx.room.*

@Dao
interface CompanyDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(company: OfflineCompany)

    @Query("SELECT * FROM companies ORDER BY lastSynced DESC")
    suspend fun getAllCompanies(): List<OfflineCompany>
}