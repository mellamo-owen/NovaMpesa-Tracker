package com.novanest.mpesatracker.data.dao
import androidx.room.*
import com.novanest.mpesatracker.data.Category
import kotlinx.coroutines.flow.Flow
@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY sortOrder ASC, name ASC")
    fun getAll(): Flow<List<Category>>
    @Query("SELECT * FROM categories ORDER BY sortOrder ASC, name ASC")
    suspend fun getAllOnce(): List<Category>
    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: Long): Category?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: Category): Long
    @Update
    suspend fun update(category: Category)
    @Delete
    suspend fun delete(category: Category)
    @Query("SELECT COUNT(*) FROM categories")
    suspend fun count(): Int
}
