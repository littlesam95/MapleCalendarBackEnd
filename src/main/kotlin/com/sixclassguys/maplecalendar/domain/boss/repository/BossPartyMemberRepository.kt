package com.sixclassguys.maplecalendar.domain.boss.repository

import com.sixclassguys.maplecalendar.domain.boss.entity.BossPartyMember
import com.sixclassguys.maplecalendar.domain.boss.enums.JoinStatus
import com.sixclassguys.maplecalendar.domain.boss.enums.PartyRole
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface BossPartyMemberRepository : JpaRepository<BossPartyMember, Long> {

    fun existsByBossPartyIdAndCharacterId(bossPartyId: Long, characterId: Long): Boolean

    fun findAllByBossPartyId(bossPartyId: Long): List<BossPartyMember>

    @Query("""
    SELECT m FROM BossPartyMember m 
    JOIN FETCH m.character c
    JOIN FETCH c.member mem
    WHERE m.bossParty.id IN :partyIds 
    AND m.joinStatus = :joinStatus
""")
    fun findAllWithMemberByPartyIds(
        @Param("partyIds") partyIds: List<Long>,
        @Param("joinStatus") joinStatus: JoinStatus
    ): List<BossPartyMember>

    @Query("""
    SELECT m FROM BossPartyMember m 
    JOIN FETCH m.character c
    JOIN FETCH c.member mem
    LEFT JOIN FETCH mem.tokens
    WHERE m.bossParty.id = :partyId 
    AND m.joinStatus = :joinStatus
""")
    fun findAllWithMemberAndTokensByPartyId(
        @Param("partyId") partyId: Long,
        @Param("joinStatus") joinStatus: JoinStatus
    ): List<BossPartyMember>

    @Query("""
        SELECT m FROM BossPartyMember m 
        JOIN FETCH m.character c
        JOIN FETCH c.member mem
        WHERE m.bossParty.id = :partyId 
        AND mem.email = :email
    """)
    fun findByBossPartyIdAndCharacterMemberEmailInvited(
        @Param("partyId") partyId: Long,
        @Param("email") email: String
    ): BossPartyMember?

    @Query("""
        SELECT m FROM BossPartyMember m 
        JOIN FETCH m.character c
        JOIN FETCH c.member mem
        WHERE m.bossParty.id = :partyId 
        AND mem.email = :email
        AND m.joinStatus = 'ACCEPTED'
    """)
    fun findByBossPartyIdAndCharacterMemberEmail(
        @Param("partyId") partyId: Long,
        @Param("email") email: String
    ): BossPartyMember?

    // 특정 캐릭터 조회 (fetch join 포함)
    @Query("""
        SELECT m FROM BossPartyMember m
        JOIN FETCH m.character c
        JOIN FETCH c.member mem
        WHERE m.bossParty.id = :partyId
        AND c.id = :characterId
    """)
    fun findByBossPartyIdAndCharacterId(
        @Param("partyId") bossPartyId: Long,
        @Param("characterId") characterId: Long
    ): BossPartyMember?

    // 특정 이메일 + Role 조회 (리더 권한 체크용)
    @Query("""
        SELECT m FROM BossPartyMember m
        JOIN FETCH m.character c
        JOIN FETCH c.member mem
        WHERE m.bossParty.id = :partyId
        AND mem.email = :email
        AND m.role = :role
    """)
    fun findByBossPartyIdAndCharacterMemberEmailAndRole(
        @Param("partyId") bossPartyId: Long,
        @Param("email") email: String,
        @Param("role") role: PartyRole
    ): BossPartyMember?
}
