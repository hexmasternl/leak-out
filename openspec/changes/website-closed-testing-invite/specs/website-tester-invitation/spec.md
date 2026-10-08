# Spec Delta

## Purpose

Invites visitors of the public website to join the closed test of Leak Out on Google Play and explains how to become a tester.

## ADDED Requirements

### Requirement: Tester invitation dialog content
The website SHALL offer a tester invitation dialog that explains the game is in closed testing, links to the leak-out Google Group, links to the Google Play testing opt-in page for `nl.hexmaster.leakout`, links to the game's Google Play Store listing, and states that it can take up to 15 minutes before the app becomes available after becoming a tester.

#### Scenario: Dialog shows how to become a tester
- **WHEN** the tester invitation dialog is open
- **THEN** it shows the explanation, a link to `https://groups.google.com/g/leak-out`, a link to `https://play.google.com/apps/testing/nl.hexmaster.leakout`, a link to the Play Store listing and the note about the 15-minute delay

#### Scenario: Dialog in the page language
- **WHEN** a visitor opens the dialog on a page in English, Dutch, French, German or Spanish
- **THEN** the dialog's texts are in that page's language

### Requirement: Test the app menu item
The main navigation on every page SHALL contain a "Test the app" item that opens the tester invitation dialog.

#### Scenario: Open from the menu
- **WHEN** a visitor activates "Test the app" in the navigation
- **THEN** the tester invitation dialog opens

### Requirement: Become a tester section on the home page
The home page SHALL contain a section that explains how to become a tester, with a button that opens the tester invitation dialog.

#### Scenario: Open from the home page
- **WHEN** a visitor activates the button in the home page's tester section
- **THEN** the tester invitation dialog opens

### Requirement: Automatic opening
The tester invitation dialog SHALL open automatically when a visitor has stayed on a page for more than 15 seconds, unless the visitor has dismissed the dialog before in the same browser.

#### Scenario: First visit
- **WHEN** a visitor who never dismissed the dialog stays on a page for more than 15 seconds
- **THEN** the tester invitation dialog opens

#### Scenario: Leaving early
- **WHEN** a visitor leaves a page within 15 seconds
- **THEN** the dialog does not open on that page

#### Scenario: Already open
- **WHEN** the visitor opened the dialog themselves before the 15 seconds passed
- **THEN** it is not opened a second time

### Requirement: Dismissal is remembered
When a visitor closes the tester invitation dialog, the website SHALL remember this in the visitor's browser and SHALL NOT open the dialog automatically again; the menu item and the home page button SHALL still open it.

#### Scenario: Dismissed before
- **WHEN** a visitor who dismissed the dialog stays on any page for more than 15 seconds
- **THEN** the dialog does not open automatically

#### Scenario: Reopen after dismissal
- **WHEN** a visitor who dismissed the dialog activates "Test the app" or the home page button
- **THEN** the dialog opens

#### Scenario: Storage unavailable
- **WHEN** the browser does not allow the website to store the dismissal
- **THEN** the dialog still opens and closes normally, and the menu item and button still work

### Requirement: Accessible dialog
The tester invitation dialog SHALL be a modal dialog that can be closed with a visible close button and with the Escape key, moves keyboard focus into the dialog when it opens and returns focus to where it was when it closes.

#### Scenario: Close with Escape
- **WHEN** the dialog is open and the visitor presses Escape
- **THEN** the dialog closes and counts as dismissed

### Requirement: Works without JavaScript
Without JavaScript, the menu item and the home page button SHALL still lead to the testing information, and the website SHALL NOT open any dialog automatically.

#### Scenario: JavaScript disabled
- **WHEN** a visitor with JavaScript disabled activates "Test the app"
- **THEN** they reach the home page's tester section, which shows the Google Group and testing links
