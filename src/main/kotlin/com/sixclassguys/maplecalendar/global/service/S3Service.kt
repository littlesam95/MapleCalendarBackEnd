package com.sixclassguys.maplecalendar.global.service

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.CannedAccessControlList
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.model.PutObjectRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile

@Service
class S3Service(
    private val amazonS3: AmazonS3,
    @Value("\${s3.bucket.name}")
    private val bucket: String
) {

    fun uploadFile(multipartFile: MultipartFile, dirName: String): String {
        // 1. 파일명 중복 방지를 위해 UUID와 결합
        val fileName = "$dirName/\${UUID.randomUUID()}-\${multipartFile.originalFilename}"

        // 2. 메타데이터 설정 (파일 타입, 크기 등)
        val objectMetadata = ObjectMetadata().apply {
            contentLength = multipartFile.size
            contentType = multipartFile.contentType
        }

        // 3. S3에 업로드
        amazonS3.putObject(
            PutObjectRequest(bucket, fileName, multipartFile.inputStream, objectMetadata)
        )

        // 4. 업로드된 파일의 URL 반환
        return amazonS3.getUrl(bucket, fileName).toString()
    }
}