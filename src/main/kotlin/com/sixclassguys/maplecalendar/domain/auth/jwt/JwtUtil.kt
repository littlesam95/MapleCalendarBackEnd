package com.sixclassguys.maplecalendar.domain.auth.jwt

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.Date;
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component;

@Component
class JwtUtil(@Value("\${jwt.secret}") private val secretKey: String) {

    private val key: Key = Keys.hmacShaKeyFor(
        secretKey.toByteArray()
    )

    private val ACCESS_EXP = 1000 * 60 * 15      // 15분
    private val REFRESH_EXP = 1000 * 60 * 60 * 24 * 7 // 7일

    fun createAccessToken(username: String): String =
        Jwts.builder()
            .setHeaderParam("typ", "JWT") // 헤더에 타입 명시
            .setSubject(username)
            .claim("type", "access")
            .setIssuedAt(Date())
            .setExpiration(Date(System.currentTimeMillis() + ACCESS_EXP))
            .signWith(key, SignatureAlgorithm.HS256)
            .compact()

    fun createRefreshToken(username: String): String =
        Jwts.builder()
            .setSubject(username)
            .claim("type", "refresh")
            .setIssuedAt(Date())
            .setExpiration(Date(System.currentTimeMillis() + REFRESH_EXP))
            .signWith(key, SignatureAlgorithm.HS256)
            .compact()

    fun parseClaims(token: String): Claims =
        Jwts.parserBuilder()
            .setSigningKey(key)
            .build()
            .parseClaimsJws(token)
            .body
}
