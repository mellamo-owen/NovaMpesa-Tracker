package com.novanest.mpesatracker.data
import androidx.room.Entity
import androidx.room.PrimaryKey
@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val isDefault: Boolean = false,
    val sortOrder: Int = 0
)
