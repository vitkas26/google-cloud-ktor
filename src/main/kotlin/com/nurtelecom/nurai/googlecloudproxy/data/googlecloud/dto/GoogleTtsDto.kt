package com.nurtelecom.nurai.googlecloudproxy.data.googlecloud.dto

import kotlinx.serialization.Serializable

@Serializable
data class GoogleTtsRequestDto(
    val input: GoogleTtsInputDto,
    val voice: GoogleTtsVoiceDto,
    val audioConfig: GoogleTtsAudioConfigDto
)

@Serializable
data class GoogleTtsInputDto(val text: String)

@Serializable
data class GoogleTtsVoiceDto(
    val languageCode: String,
    val name: String
)

@Serializable
data class GoogleTtsAudioConfigDto(
    val audioEncoding: String
)

@Serializable
data class GoogleTtsResponseDto(
    val audioContent: String // base64-encoded audio
)
