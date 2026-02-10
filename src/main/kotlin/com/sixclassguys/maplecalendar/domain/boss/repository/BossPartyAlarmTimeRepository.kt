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

    // 2. 중복 예약 방지를 위한 존재 확인
    // 같은 시간에 이미 알람이 있는지 확인 (SELECT, PERIODIC 구분 없이 체크)
    fun existsByBossPartyIdAndAlarmTime(bossPartyId: Long, alarmTime: LocalDateTime): Boolean
}