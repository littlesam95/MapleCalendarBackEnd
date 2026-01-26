package com.sixclassguys.maplecalendar.domain.member.repository

import com.sixclassguys.maplecalendar.domain.member.entity.NexonApiKey
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface NexonApiKeyRepository : JpaRepository<NexonApiKey, Long> {

    fun findByApiKeyHash(apiKeyHash: String): NexonApiKey?
}