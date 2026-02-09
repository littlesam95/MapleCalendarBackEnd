package com.sixclassguys.maplecalendar.global.config

import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.CustomExchange
import org.springframework.amqp.core.Queue
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RabbitConfig {

    @Bean
    fun delayedExchange(): CustomExchange {
        val args = mapOf("x-delayed-type" to "direct")

        return CustomExchange("alarm.exchange", "x-delayed-message", true, false, args)
    }

    @Bean
    fun alarmQueue(): Queue = Queue("alarm.queue")

    @Bean
    fun alarmBinding(alarmQueue: Queue, delayedExchange: CustomExchange): Binding {
        return BindingBuilder.bind(alarmQueue).to(delayedExchange).with("alarm.routing.key").noargs()
    }
}