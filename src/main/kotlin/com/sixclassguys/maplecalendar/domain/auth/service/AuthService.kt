package com.sixclassguys.maplecalendar.domain.auth.service

import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.sixclassguys.maplecalendar.domain.auth.dto.AuthResult
import com.sixclassguys.maplecalendar.domain.auth.dto.LoginResponse
import com.sixclassguys.maplecalendar.domain.auth.jwt.JwtUtil
import com.sixclassguys.maplecalendar.domain.character.service.MapleCharacterService
import com.sixclassguys.maplecalendar.domain.member.entity.Member
import com.sixclassguys.maplecalendar.domain.member.repository.MemberRepository
import com.sixclassguys.maplecalendar.domain.member.service.MemberService
import com.sixclassguys.maplecalendar.domain.notification.dto.Platform
import com.sixclassguys.maplecalendar.domain.notification.dto.TokenRequest
import com.sixclassguys.maplecalendar.domain.notification.service.NotificationService
import com.sixclassguys.maplecalendar.domain.util.getZonedDateTime
import com.sixclassguys.maplecalendar.global.config.GoogleOAuthProperties
import com.sixclassguys.maplecalendar.infrastructure.external.NexonApiClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class AuthService(
    private val nexonApiClient: NexonApiClient,
    private val memberService: MemberService,
    private val memberRepository: MemberRepository,
    private val mapleCharacterService: MapleCharacterService,
    private val jwtUtil: JwtUtil,
    private val notificationService: NotificationService,
    private val googleOAuthProperties: GoogleOAuthProperties
) {

    private val log = LoggerFactory.getLogger(javaClass)

//    @Transactional
//    suspend fun loginAndGetCharacters(apiKey: String): LoginResponse = coroutineScope {
//        // 1. DB 먼저 확인 (이미 등록된 유저인지)
//        var existingMember = memberService.findByRawKey(apiKey)
//
//        // 2. 이미 대표 캐릭터가 설정된 경우, 바로 반환
//        if (existingMember?.representativeOcid != null) {
//            return@coroutineScope LoginResponse(
//                representativeOcid = existingMember.representativeOcid,
//                isGlobalAlarmEnabled = existingMember.isGlobalAlarmEnabled
//            )
//        }
//
//        // 3. 넥슨 API 호출 (조회 성공 시에만 다음 단계 진행)
//        val nexonAccounts = nexonApiClient.getCharacters(apiKey)
//
//        // 4. 가공 전 데이터 검증 (가져오는 데 실패했거나 데이터가 없는 경우)
//        if (nexonAccounts.isEmpty()) {
//            throw InvalidApiKeyException()
//        }
//
//        // 5. Member 엔티티 저장 또는 조회 (Upsert 로직)
//        // API Key를 고유 식별자로 사용하여 유저 관리
//        if (existingMember == null) {
//            existingMember = memberService.saveNewMember(apiKey)
//        }
//
//        // 6. 모든 계정의 캐릭터를 평면화
//        val allCharacters = nexonAccounts.flatMap { it.characters }
//
//        // 7. 앱에 전달할 형태로 데이터 가공 (각 계정의 캐릭터 리스트를 월드별로 그룹화)
//        val characters = allCharacters.map { character ->
//            AccountCharacterResponse(
//                ocid = character.ocid,
//                characterName = character.characterName,
//                worldName = character.worldName,
//                characterClass = character.characterClass,
//                characterLevel = character.characterLevel
//            )
//        }.groupBy { it.worldName } // 월드별로 그룹핑하여 전달
//
//        LoginResponse(characters = characters)
//    }

//    fun processAutoLogin(apiKey: String, request: TokenRequest): AutoLoginResponse {
//        // 1. DB에서 해당 API Key를 가진 유저 조회
//        val user = memberService.findByRawKey(apiKey)
//            ?: return AutoLoginResponse(false, "존재하지 않는 회원입니다.")
//
//        try {
//            notificationService.registerToken(
//                request = TokenRequest(token = request.token, platform = request.platform),
//                memberId = user.id
//            )
//            log.info("유저(${user.id})의 FCM 토큰 업데이트 성공")
//        } catch (e: Exception) {
//            log.error("FCM 토큰 업데이트 실패: ${e.message}")
//            // 토큰 업데이트 실패가 로그인을 막으면 안 되므로 로그만 남깁니다.
//        }
//
//        // 2. 대표 캐릭터 OCID가 설정되어 있는지 확인
//        val ocid = user.representativeOcid
//            ?: return AutoLoginResponse(false, "대표 캐릭터가 없습니다.")
//
//        return try {
//            // 3. 넥슨 API를 통해 해당 OCID의 최신 정보 조회 (이미지, 레벨 등)
//            val characterBasic = nexonApiClient.getCharacterBasic(apiKey, ocid)
//
//            val overAllRanking = nexonApiClient.getOverAllRanking(apiKey, ocid, getZonedDateTime())
//            val worldName = overAllRanking?.ranking[0]?.worldName
//            val serverRanking = worldName?.let {
//                nexonApiClient.getServerRanking(apiKey, ocid, getZonedDateTime(), it)
//            }
//            val union = nexonApiClient.getUnionInfo(apiKey, ocid)
//            val dojang = nexonApiClient.getDojangInfo(apiKey, ocid)
//
//            log.info("캐릭터 정보: $characterBasic")
//
//            val customImage = characterBasic?.let {
//                if (it.characterImage != null) {
//                    val baseUrl = characterBasic.characterImage
//                    // 쿼리 스트링 조합
//                    // 예: baseUrl?action=stand1&emotion=default&wmotion=default
////                    "$baseUrl?action=${user.charAction}&emotion=${user.charEmotion}&wmotion=${user.charWeaponMotion}"
//                } else {
//                    null
//                }
//            }
//
//            AutoLoginResponse(
//                isSuccess = true,
//                message = "자동 로그인 성공",
//                characterBasic = characterBasic?.copy(characterImage = customImage),
//                isGlobalAlarmEnabled = user.isGlobalAlarmEnabled,
//                characterPopularity = overAllRanking?.ranking[0]?.characterPopularity,
//                characterOverallRanking = overAllRanking?.ranking[0],
//                characterServerRanking = serverRanking?.ranking[0],
//                characterUnionLevel = union,
//                characterDojang = dojang
//            )
//        } catch (e: Exception) {
//            // 넥슨 API 키가 만료되었거나 서버 통신 오류 시
//            AutoLoginResponse(false, "캐릭터 정보를 가져오는데 실패했습니다: ${e.message}")
//        }
//    }

    @Transactional
    fun loginWithGoogle(idToken: String, fcmToken: String, platform: Platform): Pair<LoginResponse, String> {
        // Google 공식 검증기 설정
        val verifier = GoogleIdTokenVerifier.Builder(NetHttpTransport(), GsonFactory())
            .setAudience(googleOAuthProperties.clientIds)
            .setIssuer("https://accounts.google.com")
            .build()

        log.info("Google Id Token: $idToken")
        val googleIdToken = verifier.verify(idToken)
            ?: throw IllegalArgumentException("Invalid Google Token")

        val payload = googleIdToken.payload
        val googleUid = payload.subject // Google의 subject(providerId)
        val email = payload.email
        val name = payload["name"] as? String // 유저 전체 이름
        val pictureUrl = payload["picture"] as? String // 프로필 사진 URL
        log.info("이메일: ${email}, 닉네임: ${name}, 프사: ${pictureUrl}")

        // 유저 조회, 없으면 가입
        val authResult = loginOrRegister(
            provider = "google",
            providerId = googleUid,
            email = email,
            nickname = name,
            profileImageUrl = pictureUrl
        )
        val user = authResult.member

        // 비동기로 보유한 모든 API Key로 캐릭터 목록을 조회 및 갱신
        mapleCharacterService.refreshUserCharacters(user)

        // FCM 토큰 업데이트
        try {
            notificationService.registerToken(
                // 플랫폼은 안드로이드로 고정하거나 파라미터로 받기
                request = TokenRequest(token = fcmToken, platform = platform),
                memberId = user.id!!
            )
        } catch (e: Exception) {
            log.error("FCM 토큰 업데이트 실패: ${e.message}")
        }

        val accessToken = jwtUtil.createAccessToken(user.email)
        val refreshToken = jwtUtil.createRefreshToken(user.email)

        // 대표 캐릭터 정보 조회, 대표 캐릭터 OCID가 없으면 기본 프로필 정보만 담은 Response 반환
        val ocid = user.representativeOcid ?: return LoginResponse.fromEntity(user, null, accessToken, authResult.isNewMember)  to refreshToken

        val loginResponse = if (ocid != null) {
            try {
                // 5. 넥슨 API를 통해 최신 캐릭터 정보 수집
                val characterBasic = nexonApiClient.getCharacterBasic(ocid)

                val overAllRanking = nexonApiClient.getOverAllRanking(ocid, getZonedDateTime())
                val worldName = overAllRanking?.ranking[0]?.worldName
                val serverRanking = worldName?.let {
                    nexonApiClient.getServerRanking(ocid, getZonedDateTime(), it)
                }
                val union = nexonApiClient.getUnionInfo(ocid)
                val dojang = nexonApiClient.getDojangInfo(ocid)

                // 6. 통합된 LoginResponse 반환
                LoginResponse(
                    id = user.id!!,
                    email = user.email,
                    provider = user.provider,
                    nickname = name,
                    profileImageUrl = pictureUrl,
                    isGlobalAlarmEnabled = user.isGlobalAlarmEnabled,
                    accessToken = accessToken,
                    isNewMember = authResult.isNewMember,
                    characterBasic = characterBasic,
                    characterPopularity = overAllRanking?.ranking?.getOrNull(0)?.characterPopularity,
                    characterOverallRanking = overAllRanking?.ranking[0],
                    characterServerRanking = serverRanking?.ranking[0],
                    characterUnionLevel = union,
                    characterDojang = dojang,
                    lastLoginAt = user.lastLoginAt
                )
            } catch (e: Exception) {
                // API 키 만료 등의 경우 캐릭터 정보 없이 기본 정보만 반환
                log.warn("캐릭터 정보 로딩 실패: ${e.message}")
                LoginResponse.fromEntity(user, null)
            }
        } else {
            // 대표 캐릭터가 없는 경우 기본 정보만
            LoginResponse.fromEntity(user, null, accessToken)
        }

        return loginResponse to refreshToken
    }

    @Transactional
    fun loginWithApple(appleSub: String, email: String): AuthResult {
        // Apple 로그인은 ID 토큰 검증은 필요하지만 여기선 sub와 이메일만 사용
        return loginOrRegister(provider = "apple", providerId = appleSub, email = email, null, null)
    }

    private fun loginOrRegister(
        provider: String,
        providerId: String,
        email: String,
        nickname: String?,
        profileImageUrl: String?
    ): AuthResult {
        var member = memberRepository.findByProviderAndProviderId(provider, providerId)

        return if (member == null) {
            // 이메일 중복 체크
            val existing = memberRepository.findByEmail(email)
            if (existing != null) {
                throw IllegalArgumentException("Email already exists with a different provider")
            }

            member = Member(
                provider = provider,
                providerId = providerId,
                email = email,
                nickname = nickname,
                profileImageUrl = profileImageUrl,
                lastLoginAt = LocalDateTime.now()
            )
            log.info("Member: $member")
            memberRepository.save(member)
            AuthResult(memberRepository.save(member), true)
        } else {
            // 기존 회원이면 lastLoginAt 업데이트
            member.nickname = nickname ?: member.nickname
            member.profileImageUrl = profileImageUrl ?: member.profileImageUrl
            member.lastLoginAt = LocalDateTime.now()
            member.updatedAt = LocalDateTime.now()
            memberRepository.save(member)
            AuthResult(memberRepository.save(member), false)
        }
    }
}