package com.nurtelecom.nurai.googlecloudproxy.domain

/**
 * Abstraction over "something that turns text into audio".
 *
 * The route layer (presentation) only knows this interface, never the
 * concrete Google Cloud implementation. This is what lets you swap in
 * Piper/AkylAI later, or run both side by side for the A/B comparison,
 * without touching the routes or the Android client contract.
 */
interface TextToSpeechProvider {
    suspend fun synthesize(request: SynthesizeRequest): SynthesizeResult
}

data class SynthesizeRequest(
    val text: String,
    val languageCode: String,
    val voiceName: String,
    val audioEncoding: String = "OGG_OPUS"
)

data class SynthesizeResult(
    val audioBytes: ByteArray,
    val contentType: String,
    val processingTimeMs: Long
)
