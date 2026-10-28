package com.novanest.mpesatracker.data
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.novanest.mpesatracker.data.dao.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
@Database(
    entities = [Category::class, Transaction::class, Budget::class, AppState::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun appStateDao(): AppStateDao
    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun get(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mpesa_tracker.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { db ->
                        INSTANCE = db
                        seedDefaults(db)
                    }
            }
        }
        private fun seedDefaults(db: AppDatabase) {
            CoroutineScope(Dispatchers.IO).launch {
                if (db.categoryDao().count() == 0) {
                    val defaults = listOf(
                        "Food", "Transport", "Giving", "Clothes",
                        "Shoes and Jewellery", "Cosmetics",
                        "Hair Dressing and Nails", "Other"
                    )
                    defaults.forEachIndexed { index, name ->
                        db.categoryDao().insert(
                            Category(name = name, isDefault = true, sortOrder = index)
                        )
                    }
                }
                if (db.appStateDao().getOnce() == null) {
                    db.appStateDao().save(AppState())
                }
            }
        }
    }
}
