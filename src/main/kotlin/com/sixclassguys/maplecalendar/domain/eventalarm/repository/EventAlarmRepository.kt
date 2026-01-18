package com.sixclassguys.maplecalendar.domain.eventalarm.repository

import com.sixclassguys.maplecalendar.domain.event.entity.Event
import com.sixclassguys.maplecalendar.domain.eventalarm.entity.EventAlarm
import com.sixclassguys.maplecalendar.domain.member.entity.Member
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface EventAlarmRepository : JpaRepository<EventAlarm, Long> {

    /**
     * 1. 스케줄러용: 현재 시각에 발송해야 할 알람들 조회
     * isEnabled가 true이고, 아직 발송되지 않았으며(isSent = false), 시간이 도래한 것
     */
    @Query("""
        SELECT DISTINCT a 
        FROM EventAlarm a 
        JOIN a.alarmTimes t 
        WHERE t.alarmTime <= :now 
          AND t.isSent = false 
    """)
    fun findAllToSendMessage(@Param("now") now: LocalDateTime): List<EventAlarm>

    /**
     * 2. UI 표시용: 특정 멤버의 이벤트들 중 '아직 전송되지 않은' 미래 알람만 조회
     * FETCH JOIN을 사용하여 N+1 문제를 방지합니다.
     */
    @Query("""
        SELECT DISTINCT a 
        FROM EventAlarm a 
        LEFT JOIN FETCH a.alarmTimes t 
        WHERE a.member = :member 
          AND a.event.id IN :eventIds 
    """)
    fun findAllActiveByMemberAndEventIds(
        @Param("member") member: Member,
        @Param("eventIds") list: List<Long>
    ): List<EventAlarm>

    // 나머지 기본 메서드들
    fun findByMemberAndEvent(member: Member, event: Event): EventAlarm?

    fun findAllByMember(member: Member): List<EventAlarm>
}