package com.nurtelecom.nurai.googlecloudproxy.data.googlecloud.dto

import kotlinx.serialization.Serializable

@Serializable
data class GoogleSttRequestDto(
    val config: GoogleSttConfigDto,
    val audio: GoogleSttAudioDto
)

@Serializable
data class GoogleSttConfigDto(
    val encoding: String,
    val sampleRateHertz: Int,
    val languageCode: String,
    // Google supports up to 3 extra languages alongside the primary one.
    val alternativeLanguageCodes: List<String> = emptyList()
)

@Serializable
data class GoogleSttAudioDto(val content: String) // base64-encoded audio

@Serializable
data class GoogleSttResponseDto(
    val results: List<GoogleSttResultDto> = emptyList()
)

@Serializable
data class GoogleSttResultDto(
    val alternatives: List<GoogleSttAlternativeDto> = emptyList(),
    val languageCode: String? = null
)

@Serializable
data class GoogleSttAlternativeDto(
    val transcript: String = "",
    val confidence: Float = 0f
)
