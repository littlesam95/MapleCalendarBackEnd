package com.sixclassguys.maplecalendar.domain.boss.repository

import com.sixclassguys.maplecalendar.domain.boss.entity.BossPartyAlarmTime
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface BossPartyAlarmTimeRepository : JpaRepository<BossPartyAlarmTime, Long> {

    @Query("""
        SELECT bat
        FROM BossPartyAlarmTime bat
        JOIN MemberBossPartyMapping mbpm
          ON bat.bossPartyMemberMappingId = mbpm.id
        WHERE mbpm.bossPartyId = :bossPartyId
    """)
    fun findByBossPartyId(@Param("bossPartyId") bossPartyId: Long): List<BossPartyAlarmTime>

    fun findByIsSentFalse(): List<BossPartyAlarmTime>
}