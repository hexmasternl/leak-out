package nl.hexmaster.leakout

/**
 * The app's feature flags, fixed at build time per build type and never changed at runtime. A
 * feature that is off has no entry point and cannot be navigated to. Composables take callbacks
 * instead of reading these, so they can be tested with a feature either way.
 */
object FeatureFlags {
    /** The level designer: on in debug builds, off in release builds. */
    val levelDesigner: Boolean = BuildConfig.LEVEL_DESIGNER
}
