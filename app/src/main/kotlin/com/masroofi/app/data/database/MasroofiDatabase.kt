package com.masroofi.app.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
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
    version = 2,
    exportSchema = false
)
abstract class MasroofiDatabase : RoomDatabase() {

    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun monthlyBudgetDao(): MonthlyBudgetDao

    companion object {
        const val DATABASE_NAME = "masroofi.db"

        /**
         * ترحيل آمن لقاعدة البيانات من الإصدار 1 إلى 2:
         * إضافة عمود كود العملة (currencyCode) إلى جدولي transactions و monthly_budgets
         * مع تعيين القيمة الافتراضية 'YER' (الريال اليمني) لجميع السجلات القديمة تلقائياً دون أي فقدان للبيانات
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `transactions` ADD COLUMN `currencyCode` TEXT NOT NULL DEFAULT 'YER'")
                db.execSQL("ALTER TABLE `monthly_budgets` ADD COLUMN `currencyCode` TEXT NOT NULL DEFAULT 'YER'")
            }
        }

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
                    .addMigrations(MIGRATION_1_2)
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
