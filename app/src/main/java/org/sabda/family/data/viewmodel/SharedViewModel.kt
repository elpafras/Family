package org.sabda.family.data.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.sabda.family.model.FamilyData
import org.sabda.family.model.RenunganData

class SharedViewModel : ViewModel() {

    private val _renunganData = MutableLiveData<RenunganData?>()
    val renunganData: LiveData<RenunganData?> get() = _renunganData

    fun setRenunganData(data: RenunganData?) {
        _renunganData.value = data
    }

    fun hasData(): Boolean = _renunganData.value != null

    /* /////////////////////////////////////////////////// */

    private val _familyData = MutableLiveData<List<FamilyData>>()
    val familyData: MutableLiveData<List<FamilyData>> get() = _familyData

    fun setFamilyData(data: List<FamilyData>) {
        _familyData.value = data
    }

    fun hasFamilyData(): Boolean = _familyData.value != null

}