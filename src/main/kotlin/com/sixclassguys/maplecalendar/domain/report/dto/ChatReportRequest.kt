package com.sixclassguys.maplecalendar.domain.report.dto

import com.sixclassguys.maplecalendar.domain.report.enum.ReportReason

data class ChatReportRequest(
    val chatId: Long,
    val reason: ReportReason,
    val reasonDetail: String? = null
)