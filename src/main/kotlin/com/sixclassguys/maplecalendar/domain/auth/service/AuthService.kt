package com.sixclassguys.maplecalendar.domain.auth.service

import com.sixclassguys.maplecalendar.domain.auth.dto.AccountCharacterResponse
import com.sixclassguys.maplecalendar.domain.auth.dto.AutoLoginResponse
import com.sixclassguys.maplecalendar.domain.auth.dto.LoginResponse
import com.sixclassguys.maplecalendar.domain.member.entity.Member
import com.sixclassguys.maplecalendar.domain.member.repository.MemberRepository
import com.sixclassguys.maplecalendar.global.exception.InvalidApiKeyException
import com.sixclassguys.maplecalendar.infrastructure.external.NexonApiClient
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthService(
    private val nexonApiClient: NexonApiClient,
    private val memberRepository: MemberRepository
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    suspend fun loginAndGetCharacters(apiKey: String): LoginResponse = coroutineScope {
        // 1. DB 먼저 확인 (이미 등록된 유저인지)
        val existingMember = memberRepository.findByNexonApiKey(apiKey)

        // 2. 이미 대표 캐릭터가 설정된 경우, 바로 반환
        if (existingMember?.representativeOcid != null) {
            return@coroutineScope LoginResponse(
                representativeOcid = existingMember.representativeOcid
            )
        }

        // 3. 넥슨 API 호출 (조회 성공 시에만 다음 단계 진행)
        val nexonAccounts = nexonApiClient.getCharacters(apiKey)

        // 4. 가공 전 데이터 검증 (가져오는 데 실패했거나 데이터가 없는 경우)
        if (nexonAccounts.isEmpty()) {
            throw InvalidApiKeyException()
        }

        // 5. Member 엔티티 저장 또는 조회 (Upsert 로직)
        // API Key를 고유 식별자로 사용하여 유저 관리
        if (existingMember == null) {
            memberRepository.save(Member(nexonApiKey = apiKey))
        }

        // 6. 모든 계정의 캐릭터를 평면화
        val allCharacters = nexonAccounts.flatMap { it.characters }

        // 7. 앱에 전달할 형태로 데이터 가공 (각 계정의 캐릭터 리스트를 월드별로 그룹화)
        val characters = allCharacters.map { character ->
            AccountCharacterResponse(
                ocid = character.ocid,
                characterName = character.characterName,
                worldName = character.worldName,
                characterClass = character.characterClass,
                characterLevel = character.characterLevel
            )
        }.groupBy { it.worldName } // 월드별로 그룹핑하여 전달

        LoginResponse(characters = characters)
    }

    fun processAutoLogin(apiKey: String): AutoLoginResponse {
        // 1. DB에서 해당 API Key를 가진 유저 조회
        val user = memberRepository.findByNexonApiKey(apiKey)
            ?: return AutoLoginResponse(false, "존재하지 않는 회원입니다.")

        // 2. 대표 캐릭터 OCID가 설정되어 있는지 확인
        val ocid = user.representativeOcid
            ?: return AutoLoginResponse(false, "대표 캐릭터가 없습니다.")

        return try {
            // 3. 넥슨 API를 통해 해당 OCID의 최신 정보 조회 (이미지, 레벨 등)
            val characterBasic = nexonApiClient.getCharacterBasic(apiKey, ocid)
            log.info("캐릭터 정보: $characterBasic")

            AutoLoginResponse(
                isSuccess = true,
                message = "자동 로그인 성공",
                characterBasic = characterBasic
            )
        } catch (e: Exception) {
            // 넥슨 API 키가 만료되었거나 서버 통신 오류 시
            AutoLoginResponse(false, "캐릭터 정보를 가져오는데 실패했습니다: ${e.message}")
        }
    }
}