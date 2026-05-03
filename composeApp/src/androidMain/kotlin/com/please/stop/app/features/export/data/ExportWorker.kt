package com.please.stop.app.features.export.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.please.stop.app.core.IFcmTokenProvider
import com.please.stop.app.core.db.dao.CategoryDao
import com.please.stop.app.core.db.dao.ExpenseDao
import com.please.stop.app.core.db.dao.ExportHistoryDao
import com.please.stop.app.core.db.dao.SubcategoryDao
import com.please.stop.app.core.db.dao.UserProfileDao
import com.please.stop.app.core.db.entity.ExportStatus
import com.please.stop.app.features.expenses.data.remote.FirebaseCallableFunctions
import com.please.stop.app.utils.date.now
import kotlinx.coroutines.flow.first

internal class ExportWorker(
    context: Context,
    params: WorkerParameters,
    private val expenseDao: ExpenseDao,
    private val categoryDao: CategoryDao,
    private val subcategoryDao: SubcategoryDao,
    private val userProfileDao: UserProfileDao,
    private val callableFunctions: FirebaseCallableFunctions,
    private val fcmTokenProvider: IFcmTokenProvider,
    private val exportHistoryDao: ExportHistoryDao,
) : CoroutineWorker(context, params) {

    @Suppress("TooGenericExceptionCaught")
    override suspend fun doWork(): Result {
        val accessToken = inputData.getString(KEY_ACCESS_TOKEN) ?: return Result.failure()
        val startDate = inputData.getLong(KEY_START_DATE, 0)
        val endDate = inputData.getLong(KEY_END_DATE, 0)
        val exportId = inputData.getLong(KEY_EXPORT_ID, 0)
        val tabLayout = inputData.getString(KEY_TAB_LAYOUT) ?: "single_tab"

        return try {
            val expenses = expenseDao.getExpensesInRange(startDate, endDate)
            val allCategories = categoryDao.observeAllIncludingArchived().first()
            val allSubcategories = subcategoryDao.observeAllIncludingArchived().first()
            val userProfile = userProfileDao.get()

            val categoryMap = allCategories.associateBy { it.id }
            val subcategoryMap = allSubcategories.associateBy { it.id }

            val expensesList = expenses.map { expense ->
                mapOf<String, Any?>(
                    "date" to expense.dateEpochMillis,
                    "title" to expense.title,
                    "category" to (categoryMap[expense.categoryId]?.name.orEmpty()),
                    "subcategory" to (subcategoryMap[expense.subcategoryId]?.name.orEmpty()),
                    "amount" to expense.amountMinorUnits.toString(),
                    "notes" to (expense.notes.orEmpty()),
                    "originalAmount" to expense.originalAmountMinorUnits?.toString(),
                    "originalCurrency" to expense.originalCurrencyCode,
                )
            }

            val fcmToken = fcmTokenProvider.getToken()

            val payload = mapOf(
                "googleAccessToken" to accessToken,
                "fcmToken" to fcmToken,
                "tabLayout" to tabLayout,
                "currencySymbol" to (userProfile?.currencySymbol.orEmpty()),
                "decimalPlaces" to (userProfile?.decimalPlaces ?: 2),
                "compressed" to false,
                "expenses" to expensesList,
            )

            val response = callableFunctions.call("exportToSheets", payload)
            response.fold(
                onSuccess = { data ->
                    val url = data["spreadsheetUrl"] as? String
                    exportHistoryDao.updateResult(
                        id = exportId,
                        status = ExportStatus.SUCCESS,
                        url = url,
                        completedAt = now().toEpochMilliseconds(),
                    )
                    Result.success()
                },
                onFailure = { error ->
                    exportHistoryDao.updateError(
                        id = exportId,
                        status = ExportStatus.FAILED,
                        error = error.message,
                        completedAt = now().toEpochMilliseconds(),
                    )
                    Result.failure()
                },
            )
        } catch (e: Exception) {
            exportHistoryDao.updateError(
                id = exportId,
                status = ExportStatus.FAILED,
                error = e.message,
                completedAt = now().toEpochMilliseconds(),
            )
            Result.failure()
        }
    }

    companion object {
        const val KEY_ACCESS_TOKEN = "accessToken"
        const val KEY_START_DATE = "startDate"
        const val KEY_END_DATE = "endDate"
        const val KEY_EXPORT_ID = "exportId"
        const val KEY_TAB_LAYOUT = "tabLayout"
    }
}
