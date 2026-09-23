package com.example.tallycustomerapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope // Very important!

import com.example.tallycustomerapp.data.OfflineCompany
import com.example.tallycustomerapp.data.CompanyDao
import kotlinx.coroutines.launch

class OfflineViewModel(
    private val dao: CompanyDao
) : ViewModel() {
    val companiesLiveData = MutableLiveData<List<OfflineCompany>>()

    fun loadCompanies() {
        viewModelScope.launch {
            try {
                val companies = dao.getAllCompanies()
                companiesLiveData.postValue(companies)
            } catch (e: Exception) {
                companiesLiveData.postValue(emptyList())
            }
        }
    }
}
