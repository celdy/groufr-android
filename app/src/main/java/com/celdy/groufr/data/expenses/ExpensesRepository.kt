package com.celdy.groufr.data.expenses

import com.celdy.groufr.data.local.ExpenseDao
import com.celdy.groufr.data.local.toDto
import com.celdy.groufr.data.local.toEntity
import com.celdy.groufr.data.network.ApiService
import javax.inject.Inject

class ExpensesRepository @Inject constructor(
    private val apiService: ApiService,
    private val expenseDao: ExpenseDao
) {
    suspend fun loadEventExpenses(eventId: Long): EventExpensesResponse {
        return try {
            val response = apiService.getEventExpenses(eventId)
            expenseDao.replaceAllByEvent(eventId, response.expenses.map { it.toEntity() })
            response
        } catch (exception: Exception) {
            val cached = expenseDao.getByEvent(eventId)
            if (cached.isNotEmpty()) {
                EventExpensesResponse(
                    expenses = cached.map { it.toDto() },
                    balances = emptyList(),
                    settlements = emptyList(),
                    totalCents = 0,
                    currency = null,
                    meta = null
                )
            } else {
                throw exception
            }
        }
    }

    suspend fun getExpenseDetail(expenseId: Long): ExpenseDetailDto {
        return apiService.getExpenseDetail(expenseId)
    }

    suspend fun createExpense(eventId: Long, request: CreateExpenseRequest): ExpenseDetailDto {
        val detail = apiService.createExpense(eventId, request)
        expenseDao.upsert(detail.toEntity())
        return detail
    }

    suspend fun updateExpense(expenseId: Long, request: UpdateExpenseRequest): ExpenseDetailDto {
        val detail = apiService.updateExpense(expenseId, request)
        expenseDao.upsert(detail.toEntity())
        return detail
    }

    suspend fun deleteExpense(expenseId: Long) {
        apiService.deleteExpense(expenseId)
        expenseDao.deleteById(expenseId)
    }

    suspend fun confirmExpense(expenseId: Long): ExpenseActionResponse {
        val response = apiService.confirmExpense(expenseId)
        response.expense?.let { expenseDao.upsert(it.toEntity()) }
        return response
    }

    suspend fun confirmAllExpenses(eventId: Long): ConfirmAllResponse {
        val response = apiService.confirmAllExpenses(eventId)
        expenseDao.replaceAllByEvent(eventId, response.expenses.map { it.toEntity() })
        return response
    }

    suspend fun disputeExpense(expenseId: Long, reason: String): ExpenseActionResponse {
        val response = apiService.disputeExpense(expenseId, DisputeExpenseRequest(reason))
        response.expense?.let { expenseDao.upsert(it.toEntity()) }
        return response
    }

    suspend fun settleExpense(expenseId: Long): SettleResponse {
        return apiService.settleExpense(expenseId)
    }

    suspend fun getGroupBalances(groupId: Long): GroupBalancesResponse {
        return apiService.getGroupBalances(groupId)
    }

    suspend fun getUserBalances(): UserBalancesResponse {
        return apiService.getUserBalances()
    }
}
