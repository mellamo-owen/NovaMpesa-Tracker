package com.novanest.mpesatracker.data
import android.content.Context
import androidx.room.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@Entity(tableName = "app_state")
data class AppState(
    @PrimaryKey val id: Int = 1,
    val userName: String = "",
    val trialStart: Long = 0L,
    val subscriptionExpiry: Long? = null
)

@Entity(tableName = "categories")
data class Category(@PrimaryKey(autoGenerate = true) val id: Long = 0, val name: String)

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double, 
    val recipient: String, 
    val categoryId: Long?,
    val timestamp: Long, 
    val isLoan: Boolean = false,
    val balance: Double? = null
)

@Dao interface AppStateDao {
    @Query("SELECT * FROM app_state WHERE id = 1") suspend fun getState(): AppState?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveState(state: AppState)
}
@Dao interface CategoryDao {
    @Query("SELECT * FROM categories") fun getAll(): Flow<List<Category>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(cat: Category)
    @Query("SELECT COUNT(*) FROM categories") suspend fun count(): Int
}
@Dao interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC") fun getAll(): Flow<List<Transaction>>
    @Insert suspend fun insert(txn: Transaction): Long
    @Query("UPDATE transactions SET categoryId = :catId WHERE id = :txnId") suspend fun updateCategory(txnId: Long, catId: Long)
    @Query("UPDATE transactions SET isLoan = :isLoan WHERE id = :txnId") suspend fun updateLoanStatus(txnId: Long, isLoan: Boolean)
    @Query("DELETE FROM transactions WHERE id = :id") suspend fun deleteById(id: Long)
}

@Database(entities = [AppState::class, Category::class, Transaction::class], version = 6, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appStateDao(): AppStateDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun get(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "clean_nova_v6.db")
                    .fallbackToDestructiveMigration().build().also { db ->
                        INSTANCE = db
                        seedCategories(db)
                    }
            }
        }

        private fun seedCategories(db: AppDatabase) {
            CoroutineScope(Dispatchers.IO).launch {
                if (db.categoryDao().count() == 0) {
                    val defaults = listOf("Food", "Transport", "Giving", "Clothes", "Shoes and Jewellery", "Cosmetics", "Hair Dressing and Nails", "Other", "External Deposit")
                    defaults.forEach { name -> db.categoryDao().insert(Category(name = name)) }
                }
            }
        }
    }
}
