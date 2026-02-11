package com.sixclassguys.maplecalendar.global.service

import com.amazonaws.AmazonServiceException
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.model.PutObjectRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.util.*

@Service
class S3Service(
    private val amazonS3: AmazonS3,
    @Value("\${s3.bucket.name}")
    private val bucket: String,
    @Value("\${s3.bucket.region:ap-northeast-2}")
    private val region: String
) {
    companion object {
        private const val MAX_FILE_SIZE = 10 * 1024 * 1024L // 10MB
        private val ALLOWED_EXTENSIONS = setOf("jpg", "jpeg", "png", "gif", "webp")
        private val ALLOWED_MIME_TYPES = setOf(
            "image/jpeg", "image/png", "image/gif", "image/webp"
        )
    }

    fun uploadFile(multipartFile: MultipartFile, dirName: String): String {
        // 1. 파일 검증
        validateFile(multipartFile)

        // 2. 파일명 생성
        val fileName = generateFileName(dirName, multipartFile.originalFilename!!)

        // 3. 메타데이터 설정
        val objectMetadata = ObjectMetadata().apply {
            contentLength = multipartFile.size
            contentType = multipartFile.contentType
            cacheControl = "max-age=31536000" // 1년 캐시
        }

        // 4. S3에 업로드
        try {
            amazonS3.putObject(
                PutObjectRequest(bucket, fileName, multipartFile.inputStream, objectMetadata)
                    .withCannedAcl(com.amazonaws.services.s3.model.CannedAccessControlList.PublicRead)
            )
        } catch (e: AmazonServiceException) {
            throw RuntimeException("S3 업로드 실패: ${e.message}", e)
        }

        // 5. URL 반환 (직접 구성 - API 호출 제거)
        return "https://$bucket.s3.$region.amazonaws.com/$fileName"
    }

    private fun validateFile(file: MultipartFile) {
        require(!file.isEmpty) { "파일이 비어있습니다." }
        require(file.size <= MAX_FILE_SIZE) { "파일 크기가 ${MAX_FILE_SIZE / (1024 * 1024)}MB를 초과합니다." }
        
        val extension = file.originalFilename?.substringAfterLast(".")?.lowercase() ?: ""
        require(extension in ALLOWED_EXTENSIONS) { "허용되지 않는 파일 형식: $extension" }
        
        val mimeType = file.contentType ?: ""
        require(mimeType in ALLOWED_MIME_TYPES) { "허용되지 않는 MIME 타입: $mimeType" }
    }

    private fun generateFileName(dirName: String, originalFilename: String): String {
        val sanitizedDir = dirName.replace("[^a-zA-Z0-9-_]".toRegex(), "")
        val sanitizedName = originalFilename
            .substringBeforeLast(".")
            .replace("[^a-zA-Z0-9-_]".toRegex(), "")
        val extension = originalFilename.substringAfterLast(".")
        val timestamp = System.currentTimeMillis()
        val uuid = UUID.randomUUID().toString().take(8)
        
        return "$sanitizedDir/$timestamp-$uuid-$sanitizedName.$extension"
    }
}