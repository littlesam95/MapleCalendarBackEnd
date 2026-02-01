package com.sixclassguys.maplecalendar.domain.boss.service

import com.sixclassguys.maplecalendar.domain.boss.dto.BossPartyCreateRequest
import com.sixclassguys.maplecalendar.domain.boss.dto.BossPartyChatMessageResponse
import com.sixclassguys.maplecalendar.domain.boss.entity.BossParty
import com.sixclassguys.maplecalendar.domain.boss.entity.BossPartyChatMessage
import com.sixclassguys.maplecalendar.domain.boss.entity.BossPartyMember
import com.sixclassguys.maplecalendar.domain.boss.enums.JoinStatus
import com.sixclassguys.maplecalendar.domain.boss.enums.PartyRole
import com.sixclassguys.maplecalendar.domain.boss.repository.BossPartyChatMessageRepository
import com.sixclassguys.maplecalendar.domain.boss.repository.BossPartyMemberRepository
import com.sixclassguys.maplecalendar.domain.boss.repository.BossPartyRepository
import com.sixclassguys.maplecalendar.domain.character.repository.MapleCharacterRepository
import com.sixclassguys.maplecalendar.domain.boss.enums.BossPartyChatMessageType
import com.sixclassguys.maplecalendar.global.exception.BossPartyChatMessageNotFoundException
import com.sixclassguys.maplecalendar.global.exception.BossPartyNotFoundException
import com.sixclassguys.maplecalendar.global.exception.DeleteBossPartyChatMessageDeniedException
import com.sixclassguys.maplecalendar.global.exception.MapleCharacterNotFoundException
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class BossPartyService(
    private val bossPartyRepository: BossPartyRepository,
    private val bossPartyMemberRepository: BossPartyMemberRepository,
    private val mapleCharacterRepository: MapleCharacterRepository,
    private val bossPartyChatMessageRepository: BossPartyChatMessageRepository
) {

    @Transactional
    fun createParty(
        req: BossPartyCreateRequest
    ): Long {
        val character = mapleCharacterRepository.findById(req.characterId)
            .orElseThrow { IllegalArgumentException("Character not found") }

        val bossParty = BossParty(
            title = req.title,
            description = req.description,
            boss = req.boss,
            difficulty = req.difficulty
        )

        val savedParty = bossPartyRepository.save(bossParty)

        val leader = BossPartyMember(
            bossPartyId = savedParty.id,
            characterId = character.id!!,
            role = PartyRole.LEADER,
            joinStatus = JoinStatus.ACCEPTED,
            joinedAt = LocalDateTime.now()
        )

        bossPartyMemberRepository.save(leader)

        return savedParty.id
    }

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

    // 2. 메시지 내역 조회 (DTO 변환 포함)
    fun getChatHistory(partyId: Long, currentCharacterId: Long, pageable: Pageable): Slice<BossPartyChatMessageResponse> {
        val messages = bossPartyChatMessageRepository.findByBossPartyIdOrderByCreatedAtDesc(partyId, pageable)

        return messages.map { message ->
            BossPartyChatMessageResponse(
                id = message.id,
                senderId = message.character.id,
                senderName = message.character.characterName,
                senderWorld = message.character.worldName,
                senderImage = message.character.characterImage,
                content = message.content,
                messageType = message.messageType,
                createdAt = message.createdAt.toString(),
                isMine = message.character.id == currentCharacterId,
                isDeleted = message.isDeleted
            )
        }
    }

    // 3. 파티 채팅 전체 삭제
    @Transactional
    fun deleteMessage(messageId: Long, userEmail: String): BossPartyChatMessage {
        val character = mapleCharacterRepository.findFirstByMemberEmailAndIsActiveTrue(userEmail)
            ?: throw MapleCharacterNotFoundException()

        val message = bossPartyChatMessageRepository.findById(messageId)
            .orElseThrow { BossPartyChatMessageNotFoundException() }

        // 권한 확인: 본인이 쓴 메시지인지 체크
        if (message.character.id != character.id) {
            throw DeleteBossPartyChatMessageDeniedException()
        }

        // 논리 삭제 처리
        message.markAsDeleted()
        // TODO: 이 시점에 WebSocket을 통해 "특정 ID의 메시지가 삭제됨"을 파티원들에게 브로드캐스팅하는 로직을 추가

        return message
    }
}