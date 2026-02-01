package com.sixclassguys.maplecalendar.domain.boss.repository

import com.sixclassguys.maplecalendar.domain.boss.entity.BossParty
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface BossPartyRepository : JpaRepository<BossParty, Long>{
    @Query("""
        SELECT bp
        FROM BossParty bp
        JOIN MemberBossPartyMapping mbpm
            ON bp.id = mbpm.bossPartyId
        WHERE mbpm.memberId = :memberId
    """)
    fun findAllByMemberId(memberId: Long): List<BossParty>
}
