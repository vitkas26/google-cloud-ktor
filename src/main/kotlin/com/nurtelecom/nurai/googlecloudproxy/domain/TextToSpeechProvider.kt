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
    /** Explicit voice override. When null, the provider auto-selects a voice using [ssmlGender]. */
    val voiceName: String? = null,
    val ssmlGender: String = "FEMALE",
    val audioEncoding: String = "OGG_OPUS"
)

data class SynthesizeResult(
    val audioBytes: ByteArray,
    val contentType: String,
    val processingTimeMs: Long
)

/** Thrown by a [TextToSpeechProvider] implementation when a languageCode has no voice available at all. */
class UnsupportedLanguageException(val languageCode: String, message: String) : Exception(message)
