package com.sixclassguys.maplecalendar.global.config

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter
class ApiKeyConverter(
    private val encryptionUtil: EncryptionUtil
) : AttributeConverter<String, String> {

    override fun convertToDatabaseColumn(attribute: String?): String? {
        return attribute?.let { encryptionUtil.encrypt(it) }
    }

    override fun convertToEntityAttribute(dbData: String?): String? {
        return dbData?.let { encryptionUtil.decrypt(it) }
    }
}