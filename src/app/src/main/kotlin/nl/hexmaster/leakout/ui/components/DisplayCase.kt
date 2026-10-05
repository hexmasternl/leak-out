package nl.hexmaster.leakout.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import java.util.Locale

/**
 * [this] in capitals by the rules of [locale], so a Turkish "i" becomes "İ". Kotlin's plain
 * `uppercase()` ignores the language, which is fine for identifiers but not for text a player reads
 * (openspec/specs/app-localization, "Locale-aware formatting").
 */
fun String.displayUppercase(locale: Locale): String = uppercase(locale)

/** [this] in capitals by the rules of the language the UI is shown in. */
@Composable
@ReadOnlyComposable
fun String.displayUppercase(): String = displayUppercase(currentLocale())

/** The language the UI is shown in: the first of the configuration's locales. */
@Composable
@ReadOnlyComposable
fun currentLocale(): Locale = LocalConfiguration.current.locales[0]
