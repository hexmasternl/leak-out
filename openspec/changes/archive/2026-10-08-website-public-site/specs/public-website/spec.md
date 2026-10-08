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

### Requirement: Search engine metadata
Every page SHALL have a unique title and meta description in its language, a canonical URL, alternate links to every translation plus an `x-default` link to the English page, and absolute URLs based on the site's public address. The website SHALL publish a sitemap of every page and translation and a `robots.txt` that points to it. The home page SHALL describe the game, and the FAQ page its questions and answers, as schema.org structured data. A missing page SHALL answer with status 404 and a page that is not indexed.

#### Scenario: Translated page metadata
- **WHEN** a search engine reads the Dutch rules page
- **THEN** it finds a Dutch title and description, a canonical link to the Dutch rules page, and alternate links to the rules page in all five languages and `x-default`

#### Scenario: Missing page
- **WHEN** a visitor opens an address that does not exist
- **THEN** the response is 404 Not Found with a page marked `noindex` that links to every language's home page

### Requirement: Social sharing metadata
Every page SHALL have Open Graph metadata (site name, title, description, type, URL, locale and alternate locales, and an image with its type, size and text alternative) and Twitter card metadata with a large image, so a shared link shows the game's artwork with the page's own title and description.

#### Scenario: Share a page
- **WHEN** a link to the German FAQ page is shared on a social network
- **THEN** the preview shows the German title and description and the Leak Out feature graphic

### Requirement: Performance and security best practices
The website SHALL serve responsive, compressed images with their dimensions set, SHALL let browsers cache fingerprinted styles, scripts and images for a long time, and SHALL send security headers including a Content Security Policy that allows connections only to the website and the score server. An automated audit of the built site SHALL score at least 90 for performance and 100 for accessibility, best practices and SEO on mobile.

#### Scenario: Audit
- **WHEN** Lighthouse audits the English and translated pages with the mobile profile
- **THEN** performance is at least 90 and accessibility, best practices and SEO are 100
