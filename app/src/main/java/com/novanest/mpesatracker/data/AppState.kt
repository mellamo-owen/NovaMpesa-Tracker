package com.novanest.mpesatracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_state")
data class AppState(
    @PrimaryKey val id: Int = 1,
    val userName: String = "",
    val userPhone: String = "",
    val isSignedIn: Boolean = false,
    val trialStart: Long = 0L, // 0 means trial hasn't started
    val subscriptionExpiry: Long? = null,   
    val deviceId: String = ""               
)
