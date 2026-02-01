package com.sixclassguys.maplecalendar.global.config

import com.sixclassguys.maplecalendar.domain.boss.handler.BossPartyChatWebSocketHandler
import com.sixclassguys.maplecalendar.domain.boss.interceptor.BossPartyChatHandshakeInterceptor
import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry

@Configuration
@EnableWebSocket
class WebSocketConfig(
    private val bossPartyChatWebSocketHandler: BossPartyChatWebSocketHandler,
    private val bossPartyChatHandshakeInterceptor: BossPartyChatHandshakeInterceptor
) : WebSocketConfigurer {

    override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
        registry.addHandler(bossPartyChatWebSocketHandler, "/ws-chat")
            .addInterceptors(bossPartyChatHandshakeInterceptor) // 인터셉터 추가
            .setAllowedOrigins("*")
    }
}