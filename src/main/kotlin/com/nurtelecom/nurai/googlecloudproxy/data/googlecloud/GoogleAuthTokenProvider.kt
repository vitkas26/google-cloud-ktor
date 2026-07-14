package com.nurtelecom.nurai.googlecloudproxy.data.googlecloud

import com.google.auth.oauth2.GoogleCredentials
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
