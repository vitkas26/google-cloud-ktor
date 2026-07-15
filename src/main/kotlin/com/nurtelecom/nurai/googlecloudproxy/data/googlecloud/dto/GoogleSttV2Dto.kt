package com.nurtelecom.nurai.googlecloudproxy.data.googlecloud.dto

import kotlinx.serialization.Serializable

@Serializable
data class GoogleSttV2RequestDto(
    val config: GoogleSttV2ConfigDto,
    val content: String
)

@Serializable
data class GoogleSttV2ConfigDto(
    val autoDecodingConfig: GoogleSttV2AutoDecodingConfigDto = GoogleSttV2AutoDecodingConfigDto(),
    val languageCodes: List<String>,
    val model: String
)

// Deliberately empty — Google's autoDecodingConfig is an empty object marker
// that tells V2 to infer the audio container/encoding itself.
@Serializable
class GoogleSttV2AutoDecodingConfigDto

// Real shape confirmed live 2026-07-15 against eu-speech.googleapis.com/v2 with
// model=chirp_3, languageCodes=["auto"]. Unlike V1, there is NO confidence field
// in the alternative at all, and languageCode comes back short-form ("ru", not
// "ru-RU") — both defaulted defensively here rather than assumed present.
@Serializable
data class GoogleSttV2ResponseDto(
    val results: List<GoogleSttV2ResultDto> = emptyList()
)

@Serializable
data class GoogleSttV2ResultDto(
    val alternatives: List<GoogleSttV2AlternativeDto> = emptyList(),
    val languageCode: String? = null
)

@Serializable
data class GoogleSttV2AlternativeDto(
    val transcript: String = "",
    val confidence: Float = 0f
)
