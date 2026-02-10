package com.sixclassguys.maplecalendar.domain.report.enum

enum class ReportStatus {

    RECEIVED,   // 접수됨
    UNDER_REVIEW, // 검토 중
    RESOLVED,   // 처리 완료 (삭제/차단 등)
    REJECTED    // 반려 (신고 사유 부적합)
}