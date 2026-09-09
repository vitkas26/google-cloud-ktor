package com.nurtelecom.nurai.googlecloudproxy.di

import com.nurtelecom.nurai.googlecloudproxy.config.AppConfig
import com.nurtelecom.nurai.googlecloudproxy.data.googlecloud.GoogleAuthTokenProvider
import com.nurtelecom.nurai.googlecloudproxy.data.googlecloud.GoogleCloudSttProvider
import com.nurtelecom.nurai.googlecloudproxy.data.googlecloud.GoogleCloudSttProviderV2
import com.nurtelecom.nurai.googlecloudproxy.data.googlecloud.GoogleCloudTtsProvider
import com.nurtelecom.nurai.googlecloudproxy.domain.SpeechToTextProvider
import com.nurtelecom.nurai.googlecloudproxy.domain.TextToSpeechProvider
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.qualifier.named
import org.koin.dsl.module

/** Qualifiers for the STT V1/V2 A/B setup — keeps SttRoutes importing only the domain interface. */
val STT_V1 = named("sttV1")
val STT_V2 = named("sttV2")

/**
 * Single place where interfaces get bound to implementations.
 * Swapping Google Cloud for another provider later (or adding a second one
 * for A/B testing) means adding a binding here — routes never change.
 */
fun appModule(config: AppConfig) = module {
    single { config }

    single {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
    }

    single {
        GoogleAuthTokenProvider(credentialsPath = config.googleApplicationCredentialsPath)
    }

    single<TextToSpeechProvider> {
        GoogleCloudTtsProvider(httpClient = get(), authTokenProvider = get())
    }

    single<SpeechToTextProvider>(STT_V1) {
        GoogleCloudSttProvider(httpClient = get(), authTokenProvider = get())
    }

    single<SpeechToTextProvider>(STT_V2) {
        GoogleCloudSttProviderV2(httpClient = get(), authTokenProvider = get())
    }

    // Default/unqualified binding preserved for backward compatibility — same as before this change.
    single<SpeechToTextProvider> { get(STT_V1) }
}
