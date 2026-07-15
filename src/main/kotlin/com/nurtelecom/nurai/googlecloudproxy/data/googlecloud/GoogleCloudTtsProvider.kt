package com.nurtelecom.nurai.googlecloudproxy.data.googlecloud

import com.nurtelecom.nurai.googlecloudproxy.data.googlecloud.dto.GoogleTtsAudioConfigDto
import com.nurtelecom.nurai.googlecloudproxy.data.googlecloud.dto.GoogleTtsInputDto
import com.nurtelecom.nurai.googlecloudproxy.data.googlecloud.dto.GoogleTtsRequestDto
import com.nurtelecom.nurai.googlecloudproxy.data.googlecloud.dto.GoogleTtsResponseDto
import com.nurtelecom.nurai.googlecloudproxy.data.googlecloud.dto.GoogleTtsVoiceDto
import com.nurtelecom.nurai.googlecloudproxy.domain.SynthesizeRequest
import com.nurtelecom.nurai.googlecloudproxy.domain.SynthesizeResult
import com.nurtelecom.nurai.googlecloudproxy.domain.TextToSpeechProvider
import com.nurtelecom.nurai.googlecloudproxy.domain.UnsupportedLanguageException
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
import kotlinx.serialization.json.Json
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

    // Google rejects/ignores a voice object where "name" is present but null is still
    // spelled out — omit unset fields instead of sending them as JSON null.
    private val requestJson = Json { explicitNulls = false }

    override suspend fun synthesize(request: SynthesizeRequest): SynthesizeResult = withContext(ioDispatcher) {
        val timed = measureTimedValue {
            // Explicit voiceName always wins. Otherwise: Google has zero voices for
            // ky-* at all (any gender) — fail loudly instead of forwarding a request
            // Google will reject or mishandle. Otherwise use our curated FEMALE
            // Chirp3-HD pick when the language has one and the caller wants FEMALE
            // (our default); anything else falls back to plain gender-based auto-select.
            val voice = when {
                request.voiceName != null ->
                    GoogleTtsVoiceDto(languageCode = request.languageCode, name = request.voiceName)

                request.languageCode.substringBefore('-').equals("ky", ignoreCase = true) ->
                    throw UnsupportedLanguageException(
                        languageCode = request.languageCode,
                        message = "Google Cloud TTS does not support ${request.languageCode}, use akylai TTS for Kyrgyz"
                    )

                request.ssmlGender == "FEMALE" &&
                    GoogleNeuralVoiceCatalog.femaleVoiceFor(request.languageCode) != null -> {
                    val curated = GoogleNeuralVoiceCatalog.femaleVoiceFor(request.languageCode)!!
                    GoogleTtsVoiceDto(languageCode = curated.googleLanguageCode, name = curated.voiceName)
                }

                else ->
                    GoogleTtsVoiceDto(languageCode = request.languageCode, ssmlGender = request.ssmlGender)
            }

            val requestDto = GoogleTtsRequestDto(
                input = GoogleTtsInputDto(request.text),
                voice = voice,
                audioConfig = GoogleTtsAudioConfigDto(request.audioEncoding)
            )

            httpClient.post("https://texttospeech.googleapis.com/v1/text:synthesize") {
                header(HttpHeaders.Authorization, authTokenProvider.getAuthorizationHeader())
                contentType(ContentType.Application.Json)
                setBody(requestJson.encodeToString(GoogleTtsRequestDto.serializer(), requestDto))
            }.body<GoogleTtsResponseDto>()
        }

        SynthesizeResult(
            audioBytes = Base64.getDecoder().decode(timed.value.audioContent),
            contentType = "audio/ogg",
            processingTimeMs = timed.duration.inWholeMilliseconds
        )
    }
}
