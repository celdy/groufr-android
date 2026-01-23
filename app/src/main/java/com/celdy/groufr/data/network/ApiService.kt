package com.celdy.groufr.data.network

import com.celdy.groufr.data.auth.LoginRequest
import com.celdy.groufr.data.auth.RefreshRequest
import com.celdy.groufr.data.auth.TokenResponse
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
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): TokenResponse

    @POST("auth/refresh")
    suspend fun refresh(@Body request: RefreshRequest): TokenResponse

    @GET("sync")
    suspend fun sync(
        @Query("since") since: String? = null,
        @Query("include") include: String? = null
    ): SyncResponse

    @GET("groups/{groupId}/messages")
    suspend fun getGroupMessages(
        @Path("groupId") groupId: Long,
        @Query("limit") limit: Int = 50,
        @Query("before_id") beforeId: Long? = null,
        @Query("after_id") afterId: Long? = null
    ): MessagesResponse

    @GET("events/{eventId}/messages")
    suspend fun getEventMessages(
        @Path("eventId") eventId: Long,
        @Query("limit") limit: Int = 50,
        @Query("before_id") beforeId: Long? = null,
        @Query("after_id") afterId: Long? = null
    ): MessagesResponse

    @POST("groups/{groupId}/messages")
    suspend fun sendGroupMessage(
        @Path("groupId") groupId: Long,
        @Body request: SendMessageRequest
    ): MessageDto

    @POST("events/{eventId}/messages")
    suspend fun sendEventMessage(
        @Path("eventId") eventId: Long,
        @Body request: SendMessageRequest
    ): MessageDto

    @GET("groups/{groupId}/polls")
    suspend fun getGroupPolls(
        @Path("groupId") groupId: Long,
        @Query("status") status: String = "all",
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0
    ): PollsResponse

    @POST("polls/{pollId}/vote")
    suspend fun voteOnPoll(
        @Path("pollId") pollId: Long,
        @Body request: VoteRequest
    ): PollDto

    @DELETE("polls/{pollId}/vote")
    suspend fun clearPollVote(
        @Path("pollId") pollId: Long
    ): PollDto

    @POST("groups/{groupId}/polls")
    suspend fun createPoll(
        @Path("groupId") groupId: Long,
        @Body request: CreatePollRequest
    ): PollDto

    @GET("groups/{groupId}/events")
    suspend fun getGroupEvents(
        @Path("groupId") groupId: Long,
        @Query("filter") filter: String = "upcoming",
        @Query("participation") participation: String? = null,
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0
    ): EventsResponse

    @GET("events")
    suspend fun getAllEvents(
        @Query("time") time: String = "upcoming",
        @Query("participation") participation: String? = null,
        @Query("state") state: String? = null,
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0
    ): EventsResponse

    @GET("events/{eventId}")
    suspend fun getEventDetail(
        @Path("eventId") eventId: Long
    ): EventDetailDto

    @POST("events/{eventId}/join")
    suspend fun joinEvent(
        @Path("eventId") eventId: Long
    ): EventActionResponse

    @POST("events/{eventId}/decline")
    suspend fun declineEvent(
        @Path("eventId") eventId: Long
    ): EventActionResponse

    @POST("events/{eventId}/maybe")
    suspend fun maybeEvent(
        @Path("eventId") eventId: Long
    ): EventActionResponse

    @PUT("events/{eventId}")
    suspend fun updateEvent(
        @Path("eventId") eventId: Long,
        @Body request: UpdateEventRequest
    ): EventDetailDto

    @POST("groups/{groupId}/events")
    suspend fun createEvent(
        @Path("groupId") groupId: Long,
        @Body request: CreateEventRequest
    ): EventDto

    @GET("notifications")
    suspend fun getNotifications(
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0,
        @Query("unread_only") unreadOnly: Boolean = false,
        @Query("group_id") groupId: Long? = null
    ): NotificationsResponse

    @GET("notifications/count")
    suspend fun getNotificationCount(): NotificationCountResponse

    @POST("notifications/{id}/read")
    suspend fun markNotificationRead(
        @Path("id") notificationId: Long
    )

    @POST("notifications/mark-read")
    suspend fun markNotificationsRead(
        @Body request: NotificationMarkReadRequest
    ): MarkReadResponse

    @POST("notifications/read-all")
    suspend fun markAllNotificationsRead(
        @Query("group_id") groupId: Long? = null
    ): MarkReadResponse
}
