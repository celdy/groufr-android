package com.celdy.groufr.data.sync

import com.celdy.groufr.data.expenses.ExpenseDto
import com.celdy.groufr.data.groups.GroupDto
import com.celdy.groufr.data.settlements.SettlementDto
import com.google.gson.annotations.SerializedName

data class SyncResponse(
    @SerializedName("sync_timestamp")
    val syncTimestamp: String?,
    val updates: SyncUpdates?
)

data class SyncUpdates(
    val groups: List<GroupDto>? = emptyList(),
    val messages: SyncMessages? = null,
    val polls: SyncPolls? = null,
    val events: SyncEvents? = null,
    val notifications: SyncNotifications? = null,
    val expenses: SyncExpenses? = null,
    val settlements: SyncSettlements? = null
)

data class SyncMessages(
    val created: List<Any>? = emptyList(),
    val deleted: List<Long>? = emptyList(),
    @SerializedName("has_more")
    val hasMore: Boolean = false
)

data class SyncPolls(
    val created: List<Any>? = emptyList(),
    val updated: List<Any>? = emptyList(),
    val closed: List<Long>? = emptyList(),
    @SerializedName("has_more")
    val hasMore: Boolean = false
)

data class SyncEvents(
    val created: List<Any>? = emptyList(),
    val updated: List<Any>? = emptyList(),
    val deleted: List<Long>? = emptyList(),
    @SerializedName("has_more")
    val hasMore: Boolean = false
)

data class SyncNotifications(
    @SerializedName("new_count")
    val newCount: Int = 0
)

data class SyncExpenses(
    val created: List<ExpenseDto>? = emptyList(),
    val updated: List<ExpenseDto>? = emptyList(),
    val settled: List<Long>? = emptyList(),
    val deleted: List<Long>? = emptyList(),
    @SerializedName("has_more")
    val hasMore: Boolean = false
)

data class SyncSettlements(
    val created: List<SettlementDto>? = emptyList(),
    val updated: List<SettlementDto>? = emptyList(),
    val deleted: List<Long>? = emptyList(),
    @SerializedName("pending_for_you")
    val pendingForYou: Int = 0,
    @SerializedName("has_more")
    val hasMore: Boolean = false
)
