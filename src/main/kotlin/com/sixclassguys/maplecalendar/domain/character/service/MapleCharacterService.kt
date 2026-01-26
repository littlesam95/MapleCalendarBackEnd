package com.sixclassguys.maplecalendar.domain.character.service

import com.sixclassguys.maplecalendar.domain.character.entity.MapleCharacter
import com.sixclassguys.maplecalendar.domain.character.repository.MapleCharacterRepository
import com.sixclassguys.maplecalendar.domain.member.entity.Member
import com.sixclassguys.maplecalendar.infrastructure.external.NexonApiClient
import com.sixclassguys.maplecalendar.infrastructure.external.dto.AccountCharacter
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime

@Service
class MapleCharacterService(
    private val mapleCharacterRepository: MapleCharacterRepository,
    private val nexonApiClient: NexonApiClient
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Async("characterSyncExecutor")
    @Transactional
    fun refreshUserCharacters(member: Member) {
        val existingCharacters = mapleCharacterRepository.findAllByMember(member)
        val processedOcids = mutableSetOf<String>() // 중복 방지용 Set

        member.nexonApiKeys.forEach { apiKeyEntity ->
            val accountList = nexonApiClient.getCharacters(apiKeyEntity.nexonApiKey)

            accountList.forEach { account ->
                account.characters.forEach { characterDto ->
                    // 이미 다른 API 키를 통해 처리된 캐릭터라면 스킵
                    if (processedOcids.contains(characterDto.ocid)) return@forEach
                    processedOcids.add(characterDto.ocid)

                    // 닉네임과 월드가 모두 일치하는 캐릭터 찾기
                    val match = existingCharacters.find { it.characterName == characterDto.characterName }

                    if (match != null) {
                        if (match.ocid != characterDto.ocid) {
                            // CASE 1: 닉네임은 같은데 OCID가 바뀐 경우
                            match.isActive = false
                            saveNewCharacter(member, characterDto)
                            if (member.representativeOcid == match.ocid) {
                                member.representativeOcid = characterDto.ocid
                            }
                        } else {
                            // CASE 2: 기존 캐릭터 정보가 업데이트된 경우
                            updateCharacterInfo(match, characterDto)
                        }
                    }
                }
            }
        }

        // CASE 3: 이번 API 응답에는 없는데 DB에는 isActive=true인 캐릭터들 (삭제/월드리프) 비활성화
        existingCharacters.filter { it.isActive && it.ocid !in processedOcids }.forEach {
            it.isActive = false
            log.info("캐릭터 비활성화 처리: ${it.characterName} (${it.worldName})")
        }
    }

    private fun updateCharacterInfo(target: MapleCharacter, dto: AccountCharacter) {
        target.characterLevel = dto.characterLevel
        target.isActive = true
        target.lastUpdatedAt = LocalDateTime.now()
    }

    private fun saveNewCharacter(member: Member, characterDto: AccountCharacter) {
        val basic = nexonApiClient.getCharacterBasic(characterDto.ocid)

        val parsedCreateDate = try {
            basic?.characterDateCreate?.let {
                OffsetDateTime.parse(it).toLocalDate()
            } ?: LocalDate.now() // 값이 없으면 현재 날짜
        } catch (e: Exception) {
            log.warn("날짜 파싱 실패: ${basic?.characterDateCreate}, 사유: ${e.message}")
            LocalDate.now()
        }

        val newCharacter = MapleCharacter(
            member = member,
            ocid = characterDto.ocid,
            characterName = basic?.characterName ?: characterDto.characterName,
            worldName = basic?.worldName ?: characterDto.worldName,
            characterGender = basic?.characterGender ?: "",
            characterClass = basic?.characterClass ?: characterDto.characterClass,
            characterClassLevel = basic?.characterClassLevel ?: "0",
            characterLevel = basic?.characterLevel ?: characterDto.characterLevel,
            characterExp = basic?.characterExp ?: 0L,
            characterExpRate = basic?.characterExpRate ?: "0",
            characterGuildName = basic?.characterGuildName,
            characterImage = basic?.characterImage, // 💡 이게 있어야 UI가 예쁩니다.
            characterDateCreate = parsedCreateDate,
            accessFlag = "true", // 혹은 API 응답의 유무에 따라 설정
            liberationQuestClear = basic?.liberationQuestClear ?: "0",
            isActive = true,
            lastUpdatedAt = LocalDateTime.now(),
        )
        mapleCharacterRepository.save(newCharacter)
    }
}