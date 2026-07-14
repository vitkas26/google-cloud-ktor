package com.nurtelecom.nurai.googlecloudproxy.domain

/**
 * Abstraction over "something that turns audio into text".
 * Same rationale as TextToSpeechProvider: routes depend on this, not on Google Cloud directly.
 */
interface SpeechToTextProvider {
    suspend fun recognize(request: RecognizeRequest): RecognizeResult
}

data class RecognizeRequest(
    val audioBytes: ByteArray,
    val languageCode: String,
    val alternativeLanguageCodes: List<String> = emptyList(),
    val sampleRateHertz: Int = 16000,
    val encoding: String = "OGG_OPUS"
)

data class RecognizeResult(
    val transcript: String,
    val confidence: Float,
    val languageDetected: String?,
    val processingTimeMs: Long
)
