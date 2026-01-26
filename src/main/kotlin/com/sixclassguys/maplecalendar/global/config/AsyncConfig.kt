package com.sixclassguys.maplecalendar.global.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor

@Configuration
@EnableAsync
class AsyncConfig {

    @Bean(name = ["characterSyncExecutor"])
    fun characterSyncExecutor(): Executor {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = 5        // 기본으로 유지할 스레드 수
        executor.maxPoolSize = 10       // 최대 스레드 수
        executor.setQueueCapacity(500)  // 대기 큐 크기
        executor.setThreadNamePrefix("CharSync-") // 로그에서 식별하기 편하게 이름 지정
        executor.initialize()

        return executor
    }
}