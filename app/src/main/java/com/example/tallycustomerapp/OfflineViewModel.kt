package com.example.tallycustomerapp.offline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.MutableLiveData
import com.example.tallycustomerapp.data.*
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModelProvider

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

class OfflineViewModelFactory(private val dao: CompanyDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return OfflineViewModel(dao) as T
    }
}