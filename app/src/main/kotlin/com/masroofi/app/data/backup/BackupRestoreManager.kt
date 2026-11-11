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
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/**
 * واجهة مدير النسخ الاحتياطي واستعادة البيانات
 */
interface BackupManager {
    /**
     * إنشاء نص JSON كامل لجميع بيانات التطبيق
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
 * يتعامل مع قاعدة بيانات Room عبر المعاملات الذرية (withTransaction)
 * ويدعم توفير DAOs مستقلة للاختبارات المعزولة
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

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }

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

        val payload = BackupPayload(
            backupFormatVersion = 1,
            createdAt = nowStr,
            appName = "Masroofi",
            currency = "YER",
            categories = categories.map {
                CategoryBackupDto(
                    id = it.id,
                    name = it.name,
                    type = it.type.name,
                    iconKey = it.iconKey,
                    isDefault = it.isDefault,
                    isActive = it.isActive
                )
            },
            transactions = transactions.map {
                TransactionBackupDto(
                    id = it.id,
                    type = it.type.name,
                    amount = it.amount,
                    categoryId = it.categoryId,
                    transactionDate = it.transactionDate,
                    note = it.note,
                    createdAt = it.createdAt,
                    updatedAt = it.updatedAt
                )
            },
            monthlyBudgets = budgets.map {
                MonthlyBudgetBackupDto(
                    id = it.id,
                    yearMonth = it.yearMonth,
                    amount = it.amount
                )
            }
        )

        return json.encodeToString(BackupPayload.serializer(), payload)
    }

    override fun validateBackupJson(jsonString: String): Result<BackupSummary> {
        val payload = try {
            json.decodeFromString(BackupPayload.serializer(), jsonString)
        } catch (e: Exception) {
            return Result.failure(IllegalArgumentException("الملف المختار ليس بصيغة JSON صالحة أو أنه تالف."))
        }

        // 1. التحقق من إصدار النسخة الاحتياطية
        if (payload.backupFormatVersion != 1) {
            return Result.failure(
                IllegalArgumentException("إصدار النسخة الاحتياطية (${payload.backupFormatVersion}) غير متوافق مع إصدار التطبيق الحالي.")
            )
        }

        // 2. التحقق من اسم التطبيق والعملة
        if (payload.appName.isBlank() || payload.appName != "Masroofi") {
            return Result.failure(IllegalArgumentException("الملف ليس نسخة احتياطية صالحة لتطبيق مصروفي."))
        }

        if (payload.currency != "YER") {
            return Result.failure(
                IllegalArgumentException("عملة النسخة الاحتياطية (${payload.currency}) غير متوافقة مع عملة التطبيق (YER).")
            )
        }

        // 3. التحقق من سلامة التصنيفات
        for (cat in payload.categories) {
            if (cat.id.isBlank()) {
                return Result.failure(IllegalArgumentException("يوجد تصنيف بدون معرف فريد (id) في النسخة."))
            }
            if (cat.name.isBlank()) {
                return Result.failure(IllegalArgumentException("يوجد تصنيف بدون اسم في النسخة."))
            }
            try {
                TransactionType.valueOf(cat.type)
            } catch (e: Exception) {
                return Result.failure(IllegalArgumentException("نوع التصنيف '${cat.type}' غير معروف."))
            }
        }

        val availableCategoryIds = payload.categories.map { it.id }.toSet() +
                DefaultCategories.allDefaultCategories.map { it.id }.toSet()

        // 4. التحقق من سلامة العمليات المالية
        for (tx in payload.transactions) {
            if (tx.id.isBlank()) {
                return Result.failure(IllegalArgumentException("توجد عملية مالية بدون معرف فريد (id)."))
            }
            if (tx.amount <= 0) {
                return Result.failure(IllegalArgumentException("توجد عملية مالية بمبلغ غير صالح (${tx.amount}). يجب أن تكون المبالغ موجبة أكبر من صفر."))
            }
            try {
                TransactionType.valueOf(tx.type)
            } catch (e: Exception) {
                return Result.failure(IllegalArgumentException("نوع العملية '${tx.type}' غير صالح."))
            }
            try {
                LocalDate.parse(tx.transactionDate)
            } catch (e: Exception) {
                return Result.failure(IllegalArgumentException("تاريخ العملية '${tx.transactionDate}' غير صالح."))
            }
            if (tx.categoryId !in availableCategoryIds) {
                return Result.failure(
                    IllegalArgumentException("العملية '${tx.id}' تشير إلى تصنيف غير موجود (${tx.categoryId}).")
                )
            }
        }

        // 5. التحقق من سلامة الميزانيات الشهرية
        for (budget in payload.monthlyBudgets) {
            if (budget.id.isBlank()) {
                return Result.failure(IllegalArgumentException("توجد ميزانية شهرية بدون معرف فريد (id)."))
            }
            if (budget.amount < 0) {
                return Result.failure(IllegalArgumentException("مبلغ الميزانية للشهر '${budget.yearMonth}' سالب (${budget.amount})."))
            }
            try {
                YearMonth.parse(budget.yearMonth)
            } catch (e: Exception) {
                return Result.failure(IllegalArgumentException("تنسيق الشهر '${budget.yearMonth}' للميزانية غير صالح."))
            }
        }

        return Result.success(
            BackupSummary(
                createdAt = payload.createdAt,
                transactionCount = payload.transactions.size,
                categoryCount = payload.categories.size,
                budgetCount = payload.monthlyBudgets.size,
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
