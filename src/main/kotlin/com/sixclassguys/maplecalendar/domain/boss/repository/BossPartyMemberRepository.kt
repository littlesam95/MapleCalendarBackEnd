package com.sixclassguys.maplecalendar.domain.boss.repository

import com.sixclassguys.maplecalendar.domain.boss.entity.BossPartyMember
import com.sixclassguys.maplecalendar.domain.boss.enums.JoinStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface BossPartyMemberRepository : JpaRepository<BossPartyMember, Long> {
    fun existsByBossPartyIdAndCharacterId(bossPartyId: Long, characterId: Long): Boolean

    fun findAllByBossPartyId(bossPartyId: Long): List<BossPartyMember>

    fun findAllByBossPartyIdAndJoinStatus(bossPartyId: Long, joinStatus: JoinStatus): List<BossPartyMember>
}
