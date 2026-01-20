package com.celdy.groufr.data.sync

import com.celdy.groufr.data.groups.GroupDto
import com.google.gson.annotations.SerializedName

data class SyncResponse(
    @SerializedName("sync_timestamp")
    val syncTimestamp: String?,
    val updates: SyncUpdates?
)

data class SyncUpdates(
    val groups: List<GroupDto>? = emptyList()
)
