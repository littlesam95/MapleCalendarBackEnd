package com.sixclassguys.maplecalendar.domain.boss.repository

import com.sixclassguys.maplecalendar.domain.boss.entity.BossPartyAlarmTime
import com.sixclassguys.maplecalendar.domain.boss.enums.RegistrationMode
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Repository
interface BossPartyAlarmTimeRepository : JpaRepository<BossPartyAlarmTime, Long> {

    fun findByBossPartyId(bossPartyId: Long): List<BossPartyAlarmTime>

    fun findByBossPartyIdAndIsSentFalseOrderByAlarmTimeAsc(bossPartyId: Long): List<BossPartyAlarmTime>

    fun findByIsSentFalse(): List<BossPartyAlarmTime>

    @Modifying
    @Transactional
    @Query("""
        DELETE FROM BossPartyAlarmTime a 
        WHERE a.bossPartyId = :bossPartyId 
          AND a.registrationMode = :registrationMode 
          AND a.isSent = false 
          AND a.alarmTime > :now
    """)
    fun deleteFuturePeriodicAlarms(
        @Param("bossPartyId") bossPartyId: Long,
        @Param("registrationMode") registrationMode: RegistrationMode = RegistrationMode.PERIODIC,
        @Param("now") now: LocalDateTime = LocalDateTime.now()
    )

    @Query("""
        SELECT DISTINCT bpat 
        FROM BossPartyAlarmTime bpat
        JOIN MemberBossPartyMapping mbpm ON bpat.bossPartyId = mbpm.bossPartyId
        WHERE mbpm.memberId = :memberId
          AND mbpm.isPartyAlarmEnabled = true
          AND bpat.alarmTime >= :startDateTime
          AND bpat.alarmTime <= :endDateTime
        ORDER BY bpat.alarmTime ASC
    """)
    fun findMemberSchedules(
        @Param("memberId") memberId: Long,
        @Param("startDateTime") startDateTime: LocalDateTime,
        @Param("endDateTime") endDateTime: LocalDateTime
    ): List<BossPartyAlarmTime>

    fun existsByBossPartyIdAndAlarmTime(bossPartyId: Long, alarmTime: LocalDateTime): Boolean
}