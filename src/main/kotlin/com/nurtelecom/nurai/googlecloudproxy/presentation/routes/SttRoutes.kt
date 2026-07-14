package com.nurtelecom.nurai.googlecloudproxy.presentation.routes

import com.nurtelecom.nurai.googlecloudproxy.domain.RecognizeRequest
import com.nurtelecom.nurai.googlecloudproxy.domain.SpeechToTextProvider
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
    val sttProvider by inject<SpeechToTextProvider>()

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
