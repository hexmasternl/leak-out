# Design

## Context

The website (`website/src/`, Hugo) is static and works without JavaScript except for the leaderboard (`assets/js/leaderboard.js`, unit tested with Node's built-in test runner in `website/tests/`). The navigation is built in `layouts/_partials/header.html` from a fixed list of pages; texts live in `i18n/<lang>.yaml`; external URLs in `[params]` of `hugo.toml`. The footer promises "No ads, no accounts, no tracking."

## Goals / Non-Goals

**Goals:**
- One dialog, rendered once per page by a partial, opened from the menu item, the home page button and the 15-second timer.
- Keep the site usable without JavaScript.

**Non-Goals:**
- Counting or tracking who opens or dismisses the dialog.
- Changing the Play Store link elsewhere on the site.
- Hiding the invitation once the game leaves closed testing (a later change removes or changes it).

## Decisions

- **Native `<dialog>` with `showModal()`** for the modal: gives focus handling, Escape and a backdrop for free, and is supported by all current browsers. Alternative: a hand-made overlay, which needs its own focus trap; rejected as more code to get wrong.
- **Menu item and button are links to the home page section** (`#become-a-tester` on the home page of the current language) that the script intercepts to open the dialog. Without JavaScript the link simply navigates to the section, which repeats the essential information. Alternative: `<button>`s, which do nothing without JavaScript.
- **Dismissal is stored in `localStorage`** under one key (`leakout.testerInvite.dismissed`). It stays in the visitor's browser and is never sent anywhere, so it fits the "no tracking" promise. Reads and writes are wrapped in `try/catch`; when storage fails the dialog behaves as not dismissed. Any close (close button, Escape, backdrop click) counts as a dismissal. A cookie was rejected: it is sent to the server for no reason.
- **The 15-second timer runs per page view**, is started only when not dismissed, and is cancelled when the dialog was opened manually. Leaving the page ends the timer naturally.
- **Logic in a small module** (`assets/js/tester-invite.js`) whose decisions take the storage and timer as parameters, so `website/tests/` can unit test it without a browser, the same way as the leaderboard script.
- **URLs in `hugo.toml` params** (`testerGroupUrl`, `testerOptInUrl`) next to `playStoreUrl`, so they can be changed in one place.

## Risks / Trade-offs

- [An automatic popup annoys visitors] → It waits 15 seconds, appears only until the first dismissal, and is easy to close.
- [Visitors in private mode see it on every visit] → Accepted; storage lasts only for the private session there.
- [The invitation stays after the game is public] → Out of scope; noted in the README so it is removed when closed testing ends.

## Open Questions

- Should the dialog present joining the Google Group and opting in on Google Play as two numbered steps (group first)? The design assumes it does, since the opt-in only works for group members.
- Should automatic opening be skipped on the leaderboard page, where visitors may stay long to read scores? The design opens it on every page, as the issue asks.
