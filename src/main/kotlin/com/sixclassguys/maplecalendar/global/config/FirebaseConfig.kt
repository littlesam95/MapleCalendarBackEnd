package com.sixclassguys.maplecalendar.global.config

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import java.io.IOException

@Configuration
class FirebaseConfig {

    private val log = LoggerFactory.getLogger(javaClass)

    @PostConstruct
    fun init() {
        try {
            // resources 폴더에 있는 키 파일을 읽어옵니다.
            val resource = ClassPathResource("maplecalendar-4add3-firebase-adminsdk-fbsvc-8bfd99e066.json")
            val serviceAccount = resource.inputStream

            val options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .build()

            // FirebaseApp이 이미 초기화되어 있지 않은 경우에만 초기화 진행
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options)
                log.info("Firebase Admin SDK 초기화 성공")
            }
        } catch (e: IOException) {
            println("Firebase Admin SDK 초기화 실패: ${e.message}")
            e.printStackTrace()
        }
    }
}