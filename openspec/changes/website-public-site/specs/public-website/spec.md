# Spec Delta

## Purpose

The public website that promotes Leak Out and gives players its rules, answers to common questions, the leaderboard and a way to report problems and request features, in five languages.

## ADDED Requirements

### Requirement: Website pages
The website SHALL have a home page, a rules page, an FAQ page and a leaderboard page, reachable from a navigation that is shown on every page.

#### Scenario: Navigation on every page
- **WHEN** a visitor opens any page of the website
- **THEN** the navigation links to the home, rules, FAQ and leaderboard pages

### Requirement: Home page promotes the game
The home page SHALL introduce Leak Out with the game's name, a short tagline, the feature graphic or game artwork, at least two screenshots, the main selling points, and a link to the game's Google Play Store listing.

#### Scenario: Play Store link
- **WHEN** a visitor activates the Get it on Google Play link on the home page
- **THEN** the game's Google Play Store listing opens

### Requirement: Rules page explains how to play
The rules page SHALL explain the goal of the game and how to play it, using the game's terms: the playing field, the stack, pipe sections, the start and finish pipes, the flowing liquid that leaks out, points and stars.

#### Scenario: Rules content
- **WHEN** a visitor opens the rules page
- **THEN** it explains that the player connects the start pipe to the finish pipe with sections from the stack before the liquid leaks out

### Requirement: FAQ page
The FAQ page SHALL list frequently asked questions, each with its answer.

#### Scenario: FAQ entries
- **WHEN** a visitor opens the FAQ page
- **THEN** it shows a list of questions, each with its answer

### Requirement: Five languages
Every page SHALL be available in English, Dutch, French, German and Spanish, with English as the default language. Every page SHALL offer a language switcher that opens the same page in another language.

#### Scenario: Switch language
- **WHEN** a visitor on the English rules page chooses Deutsch in the language switcher
- **THEN** the German rules page opens

#### Scenario: Default language
- **WHEN** a visitor opens the website's root address
- **THEN** the English home page is shown

### Requirement: Report a problem and request a feature
Every page SHALL offer a "Report a problem" and a "Request a feature" link. They SHALL open a new issue in the public `hexmasternl/leak-out-game` GitHub repository, as a bug and as a feature request respectively.

#### Scenario: Report a problem
- **WHEN** a visitor activates "Report a problem"
- **THEN** GitHub opens a new bug issue in `hexmasternl/leak-out-game`

#### Scenario: Request a feature
- **WHEN** a visitor activates "Request a feature"
- **THEN** GitHub opens a new feature request issue in `hexmasternl/leak-out-game`

### Requirement: Design system look and feel
The website SHALL use the colours, typography and artwork of the Leak Out design system, and SHALL be usable on phone, tablet and desktop widths without horizontal scrolling.

#### Scenario: Phone width
- **WHEN** a visitor opens any page in a 360 px wide browser window
- **THEN** the content fits the width and the page does not scroll horizontally

### Requirement: Self-contained assets
Every image, font and other asset the website uses SHALL be stored inside the `website/` folder; the website SHALL NOT reference files elsewhere in the repository at build or run time.

#### Scenario: Build from the website folder only
- **WHEN** the website is built from a copy that contains only the `website/` folder
- **THEN** the build succeeds and no page has a broken image

### Requirement: Accessibility
Every page SHALL have a page title and a language attribute matching its language, every image that carries meaning SHALL have a text alternative, and text SHALL meet a contrast ratio of at least 4.5:1 against its background.

#### Scenario: Automated accessibility check
- **WHEN** an automated accessibility check runs over the built website
- **THEN** it reports no missing image alternatives, page titles, language attributes or contrast failures
