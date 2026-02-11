package com.sixclassguys.maplecalendar.domain.character.service

import com.sixclassguys.maplecalendar.domain.character.dto.CharacterAuthorityResponse
import com.sixclassguys.maplecalendar.domain.character.dto.CharacterSummaryResponse
import com.sixclassguys.maplecalendar.domain.character.dto.MapleCharacterListResponse
import com.sixclassguys.maplecalendar.domain.character.entity.MapleCharacter
import com.sixclassguys.maplecalendar.domain.character.repository.MapleCharacterRepository
import com.sixclassguys.maplecalendar.domain.member.entity.Member
import com.sixclassguys.maplecalendar.domain.member.entity.NexonApiKey
import com.sixclassguys.maplecalendar.domain.member.repository.MemberRepository
import com.sixclassguys.maplecalendar.domain.member.repository.NexonApiKeyRepository
import com.sixclassguys.maplecalendar.domain.util.MapleWorld
import com.sixclassguys.maplecalendar.global.config.EncryptionUtil
import com.sixclassguys.maplecalendar.global.exception.AccessDeniedException
import com.sixclassguys.maplecalendar.global.exception.InvalidApiKeyException
import com.sixclassguys.maplecalendar.infrastructure.external.NexonApiClient
import com.sixclassguys.maplecalendar.infrastructure.external.dto.AccountCharacter
import jakarta.persistence.EntityNotFoundException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
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
    private val nexonApiKeyRepository: NexonApiKeyRepository,
    private val memberRepository: MemberRepository,
    private val encryptionUtil: EncryptionUtil,
    private val nexonApiClient: NexonApiClient
) {

    private val log = LoggerFactory.getLogger(javaClass)

    // 1. DB 저장된 캐릭터 조회 로직 수정
    @Transactional(readOnly = true)
    fun getGroupedCharacters(email: String): MapleCharacterListResponse {
        val member = memberRepository.findByEmail(email)
            ?: throw EntityNotFoundException("유저를 찾을 수 없습니다.")

        val representativeOcid = member.representativeOcid
        val allCharacters = mapleCharacterRepository.findAllByMember(member)

        val groupedByWorld = allCharacters
            .groupBy { it.worldName } // DB에 저장된 worldName 그대로 사용
            .mapValues { (_, characters) ->
                characters.map { entity ->
                    CharacterSummaryResponse(
                        id = entity.id ?: 0L,
                        ocid = entity.ocid,
                        characterName = entity.characterName,
                        characterLevel = entity.characterLevel,
                        characterClass = entity.characterClass,
                        characterImage = entity.characterImage,
                        isRepresentativeCharacter = entity.ocid == representativeOcid
                    )
                }.sortedByDescending { it.characterLevel }
            }

        return MapleCharacterListResponse(groupedByWorld)
    }

    // 2. 넥슨 API 캐릭터 페치 로직 수정
    suspend fun fetchCharactersFromNexon(email: String, apiKey: String): MapleCharacterListResponse = coroutineScope {
        val member = memberRepository.findByEmail(email)
            ?: throw EntityNotFoundException("유저를 찾을 수 없습니다.")

        val nexonAccounts = nexonApiClient.getCharacters(apiKey)
        if (nexonAccounts.isEmpty()) throw InvalidApiKeyException()

        // 2. 🚀 유효한 키라면 DB 저장 (중복 체크 포함)
        val apiKeyHash = encryptionUtil.hashKey(apiKey)

        // 이 멤버가 이미 이 키를 등록했는지 확인 (또는 전체 유니크 체크)
        val isKeyExists = nexonApiKeyRepository.existsByApiKeyHash(apiKeyHash)

        if (!isKeyExists) {
            nexonApiKeyRepository.save(
                NexonApiKey(
                    member = member,
                    nexonApiKey = apiKey, // Converter에 의해 자동 암호화됨
                    apiKeyHash = apiKeyHash
                )
            )
        }

        val groupedByWorld = nexonAccounts.flatMap { it.characters }
            .mapNotNull { nexonChar ->
                val worldEnum = MapleWorld.fromName(nexonChar.worldName)
                if (worldEnum == null) null else nexonChar to worldEnum
            }
            .groupBy { (_, worldEnum) -> worldEnum.worldName } // 오직 '월드명'으로만 그룹화
            .mapValues { (_, worldItems) ->
                worldItems.map { (nexonChar, _) ->
                    CharacterSummaryResponse(
                        id = 0L,
                        ocid = nexonChar.ocid,
                        characterName = nexonChar.characterName,
                        characterLevel = nexonChar.characterLevel,
                        characterClass = nexonChar.characterClass,
                        characterImage = "",
                        isRepresentativeCharacter = false
                    )
                }.sortedByDescending { it.characterLevel }
            }

        MapleCharacterListResponse(groupedByWorld)
    }

    @Transactional
    suspend fun registerSelectedCharacters(email: String, ocids: List<String>) = coroutineScope {
        val member = memberRepository.findByEmail(email)
            ?: throw EntityNotFoundException("유저를 찾을 수 없습니다.")

        // 🚀 동시 실행 개수를 5개로 제한하는 세마포어 생성
        val semaphore = Semaphore(5)

        // 1. 각 OCID에 대해 비동기로 정보 가져오기 (세마포어 적용)
        val deferredCharacters = ocids.map { ocid ->
            async {
                semaphore.withPermit {
                    try {
                        val info = nexonApiClient.getCharacterBasic(ocid)
                        // 🚀 정보가 없거나 이름이 없는 경우 null 반환 (조회 실패 캐릭터 스킵)
                        if (info?.characterName == null) null else ocid to info
                    } catch (e: Exception) {
                        log.warn("캐릭터 정보 조회 실패 (OCID: $ocid), 사유: ${e.message}")
                        null
                    }
                }
            }
        }
        val characterInfos = deferredCharacters.awaitAll()

        // 2. DB 저장 및 업데이트 (Upsert)
        characterInfos.forEach { info ->
            val existingChar = mapleCharacterRepository.findByOcid(info?.first.toString())

            if (existingChar != null) {
                // 이미 등록된 캐릭터라면 정보 최신화
                existingChar.apply {
                    this.characterName = info?.second?.characterName.toString()
                    this.worldName = info?.second?.worldName.toString()
                    this.characterLevel = info?.second?.characterLevel ?: 0L
                    this.characterClass = info?.second?.characterClass.toString()
                    this.characterImage = info?.second?.characterImage.toString()
                    this.lastUpdatedAt = LocalDateTime.now()
                }
            } else {
                // 새 캐릭터라면 신규 등록
                val parsedDate = try {
                    info?.second?.characterDateCreate?.let { LocalDate.parse(it) } ?: LocalDate.now()
                } catch (e: Exception) {
                    LocalDate.now() // 파싱 실패 시 현재 날짜로 폴백
                }

                mapleCharacterRepository.save(
                    MapleCharacter(
                        member = member,
                        ocid = info?.first.toString(),
                        characterName = info?.second?.characterName.toString(),
                        worldName = info?.second?.worldName.toString(),
                        characterClass = info?.second?.characterClass.toString(),
                        characterLevel = info?.second?.characterLevel ?: 0L,
                        characterImage = info?.second?.characterImage,
                        characterGender = info?.second?.characterGender.toString(),
                        characterClassLevel = info?.second?.characterClassLevel.toString(),
                        characterExp = info?.second?.characterExp ?: 0L,
                        characterExpRate = info?.second?.characterExpRate.toString(),
                        characterDateCreate = parsedDate,
                        accessFlag = info?.second?.accessFlag.toString()
                    )
                )
            }
        }
    }

    @Transactional
    suspend fun registerAndGetList(email: String, ocids: List<String>): MapleCharacterListResponse {
        // 1. 선택한 캐릭터들 등록/업데이트 (기존 로직 수행)
        registerSelectedCharacters(email, ocids)

        // 2. 등록이 완료된 후, 해당 유저의 전체 그룹화된 목록 조회 및 반환
        return getGroupedCharacters(email)
    }

    @Transactional(readOnly = true)
    fun getCharacterAuthority(email: String, ocid: String): CharacterAuthorityResponse {
        val member = memberRepository.findByEmail(email)
            ?: throw EntityNotFoundException("유저를 찾을 수 없습니다.")

        // 1. 소유권 확인 (Repository 호출은 여기서!)
        val isOwner = mapleCharacterRepository.existsByMemberAndOcid(member, ocid)

        // 2. 대표 캐릭터 여부 확인
        val isRepresentative = member.representativeOcid == ocid

        return CharacterAuthorityResponse(
            isOwner = isOwner,
            isRepresentative = isRepresentative
        )
    }

    @Transactional
    fun updateRepresentativeCharacter(email: String, ocid: String) {
        val member = memberRepository.findByEmail(email)
            ?: throw EntityNotFoundException("유저를 찾을 수 없습니다.")

        // 보안 체크: 요청한 OCID가 실제로 이 유저의 캐릭터인지 확인
        val isOwner = mapleCharacterRepository.existsByMemberAndOcid(member, ocid)
        if (!isOwner) throw AccessDeniedException()

        member.representativeOcid = ocid
    }

    @Transactional
    fun deleteCharacter(email: String, ocid: String) {
        val member = memberRepository.findByEmail(email)
            ?: throw EntityNotFoundException("유저를 찾을 수 없습니다.")

        val character = mapleCharacterRepository.findByOcidAndMember(ocid, member)
            ?: throw EntityNotFoundException("해당 캐릭터를 찾을 수 없습니다.")

        // 만약 삭제하려는 캐릭터가 대표 캐릭터라면, 대표 캐릭터 설정도 초기화
        if (member.representativeOcid == ocid) {
            member.representativeOcid = null
        }

        mapleCharacterRepository.delete(character)
    }

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

    // 캐릭터 이름으로 관련 db에 저장된 캐릭터 이름 들어간 캐릭터 찾기
    fun searchCharactersByName(namePart: String): MapleCharacterListResponse {
        val characters = mapleCharacterRepository.findAllByCharacterNameContainingIgnoreCase(namePart)

        // CharacterSummaryResponse로 매핑
        val summaries = characters.map { c ->
            CharacterSummaryResponse(
                id = c.id,
                ocid = c.ocid,
                characterName = c.characterName,
                characterLevel = c.characterLevel,
                characterClass = c.characterClass,
                characterImage = c.characterImage,
                isRepresentativeCharacter = false // 대표 캐릭터 여부를 DB에서 가져오면 바꿀 수 있음
            )
        }

        // 예시: 월드별로 그룹화
        val grouped = summaries.groupBy { it.characterName } // 필요하면 다른 기준으로 변경 가능

        return MapleCharacterListResponse(groupedCharacters = grouped)
    }
}