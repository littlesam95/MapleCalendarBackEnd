package com.sixclassguys.maplecalendar.domain.eventalarm.repository

import com.sixclassguys.maplecalendar.domain.eventalarm.entity.EventAlarm
import com.sixclassguys.maplecalendar.domain.eventalarm.entity.EventAlarmTime
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface EventAlarmTimeRepository : JpaRepository<EventAlarmTime, Long> {

    /**
     * 마이그레이션용: 아직 발송되지 않은(isSent = false) 모든 알람 시간을 조회
     * N+1 문제를 방지하기 위해 EventAlarm, Member, Event를 모두 Fetch Join 합니다.
     */
    @Query("""
        SELECT eat FROM EventAlarmTime eat
        JOIN FETCH eat.eventAlarm ea
        JOIN FETCH ea.member
        JOIN FETCH ea.event
        WHERE eat.isSent = false
    """)
    fun findAllByIsSentFalseWithDetails(): List<EventAlarmTime>

    /**
     * 특정 알람 설정(EventAlarm)에 속한 시간들 중 아직 안 보낸 것들 조회
     */
    fun findAllByEventAlarmAndIsSentFalse(eventAlarm: EventAlarm): List<EventAlarmTime>
}