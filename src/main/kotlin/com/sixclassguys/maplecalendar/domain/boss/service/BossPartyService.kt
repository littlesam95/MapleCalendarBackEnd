package com.sixclassguys.maplecalendar.domain.boss.service

import com.sixclassguys.maplecalendar.domain.boss.dto.BossPartyAlarmTimeResponse
import com.sixclassguys.maplecalendar.domain.boss.dto.BossPartyCreateRequest
import com.sixclassguys.maplecalendar.domain.boss.dto.BossPartyMemberResponse
import com.sixclassguys.maplecalendar.domain.boss.dto.BossPartyResponse
import com.sixclassguys.maplecalendar.domain.boss.repository.BossPartyAlarmTimeRepository
import com.sixclassguys.maplecalendar.domain.boss.repository.BossPartyBoardRepository
import com.sixclassguys.maplecalendar.domain.boss.dto.BossPartyChatMessageResponse
import com.sixclassguys.maplecalendar.domain.boss.dto.BossPartyDetailResponse
import com.sixclassguys.maplecalendar.domain.boss.dto.BossPartyMemberDetail
import com.sixclassguys.maplecalendar.domain.boss.dto.toResponse
import com.sixclassguys.maplecalendar.domain.boss.entity.BossParty
import com.sixclassguys.maplecalendar.domain.boss.entity.BossPartyChatMessage
import com.sixclassguys.maplecalendar.domain.boss.entity.BossPartyMember
import com.sixclassguys.maplecalendar.domain.boss.entity.MemberBossPartyMapping
import com.sixclassguys.maplecalendar.domain.boss.enums.JoinStatus
import com.sixclassguys.maplecalendar.domain.boss.enums.PartyRole
import com.sixclassguys.maplecalendar.domain.boss.repository.BossPartyChatMessageRepository
import com.sixclassguys.maplecalendar.domain.boss.repository.BossPartyMemberRepository
import com.sixclassguys.maplecalendar.domain.boss.repository.BossPartyRepository
import com.sixclassguys.maplecalendar.domain.boss.repository.MemberBossPartyMappingRepository
import com.sixclassguys.maplecalendar.domain.character.repository.MapleCharacterRepository
import com.sixclassguys.maplecalendar.domain.member.repository.MemberRepository
import com.sixclassguys.maplecalendar.domain.boss.enums.BossPartyChatMessageType
import com.sixclassguys.maplecalendar.global.exception.AccessDeniedException
import com.sixclassguys.maplecalendar.global.exception.BossPartyChatMessageNotFoundException
import com.sixclassguys.maplecalendar.global.exception.BossPartyNotFoundException
import com.sixclassguys.maplecalendar.global.exception.DeleteBossPartyChatMessageDeniedException
import com.sixclassguys.maplecalendar.global.exception.MapleCharacterNotFoundException
import com.sixclassguys.maplecalendar.global.exception.MemberNotFoundException
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Slice
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class BossPartyService(
    private val bossPartyRepository: BossPartyRepository,
    private val bossPartyMemberRepository: BossPartyMemberRepository,
    private val memberBossPartyMappingRepository: MemberBossPartyMappingRepository,
    private val mapleCharacterRepository: MapleCharacterRepository,
    private val memberRepository: MemberRepository,
    private val bossPartyAlarmTimeRepository: BossPartyAlarmTimeRepository,
    private val bossPartyChatMessageRepository: BossPartyChatMessageRepository,
    private val bossPartyBoardRepository: BossPartyBoardRepository,
) {

    @Transactional
    fun createParty(req: BossPartyCreateRequest, userEmail: String): Long {
        // 1. 로그인한 유저(Member) 조회
        val member = memberRepository.findByEmail(userEmail)
            ?: throw IllegalArgumentException("Member not found")

        // 2. 캐릭터 조회 (본인의 캐릭터인지 검증 로직을 추가하면 더 좋음)
        val character = mapleCharacterRepository.findById(req.characterId)
            .orElseThrow { IllegalArgumentException("Character not found") }

        // 1. 파티 본체 생성
        val bossParty = BossParty(
            title = req.title,
            description = req.description,
            boss = req.boss,
            difficulty = req.difficulty
        )
        val savedParty = bossPartyRepository.save(bossParty)

        // 2. 파티 멤버 명단에 리더 추가 (BossPartyMember)
        val leader = BossPartyMember(
            bossParty = savedParty,
            character = character,
            role = PartyRole.LEADER,
            joinStatus = JoinStatus.ACCEPTED,
            joinedAt = LocalDateTime.now()
        )
        bossPartyMemberRepository.save(leader)

        // 3. 리더의 개인 알람 설정 매핑 추가 (MemberBossPartyMapping) -> 이 부분이 누락됨!
        val mapping = MemberBossPartyMapping(
            bossPartyId = savedParty.id,
            memberId = member.id, // 캐릭터가 속한 계정(Member) ID
            isPartyAlarmEnabled = true, // 기본값 true
            isChatAlarmEnabled = true   // 기본값 true
        )
        memberBossPartyMappingRepository.save(mapping)

        return savedParty.id
    }

    @Transactional(readOnly = true)
    fun getBossParties(userEmail: String): List<BossPartyResponse> {
        val member = memberRepository.findByEmail(userEmail)
            ?: throw MemberNotFoundException()

        val results = bossPartyRepository.findAllPartiesByMemberId(member.id)

        return results.map { result ->
            val p = result[0] as BossParty
            val isPartyAlarm = result[1] as Boolean
            val isChatAlarm = result[2] as Boolean

            // 방장 찾기
            val leader = p.members.find { it.role == PartyRole.LEADER }?.character?.characterName ?: "Unknown"
            // 승인된 멤버 수 계산
            val totalCount = p.members.count { it.joinStatus == JoinStatus.ACCEPTED }

            BossPartyResponse(
                id = p.id,
                title = p.title,
                description = p.description,
                boss = p.boss,
                difficulty = p.difficulty,
                isPartyAlarmEnabled = isPartyAlarm,
                isChatAlarmEnabled = isChatAlarm,
                leaderNickname = leader,
                memberCount = totalCount,
                createdAt = p.createdAt,
                updatedAt = p.updatedAt
            )
        }
    }

    @Transactional(readOnly = true)
    fun getBossPartyDetail(partyId: Long, userEmail: String): BossPartyDetailResponse {
        val party = bossPartyRepository.findDetailById(partyId)
            ?: throw BossPartyNotFoundException()

        val member = memberRepository.findByEmail(userEmail)
            ?: throw MemberNotFoundException()

        // 1. 파티원 리스트 변환
        val memberDetails = party.members.map { m ->
            BossPartyMemberDetail(
                characterId = m.character.id,
                characterName = m.character.characterName,
                worldName = m.character.worldName,
                characterClass = m.character.characterClass, // 엔티티의 필드명에 맞춰 매핑
                characterLevel = m.character.characterLevel,
                characterImage = m.character.characterImage ?: "",
                role = m.role,
                isMyCharacter = m.character.member.id == member.id,
                joinedAt = m.joinedAt.toString()
            )
        }.sortedByDescending { it.role == PartyRole.LEADER }

        val isLeader = party.members.any { m ->
            m.role == PartyRole.LEADER && m.character.member.id == member.id
        }

        // 2. 현재 로그인한 유저의 알람 설정 및 방장 여부 확인 (Mapping 테이블 조회 필요)
        val mapping = memberBossPartyMappingRepository.findByMemberIdAndBossPartyId(member.id, partyId)

        return BossPartyDetailResponse(
            id = party.id,
            title = party.title,
            description = party.description,
            boss = party.boss,
            difficulty = party.difficulty,
            members = memberDetails,
            isLeader = isLeader,
            isPartyAlarmEnabled = mapping?.isPartyAlarmEnabled ?: true,
            isChatAlarmEnabled = mapping?.isChatAlarmEnabled ?: true,
            createdAt = party.createdAt
        )
    }

    @Transactional
    fun getAlarmTimesByBossPartyId(bossPartyId: Long): List<BossPartyAlarmTimeResponse> {

        val alarmTimes = bossPartyAlarmTimeRepository.findByBossPartyId(bossPartyId)

        return alarmTimes.map {
            BossPartyAlarmTimeResponse(
                id = it.id,
                alarmTime = it.alarmTime,
                message = it.message,
                isSent = it.isSent
            )
        }
    }

    @Transactional
    fun getAcceptedMembersByBossPartyId(bossPartyId: Long): List<BossPartyMemberResponse> {

        val bossPartyMembers = bossPartyMemberRepository.findAllByBossPartyIdAndJoinStatus(
            bossPartyId,
            JoinStatus.ACCEPTED // 수락 상태만 조회
        )

        return bossPartyMembers.map {
            BossPartyMemberResponse(
                id = it.id,
                character = it.character,
                role = it.role,
                joinStatus = it.joinStatus!!,
                joinedAt = it.joinedAt
            )
        }
    }

    /*
    @Transactional(readOnly = true)
    fun getMessagesByBossPartyId(bossPartyId: Long): List<BossPartyChatMessageResponse> {

        val messages = bossPartyChatMessageRepository.findAllByBossPartyIdOrderByCreatedAtAsc(bossPartyId)

        return messages.map {
            BossPartyChatMessageResponse(
                id = it.id,
                characterId = it.character.id!!,
                characterName = it.character.characterName,
                content = it.content,
                messageType = it.messageType,
                createdAt = it.createdAt
            )
        }
    }
    */

    // 1. 메시지 저장
    @Transactional
    fun saveMessage(
        partyId: Long,
        characterId: Long,
        content: String,
        type: BossPartyChatMessageType
    ): BossPartyChatMessage {
        val party = bossPartyRepository.findById(partyId)
            .orElseThrow { BossPartyNotFoundException() }
        val character = mapleCharacterRepository.findById(characterId)
            .orElseThrow { MapleCharacterNotFoundException() }

        val message = BossPartyChatMessage(
            bossParty = party,
            character = character,
            content = content,
            messageType = type
        )
        return bossPartyChatMessageRepository.save(message)
    }

    @Transactional(readOnly = true)
    fun getChatMessages(partyId: Long, userEmail: String, page: Int, size: Int): Slice<BossPartyChatMessageResponse> {
        // 1. 이 이메일의 유저가 이 파티에 어떤 캐릭터로 참여 중인지 찾습니다.
        val partyMember = bossPartyMemberRepository.findByBossPartyIdAndCharacterMemberEmail(partyId, userEmail)
            ?: throw AccessDeniedException("해당 파티의 멤버가 아닙니다.")

        val currentCharacterId = partyMember.character.id
        val pageable = PageRequest.of(page, size, Sort.by("createdAt").descending())

        // 2. 메시지 내역을 가져오면서 찾은 characterId로 isMine을 계산합니다.
        return bossPartyChatMessageRepository.findByBossPartyIdOrderByCreatedAtDesc(partyId, pageable)
            .map { it.toResponse(currentCharacterId) }
    }

    // 3. 파티 채팅 전체 삭제
    @Transactional
    fun deleteMessage(partyId: Long, messageId: Long, userEmail: String): BossPartyChatMessage {
        // 1. 해당 파티에 참여 중인 유저의 정보를 가져옵니다. (이미 검증된 로직)
        val partyMember = bossPartyMemberRepository.findByBossPartyIdAndCharacterMemberEmail(partyId, userEmail)
            ?: throw AccessDeniedException("해당 파티의 멤버가 아닙니다.")

        val message = bossPartyChatMessageRepository.findById(messageId)
            .orElseThrow { BossPartyChatMessageNotFoundException() }

        // 2. 권한 확인: 메시지를 쓴 캐릭터 ID와 현재 파티에 참여 중인 내 캐릭터 ID가 같은지 비교
        if (message.character.id != partyMember.character.id) {
            throw DeleteBossPartyChatMessageDeniedException()
        }

        // 3. 논리 삭제 처리
        message.markAsDeleted()
        // TODO: 이 시점에 WebSocket을 통해 "특정 ID의 메시지가 삭제됨"을 파티원들에게 브로드캐스팅하는 로직을 추가

        return message
    }
}