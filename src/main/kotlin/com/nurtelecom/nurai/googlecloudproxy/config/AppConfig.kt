package com.nurtelecom.nurai.googlecloudproxy.config

/**
 * All configuration comes from environment variables — nothing sensitive is
 * ever hardcoded or committed. When you run this from Android Studio, set
 * these in the Run Configuration's "Environment variables" field.
 * When you deploy the Dockerfile to the VPS, set them in your .env file
 * (same pattern as whisper-ktor / piper-server / akylai-server).
 */
data class AppConfig(
    val port: Int,
    val googleApplicationCredentialsPath: String,
    /**
     * Optional shared-secret header check (X-Proxy-Token) so this proxy isn't
     * wide open on the VPS. Leave PROXY_AUTH_TOKEN unset to disable the check
     * during local testing in Android Studio.
     */
    val proxyAuthToken: String?
) {
    companion object {
        fun fromEnv(): AppConfig {
            val credentialsPath = System.getenv("GOOGLE_APPLICATION_CREDENTIALS")
                ?: error(
                    "GOOGLE_APPLICATION_CREDENTIALS is not set. In Android Studio: " +
                        "Run > Edit Configurations > Environment variables. It must point to " +
                        "a Google Cloud service-account JSON key file."
                )
            return AppConfig(
                port = System.getenv("PORT")?.toIntOrNull() ?: 8003,
                googleApplicationCredentialsPath = credentialsPath,
                proxyAuthToken = System.getenv("PROXY_AUTH_TOKEN")
            )
        }
    }
}
