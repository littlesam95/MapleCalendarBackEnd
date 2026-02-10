package com.sixclassguys.maplecalendar.domain.report.repository

import com.sixclassguys.maplecalendar.domain.report.entity.Report
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ReportRepository : JpaRepository<Report, Long> {

    // 중복 신고 확인용: 동일한 신고자가 동일한 메시지를 이미 신고했는지 확인
    fun existsByReporterMemberIdAndTargetChatId(reporterId: Long, chatId: Long): Boolean
}