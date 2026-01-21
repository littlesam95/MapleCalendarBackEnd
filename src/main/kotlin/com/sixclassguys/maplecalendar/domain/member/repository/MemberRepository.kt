package com.sixclassguys.maplecalendar.domain.member.repository

import com.sixclassguys.maplecalendar.domain.member.entity.Member
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface MemberRepository : JpaRepository<Member, Long> {

    // 이제 nexonApiKey 대신 해시값으로 찾습니다.
    fun findByApiKeyHash(apiKeyHash: String): Member?
}