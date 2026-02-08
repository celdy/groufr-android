package com.celdy.groufr.data.network

import com.celdy.groufr.data.auth.LoginRequest
import com.celdy.groufr.data.auth.RefreshRequest
import com.celdy.groufr.data.auth.TokenResponse
import com.celdy.groufr.data.groups.GroupActionResponse
import com.celdy.groufr.data.groups.GroupDetailDto
import com.celdy.groufr.data.groups.GroupMembersResponse
import com.celdy.groufr.data.messages.MessagesResponse
import com.celdy.groufr.data.messages.SendMessageRequest
import com.celdy.groufr.data.messages.MessageDto
import com.celdy.groufr.data.sync.SyncResponse
import com.celdy.groufr.data.events.CreateEventRequest
import com.celdy.groufr.data.events.EventActionResponse
import com.celdy.groufr.data.events.EventDetailDto
import com.celdy.groufr.data.events.EventDto
import com.celdy.groufr.data.events.EventsResponse
import com.celdy.groufr.data.events.UpdateEventRequest
import com.celdy.groufr.data.polls.PollDto
import com.celdy.groufr.data.polls.PollsResponse
import com.celdy.groufr.data.polls.VoteRequest
import com.celdy.groufr.data.polls.CreatePollRequest
import com.celdy.groufr.data.notifications.NotificationsResponse
import com.celdy.groufr.data.notifications.MarkReadResponse
import com.celdy.groufr.data.notifications.NotificationMarkReadRequest
import com.celdy.groufr.data.notifications.NotificationCountResponse
import com.celdy.groufr.data.reactions.ReactionDetail
import com.celdy.groufr.data.reactions.ReactionRequest
import com.celdy.groufr.data.reactions.ReactionSummary
import com.celdy.groufr.data.reports.CreateReportRequest
import com.celdy.groufr.data.reports.ReportResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    @POST("/api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): TokenResponse

    @POST("/api/v1/auth/refresh")
    suspend fun refresh(@Body request: RefreshRequest): TokenResponse

    @GET("/api/v1/sync")
    suspend fun sync(
        @Query("since") since: String? = null,
        @Query("include") include: String? = null
    ): SyncResponse

    @GET("/api/v1/groups/{groupId}/messages")
    suspend fun getGroupMessages(
        @Path("groupId") groupId: Long,
        @Query("limit") limit: Int = 50,
        @Query("before_id") beforeId: Long? = null,
        @Query("after_id") afterId: Long? = null
    ): MessagesResponse

    @GET("/api/v1/events/{eventId}/messages")
    suspend fun getEventMessages(
        @Path("eventId") eventId: Long,
        @Query("limit") limit: Int = 50,
        @Query("before_id") beforeId: Long? = null,
        @Query("after_id") afterId: Long? = null
    ): MessagesResponse

    @POST("/api/v1/groups/{groupId}/messages")
    suspend fun sendGroupMessage(
        @Path("groupId") groupId: Long,
        @Body request: SendMessageRequest
    ): MessageDto

    @POST("/api/v1/events/{eventId}/messages")
    suspend fun sendEventMessage(
        @Path("eventId") eventId: Long,
        @Body request: SendMessageRequest
    ): MessageDto

    @GET("/api/v1/groups/{groupId}/polls")
    suspend fun getGroupPolls(
        @Path("groupId") groupId: Long,
        @Query("status") status: String = "all",
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0
    ): PollsResponse

    @GET("/api/v1/polls/{pollId}")
    suspend fun getPollDetail(
        @Path("pollId") pollId: Long
    ): PollDto

    @POST("/api/v1/polls/{pollId}/vote")
    suspend fun voteOnPoll(
        @Path("pollId") pollId: Long,
        @Body request: VoteRequest
    ): PollDto

    @DELETE("/api/v1/polls/{pollId}/vote")
    suspend fun clearPollVote(
        @Path("pollId") pollId: Long
    ): PollDto

    @POST("/api/v1/groups/{groupId}/polls")
    suspend fun createPoll(
        @Path("groupId") groupId: Long,
        @Body request: CreatePollRequest
    ): PollDto

    @GET("/api/v1/groups/{groupId}/events")
    suspend fun getGroupEvents(
        @Path("groupId") groupId: Long,
        @Query("filter") filter: String = "upcoming",
        @Query("participation") participation: String? = null,
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0
    ): EventsResponse

    @GET("/api/v1/events")
    suspend fun getAllEvents(
        @Query("time") time: String = "upcoming",
        @Query("participation") participation: String? = null,
        @Query("state") state: String? = null,
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0
    ): EventsResponse

    @GET("/api/v1/events/{eventId}")
    suspend fun getEventDetail(
        @Path("eventId") eventId: Long
    ): EventDetailDto

    @POST("/api/v1/events/{eventId}/join")
    suspend fun joinEvent(
        @Path("eventId") eventId: Long
    ): EventActionResponse

    @POST("/api/v1/events/{eventId}/decline")
    suspend fun declineEvent(
        @Path("eventId") eventId: Long
    ): EventActionResponse

    @POST("/api/v1/events/{eventId}/maybe")
    suspend fun maybeEvent(
        @Path("eventId") eventId: Long
    ): EventActionResponse

    @PUT("/api/v1/events/{eventId}")
    suspend fun updateEvent(
        @Path("eventId") eventId: Long,
        @Body request: UpdateEventRequest
    ): EventDetailDto

    @POST("/api/v1/groups/{groupId}/events")
    suspend fun createEvent(
        @Path("groupId") groupId: Long,
        @Body request: CreateEventRequest
    ): EventDto

    @GET("/api/v1/notifications")
    suspend fun getNotifications(
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0,
        @Query("unread_only") unreadOnly: Boolean = false,
        @Query("group_id") groupId: Long? = null
    ): NotificationsResponse

    @GET("/api/v1/notifications/count")
    suspend fun getNotificationCount(): NotificationCountResponse

    @POST("/api/v1/notifications/{id}/read")
    suspend fun markNotificationRead(
        @Path("id") notificationId: Long
    )

    @POST("/api/v1/notifications/mark-read")
    suspend fun markNotificationsRead(
        @Body request: NotificationMarkReadRequest
    ): MarkReadResponse

    @POST("/api/v1/notifications/read-all")
    suspend fun markAllNotificationsRead(
        @Query("group_id") groupId: Long? = null
    ): MarkReadResponse

    @POST("/api/v1/reports")
    suspend fun createReport(
        @Body request: CreateReportRequest
    ): ReportResponse

    @POST("/api/v1/reactions")
    suspend fun toggleReaction(
        @Body request: ReactionRequest
    ): ReactionSummary

    @GET("/api/v1/reactions/{contentType}/{contentId}")
    suspend fun getReactions(
        @Path("contentType") contentType: String,
        @Path("contentId") contentId: Long
    ): ReactionDetail

    @DELETE("/api/v1/reactions/{contentType}/{contentId}")
    suspend fun clearReaction(
        @Path("contentType") contentType: String,
        @Path("contentId") contentId: Long
    ): ReactionSummary

    @GET("/api/v1/groups/{slug}")
    suspend fun getGroupDetail(
        @Path("slug") slug: String
    ): GroupDetailDto

    @GET("/api/v1/groups/{groupId}/members")
    suspend fun getGroupMembers(
        @Path("groupId") groupId: Long
    ): GroupMembersResponse

    @POST("/api/v1/groups/{groupId}/leave")
    suspend fun leaveGroup(
        @Path("groupId") groupId: Long
    ): GroupActionResponse

    @DELETE("/api/v1/groups/{groupId}")
    suspend fun deleteGroup(
        @Path("groupId") groupId: Long
    ): GroupActionResponse

    @POST("/api/v1/invitations/{token}/accept")
    suspend fun acceptInvitation(
        @Path("token") token: String
    ): InvitationAcceptResponse

    @POST("/api/v1/invitations/{token}/decline")
    suspend fun declineInvitation(
        @Path("token") token: String
    ): InvitationDeclineResponse
}

data class InvitationAcceptResponse(
    val success: Boolean,
    val group: InvitationGroupInfo?
)

data class InvitationDeclineResponse(
    val success: Boolean
)

data class InvitationGroupInfo(
    val id: Long,
    val slug: String,
    val name: String
)
