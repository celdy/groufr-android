package com.celdy.groufr.data.network

import com.celdy.groufr.data.auth.LoginRequest
import com.celdy.groufr.data.auth.RefreshRequest
import com.celdy.groufr.data.auth.TokenResponse
import com.celdy.groufr.data.groups.GroupActionResponse
import com.celdy.groufr.data.groups.GroupDetailDto
import com.celdy.groufr.data.groups.GroupMembersResponse
import com.celdy.groufr.data.groups.GroupsListResponse
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
import com.celdy.groufr.data.expenses.ConfirmAllResponse
import com.celdy.groufr.data.expenses.CreateExpenseRequest
import com.celdy.groufr.data.expenses.DisputeExpenseRequest
import com.celdy.groufr.data.expenses.EventExpensesResponse
import com.celdy.groufr.data.expenses.ExpenseActionResponse
import com.celdy.groufr.data.expenses.ExpenseDetailDto
import com.celdy.groufr.data.expenses.GroupBalancesResponse
import com.celdy.groufr.data.expenses.SettleResponse
import com.celdy.groufr.data.expenses.UpdateExpenseRequest
import com.celdy.groufr.data.expenses.UserBalancesResponse
import com.celdy.groufr.data.settlements.CreateSettlementRequest
import com.celdy.groufr.data.settlements.RejectSettlementRequest
import com.celdy.groufr.data.settlements.SettlementDto
import com.celdy.groufr.data.settlements.SettlementsResponse
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

    @GET("/api/v1/groups")
    suspend fun getGroups(): GroupsListResponse

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

    @POST("/api/v1/invitations/{id}/accept")
    suspend fun acceptInvitation(
        @Path("id") id: Long
    ): InvitationAcceptResponse

    @POST("/api/v1/invitations/{id}/decline")
    suspend fun declineInvitation(
        @Path("id") id: Long
    ): InvitationDeclineResponse

    // Expenses

    @GET("/api/v1/events/{eventId}/expenses")
    suspend fun getEventExpenses(
        @Path("eventId") eventId: Long,
        @Query("status") status: String? = null,
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0
    ): EventExpensesResponse

    @GET("/api/v1/expenses/{id}")
    suspend fun getExpenseDetail(
        @Path("id") expenseId: Long
    ): ExpenseDetailDto

    @POST("/api/v1/events/{eventId}/expenses")
    suspend fun createExpense(
        @Path("eventId") eventId: Long,
        @Body request: CreateExpenseRequest
    ): ExpenseDetailDto

    @PUT("/api/v1/expenses/{id}")
    suspend fun updateExpense(
        @Path("id") expenseId: Long,
        @Body request: UpdateExpenseRequest
    ): ExpenseDetailDto

    @DELETE("/api/v1/expenses/{id}")
    suspend fun deleteExpense(
        @Path("id") expenseId: Long
    )

    @POST("/api/v1/expenses/{id}/confirm")
    suspend fun confirmExpense(
        @Path("id") expenseId: Long
    ): ExpenseActionResponse

    @POST("/api/v1/events/{eventId}/expenses/confirm-all")
    suspend fun confirmAllExpenses(
        @Path("eventId") eventId: Long
    ): ConfirmAllResponse

    @POST("/api/v1/expenses/{id}/dispute")
    suspend fun disputeExpense(
        @Path("id") expenseId: Long,
        @Body request: DisputeExpenseRequest
    ): ExpenseActionResponse

    @POST("/api/v1/expenses/{id}/settle")
    suspend fun settleExpense(
        @Path("id") expenseId: Long
    ): SettleResponse

    // Balances

    @GET("/api/v1/groups/{groupId}/balances")
    suspend fun getGroupBalances(
        @Path("groupId") groupId: Long
    ): GroupBalancesResponse

    @GET("/api/v1/user/balances")
    suspend fun getUserBalances(): UserBalancesResponse

    // Settlements

    @POST("/api/v1/groups/{groupId}/settlements")
    suspend fun createSettlement(
        @Path("groupId") groupId: Long,
        @Body request: CreateSettlementRequest
    ): SettlementDto

    @GET("/api/v1/groups/{groupId}/settlements")
    suspend fun getGroupSettlements(
        @Path("groupId") groupId: Long,
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0
    ): SettlementsResponse

    @GET("/api/v1/settlements/{id}")
    suspend fun getSettlementDetail(
        @Path("id") settlementId: Long
    ): SettlementDto

    @POST("/api/v1/settlements/{id}/confirm")
    suspend fun confirmSettlement(
        @Path("id") settlementId: Long
    ): SettlementDto

    @POST("/api/v1/settlements/{id}/reject")
    suspend fun rejectSettlement(
        @Path("id") settlementId: Long,
        @Body request: RejectSettlementRequest
    ): SettlementDto

    @POST("/api/v1/settlements/{id}/cancel")
    suspend fun cancelSettlement(
        @Path("id") settlementId: Long
    ): SettlementDto
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
