package com.sixclassguys.maplecalendar.domain.boss.repository

import com.sixclassguys.maplecalendar.domain.boss.entity.BossPartyBoard
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface BossPartyBoardRepository : JpaRepository<BossPartyBoard, Long>{

    // 보스파티 객체 기준으로 게시글 조회 (생성일 기준 정렬)
    fun findAllByBossPartyIdOrderByCreatedAtDesc(bossPartyId: Long): List<BossPartyBoard>
}