package com.sixclassguys.maplecalendar.domain.boss.service

import com.sixclassguys.maplecalendar.domain.boss.dto.BossPartyAlarmPeriodRequest
import com.sixclassguys.maplecalendar.domain.boss.dto.BossPartyAlarmTimeResponse
import com.sixclassguys.maplecalendar.domain.boss.dto.BossPartyCreateRequest
import com.sixclassguys.maplecalendar.domain.boss.dto.BossPartyMemberResponse
import com.sixclassguys.maplecalendar.domain.boss.dto.BossPartyResponse
import com.sixclassguys.maplecalendar.domain.boss.repository.BossPartyAlarmTimeRepository
import com.sixclassguys.maplecalendar.domain.boss.dto.BossPartyChatMessageResponse
import com.sixclassguys.maplecalendar.domain.boss.dto.BossPartyDetailResponse
import com.sixclassguys.maplecalendar.domain.boss.dto.BossPartyMemberDetail
import com.sixclassguys.maplecalendar.domain.boss.dto.BossPartyScheduleResponse
import com.sixclassguys.maplecalendar.domain.boss.dto.BossPartySystemEvent
import com.sixclassguys.maplecalendar.domain.boss.dto.toResponse
import com.sixclassguys.maplecalendar.domain.boss.entity.BossParty
import com.sixclassguys.maplecalendar.domain.boss.entity.BossPartyAlarmTime
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
import com.sixclassguys.maplecalendar.domain.boss.enums.JoinStatus.*
import com.sixclassguys.maplecalendar.domain.boss.enums.RegistrationMode
import com.sixclassguys.maplecalendar.domain.notification.service.NotificationService
import com.sixclassguys.maplecalendar.global.dto.AlarmType
import com.sixclassguys.maplecalendar.global.dto.RedisAlarmDto
import com.sixclassguys.maplecalendar.global.exception.AlreadyPartyMemberException
import com.sixclassguys.maplecalendar.global.exception.BossPartyAlarmNotFoundException
import com.sixclassguys.maplecalendar.global.exception.BossPartyAlarmUnauthorizedException
import com.sixclassguys.maplecalendar.global.exception.BossPartyCapacityExceededException
import com.sixclassguys.maplecalendar.global.exception.BossPartyChatMessageNotFoundException
import com.sixclassguys.maplecalendar.global.exception.BossPartyInvitationNotFoundException
import com.sixclassguys.maplecalendar.global.exception.BossPartyMemberNotFoundException
import com.sixclassguys.maplecalendar.global.exception.BossPartyNotFoundException
import com.sixclassguys.maplecalendar.global.exception.DeleteBossPartyChatMessageDeniedException
import com.sixclassguys.maplecalendar.global.exception.InvalidAlarmTimeException
import com.sixclassguys.maplecalendar.global.exception.InvalidBossPartyAcceptInvitationException
import com.sixclassguys.maplecalendar.global.exception.InvalidBossPartyInvitationDeclineException
import com.sixclassguys.maplecalendar.global.exception.InvalidBossPartyKickException
import com.sixclassguys.maplecalendar.global.exception.InvalidBossPartyLeaderException
import com.sixclassguys.maplecalendar.global.exception.InvalidBossPartyTransferLeaderException
import com.sixclassguys.maplecalendar.global.exception.InvitationPendingException
import com.sixclassguys.maplecalendar.global.exception.MapleCharacterNotFoundException
import com.sixclassguys.maplecalendar.global.exception.MemberNotFoundException
import com.sixclassguys.maplecalendar.global.exception.SelfInvitationException
import com.sixclassguys.maplecalendar.global.util.AlarmProducer
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Slice
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

@Service
class BossPartyService(
    private val eventPublisher: ApplicationEventPublisher,
    private val bossPartyRepository: BossPartyRepository,
    private val bossPartyMemberRepository: BossPartyMemberRepository,
    private val memberBossPartyMappingRepository: MemberBossPartyMappingRepository,
    private val mapleCharacterRepository: MapleCharacterRepository,
    private val memberRepository: MemberRepository,
    private val bossPartyAlarmTimeRepository: BossPartyAlarmTimeRepository,
    private val bossPartyChatMessageRepository: BossPartyChatMessageRepository,
    private val notificationService: NotificationService,
    private val alarmProducer: AlarmProducer
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun createParty(req: BossPartyCreateRequest, userEmail: String): Long {
        // 로그인한 유저(Member) 조회
        val member = memberRepository.findByEmail(userEmail)
            ?: throw MemberNotFoundException()

        // 캐릭터 조회 (본인의 캐릭터인지 검증 로직을 추가하면 더 좋음)
        val character = mapleCharacterRepository.findById(req.characterId)
            .orElseThrow { MapleCharacterNotFoundException() }

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
            joinStatus = ACCEPTED,
            joinedAt = LocalDateTime.now()
        )
        bossPartyMemberRepository.save(leader)

        // 3. 리더의 개인 알람 설정 매핑 추가 (MemberBossPartyMapping)
        val mapping = MemberBossPartyMapping(
            bossPartyId = savedParty.id,
            memberId = member.id, // 캐릭터가 속한 계정(Member) ID
            isPartyAlarmEnabled = false, // 기본값 true
            isChatAlarmEnabled = false   // 기본값 true
        )
        memberBossPartyMappingRepository.save(mapping)

        return savedParty.id
    }

    @Transactional(readOnly = true)
    fun getBossParties(userEmail: String): List<BossPartyResponse> {
        val member = memberRepository.findByEmail(userEmail)
            ?: throw MemberNotFoundException()

        val targetStatuses = listOf(ACCEPTED, INVITED)
        val results = bossPartyRepository.findAllPartiesByMemberId(member.id, targetStatuses)

        return results.map { result ->
            val p = result[0] as BossParty
            val bm = result[1] as BossPartyMember
            val isPartyAlarm = result[2] as Boolean
            val isChatAlarm = result[3] as Boolean

            // 방장 찾기
            val leader = p.members.find { it.role == PartyRole.LEADER }?.character?.characterName ?: "알 수 없는 캐릭터"
            // 승인된 멤버 수 계산
            val totalCount = p.members.count { it.joinStatus == ACCEPTED }

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
                joinStatus = bm.joinStatus ?: INVITED,
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

        val isGlobalEnabled = member.isGlobalAlarmEnabled

        // 1. 파티원 리스트 변환 (기존 로직 동일)
        val memberDetails = party.members.filter{ it.joinStatus == ACCEPTED }.map { m ->
            BossPartyMemberDetail(
                characterId = m.character.id,
                characterName = m.character.characterName,
                worldName = m.character.worldName,
                characterClass = m.character.characterClass,
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

        // 2. 🔔 미발송 알람 리스트 조회 (isSent = false)
        val alarmTimes = bossPartyAlarmTimeRepository
            .findByBossPartyIdAndIsSentFalseOrderByAlarmTimeAsc(partyId)
            .map {
                BossPartyAlarmTimeResponse(
                    id = it.id,
                    alarmTime = it.alarmTime,
                    message = it.message,
                    isSent = it.isSent,
                    registrationMode = it.registrationMode
                )
            }

        val mapping = memberBossPartyMappingRepository.findByMemberIdAndBossPartyId(member.id, partyId)

        return BossPartyDetailResponse(
            id = party.id,
            title = party.title,
            description = party.description,
            boss = party.boss,
            difficulty = party.difficulty,
            members = memberDetails,
            alarms = alarmTimes, // 조회된 리스트 주입
            isLeader = isLeader,
            isPartyAlarmEnabled = if (!isGlobalEnabled) false else mapping?.isPartyAlarmEnabled ?: false,
            isChatAlarmEnabled = if (!isGlobalEnabled) false else mapping?.isChatAlarmEnabled ?: false,
            alarmDayOfWeek = party.alarmDayOfWeek,
            alarmHour = party.alarmHour,
            alarmMinute = party.alarmMinute,
            alarmMessage = party.alarmMessage,
            createdAt = party.createdAt
        )
    }

    @Transactional(readOnly = true)
    fun getDailyBossSchedules(year: Int, month: Int, day: Int, userEmail: String): List<BossPartyScheduleResponse> {
        val member = memberRepository.findByEmail(userEmail)
            ?: throw MemberNotFoundException()

        val startTime = LocalDateTime.of(year, month, day, 0, 0)
        val endTime = LocalDateTime.of(year, month, day, 23, 59)

        val alarms = bossPartyAlarmTimeRepository.findMemberSchedules(member.id, startTime, endTime)
        if (alarms.isEmpty()) return emptyList()

        // 같은 파티인데 알람이 여러 번 등록된 경우 중복 제거 (partyId 기준)
        val uniquePartyIds = alarms.map { it.bossPartyId }.distinct()

        if (uniquePartyIds.isEmpty()) return emptyList()

        // 1. 모든 파티 정보를 한 번에 가져오기
        val parties = bossPartyRepository.findAllById(uniquePartyIds)

        // 2. [핵심] 모든 파티의 멤버들을 한 번의 쿼리로 가져오기 (Repository에 메서드 추가 필요)
        val allMembers = bossPartyMemberRepository.findAllWithMemberByPartyIds(uniquePartyIds, ACCEPTED)

        // 3. 파티 ID별로 멤버들을 그룹화 (메모리에서 처리)
        val membersByPartyId = allMembers.groupBy { it.bossParty.id }

        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

        return alarms.sortedBy { it.alarmTime }.distinctBy { it.bossPartyId }.mapNotNull { alarm ->
            val party = parties.find { it.id == alarm.bossPartyId } ?: return@mapNotNull null
            val membersInThisParty = membersByPartyId[party.id]?.map { partyMember ->
                BossPartyMemberDetail(
                    characterId = partyMember.character.id,
                    characterName = partyMember.character.characterName,
                    worldName = partyMember.character.worldName,
                    characterClass = partyMember.character.characterClass,
                    characterLevel = partyMember.character.characterLevel,
                    characterImage = partyMember.character.characterImage ?: "",
                    role = partyMember.role,
                    isMyCharacter = partyMember.character.member.id == member.id,
                    joinedAt = partyMember.joinedAt.toString()
                )
            }?.sortedByDescending { it.role == PartyRole.LEADER } ?: emptyList()

            BossPartyScheduleResponse(
                bossPartyId = party.id,
                boss = party.boss,
                bossDifficulty = party.difficulty,
                members = membersInThisParty, // 그룹화된 맵에서 꺼내기
                time = alarm.alarmTime.format(timeFormatter)
            )
        }
    }

    @Transactional
    fun togglePartyAlarm(email: String, bossPartyId: Long): Boolean {
        val member = memberRepository.findByEmail(email)
            ?: throw MemberNotFoundException()

        val mapping = memberBossPartyMappingRepository.findByMemberIdAndBossPartyId(member.id, bossPartyId)
            ?: throw BossPartyMemberNotFoundException()

        mapping.isPartyAlarmEnabled = !mapping.isPartyAlarmEnabled
        // Dirty Checking

        return mapping.isPartyAlarmEnabled
    }

    @Transactional
    fun togglePartyChatAlarm(email: String, bossPartyId: Long): Boolean {
        val member = memberRepository.findByEmail(email)
            ?: throw MemberNotFoundException()

        val mapping = memberBossPartyMappingRepository.findByMemberIdAndBossPartyId(member.id, bossPartyId)
            ?: throw BossPartyMemberNotFoundException()

        mapping.isChatAlarmEnabled = !mapping.isChatAlarmEnabled
        // Dirty Checking

        return mapping.isChatAlarmEnabled
    }

    @Transactional
    fun createAlarmTime(partyId: Long, userEmail: String, hour: Int, minute: Int, date: LocalDate, message: String) {
        // 1. 해당 파티에 속한 유저 정보와 역할을 한 번에 조회
        val party = bossPartyRepository.findById(partyId)
            .orElseThrow { BossPartyNotFoundException() }
        val partyMember = bossPartyMemberRepository.findByBossPartyIdAndCharacterMemberEmail(partyId, userEmail)
            ?: throw BossPartyMemberNotFoundException()

        // 2. 역할(Role) 확인 (방장인지 체크)
        if (partyMember.role != PartyRole.LEADER) {
            throw InvalidBossPartyLeaderException()
        }

        val alarmDateTime = date.atTime(hour, minute)

        if (alarmDateTime.isBefore(LocalDateTime.now())) {
            throw InvalidAlarmTimeException() as Throwable
        }

        // 2. 알람 시간 데이터 저장
        val savedTime = bossPartyAlarmTimeRepository.save(
            BossPartyAlarmTime(
                bossPartyId = partyId,
                alarmTime = alarmDateTime,
                message = message,
                registrationMode = RegistrationMode.SELECT
            )
        )

        // 3. RabbitMQ 예약 (파티 단위로 1개의 메시지만 발행)
        val dto = RedisAlarmDto(
            type = AlarmType.BOSS,
            targetId = savedTime.id,
            memberId = 0L, // 개별 전송이 아니므로 0 또는 공백 처리
            contentId = partyId, // DTO에 partyId 필드 추가 필요
            title = party.title,
            message = message
        )
        alarmProducer.reserveAlarm(dto, alarmDateTime)

        notificationService.sendRefreshSignal(partyId)
    }

    private fun calculateNextAlarmTime(dayOfWeek: DayOfWeek, hour: Int, minute: Int): LocalDateTime {
        val now = LocalDateTime.now()
        var next = now.with(TemporalAdjusters.nextOrSame(dayOfWeek))
            .withHour(hour).withMinute(minute).withSecond(0).withNano(0)

        // 만약 계산된 시간이 현재보다 과거라면(오늘인데 시간이 지난 경우), 다음 주 해당 요일로 넘김
        if (next.isBefore(now)) {
            next = next.plusWeeks(1)
        }

        // 추가적인 메이플 목요일 주차 로직이 필요하다면 여기서 검증
        return next
    }

    @Transactional
    fun updateBossPartyAlarmPeriod(
        partyId: Long,
        userEmail: String,
        request: BossPartyAlarmPeriodRequest
    ) {
        // 1. 방장 권한 확인 (기존 로직 활용)
        val partyMember = bossPartyMemberRepository.findByBossPartyIdAndCharacterMemberEmail(partyId, userEmail)
            ?: throw BossPartyMemberNotFoundException()

        if (partyMember.role != PartyRole.LEADER) {
            throw InvalidBossPartyLeaderException()
        }

        // 2. BossParty 엔티티에 주기 정보 갱신
        val party = bossPartyRepository.findById(partyId)
            .orElseThrow { BossPartyNotFoundException() }

        if (request.dayOfWeek == null) {
            // [CASE] 주기를 제거하는 경우
            party.apply {
                this.alarmDayOfWeek = null
                this.alarmHour = null
                this.alarmMinute = null
                this.alarmMessage = null
            }

            // 기존의 모든 주기성 알람(PERIODIC) 삭제 (미래 알람 위주)
            bossPartyAlarmTimeRepository.deleteFuturePeriodicAlarms(
                partyId,
                RegistrationMode.PERIODIC
            )

            // RabbitMQ 예약 취소 로직이 필요하다면 여기서 추가 수행 (보통 DB 삭제 시 Consumer에서 처리하거나 여기서 별도 처리)

        } else {
            party.apply {
                this.alarmDayOfWeek = request.dayOfWeek
                this.alarmHour = request.hour
                this.alarmMinute = request.minute
                this.alarmMessage = request.message
            }

            // 3. 기존의 '미래 주기 알람(PERIODIC)' 데이터 제거
            // SELECT 모드(수동 예약)는 유지하고, 기존 주기에 의해 생성된 미발송 알람만 지웁니다.
            bossPartyAlarmTimeRepository.deleteFuturePeriodicAlarms(
                partyId,
                RegistrationMode.PERIODIC
            )

            // 4. 즉시 반영 여부에 따른 신규 알람 예약
            if (request.isImmediateApply) {
                val nextAlarmTime = calculateNextAlarmTime(request.dayOfWeek, request.hour, request.minute)

                // 중복 방지: 동일 시간에 이미 수동(SELECT) 알람이 있는지 확인
                if (!bossPartyAlarmTimeRepository.existsByBossPartyIdAndAlarmTime(partyId, nextAlarmTime)) {
                    val savedTime = bossPartyAlarmTimeRepository.save(
                        BossPartyAlarmTime(
                            bossPartyId = partyId,
                            alarmTime = nextAlarmTime,
                            message = request.message,
                            registrationMode = RegistrationMode.PERIODIC
                        )
                    )

                    // 5. RabbitMQ 예약 발송
                    val dto = RedisAlarmDto(
                        type = AlarmType.BOSS,
                        targetId = savedTime.id,
                        memberId = 0L,
                        contentId = partyId,
                        title = party.title,
                        message = request.message
                    )
                    alarmProducer.reserveAlarm(dto, nextAlarmTime)
                }
            }
        }

        notificationService.sendRefreshSignal(partyId)
    }

    @Transactional
    fun deleteAlarm(partyId: Long, alarmId: Long, userEmail: String) {
        // 1. 방장 권한 확인 (기존 로직)
        val leader = bossPartyMemberRepository.findByBossPartyIdAndCharacterMemberEmail(partyId, userEmail)
            ?: throw BossPartyMemberNotFoundException()

        if (leader.role != PartyRole.LEADER) {
            throw InvalidBossPartyLeaderException()
        }

        // 2. 알람 조회
        val alarm = bossPartyAlarmTimeRepository.findByIdOrNull(alarmId)
            ?: throw BossPartyAlarmNotFoundException()

        if (alarm.bossPartyId != partyId) {
            throw BossPartyAlarmUnauthorizedException()
        }

        // 3. 물리적 삭제 대신 상태 변경 (Soft Delete)
        // isSent를 true로 만들면 리스트 조회(findBy...AndIsSentFalse)에서도 자동으로 제외됩니다.
        alarm.isSent = true

        notificationService.sendRefreshSignal(partyId)
    }

    @Transactional
    fun getBossPartyAlarmTimes(bossPartyId: Long): List<BossPartyAlarmTimeResponse> {
        val alarmTimes = bossPartyAlarmTimeRepository.findByBossPartyIdAndIsSentFalseOrderByAlarmTimeAsc(bossPartyId)

        return alarmTimes.map {
            BossPartyAlarmTimeResponse(
                id = it.id,
                alarmTime = it.alarmTime,
                message = it.message,
                isSent = it.isSent,
                registrationMode = it.registrationMode
            )
        }
    }

    @Transactional
    fun getAcceptedMembersByBossPartyId(bossPartyId: Long): List<BossPartyMemberResponse> {
        val bossPartyMembers = bossPartyMemberRepository.findAllWithMemberAndTokensByPartyId(
            bossPartyId,
            ACCEPTED // 수락 상태만 조회
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
            ?: throw BossPartyMemberNotFoundException()

        val currentCharacterId = partyMember.character.id
        val pageable = PageRequest.of(page, size, Sort.by("createdAt").descending())

        // 2. 메시지 내역을 가져오면서 찾은 characterId로 isMine을 계산합니다.
        return bossPartyChatMessageRepository.findByBossPartyIdOrderByCreatedAtDesc(partyId, pageable)
            .map { it.toResponse(currentCharacterId) }
    }

    @Transactional
    fun hideChatMessage(partyId: Long, messageId: Long, userEmail: String): BossPartyChatMessage {
        // 1. 해당 파티에 참여 중인 유저의 정보를 가져옵니다. (이미 검증된 로직)
        val partyMember = bossPartyMemberRepository.findByBossPartyIdAndCharacterMemberEmail(partyId, userEmail)
            ?: throw BossPartyMemberNotFoundException()

        if (partyMember.role != PartyRole.LEADER) {
            throw InvalidBossPartyLeaderException("파티장만 해당 메시지를 가릴 수 있어요.")
        }

        val message = bossPartyChatMessageRepository.findById(messageId)
            .orElseThrow { BossPartyChatMessageNotFoundException() }

        message.hide()

        return message
    }

    // 3. 파티 채팅 전체 삭제
    @Transactional
    fun deleteMessage(partyId: Long, messageId: Long, userEmail: String): BossPartyChatMessage {
        // 1. 해당 파티에 참여 중인 유저의 정보를 가져옵니다. (이미 검증된 로직)
        val partyMember = bossPartyMemberRepository.findByBossPartyIdAndCharacterMemberEmail(partyId, userEmail)
            ?: throw BossPartyMemberNotFoundException()

        val message = bossPartyChatMessageRepository.findById(messageId)
            .orElseThrow { BossPartyChatMessageNotFoundException() }

        // 2. 권한 확인: 메시지를 쓴 캐릭터 ID와 현재 파티에 참여 중인 내 캐릭터 ID가 같은지 비교
        if (message.character.id != partyMember.character.id) {
            throw DeleteBossPartyChatMessageDeniedException()
        }

        // 3. 논리 삭제 처리
        message.markAsDeleted()

        return message
    }

    @Transactional
    fun inviteMember(partyId: Long, inviteeId: Long, userEmail: String) {
        val bossParty = bossPartyRepository.findByIdAndIsDeletedFalse(partyId)
            ?: throw BossPartyNotFoundException()

        if (bossParty.isFull()) {
            throw BossPartyCapacityExceededException()
        }

        val character =  mapleCharacterRepository.findById(inviteeId).orElseThrow { MapleCharacterNotFoundException() }

        if (character.member.email == userEmail) {
            throw SelfInvitationException()
        }

        val leader = bossPartyMemberRepository
            .findByBossPartyIdAndCharacterMemberEmailAndRole(partyId, userEmail, PartyRole.LEADER)
            ?: throw InvalidBossPartyLeaderException("파티장만 초대 기능을 이용할 수 있어요.")

        val exists = bossPartyMemberRepository.findByBossPartyIdAndCharacterId(partyId, inviteeId)
        if (exists != null) {
            when (exists.joinStatus) {
                ACCEPTED -> throw AlreadyPartyMemberException()
                INVITED -> throw InvitationPendingException()
                DELETED -> {
                    // 기존 DELETED 기록이 있다면 INVITED로 다시 변경 (재초대)
                    if (bossParty.isFull()) throw BossPartyCapacityExceededException()
                    exists.joinStatus = INVITED
                    // 필요하다면 초대 시간을 현재 시간으로 갱신
                    // existingMember.joinedAt = LocalDateTime.now()
                }

                null -> {}
            }
        } else {
            // 🚀 아예 기록이 없다면 새로 생성
            bossPartyMemberRepository.save(
                BossPartyMember(
                    bossParty = bossParty,
                    character = character,
                    role = PartyRole.MEMBER,
                    joinStatus = INVITED
                )
            )
            val mapping = MemberBossPartyMapping(
                bossPartyId = bossParty.id,
                memberId = character.member.id, // 캐릭터가 속한 계정(Member) ID
                isPartyAlarmEnabled = false, // 기본값 true
                isChatAlarmEnabled = false   // 기본값 true
            )
            memberBossPartyMappingRepository.save(mapping)
        }

        TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {

            override fun afterCommit() {
                notificationService.sendBossPartyInvitationAlarm(
                    partyId = bossParty.id,
                    inviteeCharacterId = character.id,
                    partyTitle = bossParty.title,
                    boss = bossParty.boss,
                    bossDifficulty = bossParty.difficulty
                )
            }
        })
    }

    // 초대 수락
    @Transactional
    fun acceptInvitation(partyId: Long, userEmail: String): Long {
        val bossParty = bossPartyRepository.findByIdAndIsDeletedFalse(partyId)
            ?: throw BossPartyNotFoundException()

        if (bossParty.isFull()) {
            throw BossPartyCapacityExceededException()
        }

        // 해당 유저(userEmail)가 이 파티(partyId)에 초대받은(INVITED) 이력이 있는지 조회
        // 만약 한 유저의 여러 캐릭터가 초대될 수 없는 구조라면 단건 조회가 적절합니다.
        val invitee = bossPartyMemberRepository.findByBossPartyIdAndCharacterMemberEmailInvited(partyId, userEmail)
            ?: throw BossPartyInvitationNotFoundException()

        // 상태 검증: 이미 수락했거나 다른 상태인지 확인
        if (invitee.joinStatus != INVITED) {
            throw InvalidBossPartyAcceptInvitationException()
        }

        // 상태 변경 (수락)
        invitee.joinStatus = ACCEPTED

        // 추방 안내 메시지를 채팅창에 발행하기
        val content = "${invitee.character.characterName}님이 파티에 가입되었어요."

        val savedMsg = saveMessage(partyId, invitee.character.id, content, BossPartyChatMessageType.JOINED)

        eventPublisher.publishEvent(
            BossPartySystemEvent(
                partyId = partyId,
                characterId = invitee.character.id,
                message = savedMsg
            )
        )

        // 트랜잭션이 성공적으로 COMMIT된 후에만 알림 발송
        TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {

            override fun afterCommit() {
                notificationService.sendBossPartyAcceptanceAlarm(
                    partyId = bossParty.id,
                    joinedCharacter = invitee.character,
                    partyTitle = bossParty.title,
                    boss = bossParty.boss,
                    bossDifficulty = bossParty.difficulty
                )
            }
        })

        return bossParty.id
    }

    // 초대 거절
    @Transactional
    fun declineInvitation(partyId: Long, userEmail: String): List<BossPartyResponse> {
        val bossParty = bossPartyRepository.findByIdAndIsDeletedFalse(partyId)
            ?: throw BossPartyNotFoundException()

        // 해당 유저가 이 파티에 초대된 기록이 있는지 조회
        val invitee = bossPartyMemberRepository.findByBossPartyIdAndCharacterMemberEmailInvited(partyId, userEmail)
            ?: throw BossPartyInvitationNotFoundException()

        // 초대(INVITED) 상태일 때만 거절 가능
        if (invitee.joinStatus != INVITED) {
            throw InvalidBossPartyInvitationDeclineException()
        }

        // 초대 정보 삭제 (거절)
        invitee.joinStatus = DELETED

        // 파티장에게 거절 알림 발송
        TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {

            override fun afterCommit() {
                notificationService.sendBossPartyDeclineAlarm(
                    partyId = partyId,
                    declinerCharacter = invitee.character,
                    partyTitle = bossParty.title,
                    boss = bossParty.boss,
                    bossDifficulty = bossParty.difficulty
                )
            }
        })

        // 갱신된 내 파티 리스트 반환
        return getBossParties(userEmail)
    }

    // 추방
    @Transactional
    fun kickMember(partyId: Long, characterId: Long, userEmail: String) {
        val bossParty = bossPartyRepository.findByIdAndIsDeletedFalse(partyId)
            ?: throw BossPartyNotFoundException()

        val leader = bossPartyMemberRepository
            .findByBossPartyIdAndCharacterMemberEmailAndRole(
                partyId,
                userEmail,
                PartyRole.LEADER
            )
            ?: throw InvalidBossPartyLeaderException("파티장만 추방할 수 있어요.")

        val target = bossPartyMemberRepository
            .findByBossPartyIdAndCharacterId(partyId, characterId)
            ?: throw BossPartyMemberNotFoundException()

        if (target.role == PartyRole.LEADER) {
            throw InvalidBossPartyKickException("파티장은 추방할 수 없어요.")
        }

        target.joinStatus = DELETED

        // 추방 안내 메시지를 채팅창에 발행하기
        val content = "${target.character.characterName}님이 파티에서 추방되었어요."

        val savedMsg = saveMessage(partyId, characterId, content, BossPartyChatMessageType.KICKED)

        eventPublisher.publishEvent(
            BossPartySystemEvent(
                partyId = partyId,
                characterId = characterId,
                message = savedMsg
            )
        )

        TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {

            override fun afterCommit() {
                notificationService.sendBossPartyKickAlarm(
                    partyId = partyId,
                    kickedCharacter = target.character,
                    partyTitle = bossParty.title,
                    boss = bossParty.boss,
                    bossDifficulty = bossParty.difficulty
                )
            }
        })
    }

    // 탈퇴
    @Transactional
    fun leaveParty(partyId: Long, userEmail: String): List<BossPartyResponse> {
        val bossParty = bossPartyRepository.findByIdAndIsDeletedFalse(partyId)
            ?: throw BossPartyNotFoundException()

        // 1. 유저 이메일과 파티 ID로 참여 정보 조회
        val bpm = bossPartyMemberRepository
            .findByBossPartyIdAndCharacterMemberEmail(partyId, userEmail)
            ?: throw BossPartyMemberNotFoundException()

        val leaverCharacterId = bpm.character.id // 탈퇴하는 캐릭터의 ID 보관
        var newLeaderName: String? = null

        // 2. 현재 활성화된(ACCEPTED) 멤버 목록 추출
        val acceptedMembers = bossPartyMemberRepository
            .findAllByBossPartyId(partyId)
            .filter { it.joinStatus == ACCEPTED }

        // 3. 1명만 남은 경우 처리
        if (acceptedMembers.size == 1) {
            bossParty.isDeleted = true
            bpm.joinStatus = DELETED

            TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {

                override fun afterCommit() {
                    notificationService.sendBossPartyLeaveAlarm(
                        partyId = partyId,
                        leaver = bpm.character,
                        newLeaderName = newLeaderName,
                        partyTitle = bossParty.title,
                        boss = bossParty.boss,
                        bossDifficulty = bossParty.difficulty
                    )
                }
            })

            return getBossParties(userEmail)
        }

        // 4. 리더 탈퇴 시 자동 위임 처리
        if (bpm.role == PartyRole.LEADER) {
            val newLeader = acceptedMembers
                .firstOrNull { it.character.id != leaverCharacterId } // 보관해둔 ID와 비교
                ?: throw BossPartyMemberNotFoundException("양도할 멤버가 없어요.")

            newLeader.role = PartyRole.LEADER
            newLeaderName = newLeader.character.characterName
        }

        // 5. 본인 데이터 삭제 및 알림 발송
        bpm.joinStatus = DELETED
        bpm.role = PartyRole.MEMBER

        // 6. 추방 안내 메시지를 채팅창에 발행하기
        val content = "${bpm.character.characterName}님이 파티에서 탈퇴했어요."
        val savedMsg = saveMessage(partyId, leaverCharacterId, content, BossPartyChatMessageType.KICKED)

        eventPublisher.publishEvent(
            BossPartySystemEvent(
                partyId = partyId,
                characterId = leaverCharacterId,
                message = savedMsg
            )
        )

        TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {

            override fun afterCommit() {
                notificationService.sendBossPartyLeaveAlarm(
                    partyId = partyId,
                    leaver = bpm.character,
                    newLeaderName = newLeaderName,
                    partyTitle = bossParty.title,
                    boss = bossParty.boss,
                    bossDifficulty = bossParty.difficulty
                )
            }
        })

        return getBossParties(userEmail)
    }

    // 파티장 양도
    @Transactional
    fun transferLeader(partyId: Long, targetCharacterId: Long, userEmail: String) {
        val bossParty = bossPartyRepository.findByIdAndIsDeletedFalse(partyId)
            ?: throw BossPartyNotFoundException()

        // 현재 리더 조회
        val currentLeader = bossPartyMemberRepository
            .findByBossPartyIdAndCharacterMemberEmailAndRole(
                partyId,
                userEmail,
                PartyRole.LEADER
            )
            ?: throw InvalidBossPartyLeaderException("파티장만 양도할 수 있어요.")

        // 자기 자신에게 양도 방지
        if (currentLeader.character.id == targetCharacterId) {
            throw InvalidBossPartyTransferLeaderException("자기 자신에게는 파티장을 양도할 수 없어요.")
        }

        // 대상 멤버 조회
        val targetMember = bossPartyMemberRepository
            .findByBossPartyIdAndCharacterId(partyId, targetCharacterId)
            ?: throw BossPartyMemberNotFoundException()

        // 초대 상태 체크
        if (targetMember.joinStatus != ACCEPTED) {
            throw InvalidBossPartyTransferLeaderException("파티원에게만 파티장을 양도할 수 있어요.")
        }

        // 역할 변경
        currentLeader.role = PartyRole.MEMBER
        targetMember.role = PartyRole.LEADER

        TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {

            override fun afterCommit() {
                notificationService.sendBossPartyTransferAlarm(
                    partyId = bossParty.id,
                    newLeader = targetMember.character,
                    partyTitle = bossParty.title,
                    boss = bossParty.boss,
                    bossDifficulty = bossParty.difficulty
                )
            }
        })
    }
}