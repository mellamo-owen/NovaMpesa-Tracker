package com.novanest.mpesatracker.data.dao
import androidx.room.*
import com.novanest.mpesatracker.data.AppState
import kotlinx.coroutines.flow.Flow
@Dao
interface AppStateDao {
    @Query("SELECT * FROM app_state WHERE id = 1")
    fun get(): Flow<AppState?>
    @Query("SELECT * FROM app_state WHERE id = 1")
    suspend fun getOnce(): AppState?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(state: AppState)
}
