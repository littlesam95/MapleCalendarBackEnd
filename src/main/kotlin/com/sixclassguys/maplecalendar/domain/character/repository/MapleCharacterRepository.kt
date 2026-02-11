package com.sixclassguys.maplecalendar.domain.character.repository

import com.sixclassguys.maplecalendar.domain.character.entity.MapleCharacter
import com.sixclassguys.maplecalendar.domain.member.entity.Member
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.data.repository.query.Param

@Repository
interface MapleCharacterRepository : JpaRepository<MapleCharacter, Long> {

    // 특정 멤버의 모든 캐릭터 조회 (동기화 비교 대상)
    fun findAllByMember(member: Member): List<MapleCharacter>

    // 특정 멤버의 활성화된 캐릭터만 조회 (UI 노출용)
    fun findAllByMemberAndIsActiveTrue(member: Member): List<MapleCharacter>

    // 닉네임과 월드 조합으로 기존 캐릭터 검색 (OCID 변경 감지용)
    fun findByCharacterNameAndWorldNameAndMember(
        characterName: String,
        worldName: String,
        member: Member
    ): MapleCharacter?

    // 특정 OCID로 캐릭터 조회 (대표 캐릭터 정보 갱신용)
    fun findByOcid(ocid: String): MapleCharacter?

    fun existsByMemberAndOcid(member: Member, ocid: String): Boolean

    fun findByOcidAndMember(ocid: String, member: Member): MapleCharacter?

    // 유저의 이메일로 현재 활성화된(isActive) 캐릭터 하나를 찾는 쿼리
    @Query("select c from MapleCharacter c where c.member.email = :email and c.isActive = true")
    fun findFirstByMemberEmailAndIsActiveTrue(email: String): MapleCharacter?

    // 캐릭터 이름에 포함된 모든 캐릭터 조회 (대소문자 무시)
    @Query("""
        SELECT c 
        FROM MapleCharacter c
        JOIN FETCH c.member m
        WHERE LOWER(c.characterName) LIKE LOWER(CONCAT('%', :namePart, '%'))
        AND c.isActive = true
    """)
    fun findAllByCharacterNameContainingIgnoreCase(
        @Param("namePart") namePart: String
    ): List<MapleCharacter>
}