package com.example.tallycustomerapp.offline

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tallycustomerapp.data.CompanyDao
import com.example.tallycustomerapp.data.OfflineCompany
import kotlinx.coroutines.launch

class OfflineViewModel(
    private val companyDao: CompanyDao
) : ViewModel() {
    val companiesLiveData = MutableLiveData<List<OfflineCompany>>()

    fun loadCompanies() {
        viewModelScope.launch {
            try {
                val companies = companyDao.getAllCompanies()
                companiesLiveData.postValue(companies)
            } catch (e: Exception) {
                companiesLiveData.postValue(emptyList())
            }
        }
    }
}
