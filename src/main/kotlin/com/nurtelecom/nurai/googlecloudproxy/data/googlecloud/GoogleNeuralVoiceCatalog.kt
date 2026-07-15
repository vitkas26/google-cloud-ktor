package com.nurtelecom.nurai.googlecloudproxy.data.googlecloud

/**
 * Curated FEMALE voice per language, used by [GoogleCloudTtsProvider] when the
 * caller doesn't pass an explicit voiceName. Confirmed live against Google's
 * voices:list on 2026-07-15: Neural2 doesn't exist at all for ru-RU/cmn-CN/tr-TR,
 * so Chirp3-HD (Google's newest tier, generally rated above Neural2) is used
 * uniformly across all 5 languages for a consistent voice character ("Aoede").
 *
 * googleLanguageCode can differ from the map key: Google registers Chinese
 * voices under "cmn-CN", not "zh-CN". Sending voice.languageCode="zh-CN" with
 * a cmn-CN-* voice name is a hard 400 INVALID_ARGUMENT from Google, so the
 * correct Google-side code travels with the voice config instead of being
 * derived from the caller's languageCode.
 *
 * To move to a newer tier later (e.g. if Google ships Neural2/Chirp-next for
 * the missing languages), only this map needs to change.
 */
data class GoogleVoiceConfig(
    val googleLanguageCode: String,
    val voiceName: String
)

object GoogleNeuralVoiceCatalog {
    private val FEMALE_VOICES: Map<String, GoogleVoiceConfig> = mapOf(
        "ru-RU" to GoogleVoiceConfig(googleLanguageCode = "ru-RU", voiceName = "ru-RU-Chirp3-HD-Aoede"),
        "en-US" to GoogleVoiceConfig(googleLanguageCode = "en-US", voiceName = "en-US-Chirp3-HD-Aoede"),
        "de-DE" to GoogleVoiceConfig(googleLanguageCode = "de-DE", voiceName = "de-DE-Chirp3-HD-Aoede"),
        "zh-CN" to GoogleVoiceConfig(googleLanguageCode = "cmn-CN", voiceName = "cmn-CN-Chirp3-HD-Aoede"),
        "tr-TR" to GoogleVoiceConfig(googleLanguageCode = "tr-TR", voiceName = "tr-TR-Chirp3-HD-Aoede")
    )

    fun femaleVoiceFor(languageCode: String): GoogleVoiceConfig? = FEMALE_VOICES[languageCode]
}
