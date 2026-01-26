package com.sixclassguys.maplecalendar.domain.character.repository

import com.sixclassguys.maplecalendar.domain.character.entity.MapleCharacter
import com.sixclassguys.maplecalendar.domain.member.entity.Member
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

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
}