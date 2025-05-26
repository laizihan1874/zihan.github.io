package com.example.fitnesstrackerapp.tracking.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.fitnesstrackerapp.tracking.model.ActivityDao
import com.example.fitnesstrackerapp.tracking.model.ActivityLog
import com.example.fitnesstrackerapp.tracking.model.AppDatabase
import kotlinx.coroutines.launch

class ActivityDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val activityDao: ActivityDao

    private val _activityLog = MutableLiveData<ActivityLog?>()
    val activityLog: LiveData<ActivityLog?> = _activityLog

    init {
        val database = AppDatabase.getDatabase(application)
        activityDao = database.activityDao()
    }

    fun loadActivity(activityId: Long) {
        viewModelScope.launch {
            // Assuming getActivityLogById is a suspend function in ActivityDao
            // If it returns LiveData directly, this would be simpler.
            // For now, let's assume it's suspend as per previous DAO setup.
            val log = activityDao.getActivityLogById(activityId)
            _activityLog.postValue(log)
        }
    }
}
