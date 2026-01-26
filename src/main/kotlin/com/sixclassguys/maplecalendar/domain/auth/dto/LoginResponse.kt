package com.sixclassguys.maplecalendar.domain.auth.dto

import com.sixclassguys.maplecalendar.domain.character.entity.MapleCharacter
import com.sixclassguys.maplecalendar.domain.member.entity.Member
import com.sixclassguys.maplecalendar.infrastructure.external.dto.CharacterBasic
import com.sixclassguys.maplecalendar.infrastructure.external.dto.DojangRanking
import com.sixclassguys.maplecalendar.infrastructure.external.dto.Ranking
import com.sixclassguys.maplecalendar.infrastructure.external.dto.UnionResponse
import java.time.LocalDateTime

data class LoginResponse(
    val id: Long,
    val email: String,
    val nickname: String?,
    val profileImageUrl: String?,
    val provider: String,
    val isGlobalAlarmEnabled: Boolean,
    val accessToken: String,
    val isNewMember: Boolean,
    val characterBasic: CharacterBasic? = null,
    val characterPopularity: Int? = null,
    val characterOverallRanking: Ranking? = null,
    val characterServerRanking: Ranking? = null,
    val characterUnionLevel: UnionResponse? = null,
    val characterDojang: DojangRanking? = null,
    val lastLoginAt: LocalDateTime
) {

    companion object {

        /**
         * 넥슨 API 실시간 조회에 실패했거나,
         * 아직 상세 정보가 없는 경우(신규 등록 등) 사용하는 응답 변환 함수
         */
        fun fromEntity(
            member: Member,
            character: MapleCharacter? = null,
            token: String? = null,
            isNewMember: Boolean = false
        ): LoginResponse {
            return LoginResponse(
                id = member.id!!,
                email = member.email,
                nickname = member.nickname,
                profileImageUrl = member.profileImageUrl,
                provider = member.provider,
                isGlobalAlarmEnabled = member.isGlobalAlarmEnabled,
                accessToken = token ?: "",
                isNewMember = isNewMember,
                lastLoginAt = member.lastLoginAt,
                // API 호출 실패 시 우리 DB에 있는 정보라도 최대한 넣어줌
                characterBasic = character?.let {
                    CharacterBasic(
                        characterName = it.characterName,
                        worldName = it.worldName,
                        characterImage = it.characterImage, // DB에 저장된 마지막 이미지 URL
                        characterLevel = it.characterLevel,
                        characterClass = it.characterClass,
                        characterGender = it.characterGender,
                        characterGuildName = it.characterGuildName
                    )
                },
                characterPopularity = null,
                characterOverallRanking = null,
                characterServerRanking = null,
                characterUnionLevel = null,
                characterDojang = null
            )
        }
    }
}