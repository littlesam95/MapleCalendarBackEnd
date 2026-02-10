package com.sixclassguys.maplecalendar.domain.report.entity

import com.sixclassguys.maplecalendar.domain.report.enum.ReportReason
import com.sixclassguys.maplecalendar.domain.report.enum.ReportStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "reports")
class Report(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false)
    val reporterMemberId: Long, // 신고한 유저(Member) ID

    @Column(nullable = false)
    val reportedMemberId: Long,

    @Column(nullable = false)
    val reportedCharacterId: Long,

    // 신고된 채팅 메시지 ID (연관관계 대신 ID로 기록하여 메시지 삭제 시에도 에러 방지)
    @Column(nullable = false)
    val targetChatId: Long,

    // [중요] 증거 스냅샷: 신고 시점의 채팅 내용을 그대로 저장
    @Column(nullable = false, columnDefinition = "TEXT")
    val evidenceContent: String,

    // 신고 사유
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val reason: ReportReason,

    // 상세 내용 (기타 사유 등 직접 입력)
    @Column(length = 500)
    val reasonDetail: String? = null,

    // 처리 상태
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: ReportStatus = ReportStatus.RECEIVED,

    // 관리자 코멘트 (처리 결과 기록용)
    @Column(length = 1000)
    var adminComment: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)