package com.celdy.groufr.data.expenses

import com.google.gson.annotations.SerializedName

data class ExpenseUserRef(
    val id: Long,
    val name: String
)

data class ExpenseShareDto(
    val user: ExpenseUserRef,
    @SerializedName("share_cents")
    val shareCents: Long,
    @SerializedName("confirmation_status")
    val confirmationStatus: String
)

data class ExpenseShareDetailDto(
    val user: ExpenseUserRef,
    @SerializedName("share_cents")
    val shareCents: Long,
    @SerializedName("split_type_value")
    val splitTypeValue: Double?,
    @SerializedName("confirmation_status")
    val confirmationStatus: String,
    @SerializedName("dispute_reason")
    val disputeReason: String?,
    @SerializedName("confirmed_at")
    val confirmedAt: String?
)

data class ExpenseDto(
    val id: Long,
    @SerializedName("event_id")
    val eventId: Long,
    val payer: ExpenseUserRef,
    @SerializedName("created_by")
    val createdBy: ExpenseUserRef?,
    val label: String,
    @SerializedName("amount_cents")
    val amountCents: Long,
    val currency: String,
    @SerializedName("split_type")
    val splitType: String,
    val status: String,
    val shares: List<ExpenseShareDto>,
    @SerializedName("created_at")
    val createdAt: String
)

data class ExpenseDetailDto(
    val id: Long,
    @SerializedName("event_id")
    val eventId: Long,
    val payer: ExpenseUserRef,
    @SerializedName("created_by")
    val createdBy: ExpenseUserRef?,
    val label: String,
    @SerializedName("amount_cents")
    val amountCents: Long,
    val currency: String,
    @SerializedName("split_type")
    val splitType: String,
    val status: String,
    val shares: List<ExpenseShareDetailDto>,
    val note: String?,
    @SerializedName("due_type")
    val dueType: String?,
    @SerializedName("due_date")
    val dueDate: String?,
    @SerializedName("resolved_due_date")
    val resolvedDueDate: String?,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    val updatedAt: String?
)

data class UserBalanceDto(
    val user: ExpenseUserRef,
    @SerializedName("balance_cents")
    val balanceCents: Long,
    val currency: String
)

data class SuggestedSettlementDto(
    val from: ExpenseUserRef,
    val to: ExpenseUserRef,
    @SerializedName("amount_cents")
    val amountCents: Long
)

data class EventExpensesResponse(
    val expenses: List<ExpenseDto>,
    val balances: List<UserBalanceDto>,
    val settlements: List<SuggestedSettlementDto>,
    @SerializedName("total_cents")
    val totalCents: Long,
    val currency: String?,
    val meta: ExpensesMeta?
)

data class ExpensesMeta(
    val total: Int,
    val limit: Int,
    val offset: Int
)

data class ExpenseActionResponse(
    val success: Boolean,
    val expense: ExpenseDto?
)

data class ConfirmAllResponse(
    @SerializedName("confirmed_count")
    val confirmedCount: Int,
    @SerializedName("skipped_count")
    val skippedCount: Int,
    val expenses: List<ExpenseDto>
)

data class SettleResponse(
    val success: Boolean,
    val status: String
)

data class ExpenseSplitInput(
    @SerializedName("user_id")
    val userId: Long,
    val value: Double? = null
)

data class CreateExpenseRequest(
    val label: String,
    @SerializedName("amount_cents")
    val amountCents: Long,
    val currency: String,
    @SerializedName("payer_id")
    val payerId: Long?,
    @SerializedName("split_type")
    val splitType: String,
    val splits: List<ExpenseSplitInput>,
    @SerializedName("due_type")
    val dueType: String? = null,
    @SerializedName("due_date")
    val dueDate: String? = null,
    val note: String? = null
)

data class UpdateExpenseRequest(
    val label: String? = null,
    @SerializedName("amount_cents")
    val amountCents: Long? = null,
    val currency: String? = null,
    @SerializedName("split_type")
    val splitType: String? = null,
    val splits: List<ExpenseSplitInput>? = null,
    @SerializedName("due_type")
    val dueType: String? = null,
    @SerializedName("due_date")
    val dueDate: String? = null,
    val note: String? = null
)

data class DisputeExpenseRequest(
    val reason: String
)

// Balance DTOs

data class GroupBalancesResponse(
    val balances: List<UserBalanceDto>,
    val settlements: List<SuggestedSettlementDto>
)

data class UserBalancesResponse(
    val summary: List<UserBalanceSummaryDto>,
    val debts: UserDebtsDto?
)

data class UserBalanceSummaryDto(
    @SerializedName("group_id")
    val groupId: Long,
    @SerializedName("group_name")
    val groupName: String,
    @SerializedName("balance_cents")
    val balanceCents: Long,
    val currency: String
)

data class UserDebtsDto(
    val groups: List<UserDebtGroupDto>,
    val totals: List<UserDebtTotalDto>
)

data class UserDebtGroupDto(
    @SerializedName("group_id")
    val groupId: Long,
    @SerializedName("group_name")
    val groupName: String,
    val settlements: List<SuggestedSettlementDto>
)

data class UserDebtTotalDto(
    val user: ExpenseUserRef,
    @SerializedName("net_cents")
    val netCents: Long
)
