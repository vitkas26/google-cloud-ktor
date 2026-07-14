package com.nurtelecom.nurai.googlecloudproxy.data.googlecloud

import com.nurtelecom.nurai.googlecloudproxy.data.googlecloud.dto.GoogleSttAudioDto
import com.nurtelecom.nurai.googlecloudproxy.data.googlecloud.dto.GoogleSttConfigDto
import com.nurtelecom.nurai.googlecloudproxy.data.googlecloud.dto.GoogleSttRequestDto
import com.nurtelecom.nurai.googlecloudproxy.data.googlecloud.dto.GoogleSttResponseDto
import com.nurtelecom.nurai.googlecloudproxy.domain.RecognizeRequest
import com.nurtelecom.nurai.googlecloudproxy.domain.RecognizeResult
import com.nurtelecom.nurai.googlecloudproxy.domain.SpeechToTextProvider
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
 * Calls the Google Cloud Speech-to-Text REST API (synchronous "recognize" endpoint).
 *
 * NOTE: this sync endpoint is capped at ~1 minute of audio and is NOT a streaming API.
 * For short voice queries (a few seconds) that's fine. If you later need true
 * low-latency streaming recognition, Google's streaming API is gRPC-only, not REST,
 * and would need a separate implementation of this same interface.
 */
class GoogleCloudSttProvider(
    private val httpClient: HttpClient,
    private val authTokenProvider: GoogleAuthTokenProvider,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : SpeechToTextProvider {

    override suspend fun recognize(request: RecognizeRequest): RecognizeResult = withContext(ioDispatcher) {
        val timed = measureTimedValue {
            httpClient.post("https://speech.googleapis.com/v1/speech:recognize") {
                header(HttpHeaders.Authorization, authTokenProvider.getAuthorizationHeader())
                contentType(ContentType.Application.Json)
                setBody(
                    GoogleSttRequestDto(
                        config = GoogleSttConfigDto(
                            encoding = request.encoding,
                            sampleRateHertz = request.sampleRateHertz,
                            languageCode = request.languageCode,
                            alternativeLanguageCodes = request.alternativeLanguageCodes
                        ),
                        audio = GoogleSttAudioDto(
                            content = Base64.getEncoder().encodeToString(request.audioBytes)
                        )
                    )
                )
            }.body<GoogleSttResponseDto>()
        }

        val bestResult = timed.value.results.firstOrNull()
        val bestAlternative = bestResult?.alternatives?.firstOrNull()

        RecognizeResult(
            transcript = bestAlternative?.transcript ?: "",
            confidence = bestAlternative?.confidence ?: 0f,
            languageDetected = bestResult?.languageCode,
            processingTimeMs = timed.duration.inWholeMilliseconds
        )
    }
}
