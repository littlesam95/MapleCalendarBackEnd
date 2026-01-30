package com.sixclassguys.maplecalendar.global.controller

import com.sixclassguys.maplecalendar.global.service.S3Service
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/test/s3")
class S3TestController(
    private val s3Service: S3Service
) {

    @PostMapping("/upload")
    fun uploadTest(
        @RequestParam("file") file: MultipartFile,
        @RequestParam("dir") dir: String
    ): ResponseEntity<String> {
        return try {
            val imageUrl = s3Service.uploadFile(file, dir)
            ResponseEntity.ok(imageUrl) // 성공 시 업로드된 URL 반환
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.message)
        }
    }
}