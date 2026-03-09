package com.sixclassguys.maplecalendar.domain.auth.jwt

import com.sixclassguys.maplecalendar.domain.auth.dto.AppleUserInfo
import io.jsonwebtoken.Claims
import io.jsonwebtoken.JwsHeader
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SigningKeyResolverAdapter
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.math.BigInteger
import java.security.Key
import java.security.KeyFactory
import java.security.interfaces.RSAPublicKey
import java.security.spec.RSAPublicKeySpec
import java.util.Base64

@Component
class AppleJwtVerifier(
    @Value("\${apple.client-id}")
    private val appleClientId: String,
    @Value("\${apple.service-id}")
    private val appleServiceId: String,
    private val restTemplate: RestTemplate = RestTemplate()
) {
    private val APPLE_KEYS_URL = "https://appleid.apple.com/auth/keys"
    private val log = LoggerFactory.getLogger(javaClass)

    private var cachedKeys: List<Map<String, String>>? = null
    private var lastFetchedTime: Long = 0
    private val cacheTtl = 60 * 60 * 1000 // 1시간


    fun verify(idToken: String): AppleUserInfo? = try {
        val jws = Jwts.parserBuilder()
            .setSigningKeyResolver(object : SigningKeyResolverAdapter() {
                override fun resolveSigningKey(header: JwsHeader<*>, claims: Claims): Key {
                    val kid = header["kid"] as String
                    return getApplePublicKey(kid)
                }
            })
            .requireIssuer("https://appleid.apple.com")
            .build()
            .parseClaimsJws(idToken)

        val claims = jws.body
        val audience = claims.audience

        // 허용된 Audience 리스트 확인 (iOS App ID, Android Service ID)
        val allowedAudiences = listOf(appleClientId, appleServiceId)

        if (!allowedAudiences.contains(audience)) {
            throw IllegalArgumentException("Expected aud claim to be one of $allowedAudiences, but was: $audience")
        }

        AppleUserInfo(
            sub = claims.subject,
            email = claims["email"] as? String,
            name = claims["name"] as? String
        )
    } catch (e: Exception) {
        log.info("Apple JWT 검증 실패: ${e.message}")
        null
    }

    private fun getApplePublicKey(kid: String): RSAPublicKey {
        val now = System.currentTimeMillis()

        if (cachedKeys == null || now - lastFetchedTime > cacheTtl) {
            val keysResponse = restTemplate.getForObject(APPLE_KEYS_URL, Map::class.java)
            cachedKeys = keysResponse?.get("keys") as? List<Map<String, String>>
                ?: throw IllegalArgumentException("Apple keys not found")
            lastFetchedTime = now
        }

        val keyMap = cachedKeys?.firstOrNull { it["kid"] == kid } ?: throw IllegalArgumentException("Apple key not found for kid=$kid")
        val n = BigInteger(1, Base64.getUrlDecoder().decode(keyMap["n"]))
        val e = BigInteger(1, Base64.getUrlDecoder().decode(keyMap["e"]))
        val spec = RSAPublicKeySpec(n, e)
        return KeyFactory.getInstance("RSA").generatePublic(spec) as RSAPublicKey
    }
}
