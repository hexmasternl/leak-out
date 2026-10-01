package nl.hexmaster.leakout.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Colour tokens from `docs/design-system-slim.md` section 2. Components use these names, never raw
 * hex values.
 */
object LeakOutColors {
    // 2.1 Primary: Hydro Blue
    val PrimaryDark = Color(0xFF013C60)
    val PrimaryBase = Color(0xFF0277BD)
    val PrimaryMid = Color(0xFF0288D1)
    val PrimaryLight = Color(0xFF03A9F4)
    val PrimarySoft = Color(0xFFB2EBF2)
    val PrimaryBg = Color(0xFFE0F7FA)

    // 2.2 Secondary: Brass Yellow
    val SecondaryDark = Color(0xFFF57F17)
    val SecondaryBase = Color(0xFFFBC02D)
    val SecondaryLight = Color(0xFFFDD835)
    val SecondarySoft = Color(0xFFFFF59D)
    val SecondaryBg = Color(0xFFFFFDE7)

    // 2.3 Structural and neutral
    val InkDark = Color(0xFF1E293B)
    val InkLight = Color(0xFF475569)
    val SteelBase = Color(0xFF546E7A)
    val SteelDark = Color(0xFF37474F)
    val SteelLight = Color(0xFF90A4AE)
    val SurfaceCard = Color(0xFFFFFFFF)

    // 2.4 Functional and status
    val ReservoirPurple = Color(0xFF8E24AA)
    val SuccessGreen = Color(0xFF43A047)
    val ErrorRed = Color(0xFFE53935)

    // Not in the token table: the danger button needs a darker shade of ErrorRed for its bottom
    // shadow, the same way the primary and secondary families have a `-dark` token.
    val ErrorRedDark = Color(0xFFB71C1C)

    // Not in the token table either: the bottom shadow of the success button, a darker SuccessGreen.
    val SuccessGreenDark = Color(0xFF1B5E20)

    // 4.1 inner high shine: rgba(255, 255, 255, 0.4)
    val Shine = Color(0x66FFFFFF)

    // 4.2 disabled state
    val DisabledFill = Color(0xFF94A3B8)
    val DisabledShadow = Color(0xFF64748B)

    // The welcome art is a dark navy scene; this matches its base tone for the window background.
    val SceneNavy = Color(0xFF0C1A2A)

    // Deep Flow tokens (docs/design-system.md v2, Deep theme). There is no theme switch yet;
    // screens that must stay dark whatever the theme (the splash) read these directly.
    // 2.1 surface.deep
    val SurfaceDeep = Color(0xFF0C1A2A)

    // 2.1 surface.raised
    val SurfaceRaised = Color(0xFF19324A)

    // 2.1 surface.abyss and surface.base
    val SurfaceAbyss = Color(0xFF07111C)
    val SurfaceBase = Color(0xFF122538)

    // 2.5 status: success.500 and the 16 % tints used by the level tiles
    val Success500 = Color(0xFF3DDC97)
    val SuccessTint = Color(0x293DDC97)
    val LeakTint = Color(0x29FF5A5F)

    // 2.3 Liquid (hero colour)
    val LiquidGlow = Color(0xFF9DF3FF)
    val Liquid300 = Color(0xFF5EE7FF)
    val Liquid500 = Color(0xFF19C6F2)
    val Liquid700 = Color(0xFF0A8DC2)
    val Liquid900 = Color(0xFF065A82)

    // 2.5 reservoir: the tank is violet, the liquid inside it stays aqua
    val Reservoir300 = Color(0xFFC7B5FF)
    val Reservoir500 = Color(0xFFA78BFA)
    val Reservoir700 = Color(0xFF7C5CE6)

    // 2.6 Steel (pipes). The pipe art itself lives in assets/pipes/*.svg, which use the same hex
    // values; these tokens are for anything drawn around the pipes in code.
    val Steel100 = Color(0xFFC9D6E2)
    val Steel300 = Color(0xFF8FA3B8)
    val Steel500 = Color(0xFF5C7189)
    val Steel700 = Color(0xFF3A4B60)
    val Steel900 = Color(0xFF22303F)

    // 2.4 Brass (action accent)
    val Brass100 = Color(0xFFFFE9B0)
    val Brass300 = Color(0xFFFFCF5C)
    val Brass500 = Color(0xFFF5A623)
    val Brass700 = Color(0xFFC97A12)
    val Brass900 = Color(0xFF7A4708)

    // 2.7 Text: text.muted (Deep theme)
    val TextMuted = Color(0xFF6E8296)

    // 2.7 Text: text.primary and text.secondary (Deep theme)
    val TextPrimary = Color(0xFFF2F7FB)
    val TextSecondary = Color(0xFFA9BACB)

    // 2.1 surface.overlay: secondary buttons, switch track when off
    val SurfaceOverlay = Color(0xFF21405C)

    // 2.1 lines: white at 8 % (line.subtle) and 16 % (line.strong)
    val LineSubtle = Color(0x14FFFFFF)
    val LineStrong = Color(0x29FFFFFF)
}
