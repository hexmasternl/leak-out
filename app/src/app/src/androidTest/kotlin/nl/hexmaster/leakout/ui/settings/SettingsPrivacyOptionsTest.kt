package nl.hexmaster.leakout.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import nl.hexmaster.leakout.settings.Settings
import nl.hexmaster.leakout.ui.theme.LeakOutTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The "Privacy options" row of the settings screen (openspec/specs/ad-consent). */
@RunWith(AndroidJUnit4::class)
class SettingsPrivacyOptionsTest {
    @get:Rule
    val compose = createComposeRule()

    private var opened = 0

    private fun showScreen(privacyOptionsRequired: Boolean) {
        compose.setContent {
            LeakOutTheme {
                Box(Modifier.size(800.dp, 360.dp)) {
                    SettingsContent(
                        state = SettingsUiState(Settings(), ProgressSummary.None),
                        appInfo = AppInfo(versionName = "1.0.0", versionCode = 1),
                        onBack = {},
                        onVibrationChange = {},
                        onResetConfirmed = {},
                        privacyOptionsRequired = privacyOptionsRequired,
                        onPrivacyOptions = { opened++ },
                    )
                }
            }
        }
    }

    @Test
    fun rowIsShownAndOpensTheConsentFormWhenPrivacyOptionsAreRequired() {
        showScreen(privacyOptionsRequired = true)

        compose.onNodeWithTag(SettingsTags.PRIVACY_OPTIONS).performScrollTo().assertIsDisplayed().performClick()

        assertEquals(1, opened)
    }

    @Test
    fun rowIsAbsentWhenPrivacyOptionsAreNotRequired() {
        showScreen(privacyOptionsRequired = false)

        compose.onNodeWithTag(SettingsTags.PRIVACY_OPTIONS).assertDoesNotExist()
    }
}

