package com.nurtelecom.nurai.googlecloudproxy.data.googlecloud

import com.nurtelecom.nurai.googlecloudproxy.data.googlecloud.dto.GoogleSttV2ConfigDto
import com.nurtelecom.nurai.googlecloudproxy.data.googlecloud.dto.GoogleSttV2RequestDto
import com.nurtelecom.nurai.googlecloudproxy.data.googlecloud.dto.GoogleSttV2ResponseDto
import com.nurtelecom.nurai.googlecloudproxy.domain.RecognizeRequest
import com.nurtelecom.nurai.googlecloudproxy.domain.RecognizeResult
import com.nurtelecom.nurai.googlecloudproxy.domain.SpeechToTextProvider
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.util.Base64
import kotlin.time.measureTimedValue

/**
 * Calls the Google Cloud Speech-to-Text V2 REST API with full language
 * auto-detection (languageCodes=["auto"], model="chirp_3") instead of V1's
 * explicit languageCode + up-to-3 alternativeLanguageCodes. Google doesn't
 * restrict V2 recognition to a candidate list at all — it picks from its
 * full supported-language set.
 *
 * V2 recognizers are regional (no global endpoint like V1's
 * speech.googleapis.com) — "eu" is where chirp_3 is available, confirmed
 * live. RecognizeRequest.languageCode/alternativeLanguageCodes/encoding/
 * sampleRateHertz are all ignored here on purpose: auto-detect + autoDecodingConfig
 * make them moot for this path.
 *
 * Kept alongside GoogleCloudSttProvider (V1), not replacing it — this is an
 * A/B alternative, selected per-request via SttRoutes.
 */
class GoogleCloudSttProviderV2(
    private val httpClient: HttpClient,
    private val authTokenProvider: GoogleAuthTokenProvider,
    private val region: String = "eu",
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : SpeechToTextProvider {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun recognize(request: RecognizeRequest): RecognizeResult = withContext(ioDispatcher) {
        val timed = measureTimedValue {
            val audioContent = Base64.getEncoder().encodeToString(request.audioBytes)
            val requestDto = GoogleSttV2RequestDto(
                config = GoogleSttV2ConfigDto(
                    languageCodes = listOf("auto"),
                    model = "chirp_3"
                ),
                content = audioContent
            )

            val url = "https://$region-speech.googleapis.com/v2/projects/" +
                "${authTokenProvider.projectId}/locations/$region/recognizers/_:recognize"

            // TEMP DEBUG — same pattern as V1, remove once the V2 path is proven stable.
            println("=== OUTGOING GOOGLE STT V2 REQUEST (url=$url) ===")
            println(
                json.encodeToString(
                    GoogleSttV2RequestDto.serializer(),
                    requestDto.copy(content = audioContent.take(50) + "...")
                )
            )

            val response = httpClient.post(url) {
                header(HttpHeaders.Authorization, authTokenProvider.getAuthorizationHeader())
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(GoogleSttV2RequestDto.serializer(), requestDto))
            }

            // TEMP DEBUG — same pattern as V1, remove once the V2 path is proven stable.
            val rawBody = response.bodyAsText()
            println("=== RAW GOOGLE STT V2 RESPONSE (status=${response.status}) ===")
            println(rawBody)

            json.decodeFromString(GoogleSttV2ResponseDto.serializer(), rawBody)
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
