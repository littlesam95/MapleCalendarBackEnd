package com.sixclassguys.maplecalendar.domain.boss.repository

import com.sixclassguys.maplecalendar.domain.boss.entity.MemberBossPartyMapping
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface MemberBossPartyMappingRepository : JpaRepository<MemberBossPartyMapping, Long> {

    fun findByMemberIdAndBossPartyId(memberId: Long, bossPartyId: Long): MemberBossPartyMapping?
}