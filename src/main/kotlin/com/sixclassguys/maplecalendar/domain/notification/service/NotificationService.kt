package com.sixclassguys.maplecalendar.domain.notification.service

import com.google.firebase.messaging.AndroidConfig
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.Notification
import com.sixclassguys.maplecalendar.domain.boss.entity.BossPartyAlarmTime
import com.sixclassguys.maplecalendar.domain.boss.enums.JoinStatus
import com.sixclassguys.maplecalendar.domain.boss.enums.RegistrationMode
import com.sixclassguys.maplecalendar.domain.boss.handler.BossPartyChatWebSocketHandler
import com.sixclassguys.maplecalendar.domain.boss.repository.BossPartyAlarmTimeRepository
import com.sixclassguys.maplecalendar.domain.boss.repository.BossPartyMemberRepository
import com.sixclassguys.maplecalendar.domain.boss.repository.BossPartyRepository
import com.sixclassguys.maplecalendar.domain.boss.repository.MemberBossPartyMappingRepository
import com.sixclassguys.maplecalendar.domain.eventalarm.repository.EventAlarmTimeRepository
import com.sixclassguys.maplecalendar.domain.member.entity.Member
import com.sixclassguys.maplecalendar.domain.member.repository.MemberRepository
import com.sixclassguys.maplecalendar.domain.notification.dto.FcmTokenRequest
import com.sixclassguys.maplecalendar.domain.notification.entity.NotificationToken
import com.sixclassguys.maplecalendar.domain.notification.repository.NotificationTokenRepository
import com.sixclassguys.maplecalendar.global.dto.AlarmType
import com.sixclassguys.maplecalendar.global.dto.RedisAlarmDto
import com.sixclassguys.maplecalendar.global.util.AlarmProducer
import com.sixclassguys.maplecalendar.infrastructure.persistence.event.EventRepository
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Service
@Transactional
class NotificationService(
    private val redisTemplate: RedisTemplate<String, String>,
    private val notificationTokenRepository: NotificationTokenRepository,
    private val eventAlarmTimeRepository: EventAlarmTimeRepository,
    private val bossPartyRepository: BossPartyRepository,
    private val bossPartyMemberRepository: BossPartyMemberRepository,
    private val bossPartyAlarmTimeRepository: BossPartyAlarmTimeRepository,
    private val memberBossPartyMappingRepository: MemberBossPartyMappingRepository,
    private val eventRepository: EventRepository,
    private val memberRepository: MemberRepository,
    private val alarmProducer: AlarmProducer
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun processAlarm(alarm: RedisAlarmDto) {
        when (alarm.type) {
            AlarmType.EVENT -> processEventAlarm(alarm)
            AlarmType.BOSS -> processBossPartyAlarm(alarm)
            else -> {}
        }
    }

    @Transactional
    fun processEventAlarm(alarm: RedisAlarmDto) {
        // 1. 유효성 검사 (알람이 꺼져있거나 이미 발송되었는지 체크)
        if (!checkEventAlarmValid(alarm.targetId) || isAlarmCancelled(alarm)) {
            log.info("🚫 취소되었거나 유효하지 않은 이벤트 알람입니다. targetId=${alarm.targetId}")
            return
        }

        // 2. 대상 유저 조회
        val member = memberRepository.findByIdOrNull(alarm.memberId)
            ?: return

        // 3. FCM 발송
        sendFcmPush(member, alarm)

        // 4. 발송 완료 처리
        markAsSent(alarm)
        log.info("🎁 이벤트 알람 발송 완료: 유저=${member.id}, targetId=${alarm.targetId}")
    }

    @Transactional
    fun processBossPartyAlarm(alarm: RedisAlarmDto) {
        val alarmTimeEntity = bossPartyAlarmTimeRepository.findByIdOrNull(alarm.targetId)
            ?: return

        if (!checkBossAlarmValid(alarm.targetId) || isAlarmCancelled(alarm)) {
            log.info("🚫 취소되었거나 이미 처리된 알람입니다. targetId=${alarm.targetId}")
            return
        }

        val partyId = alarm.contentId

        // 1. 해당 파티의 승인된 멤버(ACCEPTED) 목록을 가져옴
        val members = bossPartyMemberRepository.findAllWithMemberAndTokensByPartyId(partyId, JoinStatus.ACCEPTED)

        members.forEach { partyMember ->
            val member = partyMember.character.member

            // 2. 개별 유저의 알람 설정(On/Off) 확인
            val mapping = memberBossPartyMappingRepository.findByMemberIdAndBossPartyId(member.id, alarm.contentId)

            if (mapping?.isPartyAlarmEnabled == true) {
                sendFcmPush(member, alarm) // 실제 발송
            }
        }

        // 3. 발송 완료 처리 (파티 알람 레코드 1개만 업데이트)
        markAsSent(alarm)

        // 3. 💡 주기 모드(PERIODIC)라면 다음 주 알람 예약 로직 실행
        if (alarmTimeEntity.registrationMode == RegistrationMode.PERIODIC) {
            scheduleNextPeriodicAlarm(alarmTimeEntity, alarm)
        }
    }

    @Transactional(readOnly = true)
    fun sendBossPartyChatAlarm(partyId: Long, senderCharacterId: Long, content: String, senderName: String) {
        // 1. 해당 파티의 승인된 멤버들 조회
        val members = bossPartyMemberRepository.findAllWithMemberAndTokensByPartyId(partyId, JoinStatus.ACCEPTED)

        // 2. 현재 웹소켓 세션에 연결된 캐릭터 ID 목록 가져오기
        val activeCharacterIds = BossPartyChatWebSocketHandler.getActiveCharacterIds(partyId)

        members.forEach { partyMember ->
            val member = partyMember.character.member
            val targetCharacterId = partyMember.character.id

            // 본인 제외 AND 현재 채팅방 접속자 제외
            if (targetCharacterId != senderCharacterId && !activeCharacterIds.contains(targetCharacterId)) {
                member.tokens.forEach { tokenEntity ->
                    val message = Message.builder()
                        .setToken(tokenEntity.token)
                        // ✅ 채팅 알림은 푸시 팝업이 떠야 하므로 Data Payload 방식을 사용
                        .putData("type", "BOSSCHAT")
                        .putData("contentId", partyId.toString())
                        .putData("title", senderName)
                        .putData("body", content)
                        .build()

                    try {
                        FirebaseMessaging.getInstance().send(message)
                    } catch (e: Exception) {
                        log.error("❌ 채팅 알림 발송 실패: 유저=${member.id}, 토큰=${tokenEntity.token.take(10)}")
                    }
                }
            }
        }
    }

    private fun scheduleNextPeriodicAlarm(currentAlarm: BossPartyAlarmTime, originalDto: RedisAlarmDto) {
        // 파티의 현재 주기 설정(DayOfWeek 등)을 다시 가져옵니다.
        // (그 사이에 방장이 주기를 수정했을 수도 있으므로 DB 조회가 필요합니다)
        val bossParty = bossPartyRepository.findByIdOrNull(currentAlarm.bossPartyId) ?: return

        // 주기가 설정되어 있는 파티인 경우에만 진행
        if (bossParty.alarmDayOfWeek != null) {
            // 현재 알람 예정 시각으로부터 정확히 7일 뒤 계산
            val nextTime = currentAlarm.alarmTime.plusWeeks(1)

            // 중복 방지: 이미 해당 시간에 알람이 있는지 확인
            if (!bossPartyAlarmTimeRepository.existsByBossPartyIdAndAlarmTime(bossParty.id, nextTime)) {
                val nextAlarmEntity = bossPartyAlarmTimeRepository.save(
                    BossPartyAlarmTime(
                        bossPartyId = bossParty.id,
                        alarmTime = nextTime,
                        message = bossParty.alarmMessage ?: currentAlarm.message,
                        registrationMode = RegistrationMode.PERIODIC
                    )
                )

                // RabbitMQ에 다음 주차 알람 예약 발송
                val nextDto = originalDto.copy(targetId = nextAlarmEntity.id)
                alarmProducer.reserveAlarm(nextDto, nextTime)

                log.info("🗓️ 다음 주기 알람 예약 완료: 파티=${bossParty.id}, 시간=$nextTime")
            }
        }
    }

    private fun isAlarmCancelled(alarm: RedisAlarmDto): Boolean {
        // Redis에 "alarm:cancel:BOSS:123" 같은 키가 있는지 확인
        val cancelKey = "alarm:cancel:${alarm.type}:${alarm.targetId}"
        return redisTemplate.hasKey(cancelKey)
    }

    private fun checkEventAlarmValid(targetId: Long): Boolean {
        val alarmTime = eventAlarmTimeRepository.findByIdOrNull(targetId) ?: return false
        // 알람이 활성화(isEnabled) 되어 있고, 아직 발송되지 않았어야(isSent == false) 함
        return alarmTime.eventAlarm.isEnabled && !alarmTime.isSent
    }

    private fun checkBossAlarmValid(targetId: Long): Boolean {
        val alarmTime = bossPartyAlarmTimeRepository.findByIdOrNull(targetId) ?: return false
        return !alarmTime.isSent // 보스 알람은 별도의 isEnabled가 없다면 isSent만 체크
    }

    private fun sendFcmPush(member: Member, alarm: RedisAlarmDto) {
        member.tokens.forEach { tokenEntity ->
            val message = Message.builder()
                .setToken(tokenEntity.token)
                .setNotification(
                    Notification.builder()
                        .setTitle(alarm.title)
                        .setBody(alarm.message) // 💡 남은 기간 표시
                        .build()
                )
                .putData("type", alarm.type.name)
                .putData("targetId", alarm.targetId.toString())
                .putData("contentId", alarm.contentId.toString()) // 추가 정보가 있다면 포함
                .build()

            try {
                FirebaseMessaging.getInstance().send(message)
                log.info("🚀 FCM 데이터 메시지 발송 성공: 유저=${member.id}")
            } catch (e: Exception) {
                log.error("❌ FCM 발송 실패: 토큰=${tokenEntity.token.take(10)}..., 사유=${e.message}")
            }
        }
    }

    @Transactional(readOnly = true)
    fun sendRefreshSignal(partyId: Long) {
        // 1. 해당 파티의 모든 승인된 멤버 조회
        val members = bossPartyMemberRepository.findAllWithMemberAndTokensByPartyId(partyId, JoinStatus.ACCEPTED)

        members.forEach { partyMember ->
            val member = partyMember.character.member
            member.tokens.forEach { tokenEntity ->
                val message = Message.builder()
                    .setToken(tokenEntity.token)
                    // Notification(알림창) 없이 Data만 포함하여 Silent Push로 전송
                    .putData("type", "REFRESH_BOSS_ALARM")
                    .putData("partyId", partyId.toString())
                    .build()

                try {
                    FirebaseMessaging.getInstance().send(message)
                    log.info("📡 갱신 신호 발송 완료: 유저=${member.id}")
                } catch (e: Exception) {
                    log.error("❌ 갱신 신호 발송 실패: 유저=${member.id}, 사유=${e.message}")
                }
            }
        }
    }

    private fun markAsSent(alarm: RedisAlarmDto) {
        when (alarm.type) {
            AlarmType.EVENT -> eventAlarmTimeRepository.findByIdOrNull(alarm.targetId)?.apply { isSent = true }
            AlarmType.BOSS -> bossPartyAlarmTimeRepository.findByIdOrNull(alarm.targetId)?.apply { isSent = true }
            else -> {}
        }
    }

    fun sendEndingEventNotifications() {
        // 1. 오늘 종료되는 이벤트 조회
        val startOfToday = LocalDate.now().atStartOfDay()
        val endOfToday = LocalDate.now().atTime(LocalTime.MAX)
        val endingEvents = eventRepository.findAllByEndDateBetween(startOfToday, endOfToday)

        // 2. 메시지 내용 구성 (이벤트 유무에 따라 다르게)
        val title = "메이플 캘린더 오늘의 소식 🍁"
        val randomMessages = listOf(
            "오늘도 즐거운 메이플 되세요!",
            "재획하기 좋은 날씨네요!",
            "스타포스 대박 나시길 기원합니다.",
            "일퀘 몬파 하러갑시다!"
        )
        val body = if (endingEvents.isNotEmpty()) {
            val eventNames = endingEvents.take(2).joinToString(", ") { it.title }
            val suffix = if (endingEvents.size > 2) " 외 ${endingEvents.size - 2}개" else ""
            "오늘 [$eventNames]$suffix 이벤트가 종료됩니다! 보상을 수령하셨나요?"
        } else { randomMessages.random() }

        // 3. 모든 토큰 조회
        val tokens = notificationTokenRepository.findAllByMemberIsGlobalAlarmEnabledTrue()

        if (tokens.isEmpty()) return

        // 4. 발송 로직
        tokens.forEach { tokenEntity ->
            val message = Message.builder()
                .setToken(tokenEntity.token)
                .setNotification(
                    Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build()
                )
                .setAndroidConfig(
                    AndroidConfig.builder()
                    .setPriority(AndroidConfig.Priority.HIGH)
                    .build())
                .build()

            try {
                FirebaseMessaging.getInstance().send(message)
                log.info("푸시 알림 발송 성공: ${tokenEntity.token.take(8)}...")
            } catch (e: Exception) {
                log.error("푸시 알림 발송 실패: ${e.message}")
            }
        }
    }

    fun registerToken(request: FcmTokenRequest, memberId: Long? = null) {
        val existingToken = notificationTokenRepository.findByToken(request.token)
        val member = memberId?.let { memberRepository.findByIdOrNull(it) }

        if (existingToken != null) {
            existingToken.platform = request.platform
            existingToken.lastRegisteredAt = LocalDateTime.now()
            // 💡 로그인 상태라면 토큰의 주인(Member)을 업데이트
            if (member != null) existingToken.member = member
        } else {
            notificationTokenRepository.save(
                NotificationToken(
                    token = request.token,
                    platform = request.platform,
                    member = member // 💡 새 토큰 생성 시 멤버 연결
                )
            )
        }
    }

//    @Transactional
//    fun unregisterToken(apiKey: String, token: String) {
//        val member = memberService.findByRawKey(apiKey)
//            ?: return // 유저가 없으면 이미 로그아웃된 것으로 간주
//
//        notificationTokenRepository.deleteByMemberAndToken(member, token)
//        log.info("토큰 삭제 완료: 유저=${member.id}, 토큰=${token.take(10)}...")
//    }
}