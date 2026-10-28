package com.novanest.mpesatracker.data.dao
import androidx.room.*
import com.novanest.mpesatracker.data.Budget
import kotlinx.coroutines.flow.Flow
@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets")
    fun getAll(): Flow<List<Budget>>
    @Query("SELECT * FROM budgets WHERE categoryId = :categoryId LIMIT 1")
    suspend fun getForCategory(categoryId: Long): Budget?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(budget: Budget)
    @Delete
    suspend fun delete(budget: Budget)
}
