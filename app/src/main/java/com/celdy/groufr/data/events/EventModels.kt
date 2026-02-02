package com.celdy.groufr.data.events

import com.google.gson.annotations.SerializedName

data class EventsResponse(
    val events: List<EventDto>,
    val meta: EventsMeta? = null
)

data class EventsMeta(
    val total: Int,
    val limit: Int,
    val offset: Int
)

data class EventDto(
    val id: Long,
    @SerializedName("group_id")
    val groupId: Long,
    val group: EventGroupRef? = null,
    val title: String,
    val description: String?,
    val place: String?,
    val state: String,
    @SerializedName("start_at")
    val startAt: String?,
    @SerializedName("end_at")
    val endAt: String?,
    @SerializedName("deadline_join_at")
    val deadlineJoinAt: String?,
    @SerializedName("your_status")
    val yourStatus: String,
    val participants: Map<String, Int>,
    @SerializedName("created_at")
    val createdAt: String
)

data class EventGroupRef(
    val id: Long,
    val name: String,
    val slug: String
)

data class EventDetailDto(
    val id: Long,
    @SerializedName("group_id")
    val groupId: Long,
    @SerializedName("group_name")
    val groupName: String?,
    val title: String,
    val description: String?,
    val place: String?,
    val state: String,
    @SerializedName("start_at")
    val startAt: String?,
    @SerializedName("end_at")
    val endAt: String?,
    @SerializedName("deadline_join_at")
    val deadlineJoinAt: String?,
    @SerializedName("min_participants")
    val minParticipants: Int? = null,
    @SerializedName("max_participants")
    val maxParticipants: Int? = null,
    @SerializedName("your_status")
    val yourStatus: String,
    @SerializedName("your_role")
    val yourRole: String? = null,
    val participants: Map<String, Int>,
    @SerializedName("participants_list")
    val participantsList: List<EventParticipantDto>,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    val updatedAt: String?
)

data class EventParticipantDto(
    val user: EventUserDto,
    val status: String,
    val role: String
)

data class EventUserDto(
    val id: Long,
    val name: String
)

data class EventActionResponse(
    val success: Boolean,
    @SerializedName("your_status")
    val yourStatus: String
)

data class CreateEventRequest(
    val title: String,
    val description: String?,
    val place: String?,
    @SerializedName("start_at")
    val startAt: String? = null,
    @SerializedName("end_at")
    val endAt: String?,
    @SerializedName("deadline_join_at")
    val deadlineJoinAt: String?,
    @SerializedName("min_participants")
    val minParticipants: Int? = null,
    @SerializedName("max_participants")
    val maxParticipants: Int? = null,
    val state: String? = null
)

data class UpdateEventRequest(
    val title: String? = null,
    val description: String? = null,
    val place: String? = null,
    @SerializedName("start_at")
    val startAt: String? = null,
    @SerializedName("end_at")
    val endAt: String? = null,
    @SerializedName("deadline_join_at")
    val deadlineJoinAt: String? = null,
    @SerializedName("min_participants")
    val minParticipants: Int? = null,
    @SerializedName("max_participants")
    val maxParticipants: Int? = null,
    val state: String? = null
)
