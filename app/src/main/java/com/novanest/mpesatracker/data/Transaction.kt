package com.novanest.mpesatracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val recipient: String,          
    val balance: Double?,           
    val txnCode: String?,           
    val categoryId: Long?,          
    val timestamp: Long,            
    val rawSms: String?,            
    val isManual: Boolean = false,
    val isLoan: Boolean = false, // NEW: Tracks if it's a loan
    val createdAt: Long = System.currentTimeMillis()
)
