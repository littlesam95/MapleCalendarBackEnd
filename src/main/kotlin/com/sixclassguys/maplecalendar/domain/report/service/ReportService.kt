package com.sixclassguys.maplecalendar.domain.report.service

import com.sixclassguys.maplecalendar.domain.boss.repository.BossPartyChatMessageRepository
import com.sixclassguys.maplecalendar.domain.character.repository.MapleCharacterRepository
import com.sixclassguys.maplecalendar.domain.member.repository.MemberRepository
import com.sixclassguys.maplecalendar.domain.report.entity.Report
import com.sixclassguys.maplecalendar.domain.report.enum.ReportReason
import com.sixclassguys.maplecalendar.domain.report.repository.ReportRepository
import com.sixclassguys.maplecalendar.global.exception.MemberNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ReportService(
    private val reportRepository: ReportRepository,
    private val memberRepository: MemberRepository,
    private val bossPartyChatMessageRepository: BossPartyChatMessageRepository,
    private val mapleCharacterRepository: MapleCharacterRepository
) {

    @Transactional
    fun reportChat(userEmail: String, chatId: Long, reason: ReportReason, detail: String?) {
        val reporter = memberRepository.findByEmail(userEmail)
            ?: throw MemberNotFoundException()

        println("신고자: $reporter")

        // 1. 신고 대상 메시지 존재 확인
        val targetChat = bossPartyChatMessageRepository.findById(chatId)
            .orElseThrow { IllegalArgumentException("존재하지 않는 메시지입니다.") }

        println("메시지: $targetChat")

        val reportedCharacter = mapleCharacterRepository.findById(targetChat.character.id)
            .orElseThrow { IllegalArgumentException("존재하지 않는 캐릭터입니다.") }

        println("신고 대상 캐릭터: $reportedCharacter")

        // 2. 캐릭터를 보유한 실제 유저(Member) 식별
        val reportedMemberId = reportedCharacter.member.id

        println("신고 대상자: $reportedMemberId")

        // 2. 본인 메시지 신고 방지
        if (reportedMemberId == reporter.id) {
            throw IllegalArgumentException("자신의 메시지는 신고할 수 없습니다.")
        }

        // 3. 중복 신고 방지
        if (reportRepository.existsByReporterMemberIdAndTargetChatId(reporter.id, chatId)) {
            throw IllegalStateException("이미 신고한 메시지입니다.")
        }

        // 4. 신고 엔터티 생성 및 저장 (당시 내용 스냅샷 포함)
        val report = Report(
            reporterMemberId = reporter.id,
            reportedMemberId = reportedMemberId,
            reportedCharacterId = reportedCharacter.id,
            targetChatId = chatId,
            evidenceContent = targetChat.content,
            reason = reason,
            reasonDetail = detail
        )

        reportRepository.save(report)
    }
}