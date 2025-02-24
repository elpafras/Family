package org.sabda.family.data.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.sabda.family.model.FamilyData
import org.sabda.family.model.RenunganData
import java.util.Calendar

class SharedViewModel : ViewModel() {

    private val _renunganData = MutableLiveData<RenunganData?>()
    val renunganData: LiveData<RenunganData?> get() = _renunganData

    fun setRenunganData(data: RenunganData?) {
        _renunganData.value = data
    }

    fun hasData(): Boolean = _renunganData.value != null

    /* /////////////////////////////////////////////////// */

    // Data untuk HomeFragment
    private val _homeFamilyData = MutableLiveData<List<FamilyData>>()
    val homeFamilyData: LiveData<List<FamilyData>> get() = _homeFamilyData

    fun setHomeFamilyData(data: List<FamilyData>) {
        _homeFamilyData.value = data
    }

    fun hasHomeFamilyData(): Boolean = _homeFamilyData.value != null

    // Data untuk ResourcesFragment
    private val _resourcesFamilyData = MutableLiveData<List<FamilyData>>()
    val resourcesFamilyData: LiveData<List<FamilyData>> get() = _resourcesFamilyData

    fun setResourcesFamilyData(data: List<FamilyData>) {
        _resourcesFamilyData.value = data
    }

    fun hasResourcesFamilyData(): Boolean = _resourcesFamilyData.value != null

    /* /////////////////////////////////////////////////// */

    // Menambahkan LiveData untuk currentDate
    private val _currentDate = MutableLiveData<Calendar>()
    val currentDate: LiveData<Calendar> get() = _currentDate

    // Setter untuk currentDate
    fun setCurrentDate(date: Calendar) {
        _currentDate.value = date
    }

    // Getter untuk currentDate (langsung dari LiveData)
    fun getCurrentDate(): Calendar? {
        return _currentDate.value
    }
}