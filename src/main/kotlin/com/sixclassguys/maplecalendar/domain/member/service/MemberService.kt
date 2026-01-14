package com.sixclassguys.maplecalendar.domain.member.service

import com.sixclassguys.maplecalendar.domain.member.repository.MemberRepository
import com.sixclassguys.maplecalendar.global.exception.MemberNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MemberService(
    private val memberRepository: MemberRepository
) {

    @Transactional
    fun updateRepresentativeCharacter(apiKey: String, ocid: String) {
        // 1. API Key로 유저 조회
        val member = memberRepository.findByNexonApiKey(apiKey)
            ?: throw MemberNotFoundException()

        // 2. 대표 캐릭터 OCID 업데이트
        // JPA의 변경 감지(Dirty Checking) 기능으로 인해 데이터를 변경하면 트랜잭션 종료 시 자동 저장
        member.representativeOcid = ocid
    }

    @Transactional
    fun updateGlobalAlarmStatus(apiKey: String): Boolean {
        val member = memberRepository.findByNexonApiKey(apiKey)
            ?: throw Exception("User not found")

        member.isGlobalAlarmEnabled = !member.isGlobalAlarmEnabled
        // Dirty Checking으로 자동 저장

        return member.isGlobalAlarmEnabled
    }
}