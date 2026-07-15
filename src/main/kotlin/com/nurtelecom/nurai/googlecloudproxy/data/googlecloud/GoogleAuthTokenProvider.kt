package com.nurtelecom.nurai.googlecloudproxy.data.googlecloud

import com.google.auth.oauth2.GoogleCredentials
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.io.FileInputStream

/**
 * Loads a service-account key and produces the Authorization header both
 * Google Cloud providers need. GoogleCredentials caches the token and
 * refreshes it internally once it's close to expiry, so callers can just
 * ask for the header on every request without worrying about refresh logic.
 */
class GoogleAuthTokenProvider(credentialsPath: String) {

    private val credentials: GoogleCredentials = FileInputStream(credentialsPath).use { stream ->
        GoogleCredentials.fromStream(stream).createScoped(SCOPES)
    }

    // Parsed straight from the JSON rather than cast to ServiceAccountCredentials —
    // GoogleCredentials (the type we hold) doesn't expose getProjectId() itself,
    // only its ServiceAccountCredentials subtype does.
    val projectId: String = Json.parseToJsonElement(File(credentialsPath).readText())
        .jsonObject["project_id"]?.jsonPrimitive?.content
        ?: error("Service account JSON at $credentialsPath is missing project_id")

    fun getAuthorizationHeader(): String {
        val metadata = credentials.getRequestMetadata()
        return metadata[AUTHORIZATION_HEADER]?.firstOrNull()
            ?: error("Google credentials did not return an $AUTHORIZATION_HEADER header")
    }

    companion object {
        private const val AUTHORIZATION_HEADER = "Authorization"
        private val SCOPES = listOf("https://www.googleapis.com/auth/cloud-platform")
    }
}
