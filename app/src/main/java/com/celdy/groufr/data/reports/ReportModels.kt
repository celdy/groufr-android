package com.celdy.groufr.data.reports

import com.google.gson.annotations.SerializedName

data class CreateReportRequest(
    @SerializedName("content_type") val contentType: String,
    @SerializedName("content_id") val contentId: Long,
    val reason: String,
    val comment: String? = null
)

data class ReportResponse(
    val id: Long,
    @SerializedName("content_type") val contentType: String,
    @SerializedName("content_id") val contentId: Long,
    val reason: String,
    val status: String,
    @SerializedName("created_at") val createdAt: String
)

object ReportContentType {
    const val MESSAGE = "message"
    const val EVENT = "event"
    const val POLL = "poll"
    const val USER = "user"
}

object ReportReason {
    const val SPAM = "spam"
    const val HARASSMENT = "harassment"
    const val HATE_SPEECH = "hate_speech"
    const val ILLEGAL = "illegal"
    const val VIOLENCE = "violence"
    const val INAPPROPRIATE = "inappropriate"
    const val OTHER = "other"
}
