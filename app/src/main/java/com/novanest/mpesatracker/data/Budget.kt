package com.novanest.mpesatracker.data
import androidx.room.Entity
import androidx.room.PrimaryKey
@Entity(tableName = "budgets")
data class Budget(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryId: Long,
    val weeklyAmount: Double
)
