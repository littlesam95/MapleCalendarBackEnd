package com.sixclassguys.maplecalendar.domain.notification.repository

import com.sixclassguys.maplecalendar.domain.member.entity.Member
import com.sixclassguys.maplecalendar.domain.notification.entity.NotificationToken
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface NotificationTokenRepository : JpaRepository<NotificationToken, Long> {

    fun findByToken(token: String): NotificationToken?

    @Query("SELECT t FROM NotificationToken t JOIN t.member m WHERE m.isGlobalAlarmEnabled = true")
    fun findAllByMemberIsGlobalAlarmEnabledTrue(): List<NotificationToken>

    // 💡 특정 멤버 ID의 모든 토큰 조회
    fun findAllByMemberId(memberId: Long): List<NotificationToken>

    fun deleteByMemberAndToken(member: Member, token: String)
}