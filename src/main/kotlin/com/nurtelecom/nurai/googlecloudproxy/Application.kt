package com.nurtelecom.nurai.googlecloudproxy

import com.nurtelecom.nurai.googlecloudproxy.config.AppConfig
import com.nurtelecom.nurai.googlecloudproxy.di.appModule
import com.nurtelecom.nurai.googlecloudproxy.domain.UnsupportedLanguageException
import com.nurtelecom.nurai.googlecloudproxy.presentation.routes.sttRoutes
import com.nurtelecom.nurai.googlecloudproxy.presentation.routes.ttsRoutes
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.Json
import org.koin.ktor.plugin.Koin
import org.slf4j.event.Level

fun main() {
    val config = AppConfig.fromEnv()

    embeddedServer(Netty, port = config.port) {
        module(config)
    }.start(wait = true)
}

fun Application.module(config: AppConfig = AppConfig.fromEnv()) {
    install(Koin) {
        modules(appModule(config))
    }

    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }

    install(CallLogging) {
        level = Level.INFO
    }

    install(StatusPages) {
        exception<UnsupportedLanguageException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to cause.message))
        }
        exception<Throwable> { call, cause ->
            call.application.environment.log.error("Unhandled error", cause)
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (cause.message ?: "unknown error")))
        }
    }

    // Simple shared-secret check. Disabled automatically if PROXY_AUTH_TOKEN isn't set,
    // so local testing in Android Studio needs zero extra setup.
    if (config.proxyAuthToken != null) {
        intercept(io.ktor.server.application.ApplicationCallPipeline.Plugins) {
            val token = call.request.header("X-Proxy-Token")
            if (token != config.proxyAuthToken) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "invalid or missing X-Proxy-Token"))
                finish()
            }
        }
    }

    routing {
        get("/health") {
            call.respondText("OK")
        }
        ttsRoutes()
        sttRoutes()
    }
}
