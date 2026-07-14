package com.nurtelecom.nurai.googlecloudproxy.presentation.routes

import com.nurtelecom.nurai.googlecloudproxy.domain.SynthesizeRequest
import com.nurtelecom.nurai.googlecloudproxy.domain.TextToSpeechProvider
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.header
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.inject

@Serializable
data class TtsRequestBody(
    val text: String,
    val languageCode: String,   // e.g. "tr-TR"
    val voiceName: String       // e.g. "tr-TR-Wavenet-D"
)

fun Route.ttsRoutes() {
    val ttsProvider by inject<TextToSpeechProvider>()

    // Same shape as your existing piper-server contract: POST text in, audio bytes out.
    post("/tts") {
        val body = call.receive<TtsRequestBody>()

        val result = ttsProvider.synthesize(
            SynthesizeRequest(
                text = body.text,
                languageCode = body.languageCode,
                voiceName = body.voiceName
            )
        )

        call.response.header("X-Processing-Time-Ms", result.processingTimeMs.toString())
        call.respondBytes(
            bytes = result.audioBytes,
            contentType = ContentType.parse(result.contentType)
        )
    }
}
