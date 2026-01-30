package com.sixclassguys.maplecalendar.global.config

import com.sixclassguys.maplecalendar.domain.auth.jwt.JwtAuthenticationFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.web.firewall.HttpFirewall
import org.springframework.security.web.firewall.StrictHttpFirewall

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter
) {

    @Bean
    fun strictHttpFirewall(): HttpFirewall {
        val firewall = StrictHttpFirewall()
        firewall.setAllowSemicolon(true) // 세미콜론 허용
        firewall.setAllowUrlEncodedSlash(true) // 인코딩된 슬래시 허용
        firewall.setAllowBackSlash(true)
        return firewall
    }

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http.httpBasic { it.disable() }
            .formLogin { it.disable() }
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests {
                it.requestMatchers(
                    "/api/events/today",
                    "/api/auth/**",
                    "/api/character/**",
                    "/api/test/s3/**"
                ).permitAll()
                it.requestMatchers(
                    "/v2/api-docs",
                    "/v3/api-docs/**",
                    "/swagger-resources/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/webjars/**",
                ).permitAll()
                it.anyRequest().authenticated()
            }
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }
}