# Proposal

## Why

Leak Out is in closed testing on Google Play: only people who join the [leak-out Google Group](https://groups.google.com/g/leak-out) and opt in as testers can install it. The public website does not tell visitors this, so a visitor who follows the Play Store link finds nothing to install and leaves. The website should invite visitors to become a tester and explain how. Tracked in [#125](https://github.com/hexmasternl/leak-out/issues/125).

## What Changes

- A **tester invitation dialog** on the website: it explains the closed test, links to the Google Group and to the Play testing opt-in page (`https://play.google.com/apps/testing/nl.hexmaster.leakout`), links to the Play Store listing, and says it can take up to 15 minutes before the app becomes available after opting in.
- A **Test the app** item in the main navigation on every page, which opens the dialog.
- A **Become a tester** section on the home page with a button that opens the dialog.
- The dialog **opens automatically** once a visitor has been on a page for more than 15 seconds, unless the visitor has dismissed it before; after a dismissal it only opens from the menu item or the button.
- All new texts in the five website languages.

## Capabilities

### New Capabilities
- `website-tester-invitation`: inviting website visitors to the closed test: the dialog's content, the ways to open it, the automatic opening and remembering a dismissal.

### Modified Capabilities
- (none)

## Impact

- `website/src/`: header navigation, home layout, a new dialog partial, a small script, styles, the five `i18n/*.yaml` files and the site parameters (Google Group and testing URLs).
- `website/tests/`: unit tests for the script's open/dismiss logic.
- `website/README.md`: mention the dialog and its stored dismissal.
- No change to the Android app or the server.
