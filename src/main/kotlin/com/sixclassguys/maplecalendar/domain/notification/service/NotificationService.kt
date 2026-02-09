package com.sixclassguys.maplecalendar.domain.notification.service

import com.google.firebase.messaging.AndroidConfig
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.Notification
import com.sixclassguys.maplecalendar.domain.boss.repository.BossPartyAlarmTimeRepository
import com.sixclassguys.maplecalendar.domain.eventalarm.repository.EventAlarmTimeRepository
import com.sixclassguys.maplecalendar.domain.eventalarm.repository.EventAlarmRepository
import com.sixclassguys.maplecalendar.domain.member.entity.Member
import com.sixclassguys.maplecalendar.domain.member.repository.MemberRepository
import com.sixclassguys.maplecalendar.domain.member.service.MemberService
import com.sixclassguys.maplecalendar.domain.notification.dto.FcmTokenRequest
import com.sixclassguys.maplecalendar.domain.notification.entity.NotificationToken
import com.sixclassguys.maplecalendar.domain.notification.repository.NotificationTokenRepository
import com.sixclassguys.maplecalendar.global.dto.AlarmType
import com.sixclassguys.maplecalendar.global.dto.RedisAlarmDto
import com.sixclassguys.maplecalendar.infrastructure.persistence.event.EventRepository
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Service
@Transactional
class NotificationService(
    private val notificationTokenRepository: NotificationTokenRepository,
    private val eventAlarmTimeRepository: EventAlarmTimeRepository,
    private val bossPartyAlarmTimeRepository: BossPartyAlarmTimeRepository,
    private val eventRepository: EventRepository,
    private val memberRepository: MemberRepository,
    private val eventAlarmRepository: EventAlarmRepository
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun processRedisAlarm(alarm: RedisAlarmDto) {
        // 1. 최신 DB 상태 확인 (사용자가 알람을 취소했거나 삭제했을 수 있음)
        val isStillValid = when (alarm.type) {
            AlarmType.EVENT -> checkEventAlarmValid(alarm.targetId)
            AlarmType.BOSS -> checkBossAlarmValid(alarm.targetId)
        }

        if (!isStillValid) {
            log.info("🚫 알람 발송 취소: 유효하지 않거나 이미 발송됨 (ID: ${alarm.targetId}, Type: ${alarm.type})")
            return
        }

        // 2. 수신자 토큰 조회
        val member = memberRepository.findByIdOrNull(alarm.memberId)
        if (member == null || member.tokens.isEmpty()) {
            log.warn("⚠️ 알람 발송 실패: 유저를 찾을 수 없거나 등록된 FCM 토큰이 없음 (MemberID: ${alarm.memberId})")
            return
        }

        // 3. 실제 FCM 발송
        sendFcmPush(member, alarm)

        // 4. 발송 완료 상태 업데이트 (Postgres)
        markAsSent(alarm)
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
                        .setBody(alarm.message)
                        .build()
                )
                .putData("type", alarm.type.name)
                .putData("targetId", alarm.targetId.toString())
                .build()

            try {
                FirebaseMessaging.getInstance().send(message)
                log.info("🚀 FCM 발송 성공: 유저=${member.id}, 제목=${alarm.title}")
            } catch (e: Exception) {
                log.error("❌ FCM 발송 실패: 토큰=${tokenEntity.token.take(10)}..., 사유=${e.message}")
            }
        }
    }

    private fun markAsSent(alarm: RedisAlarmDto) {
        when (alarm.type) {
            AlarmType.EVENT -> eventAlarmTimeRepository.findByIdOrNull(alarm.targetId)?.apply { isSent = true }
            AlarmType.BOSS -> bossPartyAlarmTimeRepository.findByIdOrNull(alarm.targetId)?.apply { isSent = true }
        }
    }

//    private fun sendFcmMessage(alarmSetting: EventAlarm) {
//        val member = alarmSetting.member
//        val event = alarmSetting.event
//
//        val tokensFromDb = member.id?.let { notificationTokenRepository.findAllByMemberId(it) }
//        tokensFromDb?.let { log.info("📢 [검증] 유저 ID: ${member.id}, DB에 등록된 실제 토큰 개수: ${it.size}") }
//
//        // 💡 남은 일수 계산
//        val daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), event.endDate.toLocalDate())
//        val dDayText = when {
//            daysLeft > 0L -> "${daysLeft}일 남았습니다!"
//            daysLeft == 0L -> "오늘 종료됩니다! 서두르세요!"
//            else -> "종료되었습니다."
//        }
//
//        member.tokens.forEach { tokenEntity ->
//            val message = Message.builder()
//                .setToken(tokenEntity.token)
//                .setNotification(
//                    Notification.builder()
//                        .setTitle("⏰ 설정하신 알림 시간입니다!")
//                        .setBody("[${event.title}] $dDayText") // 💡 남은 기간 표시
//                        .build()
//                )
//                .putData("eventId", event.id.toString())
//                .putData("type", "EVENT_ALARM")
//                .build()
//
//            try {
//                FirebaseMessaging.getInstance().send(message)
//                log.info("개별 알람 발송 성공: 유저=${member.id}, 이벤트=${event.id}")
//            } catch (e: Exception) {
//                log.error("푸시 실패: ${tokenEntity.token.take(10)}... - ${e.message}")
//            }
//        }
//    }

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

    /**
     * 사용자가 개별 설정한 알람 시간에 맞춰 푸시 발송
     * 스케줄러에 의해 매 분(1분 단위) 호출됨
     */
//    fun sendCustomEventNotifications() {
//        val now = LocalDateTime.now().withSecond(0).withNano(0)
//
//        // 💡 쿼리 단계에서 isEnabled = true인 것만 가져오도록 수정 (Repository 쿼리 확인 필요)
//        val activeAlarms = eventAlarmRepository.findAllToSendMessage(now)
//
//        activeAlarms.forEach { alarmSetting ->
//            val targets = alarmSetting.alarmTimes.filter { it.alarmTime <= now && !it.isSent }
//
//            targets.forEach { target ->
//                target.isSent = true
//
//                // 3. 💡 [조건부 발송]
//                // - 알람 설정이 켜져 있고(isEnabled)
//                // - 정확히 '현재 시각'에 해당하는 알람인 경우에만 실제로 발송
//                if (alarmSetting.isEnabled && target.alarmTime == now) {
//                    sendFcmMessage(alarmSetting) // 실제 FCM 발송 로직 분리
//                } else if (target.alarmTime < now) {
//                    log.info("과거 알람(시간: ${target.alarmTime})을 미발송 처리하고 완료 상태로 갱신합니다. 유저: ${alarmSetting.member.id}")
//                }
//            }
//        }
//    }

//    @Transactional
//    fun unregisterToken(apiKey: String, token: String) {
//        val member = memberService.findByRawKey(apiKey)
//            ?: return // 유저가 없으면 이미 로그아웃된 것으로 간주
//
//        notificationTokenRepository.deleteByMemberAndToken(member, token)
//        log.info("토큰 삭제 완료: 유저=${member.id}, 토큰=${token.take(10)}...")
//    }
}