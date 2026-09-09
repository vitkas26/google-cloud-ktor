package com.nurtelecom.nurai.googlecloudproxy.presentation.routes

import com.nurtelecom.nurai.googlecloudproxy.di.STT_V1
import com.nurtelecom.nurai.googlecloudproxy.di.STT_V2
import com.nurtelecom.nurai.googlecloudproxy.domain.RecognizeRequest
import com.nurtelecom.nurai.googlecloudproxy.domain.SpeechToTextProvider
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.inject

@Serializable
data class SttResponseBody(
    val transcript: String,
    val confidence: Float,
    val languageDetected: String?,
    val processingTimeMs: Long
)

fun Route.sttRoutes() {
    val sttProviderV1 by inject<SpeechToTextProvider>(STT_V1)
    val sttProviderV2 by inject<SpeechToTextProvider>(STT_V2)

    // Same shape as your existing whisper-ktor contract: raw audio bytes in,
    // JSON transcript out. Language hints come from query params so the
    // Android client can pass whatever it already detected/selected.
    post("/stt") {
        val languageCode = call.request.queryParameters["lang"] ?: "ru-RU"
        val alternativeLanguages = call.request.queryParameters["alt"]
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?: emptyList()

        // V2 (?apiVersion=v2) ignores languageCode/alt entirely — it's full auto-detect.
        // Default stays v1 so existing callers that pass nothing are unaffected.
        val apiVersion = call.request.queryParameters["apiVersion"] ?: "v1"
        val sttProvider = when (apiVersion) {
            "v1" -> sttProviderV1
            "v2" -> sttProviderV2
            else -> {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "apiVersion must be v1 or v2"))
                return@post
            }
        }

        val audioBytes = call.receive<ByteArray>()

        val result = sttProvider.recognize(
            RecognizeRequest(
                audioBytes = audioBytes,
                languageCode = languageCode,
                alternativeLanguageCodes = alternativeLanguages
            )
        )

        call.respond(
            SttResponseBody(
                transcript = result.transcript,
                confidence = result.confidence,
                languageDetected = result.languageDetected,
                processingTimeMs = result.processingTimeMs
            )
        )
    }
}
