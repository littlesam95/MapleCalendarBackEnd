package com.sixclassguys.maplecalendar.domain.boss.repository

import com.sixclassguys.maplecalendar.domain.boss.dto.BossPartyResponse
import com.sixclassguys.maplecalendar.domain.boss.entity.BossParty
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface BossPartyRepository : JpaRepository<BossParty, Long>{
    @Query("""
        SELECT bp
        FROM BossParty bp
        JOIN MemberBossPartyMapping mbpm
            ON bp.id = mbpm.bossPartyId
        WHERE mbpm.memberId = :memberId AND bp.isDeleted = false
    """)
    fun findAllByMemberId(memberId: Long): List<BossParty>

    @Query("""
    SELECT p, bm, m.isPartyAlarmEnabled, m.isChatAlarmEnabled
    FROM BossParty p
    JOIN FETCH p.members pm
    JOIN FETCH pm.character c
    JOIN MemberBossPartyMapping m ON p.id = m.bossPartyId
    JOIN BossPartyMember bm ON p.id = bm.bossParty.id
    WHERE m.memberId = :memberId AND bm.character.member.id = :memberId AND p.isDeleted = false
""")
    fun findAllPartiesByMemberId(@Param("memberId") memberId: Long): List<Array<Any>>

    @Query("""
        SELECT p 
        FROM BossParty p 
        JOIN FETCH p.members m 
        JOIN FETCH m.character c 
        WHERE p.id = :partyId AND p.isDeleted = false
    """)
    fun findDetailById(@Param("partyId") partyId: Long): BossParty?

    fun findByIdAndIsDeletedFalse(id: Long): BossParty?
}
