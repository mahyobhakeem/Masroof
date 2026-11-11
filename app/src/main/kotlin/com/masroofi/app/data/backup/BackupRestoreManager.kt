package com.masroofi.app.data.backup

import androidx.room.withTransaction
import com.masroofi.app.data.dao.CategoryDao
import com.masroofi.app.data.dao.MonthlyBudgetDao
import com.masroofi.app.data.dao.TransactionDao
import com.masroofi.app.data.database.DefaultCategories
import com.masroofi.app.data.database.MasroofiDatabase
import com.masroofi.app.data.entity.CategoryEntity
import com.masroofi.app.data.entity.MonthlyBudgetEntity
import com.masroofi.app.data.entity.TransactionEntity
import com.masroofi.app.domain.model.TransactionType
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/**
 * واجهة مدير النسخ الاحتياطي واستعادة البيانات
 */
interface BackupManager {
    /**
     * إنشاء نص JSON كامل لجميع بيانات التطبيق باستخدام org.json المدمجة
     */
    suspend fun createBackupJson(): String

    /**
     * التحقق الصارم من سلامة وتوافق ملف النسخة الاحتياطية قبل الاستعادة
     */
    fun validateBackupJson(jsonString: String): Result<BackupSummary>

    /**
     * تنفيذ الاستعادة الذرية الشاملة واستبدال البيانات الحالية
     */
    suspend fun restoreBackup(payload: BackupPayload): Result<Unit>
}

/**
 * التنفيذ الفعلي لمدير النسخ الاحتياطي والاستعادة الذرية (BackupRestoreManager)
 * يعتمد على مكتبة org.json المدمجة قياسياً في نظام أندرويد لضمان عدم الفشل إطلاقاً في وقت التشغيل
 */
class BackupRestoreManager(
    private val database: MasroofiDatabase? = null,
    private val categoryDaoProvider: (() -> CategoryDao)? = null,
    private val transactionDaoProvider: (() -> TransactionDao)? = null,
    private val budgetDaoProvider: (() -> MonthlyBudgetDao)? = null
) : BackupManager {

    private val categoryDao: CategoryDao
        get() = categoryDaoProvider?.invoke()
            ?: database?.categoryDao()
            ?: throw IllegalStateException("لم يتم توفير CategoryDao أو MasroofiDatabase")

    private val transactionDao: TransactionDao
        get() = transactionDaoProvider?.invoke()
            ?: database?.transactionDao()
            ?: throw IllegalStateException("لم يتم توفير TransactionDao أو MasroofiDatabase")

    private val budgetDao: MonthlyBudgetDao
        get() = budgetDaoProvider?.invoke()
            ?: database?.monthlyBudgetDao()
            ?: throw IllegalStateException("لم يتم توفير MonthlyBudgetDao أو MasroofiDatabase")

    private suspend fun <T> runInAtomicTransaction(block: suspend () -> T): T {
        val db = database
        return if (db != null) {
            db.withTransaction { block() }
        } else {
            block()
        }
    }

    override suspend fun createBackupJson(): String {
        val categories = categoryDao.getAllCategoryEntities()
        val transactions = transactionDao.getAllTransactionEntities()
        val budgets = budgetDao.getAllBudgetEntities()

        val nowStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

        val root = JSONObject()
        root.put("backupFormatVersion", 1)
        root.put("createdAt", nowStr)
        root.put("appName", "Masroofi")
        root.put("currency", "YER")

        // 1. مصفوفة التصنيفات
        val categoriesArray = JSONArray()
        for (cat in categories) {
            val catObj = JSONObject()
            catObj.put("id", cat.id)
            catObj.put("name", cat.name)
            catObj.put("type", cat.type.name)
            catObj.put("iconKey", cat.iconKey)
            catObj.put("isDefault", cat.isDefault)
            catObj.put("isActive", cat.isActive)
            categoriesArray.put(catObj)
        }
        root.put("categories", categoriesArray)

        // 2. مصفوفة العمليات المالية
        val transactionsArray = JSONArray()
        for (tx in transactions) {
            val txObj = JSONObject()
            txObj.put("id", tx.id)
            txObj.put("type", tx.type.name)
            txObj.put("amount", tx.amount)
            txObj.put("categoryId", tx.categoryId)
            txObj.put("transactionDate", tx.transactionDate)
            if (tx.note != null) {
                txObj.put("note", tx.note)
            } else {
                txObj.put("note", JSONObject.NULL)
            }
            txObj.put("createdAt", tx.createdAt)
            txObj.put("updatedAt", tx.updatedAt)
            transactionsArray.put(txObj)
        }
        root.put("transactions", transactionsArray)

        // 3. مصفوفة الميزانيات الشهرية
        val budgetsArray = JSONArray()
        for (b in budgets) {
            val bObj = JSONObject()
            bObj.put("id", b.id)
            bObj.put("yearMonth", b.yearMonth)
            bObj.put("amount", b.amount)
            budgetsArray.put(bObj)
        }
        root.put("monthlyBudgets", budgetsArray)

        return root.toString(2)
    }

    override fun validateBackupJson(jsonString: String): Result<BackupSummary> {
        val root = try {
            JSONObject(jsonString)
        } catch (e: Exception) {
            return Result.failure(IllegalArgumentException("الملف المختار ليس بصيغة JSON صالحة أو أنه تالف: ${e.message}"))
        }

        // 1. التحقق من إصدار النسخة الاحتياطية
        val formatVersion = root.optInt("backupFormatVersion", -1)
        if (formatVersion != 1) {
            return Result.failure(
                IllegalArgumentException("إصدار النسخة الاحتياطية ($formatVersion) غير متوافق مع إصدار التطبيق الحالي.")
            )
        }

        // 2. التحقق من اسم التطبيق والعملة
        val appName = root.optString("appName", "")
        if (appName.isBlank() || appName != "Masroofi") {
            return Result.failure(IllegalArgumentException("الملف ليس نسخة احتياطية صالحة لتطبيق مصروفي."))
        }

        val currency = root.optString("currency", "")
        if (currency != "YER") {
            return Result.failure(
                IllegalArgumentException("عملة النسخة الاحتياطية ($currency) غير متوافقة مع عملة التطبيق (YER).")
            )
        }

        val createdAt = root.optString("createdAt", "")

        // 3. التحقق من سلامة التصنيفات
        val categoriesJson = root.optJSONArray("categories") ?: JSONArray()
        val categoriesList = mutableListOf<CategoryBackupDto>()
        for (i in 0 until categoriesJson.length()) {
            val catObj = categoriesJson.optJSONObject(i)
                ?: return Result.failure(IllegalArgumentException("عنصر تصنيف غير صالح في النسخة."))
            val id = catObj.optString("id", "")
            val name = catObj.optString("name", "")
            val type = catObj.optString("type", "")
            val iconKey = catObj.optString("iconKey", "")
            val isDefault = catObj.optBoolean("isDefault", false)
            val isActive = catObj.optBoolean("isActive", true)

            if (id.isBlank()) return Result.failure(IllegalArgumentException("يوجد تصنيف بدون معرف فريد (id) في النسخة."))
            if (name.isBlank()) return Result.failure(IllegalArgumentException("يوجد تصنيف بدون اسم في النسخة."))
            try {
                TransactionType.valueOf(type)
            } catch (e: Exception) {
                return Result.failure(IllegalArgumentException("نوع التصنيف '$type' غير معروف."))
            }

            categoriesList.add(CategoryBackupDto(id, name, type, iconKey, isDefault, isActive))
        }

        val availableCategoryIds = categoriesList.map { it.id }.toSet() +
                DefaultCategories.allDefaultCategories.map { it.id }.toSet()

        // 4. التحقق من سلامة العمليات المالية
        val transactionsJson = root.optJSONArray("transactions") ?: JSONArray()
        val transactionsList = mutableListOf<TransactionBackupDto>()
        for (i in 0 until transactionsJson.length()) {
            val txObj = transactionsJson.optJSONObject(i)
                ?: return Result.failure(IllegalArgumentException("عنصر عملية غير صالح في النسخة."))
            val id = txObj.optString("id", "")
            val type = txObj.optString("type", "")
            val amount = txObj.optLong("amount", -1L)
            val categoryId = txObj.optString("categoryId", "")
            val transactionDate = txObj.optString("transactionDate", "")
            val note = if (txObj.isNull("note")) null else txObj.optString("note", "")
            val txCreatedAt = txObj.optLong("createdAt", 0L)
            val txUpdatedAt = txObj.optLong("updatedAt", 0L)

            if (id.isBlank()) {
                return Result.failure(IllegalArgumentException("توجد عملية مالية بدون معرف فريد (id)."))
            }
            if (amount <= 0) {
                return Result.failure(IllegalArgumentException("توجد عملية مالية بمبلغ غير صالح ($amount). يجب أن تكون المبالغ موجبة أكبر من صفر."))
            }
            try {
                TransactionType.valueOf(type)
            } catch (e: Exception) {
                return Result.failure(IllegalArgumentException("نوع العملية '$type' غير صالح."))
            }
            try {
                LocalDate.parse(transactionDate)
            } catch (e: Exception) {
                return Result.failure(IllegalArgumentException("تاريخ العملية '$transactionDate' غير صالح."))
            }
            if (categoryId !in availableCategoryIds) {
                return Result.failure(
                    IllegalArgumentException("العملية '$id' تشير إلى تصنيف غير موجود ($categoryId).")
                )
            }

            transactionsList.add(
                TransactionBackupDto(id, type, amount, categoryId, transactionDate, note, txCreatedAt, txUpdatedAt)
            )
        }

        // 5. التحقق من سلامة الميزانيات الشهرية
        val budgetsJson = root.optJSONArray("monthlyBudgets") ?: JSONArray()
        val budgetsList = mutableListOf<MonthlyBudgetBackupDto>()
        for (i in 0 until budgetsJson.length()) {
            val bObj = budgetsJson.optJSONObject(i)
                ?: return Result.failure(IllegalArgumentException("عنصر ميزانية غير صالح في النسخة."))
            val id = bObj.optString("id", "")
            val yearMonth = bObj.optString("yearMonth", "")
            val amount = bObj.optLong("amount", -1L)

            if (id.isBlank()) {
                return Result.failure(IllegalArgumentException("توجد ميزانية شهرية بدون معرف فريد (id)."))
            }
            if (amount < 0) {
                return Result.failure(IllegalArgumentException("مبلغ الميزانية للشهر '$yearMonth' سالب ($amount)."))
            }
            try {
                YearMonth.parse(yearMonth)
            } catch (e: Exception) {
                return Result.failure(IllegalArgumentException("تنسيق الشهر '$yearMonth' للميزانية غير صالح."))
            }

            budgetsList.add(MonthlyBudgetBackupDto(id, yearMonth, amount))
        }

        val payload = BackupPayload(
            backupFormatVersion = formatVersion,
            createdAt = createdAt,
            appName = appName,
            currency = currency,
            categories = categoriesList,
            transactions = transactionsList,
            monthlyBudgets = budgetsList
        )

        return Result.success(
            BackupSummary(
                createdAt = createdAt,
                transactionCount = transactionsList.size,
                categoryCount = categoriesList.size,
                budgetCount = budgetsList.size,
                payload = payload
            )
        )
    }

    override suspend fun restoreBackup(payload: BackupPayload): Result<Unit> {
        return runCatching {
            runInAtomicTransaction {
                // 1. مسح جدول العمليات أولاً مراعاة للمفتاح الخارجي
                transactionDao.deleteAllTransactions()

                // 2. مسح جدول الميزانيات والتصنيفات
                budgetDao.deleteAllBudgets()
                categoryDao.deleteAllCategories()

                // 3. تجهيز التصنيفات مع دمج التصنيفات الافتراضية لمنع فقدان أي منها
                val categoriesToInsert = payload.categories.map { dto ->
                    CategoryEntity(
                        id = dto.id,
                        name = dto.name,
                        type = TransactionType.valueOf(dto.type),
                        iconKey = dto.iconKey,
                        isDefault = dto.isDefault,
                        isActive = dto.isActive
                    )
                }.toMutableList()

                val insertedIds = categoriesToInsert.map { it.id }.toSet()
                for (defaultCat in DefaultCategories.allDefaultCategories) {
                    if (defaultCat.id !in insertedIds) {
                        categoriesToInsert.add(defaultCat)
                    }
                }
                categoryDao.insertOrReplaceCategoryEntities(categoriesToInsert)

                // 4. إدراج الميزانيات الشهرية
                val budgetsToInsert = payload.monthlyBudgets.map { dto ->
                    MonthlyBudgetEntity(
                        id = dto.id,
                        yearMonth = dto.yearMonth,
                        amount = dto.amount
                    )
                }
                if (budgetsToInsert.isNotEmpty()) {
                    budgetDao.insertBudgetEntities(budgetsToInsert)
                }

                // 5. إدراج العمليات المالية
                val transactionsToInsert = payload.transactions.map { dto ->
                    TransactionEntity(
                        id = dto.id,
                        type = TransactionType.valueOf(dto.type),
                        amount = dto.amount,
                        categoryId = dto.categoryId,
                        transactionDate = dto.transactionDate,
                        note = dto.note,
                        createdAt = if (dto.createdAt > 0) dto.createdAt else System.currentTimeMillis(),
                        updatedAt = if (dto.updatedAt > 0) dto.updatedAt else System.currentTimeMillis()
                    )
                }
                if (transactionsToInsert.isNotEmpty()) {
                    transactionDao.insertTransactionEntities(transactionsToInsert)
                }
            }
        }
    }
}
