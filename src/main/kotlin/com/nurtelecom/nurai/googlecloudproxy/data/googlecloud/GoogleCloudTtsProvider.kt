package com.nurtelecom.nurai.googlecloudproxy.data.googlecloud

import com.nurtelecom.nurai.googlecloudproxy.data.googlecloud.dto.GoogleTtsAudioConfigDto
import com.nurtelecom.nurai.googlecloudproxy.data.googlecloud.dto.GoogleTtsInputDto
import com.nurtelecom.nurai.googlecloudproxy.data.googlecloud.dto.GoogleTtsRequestDto
import com.nurtelecom.nurai.googlecloudproxy.data.googlecloud.dto.GoogleTtsResponseDto
import com.nurtelecom.nurai.googlecloudproxy.data.googlecloud.dto.GoogleTtsVoiceDto
import com.nurtelecom.nurai.googlecloudproxy.domain.SynthesizeRequest
import com.nurtelecom.nurai.googlecloudproxy.domain.SynthesizeResult
import com.nurtelecom.nurai.googlecloudproxy.domain.TextToSpeechProvider
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Base64
import kotlin.time.measureTimedValue

/**
 * Calls the Google Cloud Text-to-Speech REST API.
 * This is the ONLY class in the project that knows Google's request/response shape.
 * Everything above it (routes) only sees TextToSpeechProvider.
 */
class GoogleCloudTtsProvider(
    private val httpClient: HttpClient,
    private val authTokenProvider: GoogleAuthTokenProvider,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : TextToSpeechProvider {

    override suspend fun synthesize(request: SynthesizeRequest): SynthesizeResult = withContext(ioDispatcher) {
        val timed = measureTimedValue {
            httpClient.post("https://texttospeech.googleapis.com/v1/text:synthesize") {
                header(HttpHeaders.Authorization, authTokenProvider.getAuthorizationHeader())
                contentType(ContentType.Application.Json)
                setBody(
                    GoogleTtsRequestDto(
                        input = GoogleTtsInputDto(request.text),
                        voice = GoogleTtsVoiceDto(request.languageCode, request.voiceName),
                        audioConfig = GoogleTtsAudioConfigDto(request.audioEncoding)
                    )
                )
            }.body<GoogleTtsResponseDto>()
        }

        SynthesizeResult(
            audioBytes = Base64.getDecoder().decode(timed.value.audioContent),
            contentType = "audio/ogg",
            processingTimeMs = timed.duration.inWholeMilliseconds
        )
    }
}
