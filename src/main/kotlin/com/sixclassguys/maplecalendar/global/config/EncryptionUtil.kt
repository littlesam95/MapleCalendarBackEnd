package com.sixclassguys.maplecalendar.global.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

@Component
class EncryptionUtil(
    @Value("\${encryption.secret-key}") private val secretKey: String // 32자리의 비밀키
) {

    private val ALGORITHM = "AES/GCM/NoPadding"
    private val TAG_LENGTH_BIT = 128
    private val IV_LENGTH_BYTE = 12

    // 헬퍼 함수 (SHA-256 해싱)
    fun hashKey(key: String): String {
        return MessageDigest.getInstance("SHA-256")
            .digest(key.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }

    fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance(ALGORITHM)
        val iv = ByteArray(IV_LENGTH_BYTE).apply { SecureRandom().nextBytes(this) }
        val spec = GCMParameterSpec(TAG_LENGTH_BIT, iv)
        val keySpec = SecretKeySpec(secretKey.toByteArray(), "AES")

        cipher.init(Cipher.ENCRYPT_MODE, keySpec, spec)
        val cipherText = cipher.doFinal(plainText.toByteArray())

        // IV와 암호문을 합쳐서 Base64로 인코딩
        return Base64.getEncoder().encodeToString(iv + cipherText)
    }

    fun decrypt(encryptedText: String): String {
        val decoded = Base64.getDecoder().decode(encryptedText)
        val iv = decoded.sliceArray(0 until IV_LENGTH_BYTE)
        val cipherText = decoded.sliceArray(IV_LENGTH_BYTE until decoded.size)

        val cipher = Cipher.getInstance(ALGORITHM)
        val spec = GCMParameterSpec(TAG_LENGTH_BIT, iv)
        val keySpec = SecretKeySpec(secretKey.toByteArray(), "AES")

        cipher.init(Cipher.DECRYPT_MODE, keySpec, spec)
        return String(cipher.doFinal(cipherText))
    }
}