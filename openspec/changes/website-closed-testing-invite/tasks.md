# Tasks

## 1. Content and configuration

- [x] 1.1 Add `testerGroupUrl` (`https://groups.google.com/g/leak-out`) and `testerOptInUrl` (`https://play.google.com/apps/testing/nl.hexmaster.leakout`) to `[params]` in `website/src/hugo.toml`, and verify `hugo --source website/src --panicOnWarning` still builds
- [x] 1.2 Add the texts (menu item "Test the app", dialog title, explanation, the two steps, Play Store link text, 15-minute note, close label, home section heading, text and button) to all five `website/src/i18n/*.yaml` files, and verify the build with `--panicOnWarning` reports no missing translation

## 2. Dialog, menu item and home section

- [ ] 2.1 Create `layouts/_partials/tester-invite.html` with a native `<dialog>` holding the content from the spec, and include it once in `baseof.html`; verify in the built HTML that the pages of every language contain the dialog with the three links
- [ ] 2.2 Add the "Test the app" item to the navigation in `header.html` as a link to `#become-a-tester` on the home page of the current language; verify it appears on every page of the built site
- [ ] 2.3 Add the "Become a tester" section with `id="become-a-tester"` to `home.html`, showing the group and opt-in links and a button-styled link to the same anchor; verify the section in the built home page of each language
- [ ] 2.4 Style the dialog, its backdrop and the section in `assets/css/main.css` following `docs/design-system.md`, and verify at phone width (360 px) in `hugo server` that the dialog fits without horizontal scrolling

## 3. Behaviour script

- [ ] 3.1 Write `assets/js/tester-invite.js`: open the dialog from the menu item and button (preventing navigation), auto-open after 15 seconds unless dismissed or already open, record a dismissal in `localStorage` on any close, with all storage access in `try/catch`; load it from `baseof.html` and verify the dialog opens from both triggers in `hugo server`
- [ ] 3.2 Add `website/tests/tester-invite.test.js` covering auto-open when not dismissed, no auto-open after dismissal, no second open when already open, dismissal recorded on close, and behaviour when storage throws; verify `cd website && npm test` passes
- [ ] 3.3 Check in `hugo server` with JavaScript disabled that the menu item leads to the home page section and no dialog opens, and with JavaScript enabled that Escape closes the dialog and focus returns to the menu item

## 4. Documentation

- [ ] 4.1 Describe the tester invitation, its stored dismissal key and that it must be removed when closed testing ends in `website/README.md`, and verify the README's documented commands still run
