package com.sixclassguys.maplecalendar.infrastructure.external

import com.sixclassguys.maplecalendar.global.config.NexonProperties
import com.sixclassguys.maplecalendar.infrastructure.external.dto.Account
import com.sixclassguys.maplecalendar.infrastructure.external.dto.AccountResponse
import com.sixclassguys.maplecalendar.infrastructure.external.dto.CharacterBasic
import com.sixclassguys.maplecalendar.infrastructure.external.dto.DojangRanking
import com.sixclassguys.maplecalendar.infrastructure.external.dto.DojangResponse
import com.sixclassguys.maplecalendar.infrastructure.external.dto.EventNotice
import com.sixclassguys.maplecalendar.infrastructure.external.dto.EventNoticeResponse
import com.sixclassguys.maplecalendar.infrastructure.external.dto.RankingResponse
import com.sixclassguys.maplecalendar.infrastructure.external.dto.UnionResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.exchange
import org.springframework.web.util.UriComponentsBuilder
import java.time.LocalDate

@Component
class NexonApiClient(
    private val nexonProperties: NexonProperties,
    private val restTemplate: RestTemplate
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun getCharacterBasic(apiKey: String, ocid: String): CharacterBasic? {
        // 1. UriComponentsBuilder를 사용하여 쿼리 파라미터 추가
        val uri = UriComponentsBuilder.fromUriString("${nexonProperties.baseUrl}/character/basic")
            .queryParam("ocid", ocid)
            .build()
            .toUri() // URI 객체로 변환

        // 2. 헤더 설정 (기존과 동일)
        val headers = HttpHeaders().apply {
            set("x-nxopen-api-key", apiKey)
        }
        val entity = HttpEntity<Unit>(headers)

        // 3. exchange 호출 시 String 대신 URI 객체 전달
        val response = restTemplate.exchange<CharacterBasic>(
            uri,
            HttpMethod.GET,
            entity
        )

        log.debug("Response: {}", response.statusCode)

        return response.body
    }

    fun getCharacters(apiKey: String): List<Account> {
        val url = "${nexonProperties.baseUrl}/character/list"

        // 헤더에 API Key 설정
        val headers = HttpHeaders().apply {
            set("x-nxopen-api-key", apiKey)
        }
        val entity = HttpEntity<Unit>(headers)

        val response = restTemplate.exchange<AccountResponse>(
            url,
            HttpMethod.GET,
            entity
        )

        return response.body?.accounts ?: emptyList()
    }

    fun getRecent20Events(): List<EventNotice> {
        // 엔드포인트 설정
        val url = "${nexonProperties.baseUrl}/notice-event"

        // 헤더에 API Key 설정
        val headers = HttpHeaders().apply {
            set("x-nxopen-api-key", nexonProperties.key)
        }
        val entity = HttpEntity<Unit>(headers)

        // API 호출
        val response = restTemplate.exchange(
            url,
            HttpMethod.GET,
            entity,
            EventNoticeResponse::class.java
        )

        // body가 null일 경우 빈 리스트 반환, 데이터가 있다면 최근 20개 추출
        return response.body?.eventNotice?.take(20) ?: emptyList()
    }

    fun getOverAllRanking(apiKey: String, ocid: String, date: LocalDate): RankingResponse? {
        val url = "${nexonProperties.baseUrl}/ranking/overall"
        val uri = UriComponentsBuilder
            .fromUriString(url)
            .queryParam("ocid", ocid)
            .queryParam("date", date.toString())
            .build()
            .toUri()

        val headers = HttpHeaders().apply {
            set("x-nxopen-api-key", apiKey)
        }

        val response = restTemplate.exchange(
            uri,
            HttpMethod.GET,
            HttpEntity<Unit>(headers),
            RankingResponse::class.java
        )

        return response.body
    }

    fun getServerRanking(apiKey: String, ocid: String, date: LocalDate, worldName: String): RankingResponse? {
        val url = "${nexonProperties.baseUrl}/ranking/overall"
        val uri = UriComponentsBuilder
            .fromUriString(url)
            .queryParam("ocid", ocid)
            .queryParam("date", date.toString())
            .queryParam("world_name", worldName)
            .build()
            .toUri()
        log.info("Nexon API URL12121212 = {}", uri)
        val headers = HttpHeaders().apply {
            set("x-nxopen-api-key", apiKey)
        }

        val response = restTemplate.exchange(
            uri,
            HttpMethod.GET,
            HttpEntity<Unit>(headers),
            RankingResponse::class.java
        )

        return response.body
    }


    fun getUnionInfo(apiKey: String, ocid: String): UnionResponse? {
        val url = "${nexonProperties.baseUrl}/user/union"
        val uri = UriComponentsBuilder.fromUriString(url)
            .queryParam("ocid", ocid)
            .build()
            .toUri()

        val headers = HttpHeaders().apply {
            set("x-nxopen-api-key", apiKey)
        }
        val entity = HttpEntity<Unit>(headers)

        val response = restTemplate.exchange(
            uri,
            HttpMethod.GET,
            entity,
            UnionResponse::class.java
        )

        return response.body
    }

    fun getDojangInfo(apiKey: String, ocid: String): DojangRanking? {
        val url = "${nexonProperties.baseUrl}/character/dojang"
        val uri = UriComponentsBuilder.fromUriString(url)
            .queryParam("ocid", ocid)
            .build()
            .toUri()

        val headers = HttpHeaders().apply {
            set("x-nxopen-api-key", apiKey)
        }
        val entity = HttpEntity<Unit>(headers)

        val response = restTemplate.exchange(
            uri,
            HttpMethod.GET,
            entity,
            DojangRanking::class.java
        )

        return response.body
    }
}