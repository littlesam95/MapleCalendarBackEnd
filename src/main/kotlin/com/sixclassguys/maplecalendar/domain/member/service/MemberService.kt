package com.sixclassguys.maplecalendar.domain.member.service

import com.sixclassguys.maplecalendar.domain.member.entity.Member
import com.sixclassguys.maplecalendar.domain.member.repository.MemberRepository
import com.sixclassguys.maplecalendar.global.config.EncryptionUtil
import com.sixclassguys.maplecalendar.global.exception.MemberNotFoundException
import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MemberService(
    private val memberRepository: MemberRepository,
    private val encryptionUtil: EncryptionUtil
) {

    // 유저가 반드시 있어야 하는 경우 (업데이트, 정보 조회 등)
    fun getMemberByRawKey(rawApiKey: String): Member {
        return findByRawKey(rawApiKey)
            ?: throw MemberNotFoundException("해당 유저를 찾을 수 없습니다.")
    }

    // 유저 존재 여부만 확인하는 경우 (로그인/가입 시)
    fun findByRawKey(rawApiKey: String): Member? {
        val hashedKey = encryptionUtil.hashKey(rawApiKey)
        return memberRepository.findByApiKeyHash(hashedKey)
    }

    @Transactional
    fun saveNewMember(apiKey: String): Member {
        val hashedKey = encryptionUtil.hashKey(apiKey)
        return memberRepository.save(Member(
            nexonApiKey = apiKey,
            apiKeyHash = hashedKey
        ))
    }

    @Transactional
    fun updateRepresentativeCharacter(apiKey: String, ocid: String) {
        // 1. API Key로 유저 조회
        val member = getMemberByRawKey(apiKey)

        // 2. 대표 캐릭터 OCID 업데이트
        // JPA의 변경 감지(Dirty Checking) 기능으로 인해 데이터를 변경하면 트랜잭션 종료 시 자동 저장
        member.representativeOcid = ocid
    }

    @Transactional
    fun updateGlobalAlarmStatus(apiKey: String): Boolean {
        val member = getMemberByRawKey(apiKey)

        member.isGlobalAlarmEnabled = !member.isGlobalAlarmEnabled
        // Dirty Checking으로 자동 저장

        return member.isGlobalAlarmEnabled
    }

    // 추후 Controller에 API 추가
    @Transactional
    fun updateMemberCharacterStyle(apiKey: String, action: String, emotion: String, weapon: String) {
        val member = getMemberByRawKey(apiKey)

        member.charAction = action
        member.charEmotion = emotion
        member.charWeaponMotion = weapon
    }
}