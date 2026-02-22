package com.celdy.groufr.data.settlements

import com.celdy.groufr.data.expenses.ExpenseUserRef
import com.google.gson.annotations.SerializedName

data class SettlementDto(
    val id: Long,
    @SerializedName("group_id")
    val groupId: Long,
    val payer: ExpenseUserRef,
    val recipient: ExpenseUserRef,
    @SerializedName("amount_cents")
    val amountCents: Long,
    val currency: String,
    val note: String?,
    val status: String,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("confirmed_at")
    val confirmedAt: String?,
    @SerializedName("rejected_at")
    val rejectedAt: String?
)

data class SettlementsResponse(
    val settlements: List<SettlementDto>,
    val meta: SettlementsMeta?
)

data class SettlementsMeta(
    val total: Int,
    val limit: Int,
    val offset: Int
)

data class CreateSettlementRequest(
    @SerializedName("recipient_id")
    val recipientId: Long,
    @SerializedName("amount_cents")
    val amountCents: Long,
    val currency: String,
    val note: String? = null
)

data class RejectSettlementRequest(
    val reason: String? = null
)
