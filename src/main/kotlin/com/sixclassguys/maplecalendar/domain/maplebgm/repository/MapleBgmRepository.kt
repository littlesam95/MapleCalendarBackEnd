package com.sixclassguys.maplecalendar.domain.maplebgm.repository

import com.sixclassguys.maplecalendar.domain.maplebgm.entity.MapleBgm
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface MapleBgmRepository : JpaRepository<MapleBgm, Long> {

    // 1. 인기 차트: 좋아요 많은 순 -> ID 내림차순 (동일 좋아요 시 최신순)
    fun findAllByOrderByLikeCountDescIdDesc(pageable: Pageable): Slice<MapleBgm>

    // 2. 최신 차트: ID 내림차순 (또는 createdAt)
    fun findAllByOrderByIdDesc(pageable: Pageable): Slice<MapleBgm>

    // 3. BGM 검색
    fun findAllByTitleContainingOrMapNameContainingOrderByIdDesc(
        title: String,
        mapName: String,
        pageable: Pageable
    ): Slice<MapleBgm>
}