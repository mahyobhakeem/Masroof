package com.masroofi.app

import android.app.Application
import android.content.Context
import com.masroofi.app.data.database.MasroofiDatabase
import com.masroofi.app.data.preferences.PreferencesManager
import com.masroofi.app.data.repository.CategoryRepositoryImpl
import com.masroofi.app.data.repository.MonthlyBudgetRepositoryImpl
import com.masroofi.app.data.repository.TransactionRepositoryImpl
import com.masroofi.app.domain.repository.CategoryRepository
import com.masroofi.app.domain.repository.MonthlyBudgetRepository
import com.masroofi.app.domain.repository.TransactionRepository
import com.masroofi.app.domain.usecase.GetMonthlyFinancialSummaryUseCase
import com.masroofi.app.domain.usecase.SetMonthlyBudgetUseCase

/**
 * واجهة حاوية الاعتماديات المركزية لتطبيق مصروفي (AppContainer)
 * تضمن معمارية حقن اعتماديات يدوية نظيفة وخفيفة الوزن دون الحاجة لمكتبات معقدة
 */
interface AppContainer {
    val database: MasroofiDatabase
    val categoryRepository: CategoryRepository
    val transactionRepository: TransactionRepository
    val monthlyBudgetRepository: MonthlyBudgetRepository
    val preferencesManager: PreferencesManager
    val getMonthlyFinancialSummaryUseCase: GetMonthlyFinancialSummaryUseCase
    val setMonthlyBudgetUseCase: SetMonthlyBudgetUseCase
}

/**
 * التنفيذ الافتراضي لحاوية الاعتماديات المركزية
 */
class DefaultAppContainer(private val context: Context) : AppContainer {

    override val database: MasroofiDatabase by lazy {
        MasroofiDatabase.getDatabase(context)
    }

    override val categoryRepository: CategoryRepository by lazy {
        CategoryRepositoryImpl(database.categoryDao())
    }

    override val transactionRepository: TransactionRepository by lazy {
        TransactionRepositoryImpl(database.transactionDao())
    }

    override val monthlyBudgetRepository: MonthlyBudgetRepository by lazy {
        MonthlyBudgetRepositoryImpl(database.monthlyBudgetDao())
    }

    override val preferencesManager: PreferencesManager by lazy {
        PreferencesManager(context)
    }

    override val getMonthlyFinancialSummaryUseCase: GetMonthlyFinancialSummaryUseCase by lazy {
        GetMonthlyFinancialSummaryUseCase(transactionRepository, monthlyBudgetRepository)
    }

    override val setMonthlyBudgetUseCase: SetMonthlyBudgetUseCase by lazy {
        SetMonthlyBudgetUseCase(monthlyBudgetRepository)
    }
}

/**
 * الفئة المركزية للتطبيق المسجلة في ملف البيان AndroidManifest.xml
 */
class MasroofiApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(applicationContext)
    }
}
