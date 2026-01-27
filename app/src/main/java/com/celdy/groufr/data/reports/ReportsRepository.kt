package com.celdy.groufr.data.reports

import com.celdy.groufr.data.network.ApiService
import javax.inject.Inject

class ReportsRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun createReport(
        contentType: String,
        contentId: Long,
        reason: String,
        comment: String? = null
    ): ReportResponse {
        return apiService.createReport(
            CreateReportRequest(
                contentType = contentType,
                contentId = contentId,
                reason = reason,
                comment = comment
            )
        )
    }
}
