package nl.hexmaster.leakout

/**
 * The languages Leak Out is shown in: English, the language of res/values, plus the languages turned
 * on for Play Console's automatic app-strings translation (openspec/specs/app-localization,
 * "Supported languages"). Only Latin-script languages, because the bundled fonts only draw Latin.
 * res/xml/locales_config.xml lists the same tags; keep both in step with the Play Console.
 */
object SupportedLanguages {
    /** BCP 47 language tags, English first. */
    val TAGS: List<String> = listOf("en", "nl", "de", "fr", "es", "it", "pt-BR")
}
