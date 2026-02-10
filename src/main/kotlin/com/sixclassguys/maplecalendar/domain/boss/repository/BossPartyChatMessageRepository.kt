package com.sixclassguys.maplecalendar.domain.boss.repository

import com.sixclassguys.maplecalendar.domain.boss.entity.BossPartyChatMessage
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface BossPartyChatMessageRepository : JpaRepository<BossPartyChatMessage, Long> {

    fun findMaxIdByBossPartyId(@Param("partyId") partyId: Long): Long?

    // 특정 파티의 채팅 내역을 최신순으로 페이징 조회
    // Fetch Join을 사용하여 MapleCharacter 정보를 한 번에 가져옴 (N+1 방지)
    @Query("select m from BossPartyChatMessage m join fetch m.character where m.bossParty.id = :partyId order by m.createdAt desc")
    fun findByBossPartyIdOrderByCreatedAtDesc(partyId: Long, pageable: Pageable): Slice<BossPartyChatMessage>

    @Modifying(clearAutomatically = true) // 벌크 연산 후 영속성 컨텍스트 초기화
    @Query("""
    UPDATE BossPartyChat c 
    SET c.unreadCount = c.unreadCount - 1 
    WHERE c.bossPartyId = :partyId 
      AND c.id > :lastReadId 
      AND c.id <= :currentMaxId 
      AND c.unreadCount > 0
""")
    fun decreaseUnreadCount(
        @Param("partyId") partyId: Long,
        @Param("lastReadId") lastReadId: Long,
        @Param("currentMaxId") currentMaxId: Long
    )

    // 메시지 ID와 작성자 ID가 일치할 때만 삭제 (보안 강화)
    fun deleteByIdAndCharacterId(messageId: Long, characterId: Long)
}