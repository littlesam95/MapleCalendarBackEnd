package com.sixclassguys.maplecalendar.domain.boss.service

import com.sixclassguys.maplecalendar.domain.boss.dto.BossPartyAlarmPeriodRequest
import com.sixclassguys.maplecalendar.domain.boss.dto.BossPartyAlarmTimeResponse
import com.sixclassguys.maplecalendar.domain.boss.dto.BossPartyCreateRequest
import com.sixclassguys.maplecalendar.domain.boss.dto.BossPartyMemberResponse
import com.sixclassguys.maplecalendar.domain.boss.dto.BossPartyResponse
import com.sixclassguys.maplecalendar.domain.boss.repository.BossPartyAlarmTimeRepository
import com.sixclassguys.maplecalendar.domain.boss.repository.BossPartyBoardRepository
import com.sixclassguys.maplecalendar.domain.boss.dto.BossPartyChatMessageResponse
import com.sixclassguys.maplecalendar.domain.boss.dto.BossPartyDetailResponse
import com.sixclassguys.maplecalendar.domain.boss.dto.BossPartyMemberDetail
import com.sixclassguys.maplecalendar.domain.boss.dto.BossPartyScheduleResponse
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
import com.sixclassguys.maplecalendar.domain.boss.enums.RegistrationMode
import com.sixclassguys.maplecalendar.domain.notification.service.NotificationService
import com.sixclassguys.maplecalendar.global.dto.AlarmType
import com.sixclassguys.maplecalendar.global.dto.RedisAlarmDto
import com.sixclassguys.maplecalendar.global.exception.AccessDeniedException
import com.sixclassguys.maplecalendar.global.exception.BossPartyChatMessageNotFoundException
import com.sixclassguys.maplecalendar.global.exception.BossPartyNotFoundException
import com.sixclassguys.maplecalendar.global.exception.DeleteBossPartyChatMessageDeniedException
import com.sixclassguys.maplecalendar.global.exception.MapleCharacterNotFoundException
import com.sixclassguys.maplecalendar.global.exception.MemberNotFoundException
import com.sixclassguys.maplecalendar.global.util.AlarmProducer
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Slice
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

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
    private val notificationService: NotificationService,
    private val alarmProducer: AlarmProducer
) {

    private val log = LoggerFactory.getLogger(javaClass)

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
            val bm = result[1] as BossPartyMember
            val isPartyAlarm = result[2] as Boolean
            val isChatAlarm = result[3] as Boolean

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
                joinStatus = bm.joinStatus ?: JoinStatus.INVITED,
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

        // 1. 파티원 리스트 변환 (기존 로직 동일)
        val memberDetails = party.members.map { m ->
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
            alarms = alarmTimes, // 👈 조회된 리스트 주입
            isLeader = isLeader,
            isPartyAlarmEnabled = mapping?.isPartyAlarmEnabled ?: true,
            isChatAlarmEnabled = mapping?.isChatAlarmEnabled ?: true,
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
        val allMembers = bossPartyMemberRepository.findAllWithMemberByPartyIds(uniquePartyIds, JoinStatus.ACCEPTED)

        // 3. 파티 ID별로 멤버들을 그룹화 (메모리에서 처리)
        val membersByPartyId = allMembers.groupBy { it.bossParty.id }

        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

        return alarms.sortedBy { it.alarmTime }.distinctBy { it.bossPartyId }.mapNotNull { alarm ->
            val party = parties.find { it.id == alarm.bossPartyId } ?: return@mapNotNull null
            val membersInThisParty = membersByPartyId[party.id]?.map { member ->
                BossPartyMemberDetail(
                    characterId = member.character.id,
                    characterName = member.character.characterName,
                    worldName = member.character.worldName,
                    characterClass = member.character.characterClass,
                    characterLevel = member.character.characterLevel,
                    characterImage = member.character.characterImage ?: "",
                    role = member.role,
                    isMyCharacter = member.character.member.id == member.id,
                    joinedAt = member.joinedAt.toString()
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
            ?: throw IllegalArgumentException("존재하지 않는 사용자입니다.")

        val mapping = memberBossPartyMappingRepository.findByMemberIdAndBossPartyId(member.id, bossPartyId)
            ?: throw IllegalArgumentException("해당 파티의 멤버가 아닙니다.")

        mapping.isPartyAlarmEnabled = !mapping.isPartyAlarmEnabled
        // Dirty Checking

        return mapping.isPartyAlarmEnabled
    }

    @Transactional
    fun togglePartyChatAlarm(email: String, bossPartyId: Long): Boolean {
        val member = memberRepository.findByEmail(email)
            ?: throw IllegalArgumentException("존재하지 않는 사용자입니다.")

        val mapping = memberBossPartyMappingRepository.findByMemberIdAndBossPartyId(member.id, bossPartyId)
            ?: throw IllegalArgumentException("해당 파티의 멤버가 아닙니다.")

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
            ?: throw AccessDeniedException("해당 파티의 멤버가 아닙니다.")

        // 2. 역할(Role) 확인 (방장인지 체크)
        if (partyMember.role != PartyRole.LEADER) {
            throw AccessDeniedException("방장만 알람을 설정할 수 있습니다.")
        }

        val alarmDateTime = date.atTime(hour, minute)

        if (alarmDateTime.isBefore(LocalDateTime.now())) {
            throw AccessDeniedException("현재보다 이전 시간에 예약된 알람입니다.")
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
            ?: throw AccessDeniedException("해당 파티의 멤버가 아닙니다.")

        if (partyMember.role != PartyRole.LEADER) {
            throw AccessDeniedException("방장만 알람 주기를 설정할 수 있습니다.")
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
            ?: throw AccessDeniedException("파티 멤버가 아닙니다.")

        if (leader.role != PartyRole.LEADER) {
            throw AccessDeniedException("방장만 알람을 삭제할 수 있습니다.")
        }

        // 2. 알람 조회
        val alarm = bossPartyAlarmTimeRepository.findByIdOrNull(alarmId)
            ?: throw IllegalArgumentException("존재하지 않는 알람입니다.")

        if (alarm.bossPartyId != partyId) {
            throw IllegalArgumentException("해당 파티의 알람이 아닙니다.")
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

    @Transactional
    fun hideChatMessage(partyId: Long, messageId: Long, userEmail: String): BossPartyChatMessage {
        // 1. 해당 파티에 참여 중인 유저의 정보를 가져옵니다. (이미 검증된 로직)
        val partyMember = bossPartyMemberRepository.findByBossPartyIdAndCharacterMemberEmail(partyId, userEmail)
            ?: throw AccessDeniedException("해당 파티의 멤버가 아닙니다.")

        if (partyMember.role != PartyRole.LEADER) {
            throw AccessDeniedException("방장만 메시지를 가릴 수 있습니다.")
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
            ?: throw AccessDeniedException("해당 파티의 멤버가 아닙니다.")

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
//        val bossParty = bossPartyRepository.findById(partyId).orElseThrow { AccessDeniedException("존재하지 않거나 삭제된 파티입니다.") }
        val bossParty = bossPartyRepository.findByIdAndIsDeletedFalse(partyId)
            ?: throw IllegalStateException("존재하지 않거나 삭제된 파티입니다.")

        val character =  mapleCharacterRepository.findById(inviteeId).orElseThrow { AccessDeniedException("캐릭터 정보가 없습니다.") }

        if (character.member.email == userEmail) {
            throw IllegalStateException("자기 자신의 캐릭터를 초대할 수 없습니다.")
        }

        val leader = bossPartyMemberRepository
            .findByBossPartyIdAndCharacterMemberEmailAndRole(partyId, userEmail, PartyRole.LEADER)
            ?: throw AccessDeniedException("초대 권한이 없습니다.")

        val exists = bossPartyMemberRepository.findByBossPartyIdAndCharacterId(partyId, inviteeId)
        if (exists != null) throw IllegalStateException("이미 초대되었거나 참여 중입니다")

        bossPartyMemberRepository.save(
            BossPartyMember(
                bossParty = bossParty,
                character = character,
                role = PartyRole.MEMBER,
                joinStatus = JoinStatus.INVITED
            )
        )
    }

    // 초대 수락
    @Transactional
    fun acceptInvitation(partyId: Long, characterId: Long, userEmail: String) {
        val bossParty = bossPartyRepository.findByIdAndIsDeletedFalse(partyId)
            ?: throw IllegalStateException("존재하지 않거나 삭제된 파티입니다.")

        val invitee = bossPartyMemberRepository.findByBossPartyIdAndCharacterMemberEmail(partyId, userEmail)
            ?: throw AccessDeniedException("파티 멤버가 아닙니다.")

        if(invitee.character.id != characterId){
            throw AccessDeniedException("본인만 수락할 수 있습니다.")
        }

        when (invitee.joinStatus) {
            JoinStatus.INVITED -> invitee.joinStatus = JoinStatus.ACCEPTED
            JoinStatus.ACCEPTED -> throw IllegalStateException("이미 수락된 상태입니다")
            else -> throw IllegalStateException("알 수 없는 상태입니다")
        }
    }

    // 초대 거절
    @Transactional
    fun declineInvitation(partyId: Long, characterId: Long, userEmail: String) {
        val bossParty = bossPartyRepository.findByIdAndIsDeletedFalse(partyId)
            ?: throw IllegalStateException("존재하지 않거나 삭제된 파티입니다.")

        val bpm = bossPartyMemberRepository.findByBossPartyIdAndCharacterMemberEmail(partyId, userEmail)
            ?: throw AccessDeniedException("파티 멤버가 아닙니다.")

        if(bpm.character.id != characterId){
            throw AccessDeniedException("본인만 거절할 수 있습니다.")
        }

        if (bpm.joinStatus != JoinStatus.INVITED) {
            throw IllegalStateException("거절할 수 없는 상태입니다")
        }

        bossPartyMemberRepository.delete(bpm)
    }

    // 추방
    @Transactional
    fun kickMember(partyId: Long, characterId: Long, userEmail: String) {
        val bossParty = bossPartyRepository.findByIdAndIsDeletedFalse(partyId)
            ?: throw IllegalStateException("존재하지 않거나 삭제된 파티입니다.")

        val leader = bossPartyMemberRepository
            .findByBossPartyIdAndCharacterMemberEmailAndRole(
                partyId,
                userEmail,
                PartyRole.LEADER
            )
            ?: throw AccessDeniedException("추방 권한이 없습니다")

        val target = bossPartyMemberRepository
            .findByBossPartyIdAndCharacterId(partyId, characterId)
            ?: throw IllegalStateException("대상 멤버가 없습니다")

        if (target.role == PartyRole.LEADER) {
            throw IllegalStateException("파티장은 추방할 수 없습니다")
        }

        bossPartyMemberRepository.delete(target)
    }

    // 탈퇴
    @Transactional
    fun leaveParty(partyId: Long, characterId: Long, userEmail: String) {
        val bossParty = bossPartyRepository.findByIdAndIsDeletedFalse(partyId)
            ?: throw IllegalStateException("존재하지 않거나 삭제된 파티입니다.")

//        val bossParty = bossPartyRepository.findById(partyId)
//            .orElseThrow { IllegalStateException("파티가 존재하지 않습니다.") }

        val bpm = bossPartyMemberRepository
            .findByBossPartyIdAndCharacterMemberEmail(partyId, userEmail)
            ?: throw AccessDeniedException("파티 멤버가 아닙니다.")

        if (bpm.character.id != characterId) {
            throw AccessDeniedException("본인만 탈퇴할 수 있습니다.")
        }

        // ACCEPTED 멤버 수만 카운트 (초대 상태 제외)
        val acceptedMembers = bossPartyMemberRepository
            .findAllByBossPartyId(partyId)
            .filter { it.joinStatus == JoinStatus.ACCEPTED }

        val acceptedCount = acceptedMembers.size

        // 1명만 남은 경우 → 파티 논리 삭제
        if (acceptedCount == 1) {
            bossParty.isDeleted = true
            bossPartyRepository.save(bossParty) // <-- 명시적 저장
            bossPartyMemberRepository.delete(bpm)
            return
        }

        // 리더가 탈퇴하는 경우 → 자동 위임
        if (bpm.role == PartyRole.LEADER) {

            val newLeader = acceptedMembers
                .firstOrNull { it.character.id != characterId }
                ?: throw IllegalStateException("양도할 멤버가 없습니다.")

            newLeader.role = PartyRole.LEADER
        }

        // 본인 삭제
        bossPartyMemberRepository.delete(bpm)
    }

    // 파티장 양도
    @Transactional
    fun transferLeader(partyId: Long, targetCharacterId: Long, userEmail: String) {
        val bossParty = bossPartyRepository.findByIdAndIsDeletedFalse(partyId)
            ?: throw IllegalStateException("존재하지 않거나 삭제된 파티입니다.")

        // 현재 리더 조회
        val currentLeader = bossPartyMemberRepository
            .findByBossPartyIdAndCharacterMemberEmailAndRole(
                partyId,
                userEmail,
                PartyRole.LEADER
            )
            ?: throw AccessDeniedException("파티장만 권한을 양도할 수 있습니다.")

        // 자기 자신에게 양도 방지
        if (currentLeader.character.id == targetCharacterId) {
            throw IllegalStateException("자기 자신에게는 양도할 수 없습니다.")
        }

        // 대상 멤버 조회
        val targetMember = bossPartyMemberRepository
            .findByBossPartyIdAndCharacterId(partyId, targetCharacterId)
            ?: throw IllegalStateException("해당 캐릭터는 파티 멤버가 아닙니다.")

        // 초대 상태 체크
        if (targetMember.joinStatus != JoinStatus.ACCEPTED) {
            throw IllegalStateException("수락된 멤버에게만 양도할 수 있습니다.")
        }

        // 역할 변경
        currentLeader.role = PartyRole.MEMBER
        targetMember.role = PartyRole.LEADER
    }
}