package com.masroofi.app.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.masroofi.app.data.dao.CategoryDao
import com.masroofi.app.data.dao.MonthlyBudgetDao
import com.masroofi.app.data.dao.TransactionDao
import com.masroofi.app.data.entity.CategoryEntity
import com.masroofi.app.data.entity.MonthlyBudgetEntity
import com.masroofi.app.data.entity.TransactionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * فئة قاعدة البيانات المحلية المركزية لتطبيق مصروفي (Room Database)
 * تُخزن البيانات محلياً في ملف masroofi.db دون اتصال بالإنترنت
 */
@Database(
    entities = [
        CategoryEntity::class,
        TransactionEntity::class,
        MonthlyBudgetEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MasroofiDatabase : RoomDatabase() {

    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun monthlyBudgetDao(): MonthlyBudgetDao

    companion object {
        const val DATABASE_NAME = "masroofi.db"

        @Volatile
        private var INSTANCE: MasroofiDatabase? = null

        fun getDatabase(
            context: Context,
            scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
        ): MasroofiDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MasroofiDatabase::class.java,
                    DATABASE_NAME
                )
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        /**
         * دالة رد نداء لتهيئة وبذر التصنيفات الافتراضية بأمان عند إنشاء قاعدة البيانات لأول مرة
         */
        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch {
                        populateDefaultCategories(database.categoryDao())
                    }
                }
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                // ضمان تمييز أسماء التصنيفات الافتراضية بدقة
                try {
                    db.execSQL("UPDATE categories SET name = 'مصروفات أخرى' WHERE id = 'cat_exp_other' AND name = 'أخرى'")
                    db.execSQL("UPDATE categories SET name = 'إيرادات أخرى' WHERE id = 'cat_inc_other' AND name = 'أخرى'")
                } catch (_: Exception) {
                }
            }
        }

        /**
         * بذر التصنيفات الافتراضية الخمسة عشر (10 للمصروفات و 5 للإيرادات)
         */
        suspend fun populateDefaultCategories(categoryDao: CategoryDao) {
            categoryDao.insertCategories(DefaultCategories.allDefaultCategories)
        }
    }
}
